package com.library.util;

import java.time.LocalDate;

/**
 * Utility bridge forwarding to com.library.shared.DateUtil.
 */
public final class DateUtil {

    private DateUtil() {}

    public static String format(LocalDate date) {
        return com.library.shared.DateUtil.format(date);
    }

    public static String formatIso(LocalDate date) {
        return com.library.shared.DateUtil.formatIso(date);
    }

    public static LocalDate parseIso(String s) {
        return com.library.shared.DateUtil.parseIso(s);
    }

    public static LocalDate parseFlexible(String s) {
        return com.library.shared.DateUtil.parseFlexible(s);
    }

    public static long daysUntil(LocalDate date) {
        return com.library.shared.DateUtil.daysUntil(date);
    }

    public static long daysSince(LocalDate date) {
        return com.library.shared.DateUtil.daysSince(date);
    }
}
