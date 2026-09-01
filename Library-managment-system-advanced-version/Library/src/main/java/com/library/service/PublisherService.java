package com.library.service;

import com.library.model.Publisher;
import com.library.shared.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PublisherService {

    private static final Logger LOG = LoggerFactory.getLogger(PublisherService.class);

    public List<Publisher> getAllPublishers() {
        List<Publisher> list = new ArrayList<>();
        String sql = "SELECT publisher_id, name, contact, address, status FROM publishers ORDER BY name ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Publisher(
                    rs.getInt("publisher_id"),
                    rs.getString("name"),
                    rs.getString("contact"),
                    rs.getString("address"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            LOG.error("Failed to getAllPublishers: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<String> getActivePublisherNames() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT name FROM publishers WHERE status = 'Active' ORDER BY name ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            LOG.error("Failed to getActivePublisherNames: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<Publisher> searchPublishers(String query) {
        if (query == null || query.isBlank()) return getAllPublishers();
        List<Publisher> list = new ArrayList<>();
        String sql = "SELECT publisher_id, name, contact, address, status FROM publishers WHERE name LIKE ? OR contact LIKE ? OR address LIKE ? ORDER BY name ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String p = "%" + query.trim() + "%";
            ps.setString(1, p);
            ps.setString(2, p);
            ps.setString(3, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Publisher(
                        rs.getInt("publisher_id"),
                        rs.getString("name"),
                        rs.getString("contact"),
                        rs.getString("address"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to searchPublishers: {}", e.getMessage(), e);
        }
        return list;
    }

    public boolean addPublisher(Publisher pub) {
        if (pub == null || pub.getName() == null || pub.getName().isBlank()) return false;
        String sql = "INSERT INTO publishers (name, contact, address, status) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pub.getName().trim());
            ps.setString(2, pub.getContact() != null ? pub.getContact().trim() : "");
            ps.setString(3, pub.getAddress() != null ? pub.getAddress().trim() : "");
            ps.setString(4, pub.getStatus() != null ? pub.getStatus() : "Active");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to addPublisher: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean updatePublisher(Publisher pub) {
        if (pub == null || pub.getPublisherId() <= 0 || pub.getName() == null || pub.getName().isBlank()) return false;
        String sql = "UPDATE publishers SET name = ?, contact = ?, address = ?, status = ? WHERE publisher_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pub.getName().trim());
            ps.setString(2, pub.getContact() != null ? pub.getContact().trim() : "");
            ps.setString(3, pub.getAddress() != null ? pub.getAddress().trim() : "");
            ps.setString(4, pub.getStatus() != null ? pub.getStatus() : "Active");
            ps.setInt(5, pub.getPublisherId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to updatePublisher: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean toggleStatus(int publisherId, String currentStatus) {
        String newStatus = "Active".equalsIgnoreCase(currentStatus) ? "Inactive" : "Active";
        String sql = "UPDATE publishers SET status = ? WHERE publisher_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, publisherId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to toggleStatus for publisher {}: {}", publisherId, e.getMessage(), e);
            return false;
        }
    }

    public boolean deletePublisher(int publisherId) {
        String sql = "DELETE FROM publishers WHERE publisher_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, publisherId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to deletePublisher {}: {}", publisherId, e.getMessage(), e);
            return false;
        }
    }
}
