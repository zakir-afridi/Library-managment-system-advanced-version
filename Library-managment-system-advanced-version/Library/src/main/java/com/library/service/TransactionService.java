package com.library.service;

import com.library.cache.DashboardCache;
import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.exception.BusinessRuleException;
import com.library.exception.DatabaseException;
import com.library.model.BookCopy;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.repository.BookCopyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Service layer for Issue / Return / Renewal transactions.
 * Enforces ACID transaction boundaries, race-condition safety on physical copies, and audit logging.
 */
public class TransactionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionService.class);

    private final BookService        bookService        = new BookService();
    private final MemberService      memberService      = new MemberService();
    private final BookCopyRepository copyRepository     = new BookCopyRepository();
    private final AuditService       auditService       = AuditService.getInstance();

    // Stack for recent activity in memory
    private final Deque<Transaction> recentActivity = new ArrayDeque<>(50);

    // ── Issue Book ────────────────────────────────────────────────────────────

    /**
     * Atomically issues a book to a member.
     * Selects an available physical copy, transitions its status, inserts the transaction,
     * updates book counts, and writes an audit log within a single database transaction.
     * Returns empty string on success or user-friendly error message on failure.
     */
    public String issueBook(int bookId, int memberId, String issuedBy) {
        // 1. Validate member rules
        Member member = memberService.getMemberById(memberId);
        if (member == null) return "Member not found.";
        if (!"Active".equalsIgnoreCase(member.getStatus())) return "Member account is " + member.getStatus() + ".";
        if (member.getFineBalance() > 0.0) {
            return "Member has outstanding fine of " + AppConfig.getInstance().getCurrency() + " " +
                    String.format("%.2f", member.getFineBalance()) + ". Please clear fine before issuing.";
        }

        int activeCount = memberService.getActiveBookCount(memberId);
        if (activeCount >= member.getBookLimit()) {
            return "Member has reached maximum book limit (" + member.getBookLimit() + ").";
        }

        int loanDays = AppConfig.getInstance().getLoanDays();
        LocalDate dueDate = LocalDate.now().plusDays(loanDays > 0 ? loanDays : 14);

        // 2. Perform atomic issue in a single DB transaction
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Find an available physical copy
                String findCopySql = "SELECT copy_id, copy_code FROM book_copies WHERE book_id = ? AND status = 'Available' LIMIT 1";
                int copyId = 0;
                String copyCode = "";

                try (PreparedStatement fps = conn.prepareStatement(findCopySql)) {
                    fps.setInt(1, bookId);
                    try (ResultSet rs = fps.executeQuery()) {
                        if (rs.next()) {
                            copyId = rs.getInt("copy_id");
                            copyCode = rs.getString("copy_code");
                        }
                    }
                }

                if (copyId == 0) {
                    conn.rollback();
                    return "No physical copies are currently available for this book.";
                }

                // Lock and update the physical copy atomically (concurrency guard)
                String updateCopySql = "UPDATE book_copies SET status = 'Issued' WHERE copy_id = ? AND status = 'Available'";
                try (PreparedStatement ups = conn.prepareStatement(updateCopySql)) {
                    ups.setInt(1, copyId);
                    int updated = ups.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        return "This physical copy was just issued by another operator. Please try again.";
                    }
                }

                // Decrement available_qty on books title record
                String updateBookSql = "UPDATE books SET available_qty = MAX(0, available_qty - 1) WHERE book_id = ?";
                try (PreparedStatement bps = conn.prepareStatement(updateBookSql)) {
                    bps.setInt(1, bookId);
                    bps.executeUpdate();
                }

                // Insert circulation transaction record
                String insertTxSql = """
                    INSERT INTO transactions (book_id, copy_id, member_id, issue_date, due_date, status, issued_by, handled_by)
                    VALUES (?, ?, ?, ?, ?, 'Issued', ?, ?)
                """;
                int txId = 0;
                try (PreparedStatement ips = conn.prepareStatement(insertTxSql, Statement.RETURN_GENERATED_KEYS)) {
                    ips.setInt(1, bookId);
                    ips.setInt(2, copyId);
                    ips.setInt(3, memberId);
                    ips.setString(4, LocalDate.now().toString());
                    ips.setString(5, dueDate.toString());
                    ips.setString(6, issuedBy);
                    ips.setString(7, issuedBy);
                    ips.executeUpdate();
                    try (ResultSet rs = ips.getGeneratedKeys()) {
                        if (rs.next()) txId = rs.getInt(1);
                    }
                }

                // Commit the entire atomic transaction
                conn.commit();

                // Audit logging & cache invalidation
                auditService.log("BOOK_ISSUED", "TRANSACTION", String.valueOf(txId),
                        "Book #" + bookId + " (Copy " + copyCode + ") issued to Member #" + memberId + " by " + issuedBy);
                DashboardCache.getInstance().invalidate();
                return "";

            } catch (Exception e) {
                conn.rollback();
                LOG.error("Failed to issue book atomically: {}", e.getMessage(), e);
                return "Transaction failed: " + e.getMessage();
            }
        } catch (SQLException e) {
            LOG.error("Database connection error on issueBook: {}", e.getMessage(), e);
            return "Database error: " + e.getMessage();
        }
    }

    // ── Return Book ───────────────────────────────────────────────────────────

    /**
     * Atomically returns a book transaction.
     * Updates return date, condition, calculates fines using exact BigDecimal arithmetic,
     * updates the physical copy status back to 'Available', updates book count, and logs the action.
     */
    public double returnBook(int transactionId, String condition, String returnedBy) {
        Transaction tx = getTransactionById(transactionId);
        if (tx == null) return -1.0;
        if (!Transaction.STATUS_ISSUED.equalsIgnoreCase(tx.getStatus()) && !Transaction.STATUS_OVERDUE.equalsIgnoreCase(tx.getStatus())) {
            LOG.warn("Transaction #{} is already returned ({})", transactionId, tx.getStatus());
            return -1.0;
        }

        double fineAmount = tx.calculateFine();
        BigDecimal fineBd = BigDecimal.valueOf(fineAmount).setScale(2, RoundingMode.HALF_UP);
        LocalDate today = LocalDate.now();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update transaction record
                String updateTxSql = """
                    UPDATE transactions
                    SET return_date = ?, fine_amount = ?, status = 'Returned', return_condition = ?, handled_by = ?
                    WHERE transaction_id = ?
                """;
                try (PreparedStatement ps = conn.prepareStatement(updateTxSql)) {
                    ps.setString(1, today.toString());
                    ps.setDouble(2, fineBd.doubleValue());
                    ps.setString(3, condition != null ? condition : "Good");
                    ps.setString(4, returnedBy);
                    ps.setInt(5, transactionId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return -1.0;
                    }
                }

                // Restore physical book copy status
                if (tx.getCopyId() > 0) {
                    String updateCopySql = "UPDATE book_copies SET status = 'Available', condition = ? WHERE copy_id = ?";
                    try (PreparedStatement cps = conn.prepareStatement(updateCopySql)) {
                        cps.setString(1, condition != null ? condition : "Good");
                        cps.setInt(2, tx.getCopyId());
                        cps.executeUpdate();
                    }
                }

                // Increment available_qty on books
                String updateBookSql = "UPDATE books SET available_qty = MIN(quantity, available_qty + 1) WHERE book_id = ?";
                try (PreparedStatement bps = conn.prepareStatement(updateBookSql)) {
                    bps.setInt(1, tx.getBookId());
                    bps.executeUpdate();
                }

                // Add fine to member if applicable
                if (fineBd.compareTo(BigDecimal.ZERO) > 0) {
                    memberService.addFine(tx.getMemberId(), fineBd.doubleValue());
                }

                conn.commit();

                // Audit logging
                auditService.log("BOOK_RETURNED", "TRANSACTION", String.valueOf(transactionId),
                        "Book #" + tx.getBookId() + " returned by Member #" + tx.getMemberId() +
                        (fineBd.compareTo(BigDecimal.ZERO) > 0 ? " | Fine: " + fineBd : "") + " (Handled by: " + returnedBy + ")");
                DashboardCache.getInstance().invalidate();
                return fineBd.doubleValue();

            } catch (Exception e) {
                conn.rollback();
                LOG.error("Failed to return book atomically: {}", e.getMessage(), e);
                return -1.0;
            }
        } catch (SQLException e) {
            LOG.error("Database connection error on returnBook: {}", e.getMessage(), e);
            return -1.0;
        }
    }

    // ── Renew Book ────────────────────────────────────────────────────────────

    public String renewBook(int transactionId, int extensionDays, String renewedBy) {
        Transaction tx = getTransactionById(transactionId);
        if (tx == null) return "Transaction not found.";
        if (!Transaction.STATUS_ISSUED.equalsIgnoreCase(tx.getStatus())) {
            return "Cannot renew a book that is " + tx.getStatus() + ".";
        }

        LocalDate currentDue = tx.getDueDate() != null ? tx.getDueDate() : LocalDate.now();
        LocalDate baseDate = currentDue.isBefore(LocalDate.now()) ? LocalDate.now() : currentDue;
        LocalDate newDue = baseDate.plusDays(extensionDays > 0 ? extensionDays : 14);

        String sql = "UPDATE transactions SET due_date = ?, status = 'Issued', handled_by = ? WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newDue.toString());
            ps.setString(2, renewedBy);
            ps.setInt(3, transactionId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                tx.setDueDate(newDue);
                pushActivity(tx);
                DashboardCache.getInstance().invalidate();
                auditService.log("BOOK_RENEWED", "TRANSACTION", String.valueOf(transactionId),
                        "Transaction #" + transactionId + " renewed until " + newDue + " by " + renewedBy);
                return "";
            }
        } catch (SQLException e) {
            LOG.error("Error renewing transaction #{}: {}", transactionId, e.getMessage(), e);
            return "Database error renewing loan: " + e.getMessage();
        }
        return "Failed to renew loan.";
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Transaction getTransactionById(int id) {
        String sql = """
            SELECT t.*, b.book_name, b.author, m.name as member_name, m.student_id, bc.copy_code
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            LEFT JOIN book_copies bc ON t.copy_id = bc.copy_id
            WHERE t.transaction_id = ?
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapTransaction(rs);
            }
        } catch (SQLException e) {
            LOG.error("Error fetching transaction #{}: {}", id, e.getMessage());
        }
        return null;
    }

    public List<Transaction> getActiveTransactions() {
        String sql = """
            SELECT t.*, b.book_name, b.author, m.name as member_name, m.student_id, bc.copy_code
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            LEFT JOIN book_copies bc ON t.copy_id = bc.copy_id
            WHERE t.status = 'Issued'
            ORDER BY t.due_date ASC
        """;
        return queryTransactions(sql);
    }

    public List<Transaction> getOverdueTransactions() {
        String sql = """
            SELECT t.*, b.book_name, b.author, m.name as member_name, m.student_id, bc.copy_code
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            LEFT JOIN book_copies bc ON t.copy_id = bc.copy_id
            WHERE t.status = 'Issued' AND date(t.due_date) < date('now')
            ORDER BY t.due_date ASC
        """;
        return queryTransactions(sql);
    }

    public List<Transaction> getMemberTransactions(int memberId) {
        String sql = """
            SELECT t.*, b.book_name, b.author, m.name as member_name, m.student_id, bc.copy_code
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            LEFT JOIN book_copies bc ON t.copy_id = bc.copy_id
            WHERE t.member_id = ?
            ORDER BY t.transaction_id DESC
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching member transactions: {}", e.getMessage());
        }
        return list;
    }

    public List<Transaction> getTransactionsByDateRange(LocalDate from, LocalDate to) {
        String sql = """
            SELECT t.*, b.book_name, b.author, m.name as member_name, m.student_id, bc.copy_code
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            LEFT JOIN book_copies bc ON t.copy_id = bc.copy_id
            WHERE date(t.issue_date) >= date(?) AND date(t.issue_date) <= date(?)
            ORDER BY t.issue_date DESC
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching date range transactions: {}", e.getMessage());
        }
        return list;
    }

    public List<Transaction> getOverdueSortedByDays() {
        List<Transaction> overdues = getOverdueTransactions();
        PriorityQueue<Transaction> pq = new PriorityQueue<>(
                (a, b) -> Long.compare(b.getDaysOverdue(), a.getDaysOverdue()));
        pq.addAll(overdues);

        List<Transaction> sorted = new ArrayList<>();
        while (!pq.isEmpty()) sorted.add(pq.poll());
        return sorted;
    }

    public List<Transaction> getRecentActivity() {
        return new ArrayList<>(recentActivity);
    }

    public List<Transaction> getRecentActivity(int limit) {
        return getRecentTransactions(limit);
    }

    public List<Transaction> getRecentTransactions(int limit) {
        String sql = """
            SELECT t.*, b.book_name, b.author, m.name as member_name, m.student_id, bc.copy_code
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            LEFT JOIN book_copies bc ON t.copy_id = bc.copy_id
            ORDER BY t.transaction_id DESC
            LIMIT ?
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching recent transactions: {}", e.getMessage());
        }
        return list;
    }

    public Map<String, Integer> getMonthlyIssuedStats(int months) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
            SELECT strftime('%Y-%m', issue_date) as month, COUNT(*) as cnt
            FROM transactions
            WHERE issue_date >= date('now', '-' || ? || ' months')
            GROUP BY strftime('%Y-%m', issue_date)
            ORDER BY month ASC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("month"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error getting monthly stats: {}", e.getMessage());
        }
        return result;
    }

    public Map<String, int[]> getDailyStats(int days) {
        Map<String, int[]> result = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            result.put(d.toString(), new int[]{0, 0});
        }
        String sqlIssued = "SELECT issue_date, COUNT(*) as cnt FROM transactions WHERE issue_date >= date('now', '-' || ? || ' days') GROUP BY issue_date";
        String sqlReturned = "SELECT return_date, COUNT(*) as cnt FROM transactions WHERE return_date IS NOT NULL AND return_date >= date('now', '-' || ? || ' days') GROUP BY return_date";
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlIssued)) {
                ps.setInt(1, days);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String dt = rs.getString("issue_date");
                        if (result.containsKey(dt)) result.get(dt)[0] = rs.getInt("cnt");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlReturned)) {
                ps.setInt(1, days);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String dt = rs.getString("return_date");
                        if (result.containsKey(dt)) result.get(dt)[1] = rs.getInt("cnt");
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error("Error getting daily stats: {}", e.getMessage());
        }
        return result;
    }

    private void pushActivity(Transaction tx) {
        if (recentActivity.size() >= 50) recentActivity.removeLast();
        recentActivity.push(tx);
    }

    private List<Transaction> queryTransactions(String sql) {
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapTransaction(rs));
        } catch (SQLException e) {
            LOG.error("Error querying transactions: {}", e.getMessage());
        }
        return list;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("transaction_id"));
        t.setBookId(rs.getInt("book_id"));
        t.setMemberId(rs.getInt("member_id"));
        try { t.setCopyId(rs.getInt("copy_id")); } catch (SQLException ignored) {}
        try { t.setCopyCode(rs.getString("copy_code")); } catch (SQLException ignored) {}
        t.setBookName(rs.getString("book_name"));
        t.setAuthor(rs.getString("author"));
        t.setMemberName(rs.getString("member_name"));
        t.setStudentId(rs.getString("student_id"));

        String issueStr = rs.getString("issue_date");
        if (issueStr != null) {
            try { t.setIssueDate(LocalDate.parse(issueStr)); } catch (Exception ignored) {}
        }
        String dueStr = rs.getString("due_date");
        if (dueStr != null) {
            try { t.setDueDate(LocalDate.parse(dueStr)); } catch (Exception ignored) {}
        }
        String retStr = rs.getString("return_date");
        if (retStr != null) {
            try { t.setReturnDate(LocalDate.parse(retStr)); } catch (Exception ignored) {}
        }

        t.setFineAmount(rs.getDouble("fine_amount"));
        t.setFinePaid(rs.getInt("fine_paid") == 1);
        t.setStatus(rs.getString("status"));
        t.setReturnCondition(rs.getString("return_condition"));
        t.setIssuedBy(rs.getString("issued_by"));
        try { t.setHandledBy(rs.getString("handled_by")); } catch (SQLException ignored) {}
        return t;
    }
}
