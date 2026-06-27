package com.library.service;

import com.library.api.OpenLibraryClient;
import com.library.database.DatabaseConnection;
import com.library.model.BookMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Caching layer between OpenLibraryClient and the database.
 *
 * Stores fetched metadata in the {@code book_metadata_cache} table
 * with a 30-day TTL. On cache hit: returns DB record (no network).
 * On cache miss: fetches from API, stores result, returns it.
 * On API failure: returns empty (never throws).
 */
public class BookMetadataCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(BookMetadataCacheService.class);

    /** Cache TTL in days. */
    private static final int TTL_DAYS = 30;

    private static BookMetadataCacheService instance;

    private BookMetadataCacheService() {}

    public static synchronized BookMetadataCacheService getInstance() {
        if (instance == null) instance = new BookMetadataCacheService();
        return instance;
    }

    /**
     * Get metadata for an ISBN — from DB cache if fresh, otherwise from Open Library API.
     */
    public Optional<BookMetadata> getMetadata(String isbn) {
        if (isbn == null || isbn.isBlank()) return Optional.empty();
        String clean = isbn.replaceAll("[\\s\\-]", "");

        // Try cache first
        Optional<BookMetadata> cached = fromCache(clean);
        if (cached.isPresent()) {
            LOG.debug("Book metadata cache HIT: {}", clean);
            return cached;
        }

        // Fetch from API
        LOG.debug("Book metadata cache MISS: {} — fetching from Open Library", clean);
        Optional<BookMetadata> fetched = OpenLibraryClient.fetchByIsbn(clean);
        fetched.ifPresent(meta -> store(meta));
        return fetched;
    }

    /** Force-refresh cache entry for a given ISBN (ignores TTL). */
    public Optional<BookMetadata> refresh(String isbn) {
        if (isbn == null || isbn.isBlank()) return Optional.empty();
        String clean = isbn.replaceAll("[\\s\\-]", "");
        evict(clean);
        return getMetadata(clean);
    }

    /** Remove a cache entry. */
    public void evict(String isbn) {
        String sql = "DELETE FROM book_metadata_cache WHERE isbn=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, isbn);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warn("Cache evict failed: {}", e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Optional<BookMetadata> fromCache(String isbn) {
        String sql = """
            SELECT isbn, title, author, publisher, publish_date, page_count,
                   description, cover_url, category, fetched_at
            FROM book_metadata_cache
            WHERE isbn = ?
              AND fetched_at >= datetime('now', '-' || ? || ' days')
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, isbn);
            ps.setInt(2, TTL_DAYS);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BookMetadata m = new BookMetadata();
                    m.setIsbn(rs.getString("isbn"));
                    m.setTitle(rs.getString("title"));
                    m.setAuthor(rs.getString("author"));
                    m.setPublisher(rs.getString("publisher"));
                    m.setPublishDate(rs.getString("publish_date"));
                    m.setPageCount(rs.getInt("page_count"));
                    m.setDescription(rs.getString("description"));
                    m.setCoverUrl(rs.getString("cover_url"));
                    m.setCategory(rs.getString("category"));
                    return Optional.of(m);
                }
            }
        } catch (SQLException e) {
            LOG.warn("Cache read failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private void store(BookMetadata m) {
        String sql = """
            INSERT OR REPLACE INTO book_metadata_cache
                (isbn, title, author, publisher, publish_date, page_count,
                 description, cover_url, category, fetched_at)
            VALUES (?,?,?,?,?,?,?,?,?, datetime('now'))
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getIsbn());
            ps.setString(2, m.getTitle());
            ps.setString(3, m.getAuthor());
            ps.setString(4, m.getPublisher());
            ps.setString(5, m.getPublishDate());
            ps.setInt(6, m.getPageCount());
            ps.setString(7, m.getDescription());
            ps.setString(8, m.getCoverUrl());
            ps.setString(9, m.getCategory());
            ps.executeUpdate();
            LOG.debug("Cached metadata for ISBN {}: {}", m.getIsbn(), m.getTitle());
        } catch (SQLException e) {
            LOG.warn("Cache store failed for ISBN {}: {}", m.getIsbn(), e.getMessage());
        }
    }
}
