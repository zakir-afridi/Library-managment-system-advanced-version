package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditRepository extends BaseRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AuditRepository.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(AuditLog log) {
        String sql = """
            INSERT INTO audit_logs (user_id, username, action, entity_type, entity_id, details, ip_address, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getUserId());
            ps.setString(2, log.getUsername());
            ps.setString(3, log.getAction());
            ps.setString(4, log.getEntityType());
            ps.setString(5, log.getEntityId());
            ps.setString(6, log.getDetails());
            ps.setString(7, log.getIpAddress());
            ps.setString(8, log.getTimestamp() != null ? log.getTimestamp().format(DT_FMT) : LocalDateTime.now().format(DT_FMT));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    public List<AuditLog> getRecentLogs(int limit) {
        String sql = """
            SELECT * FROM audit_logs
            ORDER BY audit_id DESC
            LIMIT ?
        """;
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setAuditId(rs.getInt("audit_id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setUsername(rs.getString("username"));
                    log.setAction(rs.getString("action"));
                    log.setEntityType(rs.getString("entity_type"));
                    log.setEntityId(rs.getString("entity_id"));
                    log.setDetails(rs.getString("details"));
                    log.setIpAddress(rs.getString("ip_address"));
                    String ts = rs.getString("timestamp");
                    if (ts != null) {
                        try { log.setTimestamp(LocalDateTime.parse(ts, DT_FMT)); } catch (Exception ignored) {}
                    }
                    list.add(log);
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to query audit logs: {}", e.getMessage(), e);
            throw new DatabaseException("Error loading audit logs: " + e.getMessage(), e);
        }
        return list;
    }
}
