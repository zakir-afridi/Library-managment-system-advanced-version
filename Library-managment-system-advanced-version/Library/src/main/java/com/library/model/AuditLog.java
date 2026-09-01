package com.library.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a persistent immutable audit trail entry.
 */
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           auditId;
    private int           userId;
    private String        username;
    private String        action;
    private String        entityType;
    private String        entityId;
    private String        details;
    private String        ipAddress;
    private LocalDateTime timestamp;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(int auditId, int userId, String username, String action,
                    String entityType, String entityId, String details,
                    String ipAddress, LocalDateTime timestamp) {
        this.auditId    = auditId;
        this.userId     = userId;
        this.username   = username;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.details    = details;
        this.ipAddress  = ipAddress;
        this.timestamp  = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public int getAuditId() { return auditId; }
    public void setAuditId(int auditId) { this.auditId = auditId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username != null ? username : ""; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action != null ? action : ""; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType != null ? entityType : ""; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId != null ? entityId : ""; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDetails() { return details != null ? details : ""; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress != null ? ipAddress : ""; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + username + " -> " + action + " on " + entityType + ":" + entityId;
    }
}
