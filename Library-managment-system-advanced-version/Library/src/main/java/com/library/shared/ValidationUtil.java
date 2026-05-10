package com.library.shared;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputControl;

/**
 * SHARED BRANCH — input validation utilities + inline field highlighting.
 */
public class ValidationUtil {

    // ── String checks ─────────────────────────────────────────────────────────

    public static boolean isBlank(String s)    { return s == null || s.isBlank(); }
    public static boolean isNotBlank(String s) { return !isBlank(s); }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$");
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[\\d\\s+\\-()]{7,15}$");
    }

    public static boolean isValidIsbn(String isbn) {
        if (isbn == null) return false;
        String clean = isbn.replaceAll("[\\s\\-]", "");
        return clean.matches("\\d{10}") || clean.matches("\\d{13}");
    }

    public static boolean isPositiveInt(String s) {
        try { return Integer.parseInt(s.trim()) > 0; }
        catch (Exception e) { return false; }
    }

    public static boolean isPositiveDouble(String s) {
        try { return Double.parseDouble(s.trim()) >= 0; }
        catch (Exception e) { return false; }
    }

    // ── Field highlighting ────────────────────────────────────────────────────

    private static final String STYLE_ERROR =
            "-fx-border-color:#d32f2f; -fx-border-width:2; -fx-border-radius:6; -fx-background-radius:6;";
    private static final String STYLE_OK =
            "-fx-border-color:#e0e0e0; -fx-border-width:1.5; -fx-border-radius:6; -fx-background-radius:6;";

    /** Mark field red if blank, green border if valid. Returns true if valid. */
    public static boolean requireNonBlank(TextInputControl field) {
        boolean ok = isNotBlank(field.getText());
        field.setStyle(ok ? STYLE_OK : STYLE_ERROR);
        return ok;
    }

    /** Mark field red if value is null/blank. Returns true if valid. */
    public static boolean requireSelected(ComboBox<?> combo) {
        boolean ok = combo.getValue() != null
                && !combo.getValue().toString().isBlank();
        combo.setStyle(ok ? STYLE_OK : STYLE_ERROR);
        return ok;
    }

    /** Mark field red if not a positive integer. Returns true if valid. */
    public static boolean requirePositiveInt(TextInputControl field) {
        boolean ok = isPositiveInt(field.getText());
        field.setStyle(ok ? STYLE_OK : STYLE_ERROR);
        return ok;
    }

    /** Clear error highlight from a field. */
    public static void clearError(TextInputControl field) {
        field.setStyle(STYLE_OK);
    }

    /** Clear error highlight from a combo. */
    public static void clearError(ComboBox<?> combo) {
        combo.setStyle(STYLE_OK);
    }
}
