package com.library.dashboard;

import com.library.cache.DashboardCache;
import com.library.cache.DashboardStats;
import com.library.service.TransactionService;

import java.util.Map;

/**
 * DASHBOARD BRANCH — service layer.
 */
public class DashboardService {

    private final TransactionService txService = new TransactionService();

    public DashboardStats getStats() {
        return DashboardCache.getInstance().getStats();
    }

    public void invalidate() {
        DashboardCache.getInstance().invalidate();
    }

    /** Monthly issued counts for BarChart (last N months). */
    public Map<String, Integer> getMonthlyIssued(int months) {
        return txService.getMonthlyIssuedStats(months);
    }

    /** Daily issued vs returned for LineChart (last N days). */
    public Map<String, int[]> getDailyStats(int days) {
        return txService.getDailyStats(days);
    }
}
