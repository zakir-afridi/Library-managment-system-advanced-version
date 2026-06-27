package com.library.cache;

import com.library.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton cache for dashboard statistics.
 *
 * Uses a HashMap<String, Object> as the in-memory store with a
 * configurable TTL (default 60 seconds) to avoid hammering SQLite
 * on every dashboard refresh.
 */
public class DashboardCache {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardCache.class);

    private static final long TTL_MS = 60_000; // 60 seconds

    private static DashboardCache instance;

    // HashMap for O(1) stat lookups
    private final Map<String, Object> cache = new HashMap<>();
    private long lastRefresh = 0;

    private DashboardCache() {}

    public static synchronized DashboardCache getInstance() {
        if (instance == null) instance = new DashboardCache();
        return instance;
    }

    /** Force the next call to getStats() to re-query the database. */
    public void invalidate() { lastRefresh = 0; }

    /** Returns cached stats, refreshing from DB if TTL has expired. */
    public DashboardStats getStats() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh > TTL_MS) {
            refresh();
            lastRefresh = now;
        }
        return buildFromCache();
    }

    // ── DB query ──────────────────────────────────────────────────────────────

    private void refresh() {
        try (Connection c = DatabaseConnection.getConnection()) {
            cache.put("totalBooks",    queryInt(c, "SELECT COUNT(*) FROM books"));
            cache.put("availableBooks",queryInt(c, "SELECT COALESCE(SUM(available_qty),0) FROM books"));
            cache.put("issuedBooks",   queryInt(c,
                    "SELECT COUNT(*) FROM transactions WHERE status='Issued'"));
            cache.put("overdueBooks",  queryInt(c,
                    "SELECT COUNT(*) FROM transactions WHERE status='Issued' AND due_date < date('now')"));
            cache.put("totalMembers",  queryInt(c, "SELECT COUNT(*) FROM members"));
            cache.put("activeMembers", queryInt(c,
                    "SELECT COUNT(*) FROM members WHERE status='Active'"));
            cache.put("issuedToday",   queryInt(c,
                    "SELECT COUNT(*) FROM transactions WHERE issue_date = date('now')"));
            cache.put("reservations",  queryInt(c,
                    "SELECT COUNT(*) FROM reservations WHERE status='Pending'"));
            cache.put("finesCollected",queryDouble(c,
                    "SELECT COALESCE(SUM(fine_amount),0) FROM transactions WHERE fine_paid=1"));
            cache.put("pendingFines",  queryDouble(c,
                    "SELECT COALESCE(SUM(fine_amount),0) FROM transactions WHERE fine_paid=0 AND fine_amount>0"));
        } catch (SQLException e) {
            System.err.println("DashboardCache refresh error: " + e.getMessage());
        }
    }

    private DashboardStats buildFromCache() {
        return new DashboardStats(
                getInt("totalBooks"),
                getInt("availableBooks"),
                getInt("issuedBooks"),
                getInt("overdueBooks"),
                getInt("totalMembers"),
                getInt("activeMembers"),
                getInt("issuedToday"),
                getInt("reservations"),
                getDbl("finesCollected"),
                getDbl("pendingFines")
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int queryInt(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double queryDouble(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private int    getInt(String key) { Object v = cache.get(key); return v instanceof Number n ? n.intValue() : 0; }
    private double getDbl(String key) { Object v = cache.get(key); return v instanceof Number n ? n.doubleValue() : 0.0; }
}
