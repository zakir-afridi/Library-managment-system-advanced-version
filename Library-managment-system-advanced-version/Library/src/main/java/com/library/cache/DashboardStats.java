package com.library.cache;

/**
 * Immutable snapshot of dashboard KPI statistics.
 * Built by DashboardCache and consumed by DashboardController.
 */
public class DashboardStats {

    public final int    totalBooks;
    public final int    availableBooks;
    public final int    issuedBooks;
    public final int    overdueBooks;
    public final int    totalMembers;
    public final int    activeMembers;
    public final int    issuedToday;
    public final int    reservations;
    public final double totalFinesCollected;
    public final double pendingFines;

    public DashboardStats(int totalBooks, int availableBooks, int issuedBooks,
                          int overdueBooks, int totalMembers, int activeMembers,
                          int issuedToday, int reservations,
                          double totalFinesCollected, double pendingFines) {
        this.totalBooks           = totalBooks;
        this.availableBooks       = availableBooks;
        this.issuedBooks          = issuedBooks;
        this.overdueBooks         = overdueBooks;
        this.totalMembers         = totalMembers;
        this.activeMembers        = activeMembers;
        this.issuedToday          = issuedToday;
        this.reservations         = reservations;
        this.totalFinesCollected  = totalFinesCollected;
        this.pendingFines         = pendingFines;
    }

    /** Trend indicator string for UI display. */
    public static String trend(int current, int previous) {
        if (current > previous) return "↑";
        if (current < previous) return "↓";
        return "→";
    }
}
