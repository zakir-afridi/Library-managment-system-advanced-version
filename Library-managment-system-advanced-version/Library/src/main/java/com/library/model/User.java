package com.library.model;

import java.time.LocalDateTime;

/**
 * System user with role-based access control.
 * Roles: ADMIN, LIBRARIAN, VIEWER
 */
public class User {

    public static final String ROLE_ADMIN     = "ADMIN";
    public static final String ROLE_LIBRARIAN = "LIBRARIAN";
    public static final String ROLE_VIEWER    = "VIEWER";

    public static final String STATUS_ACTIVE  = "Active";
    public static final String STATUS_LOCKED  = "Locked";

    private int           userId;
    private String        username;
    private String        passwordHash;
    private String        role;
    private String        status;
    private int           failedAttempts;
    private boolean       forcePasswordChange;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime lockedUntil;         // v3: time-based lockout

    public User() {
        this.role               = ROLE_LIBRARIAN;
        this.status             = STATUS_ACTIVE;
        this.failedAttempts     = 0;
        this.forcePasswordChange = false;
        this.createdAt          = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int           getUserId()                  { return userId; }
    public void          setUserId(int v)             { this.userId = v; }

    public String        getUsername()                { return username; }
    public void          setUsername(String v)        { this.username = v; }

    public String        getPasswordHash()            { return passwordHash; }
    public void          setPasswordHash(String v)    { this.passwordHash = v; }

    public String        getRole()                    { return role; }
    public void          setRole(String v)            { this.role = v; }

    public String        getStatus()                  { return status; }
    public void          setStatus(String v)          { this.status = v; }

    public int           getFailedAttempts()          { return failedAttempts; }
    public void          setFailedAttempts(int v)     { this.failedAttempts = v; }

    public boolean       isForcePasswordChange()      { return forcePasswordChange; }
    public void          setForcePasswordChange(boolean v){ this.forcePasswordChange = v; }

    public LocalDateTime getLastLogin()               { return lastLogin; }
    public void          setLastLogin(LocalDateTime v){ this.lastLogin = v; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ this.createdAt = v; }

    public LocalDateTime getLockedUntil()               { return lockedUntil; }
    public void          setLockedUntil(LocalDateTime v){ this.lockedUntil = v; }

    public boolean       isLocked()                   { return STATUS_LOCKED.equals(status); }
    public boolean       isAdmin()                    { return ROLE_ADMIN.equals(role); }
    public boolean       isLibrarian()                { return ROLE_LIBRARIAN.equals(role); }
    public boolean       canWrite()                   { return isAdmin() || isLibrarian(); }

    @Override
    public String toString() { return username + " [" + role + "]"; }
}
