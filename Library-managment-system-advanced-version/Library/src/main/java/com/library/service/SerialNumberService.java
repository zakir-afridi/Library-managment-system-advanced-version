package com.library.service;

import com.library.database.DatabaseConnection;

import java.sql.*;

/**
 * Manages the serial_no column for Books, Members, and Employees.
 *
 * Serial numbers are display-order positions (1, 2, 3 ...).
 * They are NOT the permanent ID — they shift when items are added or removed.
 *
 * Rules:
 *  - Active list: sequential 1..N ordered by primary key
 *  - When item is archived/deleted: remaining items shift up (gap is closed)
 *  - Archived list: separate sequence ordered by archived_date DESC
 *
 * Call resequence*() after every add, delete, or archive operation.
 */
public class SerialNumberService {

    private static SerialNumberService instance;

    private SerialNumberService() {}

    public static synchronized SerialNumberService getInstance() {
        if (instance == null) instance = new SerialNumberService();
        return instance;
    }

    // ── Books ─────────────────────────────────────────────────────────────────

    /** Resequences serial_no for all active (non-archived) books. */
    public void resequenceBooks() {
        resequence(
            "SELECT book_id FROM books WHERE status != 'Archived' ORDER BY book_id",
            "UPDATE books SET serial_no = ? WHERE book_id = ?"
        );
    }

    /** Resequences serial_no for archived books (newest archive = serial 1). */
    public void resequenceArchivedBooks() {
        resequence(
            "SELECT book_id FROM books WHERE status = 'Archived' ORDER BY archived_date DESC, book_id",
            "UPDATE books SET serial_no = ? WHERE book_id = ?"
        );
    }

    // ── Members ───────────────────────────────────────────────────────────────

    public void resequenceMembers() {
        resequence(
            "SELECT std_id FROM members WHERE status != 'Archived' ORDER BY std_id",
            "UPDATE members SET serial_no = ? WHERE std_id = ?"
        );
    }

    public void resequenceArchivedMembers() {
        resequence(
            "SELECT std_id FROM members WHERE status = 'Archived' ORDER BY archived_date DESC, std_id",
            "UPDATE members SET serial_no = ? WHERE std_id = ?"
        );
    }

    // ── Employees ─────────────────────────────────────────────────────────────

    public void resequenceEmployees() {
        resequence(
            "SELECT emp_id FROM employees WHERE status != 'Archived' ORDER BY emp_id",
            "UPDATE employees SET serial_no = ? WHERE emp_id = ?"
        );
    }

    public void resequenceArchivedEmployees() {
        resequence(
            "SELECT emp_id FROM employees WHERE status = 'Archived' ORDER BY archived_date DESC, emp_id",
            "UPDATE employees SET serial_no = ? WHERE emp_id = ?"
        );
    }

    // ── Convenience: resequence both active + archived ────────────────────────

    public void resequenceAll() {
        resequenceBooks();
        resequenceArchivedBooks();
        resequenceMembers();
        resequenceArchivedMembers();
        resequenceEmployees();
        resequenceArchivedEmployees();
    }

    // ── Core algorithm ────────────────────────────────────────────────────────

    /**
     * Fetches IDs in the given order, then assigns serial_no = 1, 2, 3 ...
     * Uses a single transaction for atomicity.
     */
    private void resequence(String selectSql, String updateSql) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int serial = 1;
                try (Statement sel = c.createStatement();
                     ResultSet rs  = sel.executeQuery(selectSql);
                     PreparedStatement upd = c.prepareStatement(updateSql)) {
                    while (rs.next()) {
                        upd.setInt(1, serial++);
                        upd.setInt(2, rs.getInt(1));
                        upd.addBatch();
                    }
                    upd.executeBatch();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                System.err.println("SerialNumberService resequence error: " + e.getMessage());
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("SerialNumberService connection error: " + e.getMessage());
        }
    }
}
