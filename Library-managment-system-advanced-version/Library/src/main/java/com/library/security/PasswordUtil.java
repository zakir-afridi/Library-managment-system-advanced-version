package com.library.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility for BCrypt password hashing and verification.
 * Uses work factor 12 — strong enough for a desktop app.
 */
public final class PasswordUtil {

    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {}

    /** Hash a plain-text password. */
    public static String hash(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(WORK_FACTOR));
    }

    /** Verify a plain-text password against a stored BCrypt hash. */
    public static boolean verify(String plainText, String hashed) {
        if (plainText == null || hashed == null) return false;
        try {
            return BCrypt.checkpw(plainText, hashed);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate password strength.
     * Rules: min 8 chars, at least one uppercase, one digit, one special char.
     */
    public static boolean isStrong(String password) {
        if (password == null || password.length() < 8) return false;
        boolean upper   = password.chars().anyMatch(Character::isUpperCase);
        boolean digit   = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(c ->
                "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        return upper && digit && special;
    }

    /**
     * Returns a 0–4 strength score for a password (for the UI strength bar).
     * 0 = very weak, 4 = strong.
     */
    public static int strengthScore(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8)  score++;
        if (password.length() >= 12) score++;
        if (password.chars().anyMatch(Character::isUpperCase) &&
            password.chars().anyMatch(Character::isLowerCase)) score++;
        if (password.chars().anyMatch(Character::isDigit)) score++;
        if (password.chars().anyMatch(c ->
                "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0)) score++;
        return Math.min(score, 4);
    }
}
