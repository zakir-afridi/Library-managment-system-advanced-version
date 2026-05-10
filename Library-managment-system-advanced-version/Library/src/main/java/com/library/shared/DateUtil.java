package com.library.shared;

import com.library.config.AppConfig;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * SHARED BRANCH — date formatting utilities.
 */
public class DateUtil {

    public static String format(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern(
                AppConfig.getInstance().getDateFormat()));
    }

    public static String formatIso(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    public static LocalDate parseIso(String s) {
        try { return s != null && !s.isBlank() ? LocalDate.parse(s) : null; }
        catch (Exception e) { return null; }
    }

    public static long daysUntil(LocalDate date) {
        if (date == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    public static long daysSince(LocalDate date) {
        if (date == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now());
    }
}
