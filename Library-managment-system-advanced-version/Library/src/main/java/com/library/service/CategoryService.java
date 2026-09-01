package com.library.service;

import com.library.model.Category;
import com.library.shared.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryService.class);

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id, name, description, status FROM categories ORDER BY name ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(
                    rs.getInt("category_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            LOG.error("Failed to getAllCategories: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<String> getActiveCategoryNames() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT name FROM categories WHERE status = 'Active' ORDER BY name ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            LOG.error("Failed to getActiveCategoryNames: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<Category> searchCategories(String query) {
        if (query == null || query.isBlank()) return getAllCategories();
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id, name, description, status FROM categories WHERE name LIKE ? OR description LIKE ? ORDER BY name ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String p = "%" + query.trim() + "%";
            ps.setString(1, p);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Category(
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to searchCategories: {}", e.getMessage(), e);
        }
        return list;
    }

    public boolean addCategory(Category cat) {
        if (cat == null || cat.getName() == null || cat.getName().isBlank()) return false;
        String sql = "INSERT INTO categories (name, description, status) VALUES (?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getName().trim());
            ps.setString(2, cat.getDescription() != null ? cat.getDescription().trim() : "");
            ps.setString(3, cat.getStatus() != null ? cat.getStatus() : "Active");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to addCategory: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean updateCategory(Category cat) {
        if (cat == null || cat.getCategoryId() <= 0 || cat.getName() == null || cat.getName().isBlank()) return false;
        String sql = "UPDATE categories SET name = ?, description = ?, status = ? WHERE category_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getName().trim());
            ps.setString(2, cat.getDescription() != null ? cat.getDescription().trim() : "");
            ps.setString(3, cat.getStatus() != null ? cat.getStatus() : "Active");
            ps.setInt(4, cat.getCategoryId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to updateCategory: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean toggleStatus(int categoryId, String currentStatus) {
        String newStatus = "Active".equalsIgnoreCase(currentStatus) ? "Inactive" : "Active";
        String sql = "UPDATE categories SET status = ? WHERE category_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, categoryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to toggleStatus for category {}: {}", categoryId, e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteCategory(int categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to deleteCategory {}: {}", categoryId, e.getMessage(), e);
            return false;
        }
    }
}
