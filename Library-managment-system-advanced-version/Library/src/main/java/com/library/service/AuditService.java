package com.library.service;

import com.library.model.AuditLog;
import com.library.repository.AuditRepository;
import com.library.security.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for recording and auditing security-critical and circulation events.
 */
public class AuditService {

    private static AuditService instance;
    private final AuditRepository auditRepository = new AuditRepository();

    private AuditService() {}

    public static synchronized AuditService getInstance() {
        if (instance == null) {
            instance = new AuditService();
        }
        return instance;
    }

    public void log(String action, String entityType, String entityId, String details) {
        String username = SessionManager.getInstance().isLoggedIn()
                ? SessionManager.getInstance().getUsername()
                : "SYSTEM";
        int userId = 1;
        AuditLog log = new AuditLog(0, userId, username, action, entityType, entityId, details, "127.0.0.1", LocalDateTime.now());
        auditRepository.log(log);
    }

    public List<AuditLog> getRecentLogs(int limit) {
        return auditRepository.getRecentLogs(limit);
    }
}
