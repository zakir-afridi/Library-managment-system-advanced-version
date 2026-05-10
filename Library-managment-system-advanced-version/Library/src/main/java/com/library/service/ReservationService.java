package com.library.service;

import com.library.database.DatabaseConnection;
import com.library.model.Reservation;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Service layer for book reservations.
 * Uses a Queue<Reservation> (FIFO) per book for waiting list management.
 * HashMap<Integer, Queue<Reservation>> maps bookId -> waiting queue.
 */
public class ReservationService {

    // HashMap of bookId -> FIFO queue of pending reservations
    private final Map<Integer, Queue<Reservation>> waitingQueues = new HashMap<>();

    // ── Reserve a book ────────────────────────────────────────────────────────

    public String reserveBook(int bookId, int memberId) {
        // Check if member already has a pending reservation for this book
        if (hasPendingReservation(bookId, memberId))
            return "Member already has a pending reservation for this book.";

        int queuePos = getNextQueuePosition(bookId);

        String sql = """
            INSERT INTO reservations (book_id, member_id, reservation_date, status, queue_position)
            VALUES (?, ?, ?, 'Pending', ?)
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            ps.setString(3, LocalDate.now().toString());
            ps.setInt(4, queuePos);

            if (ps.executeUpdate() == 0) return "Failed to create reservation.";

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Reservation r = new Reservation();
                    r.setReservationId(keys.getInt(1));
                    r.setBookId(bookId);
                    r.setMemberId(memberId);
                    r.setQueuePosition(queuePos);
                    r.setReservationDate(LocalDate.now());

                    waitingQueues.computeIfAbsent(bookId, k -> new LinkedList<>()).offer(r);
                }
            }
            return "";

        } catch (SQLException e) {
            System.err.println("Error reserving book: " + e.getMessage());
            return "Database error: " + e.getMessage();
        }
    }

    public boolean cancelReservation(int reservationId) {
        String sql = "UPDATE reservations SET status='Cancelled' WHERE reservation_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error cancelling reservation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Called when a book is returned — notifies the next person in queue.
     * Marks their reservation as 'Ready' and returns their member ID.
     */
    public int notifyNextInQueue(int bookId) {
        String sql = """
            UPDATE reservations SET status='Ready', notified_date=?
            WHERE reservation_id = (
                SELECT reservation_id FROM reservations
                WHERE book_id=? AND status='Pending'
                ORDER BY queue_position ASC LIMIT 1
            )
        """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, bookId);
            ps.executeUpdate();

            // Return the member ID of the notified person
            String query = """
                SELECT member_id FROM reservations
                WHERE book_id=? AND status='Ready'
                ORDER BY queue_position ASC LIMIT 1
            """;
            try (PreparedStatement ps2 = c.prepareStatement(query)) {
                ps2.setInt(1, bookId);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) return rs.getInt("member_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error notifying queue: " + e.getMessage());
        }
        return -1;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<Reservation> getPendingReservations() {
        String sql = """
            SELECT r.*, b.book_name, m.name as member_name, m.student_id
            FROM reservations r
            JOIN books b ON r.book_id = b.book_id
            JOIN members m ON r.member_id = m.std_id
            WHERE r.status='Pending'
            ORDER BY r.queue_position ASC
        """;
        return queryReservations(sql);
    }

    public List<Reservation> getMemberReservations(int memberId) {
        String sql = """
            SELECT r.*, b.book_name, m.name as member_name, m.student_id
            FROM reservations r
            JOIN books b ON r.book_id = b.book_id
            JOIN members m ON r.member_id = m.std_id
            WHERE r.member_id=?
            ORDER BY r.reservation_date DESC
        """;
        List<Reservation> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching member reservations: " + e.getMessage());
        }
        return list;
    }

    public int getQueueLength(int bookId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE book_id=? AND status='Pending'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting queue length: " + e.getMessage());
        }
        return 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hasPendingReservation(int bookId, int memberId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE book_id=? AND member_id=? AND status='Pending'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private int getNextQueuePosition(int bookId) {
        String sql = "SELECT COALESCE(MAX(queue_position), 0) + 1 FROM reservations WHERE book_id=? AND status='Pending'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting queue position: " + e.getMessage());
        }
        return 1;
    }

    private List<Reservation> queryReservations(String sql) {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapReservation(rs));
        } catch (SQLException e) {
            System.err.println("Error querying reservations: " + e.getMessage());
        }
        return list;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setBookId(rs.getInt("book_id"));
        r.setMemberId(rs.getInt("member_id"));
        r.setBookName(rs.getString("book_name"));
        r.setMemberName(rs.getString("member_name"));
        r.setStudentId(rs.getString("student_id"));
        String date = rs.getString("reservation_date");
        if (date != null) r.setReservationDate(LocalDate.parse(date));
        r.setStatus(rs.getString("status"));
        r.setQueuePosition(rs.getInt("queue_position"));
        String notified = rs.getString("notified_date");
        if (notified != null) r.setNotifiedDate(LocalDate.parse(notified));
        return r;
    }
}
