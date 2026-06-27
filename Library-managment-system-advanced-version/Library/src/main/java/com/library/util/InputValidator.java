package com.library.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Comprehensive input validation utilities for LibraCore Pro v3.
 *
 * All validation methods are pure functions (no side effects).
 * For field highlighting in JavaFX, use ValidationUtil in shared/.
 */
public final class InputValidator {

    // ── Regex patterns ────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$");

    /** Pakistan format: +923XXXXXXXXX or 03XXXXXXXXX */
    private static final Pattern PHONE_PK_PATTERN =
        Pattern.compile("^(\\+92|0)3[0-9]{9}$");

    /** Generic international phone */
    private static final Pattern PHONE_INTL_PATTERN =
        Pattern.compile("^[\\d\\s+\\-()]{7,15}$");

    private static final Pattern ISBN10_PATTERN = Pattern.compile("^\\d{9}[\\dX]$");
    private static final Pattern ISBN13_PATTERN = Pattern.compile("^\\d{13}$");

    private InputValidator() {}

    // ── Basic ──────────────────────────────────────────────────────────────────

    public static boolean isBlank(String s)    { return s == null || s.isBlank(); }
    public static boolean isNotBlank(String s) { return !isBlank(s); }

    // ── Email ──────────────────────────────────────────────────────────────────

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // ── Phone ──────────────────────────────────────────────────────────────────

    /**
     * Validates Pakistan-format phone numbers (accepts both +923 and 03 prefixes).
     * Falls back to generic 7–15 digit international format.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;
        String clean = phone.replaceAll("\\s", "");
        return PHONE_PK_PATTERN.matcher(clean).matches()
            || PHONE_INTL_PATTERN.matcher(phone.trim()).matches();
    }

    // ── ISBN ───────────────────────────────────────────────────────────────────

    /**
     * Validates ISBN-10 or ISBN-13, including check digit verification.
     * Strips hyphens and spaces before checking.
     */
    public static boolean isValidIsbn(String isbn) {
        if (isbn == null) return false;
        String clean = isbn.replaceAll("[\\s\\-]", "").toUpperCase();
        if (clean.length() == 10) return isValidIsbn10(clean);
        if (clean.length() == 13) return isValidIsbn13(clean);
        return false;
    }

    private static boolean isValidIsbn10(String s) {
        if (!ISBN10_PATTERN.matcher(s).matches()) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (i + 1) * (s.charAt(i) - '0');
        char check = s.charAt(9);
        int checkVal = check == 'X' ? 10 : check - '0';
        return (sum % 11) == checkVal;
    }

    private static boolean isValidIsbn13(String s) {
        if (!ISBN13_PATTERN.matcher(s).matches()) return false;
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = s.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int check = (10 - (sum % 10)) % 10;
        return check == (s.charAt(12) - '0');
    }

    // ── Numbers ────────────────────────────────────────────────────────────────

    public static boolean isPositiveInt(String s) {
        try { return s != null && Integer.parseInt(s.trim()) > 0; }
        catch (NumberFormatException e) { return false; }
    }

    public static boolean isNonNegativeDouble(String s) {
        try { return s != null && Double.parseDouble(s.trim()) >= 0; }
        catch (NumberFormatException e) { return false; }
    }

    /** Use BigDecimal for monetary parsing to avoid floating-point errors. */
    public static BigDecimal parseMoney(String s) {
        try { return s == null ? BigDecimal.ZERO : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    // ── Year ──────────────────────────────────────────────────────────────────

    public static boolean isValidYear(int year) {
        int current = java.time.Year.now().getValue();
        return year >= 1000 && year <= current + 1;
    }

    public static boolean isValidYear(String s) {
        try { return isValidYear(Integer.parseInt(s.trim())); }
        catch (Exception e) { return false; }
    }
}
