package com.library.cache;

import com.library.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton thread-safe cache for dashboard Business Intelligence & KPI statistics.
 */
public class DashboardCache {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardCache.class);

    private static final long TTL_MS = 60_000; // 60 seconds

    private static DashboardCache instance;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private volatile long lastRefresh = 0;

    private DashboardCache() {}

    public static synchronized DashboardCache getInstance() {
        if (instance == null) instance = new DashboardCache();
        return instance;
    }

    /** Force the next call to getStats() to re-query the database. */
    public void invalidate() {
        lastRefresh = 0;
    }

    /** Returns cached stats, refreshing from DB if TTL has expired. */
    public synchronized DashboardStats getStats() {
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
            cache.put("totalBooks",     queryInt(c, "SELECT COUNT(*) FROM books"));
            cache.put("totalCopies",    queryInt(c, "SELECT COALESCE(SUM(quantity),0) FROM books"));
            cache.put("availableBooks", queryInt(c, "SELECT COALESCE(SUM(available_qty),0) FROM books"));
            cache.put("issuedBooks",    queryInt(c, "SELECT COUNT(*) FROM transactions WHERE status='Issued'"));
            cache.put("overdueBooks",   queryInt(c, "SELECT COUNT(*) FROM transactions WHERE status='Issued' AND due_date < date('now')"));
            cache.put("totalMembers",   queryInt(c, "SELECT COUNT(*) FROM members"));
            cache.put("activeMembers",  queryInt(c, "SELECT COUNT(*) FROM members WHERE status='Active'"));
            cache.put("issuedToday",    queryInt(c, "SELECT COUNT(*) FROM transactions WHERE issue_date = date('now')"));
            cache.put("reservations",   queryInt(c, "SELECT COUNT(*) FROM reservations WHERE status='Pending'"));
            cache.put("finesCollected", queryDouble(c, "SELECT COALESCE(SUM(fine_amount),0) FROM transactions WHERE fine_paid=1"));
            cache.put("pendingFines",   queryDouble(c, "SELECT COALESCE(SUM(fine_amount),0) FROM transactions WHERE fine_paid=0 AND fine_amount>0"));
            cache.put("totalMemberDues",queryDouble(c, "SELECT COALESCE(SUM(fine_balance),0) FROM members WHERE fine_balance>0"));
            cache.put("overdueAccounts",queryInt(c, "SELECT COUNT(DISTINCT member_id) FROM transactions WHERE status='Issued' AND due_date < date('now')"));

            // Stock value calculation (total copies * 1500 PKR average unit replacement cost)
            int copies = getInt("totalCopies");
            cache.put("estimatedStockValue", (double) (copies * 1500));

            // Category breakdown
            Map<String, Integer> catMap = new LinkedHashMap<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT category, COUNT(*) as cnt FROM books GROUP BY category ORDER BY cnt DESC LIMIT 6")) {
                while (rs.next()) {
                    String cat = rs.getString("category");
                    if (cat == null || cat.isBlank()) cat = "General";
                    catMap.put(cat, rs.getInt("cnt"));
                }
            }
            cache.put("categories", catMap);

            // Monthly revenue breakdown (last 6 months)
            Map<String, Double> revMap = new LinkedHashMap<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT strftime('%Y-%m', issue_date) as m, COALESCE(SUM(fine_amount),0) as rev FROM transactions WHERE fine_paid=1 GROUP BY m ORDER BY m DESC LIMIT 6")) {
                while (rs.next()) {
                    String m = rs.getString("m");
                    if (m != null) revMap.put(m, rs.getDouble("rev"));
                }
            }
            cache.put("monthlyRevenue", revMap);

        } catch (SQLException e) {
            LOG.error("DashboardCache refresh error: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private DashboardStats buildFromCache() {
        Map<String, Integer> cats = (Map<String, Integer>) cache.get("categories");
        Map<String, Double> revs = (Map<String, Double>) cache.get("monthlyRevenue");
        return new DashboardStats(
                getInt("totalBooks"),
                getInt("totalCopies"),
                getInt("availableBooks"),
                getInt("issuedBooks"),
                getInt("overdueBooks"),
                getInt("totalMembers"),
                getInt("activeMembers"),
                getInt("issuedToday"),
                getInt("reservations"),
                getDbl("finesCollected"),
                getDbl("pendingFines"),
                getDbl("totalMemberDues"),
                getInt("overdueAccounts"),
                getDbl("estimatedStockValue"),
                cats,
                revs
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
