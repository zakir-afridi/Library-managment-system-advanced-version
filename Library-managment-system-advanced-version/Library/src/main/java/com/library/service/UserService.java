package com.library.service;

import com.library.database.DatabaseConnection;
import com.library.model.User;
import com.library.security.PasswordUtil;
import com.library.security.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UserService — v3 upgrades:
 *  - 15-minute time-based lockout after 5 failed attempts (locked_until column)
 *  - SLF4J logging throughout
 *  - Virtual-thread safe (HikariCP pool)
 */
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES     = 15;

    // ── Authentication ────────────────────────────────────────────────────────

    public User authenticate(String username, String password) {
        User user = getUserByUsername(username);
        if (user == null) return null;
        if (isTimeLocked(user)) {
            LOG.warn("Login blocked — account '{}' locked until {}", username, user.getLockedUntil());
            return null;
        }
        if (user.isLocked()) return null;
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            incrementFailedAttempts(user);
            return null;
        }
        resetFailedAttempts(user.getUserId());
        updateLastLogin(user.getUserId());
        user.setLastLogin(LocalDateTime.now());
        user.setFailedAttempts(0);
        return user;
    }

    private boolean isTimeLocked(User user) {
        return user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil());
    }

    // ── Password Management ───────────────────────────────────────────────────

    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) return false;
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) return false;
        if (!PasswordUtil.isStrong(newPassword)) return false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET password_hash=?, force_password_change=0 WHERE user_id=?")) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error changing password: {}", e.getMessage());
            return false;
        }
    }

    public boolean adminResetPassword(int userId, String newPassword) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET password_hash=?, force_password_change=0, " +
                 "failed_attempts=0, status='Active', locked_until=NULL WHERE user_id=?")) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error resetting password: {}", e.getMessage());
            return false;
        }
    }

    // ── User CRUD ─────────────────────────────────────────────────────────────

    public boolean createUser(String username, String password, String role) {
        if (!PasswordUtil.isStrong(password)) return false;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error creating user: {}", e.getMessage());
            return false;
        }
    }

    public boolean updateUserRole(int userId, String role) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET role=? WHERE user_id=?")) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error updating role: {}", e.getMessage());
            return false;
        }
    }

    public boolean unlockUser(int userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET status='Active', failed_attempts=0, locked_until=NULL WHERE user_id=?")) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error unlocking user: {}", e.getMessage());
            return false;
        }
    }

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
        List<User> users = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM users ORDER BY username")) {
            while (rs.next()) users.add(mapUser(rs));
        } catch (SQLException e) {
            LOG.error("Error fetching users: {}", e.getMessage());
        }
        return users;
    }

    public User getUserByUsername(String username) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            LOG.error("Error fetching user '{}': {}", username, e.getMessage());
        }
        return null;
    }

    public User getUserById(int userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            LOG.error("Error fetching user {}: {}", userId, e.getMessage());
        }
        return null;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void incrementFailedAttempts(User user) {
        int attempts = user.getFailedAttempts() + 1;
        String status      = attempts >= MAX_FAILED_ATTEMPTS ? "Locked"  : "Active";
        String lockedUntil = attempts >= MAX_FAILED_ATTEMPTS
            ? LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES).toString() : null;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET failed_attempts=?, status=?, locked_until=? WHERE user_id=?")) {
            ps.setInt(1, attempts);
            ps.setString(2, status);
            ps.setString(3, lockedUntil);
            ps.setInt(4, user.getUserId());
            ps.executeUpdate();
            if (attempts >= MAX_FAILED_ATTEMPTS)
                LOG.warn("Account '{}' locked for {} min after {} failed attempts.",
                    user.getUsername(), LOCKOUT_MINUTES, MAX_FAILED_ATTEMPTS);
        } catch (SQLException e) {
            LOG.error("Error incrementing failed attempts: {}", e.getMessage());
        }
    }

    private void resetFailedAttempts(int userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET failed_attempts=0, locked_until=NULL WHERE user_id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error resetting failed attempts: {}", e.getMessage());
        }
    }

    private void updateLastLogin(int userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE users SET last_login=? WHERE user_id=?")) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error updating last login: {}", e.getMessage());
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
        try { String s = rs.getString("last_login");  if (s != null) u.setLastLogin(parseDT(s));  } catch (SQLException ignored) {}
        try { String s = rs.getString("created_at");  if (s != null) u.setCreatedAt(parseDT(s));  } catch (SQLException ignored) {}
        try { String s = rs.getString("locked_until"); if (s != null) u.setLockedUntil(parseDT(s)); } catch (SQLException ignored) {}
        return u;
    }

    private LocalDateTime parseDT(String s) {
        try { return LocalDateTime.parse(s); }
        catch (Exception e1) {
            try { return LocalDateTime.parse(s,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) { return LocalDateTime.now(); }
        }
    }
}
