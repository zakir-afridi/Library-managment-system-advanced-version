package com.library.students;

import com.library.database.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * STUDENTS BRANCH — service layer.
 * Full implementation (no existing StudentService to delegate to).
 */
public class StudentService {

    public int getTotalCount() {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s  = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM students WHERE is_archived=0")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("StudentService.getTotalCount: " + e.getMessage());
            return 0;
        }
    }

    public List<Map<String, String>> search(String query) {
        List<Map<String, String>> results = new ArrayList<>();
        String sql = """
            SELECT student_id, full_name, department, year, email, phone
            FROM students
            WHERE is_archived=0
              AND (full_name LIKE ? OR student_id LIKE ? OR department LIKE ?)
            LIMIT 50
        """;
        String p = "%" + query + "%";
        try (Connection c  = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p); ps.setString(2, p); ps.setString(3, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("student_id", rs.getString("student_id"));
                    row.put("full_name",  rs.getString("full_name"));
                    row.put("department", rs.getString("department"));
                    row.put("year",       String.valueOf(rs.getInt("year")));
                    row.put("email",      rs.getString("email"));
                    row.put("phone",      rs.getString("phone"));
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("StudentService.search: " + e.getMessage());
        }
        return results;
    }

    public boolean add(String studentId, String fullName, String department,
                       int year, String email, String phone) {
        String sql = """
            INSERT INTO students (student_id, full_name, department, year, email, phone, is_archived)
            VALUES (?, ?, ?, ?, ?, ?, 0)
        """;
        try (Connection c  = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, fullName);
            ps.setString(3, department);
            ps.setInt(4, year);
            ps.setString(5, email);
            ps.setString(6, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("StudentService.add: " + e.getMessage());
            return false;
        }
    }

    public boolean archive(String studentId) {
        String sql = "UPDATE students SET is_archived=1 WHERE student_id=?";
        try (Connection c  = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("StudentService.archive: " + e.getMessage());
            return false;
        }
    }

    public boolean unarchive(String studentId) {
        String sql = "UPDATE students SET is_archived=0 WHERE student_id=?";
        try (Connection c  = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("StudentService.unarchive: " + e.getMessage());
            return false;
        }
    }
}
