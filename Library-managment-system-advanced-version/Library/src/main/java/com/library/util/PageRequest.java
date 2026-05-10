package com.library.util;

import com.library.config.AppConfig;

/**
 * Immutable pagination request.
 * All service list methods accept a PageRequest to enforce limits.
 *
 * Usage:
 *   PageRequest.of(1, 10)          // page 1, 10 records
 *   PageRequest.defaultPage()      // page 1, AppConfig default limit
 *   PageRequest.of(page, limit).offset()  // SQL OFFSET value
 */
public final class PageRequest {

    /** Hard ceiling — no single query may return more than this. */
    public static final int MAX_LIMIT = 200;

    public final int page;
    public final int limit;

    private PageRequest(int page, int limit) {
        this.page  = Math.max(1, page);
        // Clamp: minimum 1, maximum MAX_LIMIT
        this.limit = Math.min(Math.max(1, limit), MAX_LIMIT);
    }

    public static PageRequest of(int page, int limit) {
        return new PageRequest(page, limit);
    }

    /** Uses the configured default limit from AppConfig. */
    public static PageRequest defaultPage() {
        return new PageRequest(1, AppConfig.getInstance().getDefaultLimit());
    }

    /** Page N with the configured default limit. */
    public static PageRequest page(int page) {
        return new PageRequest(page, AppConfig.getInstance().getDefaultLimit());
    }

    /** SQL OFFSET = (page - 1) * limit */
    public int offset() {
        return (page - 1) * limit;
    }

    /** Total pages given a total record count. */
    public int totalPages(int totalRecords) {
        return Math.max(1, (int) Math.ceil((double) totalRecords / limit));
    }

    @Override
    public String toString() {
        return "PageRequest{page=" + page + ", limit=" + limit
                + ", offset=" + offset() + "}";
    }
}
