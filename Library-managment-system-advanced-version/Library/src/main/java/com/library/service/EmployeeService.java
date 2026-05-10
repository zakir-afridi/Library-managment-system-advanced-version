package com.library.service;

import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.model.Employee;
import com.library.util.IdGenerator;
import com.library.util.PageRequest;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class EmployeeService {

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean addEmployee(Employee e) {
        // Auto-assign structured ID: EP00000001
        if (e.getEmployeeCode() == null || e.getEmployeeCode().isBlank())
            e.setEmployeeCode(IdGenerator.next(IdGenerator.Type.EMPLOYEE));

        String sql = """
            INSERT INTO employees (employee_code, name, designation, department,
                contact, email, cnic, address, join_date, status, salary, notes, profile_pic)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getEmployeeCode());
            ps.setString(2, e.getName());
            ps.setString(3, e.getDesignation());
            ps.setString(4, e.getDepartment());
            ps.setString(5, e.getContact());
            ps.setString(6, e.getEmail() != null ? e.getEmail() : "");
            ps.setString(7, e.getCnic());
            ps.setString(8, e.getAddress());
            ps.setString(9, e.getJoinDate() != null ? e.getJoinDate().toString() : LocalDate.now().toString());
            ps.setString(10, e.getStatus());
            ps.setDouble(11, e.getSalary());
            ps.setString(12, e.getNotes());
            ps.setBytes(13, e.getProfilePic());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) SerialNumberService.getInstance().resequenceEmployees();
            return ok;
        } catch (SQLException ex) {
            System.err.println("Error adding employee: " + ex.getMessage());
            return false;
        }
    }

    public boolean updateEmployee(Employee e) {
        String sql = """
            UPDATE employees SET name=?, designation=?, department=?, contact=?,
                email=?, cnic=?, address=?, status=?, salary=?, notes=?, profile_pic=?
            WHERE emp_id=?
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getDesignation());
            ps.setString(3, e.getDepartment());
            ps.setString(4, e.getContact());
            ps.setString(5, e.getEmail() != null ? e.getEmail() : "");
            ps.setString(6, e.getCnic());
            ps.setString(7, e.getAddress());
            ps.setString(8, e.getStatus());
            ps.setDouble(9, e.getSalary());
            ps.setString(10, e.getNotes());
            ps.setBytes(11, e.getProfilePic());
            ps.setInt(12, e.getEmpId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Error updating employee: " + ex.getMessage());
            return false;
        }
    }

    public boolean deleteEmployee(int empId) {
        String sql = "DELETE FROM employees WHERE emp_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) SerialNumberService.getInstance().resequenceEmployees();
            return ok;
        } catch (SQLException e) {
            System.err.println("Error deleting employee: " + e.getMessage());
            return false;
        }
    }

    public Employee getEmployeeById(int empId) {
        String sql = "SELECT * FROM employees WHERE emp_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching employee: " + e.getMessage());
        }
        return null;
    }

    /** Lookup by structured code e.g. EP00000001 */
    public Employee getEmployeeByCode(String code) {
        String sql = "SELECT * FROM employees WHERE employee_code=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching employee by code: " + e.getMessage());
        }
        return null;
    }

    // ── Pagination — ordered by serial_no ────────────────────────────────────

    public List<Employee> getAllEmployees(int page, int pageSize) {
        PageRequest pr = PageRequest.of(page, pageSize);
        String sql = "SELECT * FROM employees WHERE status != 'Archived' ORDER BY COALESCE(serial_no, emp_id) LIMIT ? OFFSET ?";
        List<Employee> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pr.limit);
            ps.setInt(2, pr.offset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching employees: " + e.getMessage());
        }
        return list;
    }

    public int getTotalEmployees() {
        String sql = "SELECT COUNT(*) FROM employees WHERE status != 'Archived'";
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting employees: " + e.getMessage());
        }
        return 0;
    }

    // ── Archive — resequences both lists ─────────────────────────────────────

    public boolean archiveEmployee(int empId) {
        String sql = "UPDATE employees SET status='Archived', archived_date=? WHERE emp_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, empId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                SerialNumberService.getInstance().resequenceEmployees();
                SerialNumberService.getInstance().resequenceArchivedEmployees();
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error archiving employee: " + e.getMessage());
            return false;
        }
    }

    public boolean unarchiveEmployee(int empId) {
        String sql = "UPDATE employees SET status='Active', archived_date=NULL WHERE emp_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, empId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                SerialNumberService.getInstance().resequenceEmployees();
                SerialNumberService.getInstance().resequenceArchivedEmployees();
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error unarchiving employee: " + e.getMessage());
            return false;
        }
    }

    public List<Employee> getArchivedEmployees(int page, int pageSize) {
        PageRequest pr = PageRequest.of(page, pageSize);
        String sql = "SELECT * FROM employees WHERE status='Archived' ORDER BY COALESCE(serial_no, emp_id) LIMIT ? OFFSET ?";
        List<Employee> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pr.limit);
            ps.setInt(2, pr.offset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching archived employees: " + e.getMessage());
        }
        return list;
    }

    // ── Search — includes employee_code ──────────────────────────────────────

    public List<Employee> searchEmployees(String query) {
        return searchEmployees(query, false);
    }

    public List<Employee> searchEmployees(String query, boolean includeArchived) {
        int limit = Math.min(AppConfig.getInstance().getDefaultLimit(), PageRequest.MAX_LIMIT);
        String statusClause = includeArchived ? "" : "status != 'Archived' AND ";
        // Search by EP code, name, email, contact
        String sql = "SELECT * FROM employees WHERE " + statusClause +
                     "(name LIKE ? OR employee_code LIKE ? OR email LIKE ? OR contact LIKE ?) " +
                     "ORDER BY COALESCE(serial_no, emp_id) LIMIT ?";
        List<Employee> list = new ArrayList<>();
        String p = "%" + query + "%";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p); ps.setString(2, p);
            ps.setString(3, p); ps.setString(4, p);
            ps.setInt(5, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching employees: " + e.getMessage());
        }
        return list;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmpId(rs.getInt("emp_id"));
        e.setEmployeeCode(rs.getString("employee_code"));
        e.setName(rs.getString("name"));
        e.setDesignation(rs.getString("designation"));
        e.setDepartment(rs.getString("department"));
        e.setContact(rs.getString("contact"));
        e.setEmail(rs.getString("email"));
        e.setCnic(rs.getString("cnic"));
        e.setAddress(rs.getString("address"));
        String jd = rs.getString("join_date");
        if (jd != null) e.setJoinDate(LocalDate.parse(jd));
        e.setStatus(rs.getString("status"));
        e.setSalary(rs.getDouble("salary"));
        e.setNotes(rs.getString("notes"));
        e.setProfilePic(rs.getBytes("profile_pic"));
        String ad = rs.getString("archived_date");
        if (ad != null) e.setArchivedDate(LocalDate.parse(ad));
        try { e.setSerialNo(rs.getInt("serial_no")); } catch (SQLException ignored) {}
        return e;
    }
}
