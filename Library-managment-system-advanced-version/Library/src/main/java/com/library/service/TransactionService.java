package com.library.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.library.cache.DashboardCache;
import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.model.Transaction;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Service layer for Issue / Return transactions.
 *
 * Data structures used:
 *  - PriorityQueue<Transaction>  â€” overdue books sorted by days overdue (max-heap)
 *  - Stack<Transaction>          â€” recent activity log (undo capability)
 *  - HashMap<Integer,List<Transaction>> â€” active borrowings per member (O(1) lookup)
 */
public class TransactionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionService.class);

    private final BookService    bookService;
    private final MemberService  memberService;

    // Stack for recent activity (last 50 transactions)
    private final Deque<Transaction> recentActivity = new ArrayDeque<>(50);

    // HashMap: memberId -> list of active transactions
    private final Map<Integer, List<Transaction>> activeBorrowings = new HashMap<>();

    public TransactionService() {
        this.bookService   = new BookService();
        this.memberService = new MemberService();
    }

    // â”€â”€ Issue Book â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Issues a book to a member.
     * Validates: book availability, member status, fine balance, book limit.
     * Returns a result message â€” empty string means success.
     */
    public String issueBook(int bookId, int memberId, String issuedBy) {
        // Validate member
        var member = memberService.getMemberById(memberId);
        if (member == null)                          return "Member not found.";
        if (!"Active".equals(member.getStatus()))    return "Member account is " + member.getStatus() + ".";
        if (member.getFineBalance() > 0)             return "Member has outstanding fine of " +
                AppConfig.getInstance().getCurrency() + " " +
                String.format("%.2f", member.getFineBalance()) + ". Please clear before issuing.";

        int activeCount = memberService.getActiveBookCount(memberId);
        if (activeCount >= member.getBookLimit())    return "Member has reached book limit (" + member.getBookLimit() + ").";

        // Validate book
        var book = bookService.getBookById(bookId);
        if (book == null)                            return "Book not found.";
        if (book.getAvailableQty() <= 0)             return "Book is not available (all copies issued).";

        // Calculate due date
        int loanDays = AppConfig.getInstance().getLoanDays();
        LocalDate dueDate = LocalDate.now().plusDays(loanDays);

        String sql = """
            INSERT INTO transactions (book_id, member_id, issue_date, due_date, status, issued_by)
            VALUES (?, ?, ?, ?, 'Issued', ?)
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            ps.setString(3, LocalDate.now().toString());
            ps.setString(4, dueDate.toString());
            ps.setString(5, issuedBy);

            if (ps.executeUpdate() == 0) return "Failed to create transaction.";

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Transaction tx = new Transaction();
                    tx.setTransactionId(keys.getInt(1));
                    tx.setBookId(bookId);
                    tx.setMemberId(memberId);
                    tx.setBookName(book.getBookName());
                    tx.setMemberName(member.getName());
                    tx.setStudentId(member.getStudentId());
                    tx.setIssueDate(LocalDate.now());
                    tx.setDueDate(dueDate);
                    tx.setStatus(Transaction.STATUS_ISSUED);
                    tx.setIssuedBy(issuedBy);

                    pushActivity(tx);
                    updateActiveBorrowingsCache(memberId, tx, true);
                }
            }

            bookService.decrementAvailability(bookId);
            logActivity(issuedBy, "ISSUE",
                    "Book ID " + bookId + " issued to Member ID " + memberId);
            DashboardCache.getInstance().invalidate();
            return "";

        } catch (SQLException e) {
            LOG.error("Error issuing book: " + e.getMessage());
            return "Database error: " + e.getMessage();
        }
    }

    // â”€â”€ Return Book â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns a book. Calculates fine automatically.
     * Returns the fine amount (0 if no fine).
     */
    public double returnBook(int transactionId, String condition, String returnedBy) {
        Transaction tx = getTransactionById(transactionId);
        if (tx == null) return -1;

        double fine = tx.calculateFine();
        LocalDate today = LocalDate.now();

        String sql = """
            UPDATE transactions
            SET return_date=?, fine_amount=?, status=?, return_condition=?
            WHERE transaction_id=?
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, today.toString());
            ps.setDouble(2, fine);
            ps.setString(3, Transaction.STATUS_RETURNED);
            ps.setString(4, condition);
            ps.setInt(5, transactionId);

            if (ps.executeUpdate() == 0) return -1;

            // Add fine to member balance if applicable
            if (fine > 0) memberService.addFine(tx.getMemberId(), fine);

            bookService.incrementAvailability(tx.getBookId());

            tx.setReturnDate(today);
            tx.setFineAmount(fine);
            tx.setStatus(Transaction.STATUS_RETURNED);
            tx.setReturnCondition(condition);
            pushActivity(tx);
            updateActiveBorrowingsCache(tx.getMemberId(), tx, false);

            logActivity(returnedBy, "RETURN",
                    "Book ID " + tx.getBookId() + " returned by Member ID " + tx.getMemberId() +
                    (fine > 0 ? " | Fine: " + fine : ""));
            DashboardCache.getInstance().invalidate();
            return fine;

        } catch (SQLException e) {
            LOG.error("Error returning book: " + e.getMessage());
            return -1;
        }
    }

    // â”€â”€ Queries â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Transaction getTransactionById(int id) {
        String sql = """
            SELECT t.*, b.book_name, m.name as member_name, m.student_id
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            WHERE t.transaction_id=?
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapTransaction(rs);
            }
        } catch (SQLException e) {
            LOG.error("Error fetching transaction: " + e.getMessage());
        }
        return null;
    }

    public List<Transaction> getActiveTransactions() {
        String sql = """
            SELECT t.*, b.book_name, m.name as member_name, m.student_id
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            WHERE t.status='Issued'
            ORDER BY t.due_date ASC
        """;
        return queryTransactions(sql);
    }

    public List<Transaction> getOverdueTransactions() {
        String sql = """
            SELECT t.*, b.book_name, m.name as member_name, m.student_id
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            WHERE t.status='Issued' AND t.due_date < date('now')
            ORDER BY t.due_date ASC
        """;
        return queryTransactions(sql);
    }

    public List<Transaction> getMemberTransactions(int memberId) {
        String sql = """
            SELECT t.*, b.book_name, m.name as member_name, m.student_id
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            WHERE t.member_id=?
            ORDER BY t.issue_date DESC
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching member transactions: " + e.getMessage());
        }
        return list;
    }

    public List<Transaction> getTransactionsByDateRange(LocalDate from, LocalDate to) {
        String sql = """
            SELECT t.*, b.book_name, m.name as member_name, m.student_id
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.std_id
            WHERE t.issue_date BETWEEN ? AND ?
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
            LOG.error("Error fetching transactions by date: " + e.getMessage());
        }
        return list;
    }

    // â”€â”€ PriorityQueue: Overdue sorted by days overdue (max-heap) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns overdue transactions sorted by days overdue descending
     * using a PriorityQueue (max-heap).
     */
    public List<Transaction> getOverdueSortedByDays() {
        List<Transaction> overdue = getOverdueTransactions();
        PriorityQueue<Transaction> pq = new PriorityQueue<>(
                Comparator.comparingLong(Transaction::getDaysOverdue).reversed());
        pq.addAll(overdue);

        List<Transaction> sorted = new ArrayList<>();
        while (!pq.isEmpty()) sorted.add(pq.poll());
        return sorted;
    }

    // â”€â”€ Stack: Recent activity log â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Returns last N transactions from the in-memory activity stack. */
    public List<Transaction> getRecentActivity(int limit) {
        List<Transaction> list = new ArrayList<>();
        int count = 0;
        for (Transaction tx : recentActivity) {
            if (count++ >= limit) break;
            list.add(tx);
        }
        return list;
    }

    /** Undo the last transaction (pop from stack). */
    public Transaction undoLastTransaction() {
        return recentActivity.isEmpty() ? null : recentActivity.pop();
    }

    // â”€â”€ Monthly stats for charts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Returns issued count per month for the last N months. */
    public Map<String, Integer> getMonthlyIssuedStats(int months) {
        String sql = """
            SELECT strftime('%Y-%m', issue_date) as month, COUNT(*) as cnt
            FROM transactions
            WHERE issue_date >= date('now', ? || ' months')
            GROUP BY month ORDER BY month
        """;
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "-" + months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) stats.put(rs.getString("month"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching monthly stats: " + e.getMessage());
        }
        return stats;
    }

    /** Returns daily issued vs returned for the last N days. */
    public Map<String, int[]> getDailyStats(int days) {
        String sql = """
            SELECT issue_date as day,
                   SUM(CASE WHEN status != 'Returned' OR return_date IS NULL THEN 1 ELSE 0 END) as issued,
                   SUM(CASE WHEN return_date IS NOT NULL THEN 1 ELSE 0 END) as returned
            FROM transactions
            WHERE issue_date >= date('now', ? || ' days')
            GROUP BY day ORDER BY day
        """;
        Map<String, int[]> stats = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "-" + days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    stats.put(rs.getString("day"),
                            new int[]{rs.getInt("issued"), rs.getInt("returned")});
            }
        } catch (SQLException e) {
            LOG.error("Error fetching daily stats: " + e.getMessage());
        }
        return stats;
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private List<Transaction> queryTransactions(String sql) {
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapTransaction(rs));
        } catch (SQLException e) {
            LOG.error("Error querying transactions: " + e.getMessage());
        }
        return list;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("transaction_id"));
        t.setBookId(rs.getInt("book_id"));
        t.setMemberId(rs.getInt("member_id"));
        t.setBookName(rs.getString("book_name"));
        t.setMemberName(rs.getString("member_name"));
        t.setStudentId(rs.getString("student_id"));
        String issue = rs.getString("issue_date");
        if (issue != null) t.setIssueDate(LocalDate.parse(issue));
        String due = rs.getString("due_date");
        if (due != null) t.setDueDate(LocalDate.parse(due));
        String ret = rs.getString("return_date");
        if (ret != null) t.setReturnDate(LocalDate.parse(ret));
        t.setFineAmount(rs.getDouble("fine_amount"));
        t.setFinePaid(rs.getInt("fine_paid") == 1);
        t.setStatus(rs.getString("status"));
        t.setReturnCondition(rs.getString("return_condition"));
        t.setIssuedBy(rs.getString("issued_by"));
        return t;
    }

    private void pushActivity(Transaction tx) {
        recentActivity.push(tx);
        if (recentActivity.size() > 50) recentActivity.removeLast();
    }

    private void updateActiveBorrowingsCache(int memberId, Transaction tx, boolean add) {
        activeBorrowings.computeIfAbsent(memberId, k -> new ArrayList<>());
        if (add) {
            activeBorrowings.get(memberId).add(tx);
        } else {
            activeBorrowings.get(memberId).removeIf(t -> t.getTransactionId() == tx.getTransactionId());
        }
    }

    private void logActivity(String user, String action, String details) {
        String sql = "INSERT INTO activity_log (user_id, action, details) VALUES (0, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, action + " by " + user);
            ps.setString(2, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error logging activity: " + e.getMessage());
        }
    }
}


