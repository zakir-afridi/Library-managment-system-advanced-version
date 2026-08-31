package com.library.util;

import com.library.database.DatabaseConnection;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Thread-safe structured ID generator.
 *
 * Formats (prefix + 8 digits, no dash):
 *   Books:     BK00000001
 *   Students:  ST00000001
 *   Members:   MB00000001
 *   Employees: EP00000001
 *
 * Counter is stored in the id_counters table and increments atomically.
 * Counter never resets — IDs are permanent and unique even after deletion.
 */
public final class IdGenerator {

    public enum Type {
        BOOK    ("BK"),
        STUDENT ("ST"),
        MEMBER  ("MB"),
        EMPLOYEE("EP");

        public final String prefix;
        Type(String p) { this.prefix = p; }
    }

    private static final Logger LOG = Logger.getLogger(IdGenerator.class.getName());

    private IdGenerator() {}

    /**
     * Atomically increments the counter and returns the next formatted ID.
     * Example: BK00000001, ST00000002, EP00000001
     */
    public static synchronized String next(Type type) {
        String init = "INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES (?, 0)";
        String upd  = "UPDATE id_counters SET last_id = last_id + 1 WHERE entity = ?";
        String sel  = "SELECT last_id FROM id_counters WHERE entity = ?";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement psInit = c.prepareStatement(init)) {
                    psInit.setString(1, type.prefix);
                    psInit.executeUpdate();
                }
                try (PreparedStatement psUpd = c.prepareStatement(upd)) {
                    psUpd.setString(1, type.prefix);
                    psUpd.executeUpdate();
                }
                try (PreparedStatement psSel = c.prepareStatement(sel)) {
                    psSel.setString(1, type.prefix);
                    try (ResultSet rs = psSel.executeQuery()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            c.commit();
                            return fmt(type.prefix, id);
                        }
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOG.warning("IdGenerator.next error: " + e.getMessage());
        }
        // Fallback — should never happen in normal operation
        return type.prefix + String.format("%08d", System.currentTimeMillis() % 100_000_000L);
    }

    /**
     * Preview the next ID without incrementing (for display in empty form fields).
     */
    public static synchronized String peek(Type type) {
        String init = "INSERT OR IGNORE INTO id_counters (entity, last_id) VALUES (?, 0)";
        String sel = "SELECT last_id FROM id_counters WHERE entity = ?";
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement psInit = c.prepareStatement(init)) {
                psInit.setString(1, type.prefix);
                psInit.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, type.prefix);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return fmt(type.prefix, rs.getInt(1) + 1);
                }
            }
        } catch (SQLException e) {
            LOG.warning("IdGenerator.peek error: " + e.getMessage());
        }
        return type.prefix + "00000001";
    }

    /**
     * Returns true if the given string matches the ID format for the type.
     * Example: isValid(BOOK, "BK00000042") → true
     */
    public static boolean isValid(Type type, String id) {
        if (id == null) return false;
        return id.matches(type.prefix + "\\d{8}");
    }

    /** Format: prefix + 8-digit zero-padded number. No dash. */
    private static String fmt(String prefix, int n) {
        return prefix + String.format("%08d", n);
    }
}
