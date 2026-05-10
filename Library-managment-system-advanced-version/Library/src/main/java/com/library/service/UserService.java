package com.library.service;

import com.library.database.DatabaseConnection;
import com.library.model.User;
import com.library.security.PasswordUtil;
import com.library.security.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for User authentication and management.
 * Handles BCrypt verification, failed attempt locking (3 attempts),
 * force-password-change on first login, and RBAC.
 */
public class UserService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Authenticates a user.
     * Returns the User on success, null on failure.
     * Locks account after MAX_FAILED_ATTEMPTS.
     */
    public User authenticate(String username, String password) {
        User user = getUserByUsername(username);
        if (user == null) return null;

        if (user.isLocked()) return null;

        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            incrementFailedAttempts(user);
            return null;
        }

        // Success — reset failed attempts, update last login
        resetFailedAttempts(user.getUserId());
        updateLastLogin(user.getUserId());
        user.setLastLogin(LocalDateTime.now());
        user.setFailedAttempts(0);

        SessionManager.getInstance().login(user, () -> {
            // Timeout callback — handled by UI layer
        });

        return user;
    }

    // ── Password Management ───────────────────────────────────────────────────

    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) return false;
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) return false;
        if (!PasswordUtil.isStrong(newPassword)) return false;

        String sql = "UPDATE users SET password_hash=?, force_password_change=0 WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            return false;
        }
    }

    public boolean adminResetPassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash=?, force_password_change=0, failed_attempts=0, status='Active' WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return false;
        }
    }

    // ── User CRUD ─────────────────────────────────────────────────────────────

    public boolean createUser(String username, String password, String role) {
        if (!PasswordUtil.isStrong(password)) return false;
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUserRole(int userId, String role) {
        String sql = "UPDATE users SET role=? WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating role: " + e.getMessage());
            return false;
        }
    }

    public boolean unlockUser(int userId) {
        String sql = "UPDATE users SET status='Active', failed_attempts=0 WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error unlocking user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unlocks a locked account using the universal recovery key.
     * Resets failed_attempts and status to Active.
     * Returns the unlocked User, or null if key is wrong / user not found.
     */
    public User unlockByRecoveryKey(String username, String recoveryKey) {
        if (!com.library.util.Constants.isValidRecoveryKey(recoveryKey)) return null;
        User user = getUserByUsername(username);
        if (user == null) return null;
        unlockUser(user.getUserId());
        user.setStatus("Active");
        user.setFailedAttempts(0);
        return user;
    }

    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) users.add(mapUser(rs));
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }
        return users;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }
        return null;
    }

    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user by ID: " + e.getMessage());
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void incrementFailedAttempts(User user) {
        int attempts = user.getFailedAttempts() + 1;
        String status = attempts >= MAX_FAILED_ATTEMPTS ? "Locked" : "Active";
        String sql = "UPDATE users SET failed_attempts=?, status=? WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setString(2, status);
            ps.setInt(3, user.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error incrementing failed attempts: " + e.getMessage());
        }
    }

    private void resetFailedAttempts(int userId) {
        String sql = "UPDATE users SET failed_attempts=0 WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error resetting failed attempts: " + e.getMessage());
        }
    }

    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login=? WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        u.setForcePasswordChange(rs.getInt("force_password_change") == 1);
        String last = rs.getString("last_login");
        if (last != null) u.setLastLogin(parseSqliteDateTime(last));
        String created = rs.getString("created_at");
        if (created != null) u.setCreatedAt(parseSqliteDateTime(created));
        return u;
    }

    /** SQLite datetime() returns "yyyy-MM-dd HH:mm:ss" — handle both formats. */
    private java.time.LocalDateTime parseSqliteDateTime(String s) {
        try {
            // ISO format with T
            return java.time.LocalDateTime.parse(s);
        } catch (Exception e) {
            try {
                // SQLite format with space
                return java.time.LocalDateTime.parse(s,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                return java.time.LocalDateTime.now();
            }
        }
    }
}
