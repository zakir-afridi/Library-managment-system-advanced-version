package com.library.repository;

import com.library.exception.DatabaseException;
import com.library.model.BookCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookCopyRepository extends BaseRepository {

    private static final Logger LOG = LoggerFactory.getLogger(BookCopyRepository.class);

    public List<BookCopy> getCopiesByBookId(int bookId) {
        String sql = """
            SELECT bc.*, b.book_name, b.isbn, b.author
            FROM book_copies bc
            JOIN books b ON bc.book_id = b.book_id
            WHERE bc.book_id = ?
            ORDER BY bc.copy_id ASC
        """;
        List<BookCopy> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to retrieve copies for bookId {}: {}", bookId, e.getMessage(), e);
            throw new DatabaseException("Error retrieving physical copies: " + e.getMessage(), e);
        }
        return list;
    }

    public BookCopy getCopyById(int copyId) {
        String sql = """
            SELECT bc.*, b.book_name, b.isbn, b.author
            FROM book_copies bc
            JOIN books b ON bc.book_id = b.book_id
            WHERE bc.copy_id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to retrieve copyId {}: {}", copyId, e.getMessage(), e);
            throw new DatabaseException("Error retrieving copy by ID: " + e.getMessage(), e);
        }
        return null;
    }

    public BookCopy getCopyByCode(String codeOrBarcode) {
        String sql = """
            SELECT bc.*, b.book_name, b.isbn, b.author
            FROM book_copies bc
            JOIN books b ON bc.book_id = b.book_id
            WHERE bc.copy_code = ? OR bc.barcode = ?
            LIMIT 1
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codeOrBarcode);
            ps.setString(2, codeOrBarcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to retrieve copy by code {}: {}", codeOrBarcode, e.getMessage(), e);
            throw new DatabaseException("Error retrieving copy by barcode: " + e.getMessage(), e);
        }
        return null;
    }

    public BookCopy getFirstAvailableCopy(int bookId) {
        String sql = """
            SELECT bc.*, b.book_name, b.isbn, b.author
            FROM book_copies bc
            JOIN books b ON bc.book_id = b.book_id
            WHERE bc.book_id = ? AND bc.status = 'Available'
            ORDER BY bc.copy_id ASC
            LIMIT 1
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to find available copy for bookId {}: {}", bookId, e.getMessage(), e);
            throw new DatabaseException("Error finding available copy: " + e.getMessage(), e);
        }
        return null;
    }

    public int addCopy(BookCopy copy, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO book_copies (book_id, copy_code, barcode, status, condition, shelf_location, price, acquisition_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, copy.getBookId());
            ps.setString(2, copy.getCopyCode());
            ps.setString(3, copy.getBarcode());
            ps.setString(4, copy.getStatus());
            ps.setString(5, copy.getCondition());
            ps.setString(6, copy.getShelfLocation());
            ps.setDouble(7, copy.getPrice());
            ps.setString(8, copy.getAcquisitionDate() != null ? copy.getAcquisitionDate().toString() : LocalDate.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    copy.setCopyId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public boolean updateCopyStatus(int copyId, String status, Connection conn) throws SQLException {
        String sql = "UPDATE book_copies SET status = ? WHERE copy_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, copyId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateCopyCondition(int copyId, String condition, Connection conn) throws SQLException {
        String sql = "UPDATE book_copies SET condition = ? WHERE copy_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, condition);
            ps.setInt(2, copyId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countAvailableCopies(int bookId) {
        String sql = "SELECT COUNT(*) FROM book_copies WHERE book_id = ? AND status = 'Available'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.error("Failed to count copies: {}", e.getMessage());
        }
        return 0;
    }

    private BookCopy mapResultSet(ResultSet rs) throws SQLException {
        BookCopy c = new BookCopy();
        c.setCopyId(rs.getInt("copy_id"));
        c.setBookId(rs.getInt("book_id"));
        c.setCopyCode(rs.getString("copy_code"));
        c.setBarcode(rs.getString("barcode"));
        c.setStatus(rs.getString("status"));
        c.setCondition(rs.getString("condition"));
        c.setShelfLocation(rs.getString("shelf_location"));
        c.setPrice(rs.getDouble("price"));
        String acq = rs.getString("acquisition_date");
        if (acq != null && !acq.isBlank()) {
            try { c.setAcquisitionDate(LocalDate.parse(acq)); } catch (Exception ignored) {}
        }
        try { c.setBookTitle(rs.getString("book_name")); } catch (SQLException ignored) {}
        try { c.setIsbn(rs.getString("isbn")); } catch (SQLException ignored) {}
        try { c.setAuthor(rs.getString("author")); } catch (SQLException ignored) {}
        return c;
    }
}
