package com.library.security;

import com.library.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Singleton that tracks the currently authenticated user and
 * auto-logs out after 30 minutes of inactivity.
 */
public class SessionManager {

    private static final int TIMEOUT_MINUTES = 30;

    private static SessionManager instance;

    private User   currentUser;
    private Timeline timeoutTimer;
    private Runnable onTimeout;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    public void login(User user, Runnable onTimeoutCallback) {
        this.currentUser = user;
        this.onTimeout   = onTimeoutCallback;
        resetTimer();
    }

    public void logout() {
        currentUser = null;
        if (timeoutTimer != null) {
            timeoutTimer.stop();
            timeoutTimer = null;
        }
    }

    /** Call this on any user interaction to reset the inactivity clock. */
    public void resetTimer() {
        if (timeoutTimer != null) timeoutTimer.stop();
        timeoutTimer = new Timeline(new KeyFrame(
                Duration.minutes(TIMEOUT_MINUTES), e -> triggerTimeout()));
        timeoutTimer.setCycleCount(1);
        timeoutTimer.play();
    }

    private void triggerTimeout() {
        logout();
        if (onTimeout != null) onTimeout.run();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isLoggedIn()          { return currentUser != null; }
    public User    getCurrentUser()      { return currentUser; }
    public String  getUsername()         { return currentUser != null ? currentUser.getUsername() : ""; }
    public String  getRole()             { return currentUser != null ? currentUser.getRole() : ""; }

    public boolean isAdmin()             { return "ADMIN".equalsIgnoreCase(getRole()); }
    public boolean isLibrarian()         { return "LIBRARIAN".equalsIgnoreCase(getRole()); }
    public boolean canWrite()            { return isAdmin() || isLibrarian(); }
}
