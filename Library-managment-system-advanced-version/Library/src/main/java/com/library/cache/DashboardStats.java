package com.library.cache;

import java.util.Map;

/**
 * Immutable snapshot of dashboard Business Intelligence & KPI statistics.
 * Built by DashboardCache and consumed by DashboardController.
 */
public class DashboardStats {

    public final int    totalBooks;
    public final int    totalCopies;
    public final int    availableBooks;
    public final int    issuedBooks;
    public final int    overdueBooks;
    public final int    totalMembers;
    public final int    activeMembers;
    public final int    issuedToday;
    public final int    reservations;
    public final double totalFinesCollected;
    public final double pendingFines;
    public final double totalMemberDues;
    public final int    overdueAccountsCount;
    public final double estimatedStockValue;
    public final Map<String, Integer> categoryDistribution;
    public final Map<String, Double> monthlyRevenue;

    public DashboardStats(int totalBooks, int totalCopies, int availableBooks, int issuedBooks,
                          int overdueBooks, int totalMembers, int activeMembers,
                          int issuedToday, int reservations,
                          double totalFinesCollected, double pendingFines,
                          double totalMemberDues, int overdueAccountsCount,
                          double estimatedStockValue,
                          Map<String, Integer> categoryDistribution,
                          Map<String, Double> monthlyRevenue) {
        this.totalBooks            = totalBooks;
        this.totalCopies           = totalCopies;
        this.availableBooks        = availableBooks;
        this.issuedBooks           = issuedBooks;
        this.overdueBooks          = overdueBooks;
        this.totalMembers          = totalMembers;
        this.activeMembers         = activeMembers;
        this.issuedToday           = issuedToday;
        this.reservations          = reservations;
        this.totalFinesCollected   = totalFinesCollected;
        this.pendingFines          = pendingFines;
        this.totalMemberDues       = totalMemberDues;
        this.overdueAccountsCount  = overdueAccountsCount;
        this.estimatedStockValue   = estimatedStockValue;
        this.categoryDistribution  = categoryDistribution != null ? categoryDistribution : Map.of();
        this.monthlyRevenue        = monthlyRevenue != null ? monthlyRevenue : Map.of();
    }

    /** Trend indicator string for UI display. */
    public static String trend(int current, int previous) {
        if (current > previous) return "↑";
        if (current < previous) return "↓";
        return "→";
    }
}
