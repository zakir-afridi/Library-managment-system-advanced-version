package com.library.issuing;

import com.library.config.AppConfig;
import com.library.model.Transaction;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ISSUING BRANCH — fine calculation utility.
 * Centralises fine logic so it can be called without a full Transaction object.
 */
public class FineCalculator {

    /**
     * Calculate fine for a given due date and return/check date.
     * Applies grace period and daily rate from AppConfig.
     */
    public static double calculate(LocalDate dueDate, LocalDate checkDate) {
        if (dueDate == null || checkDate == null) return 0.0;
        long overdueDays = ChronoUnit.DAYS.between(dueDate, checkDate);
        int grace = AppConfig.getInstance().getGracePeriod();
        if (overdueDays <= grace) return 0.0;
        return (overdueDays - grace) * AppConfig.getInstance().getFineRate();
    }

    /** Calculate fine for a transaction using today as the check date. */
    public static double calculateNow(Transaction tx) {
        return tx.calculateFine();
    }

    /** Returns true if the due date has passed today (ignoring grace period). */
    public static boolean isOverdue(LocalDate dueDate) {
        return dueDate != null && LocalDate.now().isAfter(dueDate);
    }
}
