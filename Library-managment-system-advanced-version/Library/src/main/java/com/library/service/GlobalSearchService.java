package com.library.service;

import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.util.PageRequest;

import java.sql.*;
import java.util.*;

/**
 * Unified search across all entities.
 * Limit per entity type is driven by AppConfig.getDefaultLimit(),
 * clamped to PageRequest.MAX_LIMIT. No hardcoded limits.
 */
public class GlobalSearchService {

    public static class SearchResult {
        public final String type;
        public final int    dbId;
        public final String code;
        public final String name;
        public final String status;
        public final String extra;

        public SearchResult(String type, int dbId, String code,
                            String name, String status, String extra) {
            this.type   = type;
            this.dbId   = dbId;
            this.code   = code;
            this.name   = name;
            this.status = status;
            this.extra  = extra;
        }
    }

    /** Configurable limit — reads from AppConfig, clamped to MAX_LIMIT. */
    private int limitPerType() {
        return Math.min(AppConfig.getInstance().getDefaultLimit(), PageRequest.MAX_LIMIT);
    }

    public List<SearchResult> search(String query, boolean includeArchived) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.isBlank()) return results;
        String p = "%" + query.trim() + "%";
        try (Connection c = DatabaseConnection.getConnection()) {
            results.addAll(searchBooks(c, p, includeArchived));
            results.addAll(searchMembers(c, p, includeArchived));
            results.addAll(searchEmployees(c, p, includeArchived));
        } catch (SQLException e) {
            System.err.println("GlobalSearch error: " + e.getMessage());
        }
        return results;
    }

    public List<SearchResult> search(String query) {
        return search(query, false);
    }

    // ── Per-entity searches ───────────────────────────────────────────────────

    private List<SearchResult> searchBooks(Connection c, String p,
                                            boolean includeArchived) throws SQLException {
        String statusClause = includeArchived ? "" : "AND status != 'Archived' ";
        String sql = "SELECT book_id, COALESCE(book_code, 'BK-' || printf('%06d', book_id)) as code, " +
                     "book_name, author, status FROM books " +
                     "WHERE " + statusClause +
                     "(book_name LIKE ? OR author LIKE ? OR isbn LIKE ? OR COALESCE(book_code,'') LIKE ?) " +
                     "ORDER BY book_name LIMIT ?";
        List<SearchResult> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p); ps.setString(2, p);
            ps.setString(3, p); ps.setString(4, p);
            ps.setInt(5, limitPerType());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new SearchResult("BOOK", rs.getInt("book_id"),
                            rs.getString("code"), rs.getString("book_name"),
                            rs.getString("status"), rs.getString("author")));
            }
        }
        return list;
    }

    private List<SearchResult> searchMembers(Connection c, String p,
                                              boolean includeArchived) throws SQLException {
        String statusClause = includeArchived ? "" : "AND status != 'Archived' ";
        String sql = "SELECT std_id, COALESCE(member_code, student_id, 'ST-' || printf('%06d', std_id)) as code, " +
                     "name, department, status FROM members " +
                     "WHERE " + statusClause +
                     "(name LIKE ? OR student_id LIKE ? OR email LIKE ? OR COALESCE(member_code,'') LIKE ?) " +
                     "ORDER BY name LIMIT ?";
        List<SearchResult> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p); ps.setString(2, p);
            ps.setString(3, p); ps.setString(4, p);
            ps.setInt(5, limitPerType());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new SearchResult("MEMBER", rs.getInt("std_id"),
                            rs.getString("code"), rs.getString("name"),
                            rs.getString("status"), rs.getString("department")));
            }
        }
        return list;
    }

    private List<SearchResult> searchEmployees(Connection c, String p,
                                                boolean includeArchived) throws SQLException {
        String statusClause = includeArchived ? "" : "AND status != 'Archived' ";
        String sql = "SELECT emp_id, employee_code, name, designation, status FROM employees " +
                     "WHERE " + statusClause +
                     "(name LIKE ? OR employee_code LIKE ? OR email LIKE ? OR contact LIKE ?) " +
                     "ORDER BY name LIMIT ?";
        List<SearchResult> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p); ps.setString(2, p);
            ps.setString(3, p); ps.setString(4, p);
            ps.setInt(5, limitPerType());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new SearchResult("EMPLOYEE", rs.getInt("emp_id"),
                            rs.getString("employee_code"), rs.getString("name"),
                            rs.getString("status"), rs.getString("designation")));
            }
        }
        return list;
    }
}
