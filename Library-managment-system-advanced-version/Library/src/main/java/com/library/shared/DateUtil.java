package com.library.shared;

import com.library.config.AppConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * SHARED BRANCH — robust date parsing and formatting utilities.
 */
public class DateUtil {

    private static final List<DateTimeFormatter> PARSERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    public static String format(LocalDate date) {
        if (date == null) return "";
        try {
            return date.format(DateTimeFormatter.ofPattern(
                    AppConfig.getInstance().getDateFormat()));
        } catch (Exception e) {
            return date.toString();
        }
    }

    public static String formatIso(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    public static LocalDate parseIso(String s) {
        return parseFlexible(s);
    }

    /**
     * Safely parse a date string across various common formats (ISO, timestamps, slashed, dashed).
     * Returns null if input is null, blank, or unparseable, without throwing exceptions.
     */
    public static LocalDate parseFlexible(String s) {
        if (s == null || s.isBlank()) return null;
        String trimmed = s.trim();

        // If string contains space or 'T', try timestamp parsing first
        if (trimmed.length() >= 10) {
            String datePart = trimmed.substring(0, 10);
            if (datePart.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    return LocalDate.parse(datePart);
                } catch (Exception ignored) {}
            }
        }

        for (DateTimeFormatter fmt : PARSERS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (Exception ignored) {}
            try {
                return LocalDateTime.parse(trimmed, fmt).toLocalDate();
            } catch (Exception ignored) {}
        }
        return null;
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
