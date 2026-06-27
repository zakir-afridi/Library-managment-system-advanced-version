package com.library.service;

import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.model.Book;
import com.library.util.IdGenerator;
import com.library.util.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * BookService — v3 upgrades:
 *  - SLF4J logging
 *  - cover_url column stored and read
 *  - PreparedStatement on every query
 */
public class BookService {

    private static final Logger LOG = LoggerFactory.getLogger(BookService.class);

    private final Map<String, Book> isbnCache = new HashMap<>();
    private long lastCacheRefresh = 0;
    private static final long CACHE_TTL = 300_000;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean addBook(Book book) {
        String bookCode = IdGenerator.next(IdGenerator.Type.BOOK);
        String sql = """
            INSERT INTO books (isbn, book_name, author, publisher, publication_year,
                               edition, category, description, quantity, available_qty,
                               status, shelf_location, cover_image, cover_url, created_at, book_code)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  book.getIsbn());
            ps.setString(2,  book.getBookName());
            ps.setString(3,  book.getAuthor());
            ps.setString(4,  book.getPublisher());
            ps.setInt(5,     book.getPublicationYear());
            ps.setString(6,  book.getEdition());
            ps.setString(7,  book.getCategory());
            ps.setString(8,  book.getDescription());
            ps.setInt(9,     book.getQuantity());
            ps.setInt(10,    book.getAvailableQty());
            ps.setString(11, book.getStatus());
            ps.setString(12, book.getShelfLocation());
            ps.setBytes(13,  book.getCoverImage());
            ps.setString(14, book.getCoverUrl());
            ps.setString(15, LocalDate.now().toString());
            ps.setString(16, bookCode);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceBooks();
            }
            return ok;
        } catch (SQLException e) {
            LOG.error("Error adding book: {}", e.getMessage());
            return false;
        }
    }

    public boolean updateBook(Book book) {
        String sql = """
            UPDATE books SET isbn=?, book_name=?, author=?, publisher=?,
                             publication_year=?, edition=?, category=?, description=?,
                             quantity=?, available_qty=?, status=?, shelf_location=?,
                             cover_image=?, cover_url=?
            WHERE book_id=?
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  book.getIsbn());
            ps.setString(2,  book.getBookName());
            ps.setString(3,  book.getAuthor());
            ps.setString(4,  book.getPublisher());
            ps.setInt(5,     book.getPublicationYear());
            ps.setString(6,  book.getEdition());
            ps.setString(7,  book.getCategory());
            ps.setString(8,  book.getDescription());
            ps.setInt(9,     book.getQuantity());
            ps.setInt(10,    book.getAvailableQty());
            ps.setString(11, book.getStatus());
            ps.setString(12, book.getShelfLocation());
            ps.setBytes(13,  book.getCoverImage());
            ps.setString(14, book.getCoverUrl());
            ps.setInt(15,    book.getBookId());
            invalidateCache();
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error updating book: {}", e.getMessage());
            return false;
        }
    }

    public boolean deleteBook(int bookId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE book_id=?")) {
            ps.setInt(1, bookId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceBooks();
            }
            return ok;
        } catch (SQLException e) {
            LOG.error("Error deleting book: {}", e.getMessage());
            return false;
        }
    }

    public Book getBookById(int bookId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM books WHERE book_id=?")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapBook(rs);
            }
        } catch (SQLException e) {
            LOG.error("Error fetching book {}: {}", bookId, e.getMessage());
        }
        return null;
    }

    public Book getBookByCode(String bookCode) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM books WHERE book_code=?")) {
            ps.setString(1, bookCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapBook(rs);
            }
        } catch (SQLException e) {
            LOG.error("Error fetching book by code: {}", e.getMessage());
        }
        return null;
    }

    public Book getBookByIsbn(String isbn) {
        refreshCacheIfNeeded();
        if (isbnCache.containsKey(isbn)) return isbnCache.get(isbn);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM books WHERE isbn=?")) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book b = mapBook(rs);
                    isbnCache.put(isbn, b);
                    return b;
                }
            }
        } catch (SQLException e) {
            LOG.error("Error fetching book by ISBN: {}", e.getMessage());
        }
        return null;
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    public List<Book> getAllBooks(int page, int pageSize) {
        PageRequest pr = PageRequest.of(page, pageSize);
        List<Book> books = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM books WHERE status != 'Archived' ORDER BY COALESCE(serial_no, book_id) LIMIT ? OFFSET ?")) {
            ps.setInt(1, pr.limit);
            ps.setInt(2, pr.offset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) books.add(mapBook(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching books: {}", e.getMessage());
        }
        return books;
    }

    public int getTotalBooks() {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM books WHERE status != 'Archived'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOG.error("Error counting books: {}", e.getMessage());
        }
        return 0;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<Book> searchBooks(String query) { return searchBooks(query, false); }

    public List<Book> searchBooks(String query, boolean includeArchived) {
        int limit = Math.min(AppConfig.getInstance().getDefaultLimit(), PageRequest.MAX_LIMIT);
        String sc = includeArchived ? "" : "status != 'Archived' AND ";
        String sql = "SELECT * FROM books WHERE " + sc +
                     "(book_name LIKE ? OR author LIKE ? OR isbn LIKE ? OR category LIKE ? OR COALESCE(book_code,'') LIKE ?) " +
                     "ORDER BY COALESCE(serial_no, book_id) LIMIT ?";
        List<Book> books = new ArrayList<>();
        String p = "%" + query + "%";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p); ps.setString(2, p);
            ps.setString(3, p); ps.setString(4, p);
            ps.setString(5, p); ps.setInt(6, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) books.add(mapBook(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error searching books: {}", e.getMessage());
        }
        return books;
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    public boolean archiveBook(int bookId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE books SET status='Archived', archived_date=? WHERE book_id=?")) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, bookId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceBooks();
                SerialNumberService.getInstance().resequenceArchivedBooks();
            }
            return ok;
        } catch (SQLException e) {
            LOG.error("Error archiving book: {}", e.getMessage());
            return false;
        }
    }

    public boolean unarchiveBook(int bookId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE books SET status='Available', archived_date=NULL WHERE book_id=?")) {
            ps.setInt(1, bookId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceBooks();
                SerialNumberService.getInstance().resequenceArchivedBooks();
            }
            return ok;
        } catch (SQLException e) {
            LOG.error("Error unarchiving book: {}", e.getMessage());
            return false;
        }
    }

    public List<Book> getArchivedBooks() {
        List<Book> books = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT * FROM books WHERE status='Archived' ORDER BY COALESCE(serial_no, book_id)")) {
            while (rs.next()) books.add(mapBook(rs));
        } catch (SQLException e) {
            LOG.error("Error fetching archived books: {}", e.getMessage());
        }
        return books;
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    public List<Book> filterByCategory(String category) {
        int limit = Math.min(AppConfig.getInstance().getDefaultLimit(), PageRequest.MAX_LIMIT);
        List<Book> books = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM books WHERE category=? AND status != 'Archived' ORDER BY COALESCE(serial_no, book_id) LIMIT ?")) {
            ps.setString(1, category); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) books.add(mapBook(rs));
            }
        } catch (SQLException e) {
            LOG.error("Error filtering by category: {}", e.getMessage());
        }
        return books;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                 "SELECT DISTINCT category FROM books WHERE category IS NOT NULL ORDER BY category")) {
            while (rs.next()) categories.add(rs.getString(1));
        } catch (SQLException e) {
            LOG.error("Error fetching categories: {}", e.getMessage());
        }
        return categories;
    }

    // ── Availability ──────────────────────────────────────────────────────────

    public boolean decrementAvailability(int bookId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE books SET available_qty = available_qty - 1 WHERE book_id=? AND available_qty > 0")) {
            ps.setInt(1, bookId);
            invalidateCache();
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error decrementing availability: {}", e.getMessage());
            return false;
        }
    }

    public boolean incrementAvailability(int bookId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE books SET available_qty = available_qty + 1 WHERE book_id=?")) {
            ps.setInt(1, bookId);
            invalidateCache();
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Error incrementing availability: {}", e.getMessage());
            return false;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Book mapBook(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setBookId(rs.getInt("book_id"));
        b.setIsbn(rs.getString("isbn"));
        b.setBookName(rs.getString("book_name"));
        b.setAuthor(rs.getString("author"));
        b.setPublisher(rs.getString("publisher"));
        b.setPublicationYear(rs.getInt("publication_year"));
        b.setEdition(rs.getString("edition"));
        b.setCategory(rs.getString("category"));
        b.setDescription(rs.getString("description"));
        b.setQuantity(rs.getInt("quantity"));
        b.setAvailableQty(rs.getInt("available_qty"));
        b.setStatus(rs.getString("status"));
        b.setShelfLocation(rs.getString("shelf_location"));
        b.setCoverImage(rs.getBytes("cover_image"));
        try { b.setCoverUrl(rs.getString("cover_url")); } catch (SQLException ignored) {}
        String created = rs.getString("created_at");
        if (created != null) b.setCreatedAt(LocalDate.parse(created));
        try { b.setBookCode(rs.getString("book_code")); } catch (SQLException ignored) {}
        try { b.setSerialNo(rs.getInt("serial_no")); }   catch (SQLException ignored) {}
        return b;
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheRefresh > CACHE_TTL) { isbnCache.clear(); lastCacheRefresh = now; }
    }

    private void invalidateCache() { isbnCache.clear(); lastCacheRefresh = 0; }
}
