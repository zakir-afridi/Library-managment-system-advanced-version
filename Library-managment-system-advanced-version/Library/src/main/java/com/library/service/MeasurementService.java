package com.library.service;

import com.library.model.MeasurementUnit;
import com.library.shared.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MeasurementService {

    private static final Logger LOG = LoggerFactory.getLogger(MeasurementService.class);

    public List<MeasurementUnit> getAllUnits() {
        List<MeasurementUnit> list = new ArrayList<>();
        String sql = "SELECT unit_id, name, symbol, status FROM measurement_units ORDER BY name ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MeasurementUnit(
                    rs.getInt("unit_id"),
                    rs.getString("name"),
                    rs.getString("symbol"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            LOG.error("Failed to getAllUnits: {}", e.getMessage(), e);
        }
        return list;
    }

    public boolean addUnit(MeasurementUnit unit) {
        if (unit == null || unit.getName() == null || unit.getName().isBlank()) return false;
        String sql = "INSERT INTO measurement_units (name, symbol, status) VALUES (?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, unit.getName().trim());
            ps.setString(2, unit.getSymbol() != null ? unit.getSymbol().trim() : "");
            ps.setString(3, unit.getStatus() != null ? unit.getStatus() : "Active");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to addUnit: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean updateUnit(MeasurementUnit unit) {
        if (unit == null || unit.getUnitId() <= 0 || unit.getName() == null || unit.getName().isBlank()) return false;
        String sql = "UPDATE measurement_units SET name = ?, symbol = ?, status = ? WHERE unit_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, unit.getName().trim());
            ps.setString(2, unit.getSymbol() != null ? unit.getSymbol().trim() : "");
            ps.setString(3, unit.getStatus() != null ? unit.getStatus() : "Active");
            ps.setInt(4, unit.getUnitId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to updateUnit: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteUnit(int unitId) {
        String sql = "DELETE FROM measurement_units WHERE unit_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, unitId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to deleteUnit {}: {}", unitId, e.getMessage(), e);
            return false;
        }
    }
}
