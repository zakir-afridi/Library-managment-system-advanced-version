package com.library.service;

import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.model.Member;
import com.library.util.IdGenerator;
import com.library.util.PageRequest;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class MemberService {

    private final Map<String, Member> studentIdCache = new HashMap<>();
    private long lastCacheRefresh = 0;
    private static final long CACHE_TTL = 300_000;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public boolean addMember(Member member) {
        if (member.getStudentId() == null || member.getStudentId().isBlank())
            member.setStudentId(generateStudentId());
        if (member.getLibraryCardNumber() == null || member.getLibraryCardNumber().isBlank())
            member.setLibraryCardNumber("LIB-" + System.currentTimeMillis());
        if (member.getEmail() == null) member.setEmail("");

        // Auto-assign structured member code: ST00000001
        String memberCode = IdGenerator.next(IdGenerator.Type.STUDENT);

        String sql = """
            INSERT INTO members (student_id, name, fname, cnic, date_of_birth, gender,
                contact, email, emergency_contact, blood_group, address, city, province,
                postal_code, country, department, program, semester, session,
                admission_date, status, library_card_no, book_limit, membership_type,
                membership_expiry, fine_balance, notes, profile_pic, registration_date,
                member_code)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, member.getStudentId());
            ps.setString(2, member.getName());
            ps.setString(3, member.getFname());
            ps.setString(4, member.getCnic());
            ps.setString(5, member.getDateOfBirth() != null ? member.getDateOfBirth().toString() : null);
            ps.setString(6, member.getGender());
            ps.setString(7, member.getContact());
            ps.setString(8, member.getEmail());
            ps.setString(9, member.getEmergencyContact());
            ps.setString(10, member.getBloodGroup());
            ps.setString(11, member.getAddress());
            ps.setString(12, member.getCity());
            ps.setString(13, member.getProvince());
            ps.setString(14, member.getPostalCode());
            ps.setString(15, member.getCountry());
            ps.setString(16, member.getDepartment());
            ps.setString(17, member.getProgram());
            ps.setString(18, member.getSemester());
            ps.setString(19, member.getSession());
            ps.setString(20, member.getAdmissionDate() != null ? member.getAdmissionDate().toString() : null);
            ps.setString(21, member.getStatus());
            ps.setString(22, member.getLibraryCardNumber());
            ps.setInt(23, member.getBookLimit());
            ps.setString(24, member.getMembershipType());
            ps.setString(25, member.getMembershipExpiry() != null ? member.getMembershipExpiry().toString() : null);
            ps.setDouble(26, member.getFineBalance());
            ps.setString(27, member.getNotes());
            ps.setBytes(28, member.getProfilePic());
            ps.setString(29, LocalDate.now().toString());
            ps.setString(30, memberCode);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceMembers();
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error adding member: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMember(Member member) {
        String sql = """
            UPDATE members SET name=?, fname=?, cnic=?, date_of_birth=?, gender=?,
                contact=?, email=?, emergency_contact=?, blood_group=?, address=?,
                city=?, province=?, postal_code=?, country=?, department=?, program=?,
                semester=?, session=?, admission_date=?, status=?, book_limit=?,
                membership_type=?, membership_expiry=?, fine_balance=?, notes=?,
                profile_pic=?
            WHERE std_id=?
        """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getFname());
            ps.setString(3, member.getCnic());
            ps.setString(4, member.getDateOfBirth() != null ? member.getDateOfBirth().toString() : null);
            ps.setString(5, member.getGender());
            ps.setString(6, member.getContact());
            ps.setString(7, member.getEmail());
            ps.setString(8, member.getEmergencyContact());
            ps.setString(9, member.getBloodGroup());
            ps.setString(10, member.getAddress());
            ps.setString(11, member.getCity());
            ps.setString(12, member.getProvince());
            ps.setString(13, member.getPostalCode());
            ps.setString(14, member.getCountry());
            ps.setString(15, member.getDepartment());
            ps.setString(16, member.getProgram());
            ps.setString(17, member.getSemester());
            ps.setString(18, member.getSession());
            ps.setString(19, member.getAdmissionDate() != null ? member.getAdmissionDate().toString() : null);
            ps.setString(20, member.getStatus());
            ps.setInt(21, member.getBookLimit());
            ps.setString(22, member.getMembershipType());
            ps.setString(23, member.getMembershipExpiry() != null ? member.getMembershipExpiry().toString() : null);
            ps.setDouble(24, member.getFineBalance());
            ps.setString(25, member.getNotes());
            ps.setBytes(26, member.getProfilePic());
            ps.setInt(27, member.getStdId());
            invalidateCache();
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating member: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteMember(int stdId) {
        String sql = "DELETE FROM members WHERE std_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stdId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceMembers();
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error deleting member: " + e.getMessage());
            return false;
        }
    }

    public Member getMemberById(int stdId) {
        String sql = "SELECT * FROM members WHERE std_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stdId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMember(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching member: " + e.getMessage());
        }
        return null;
    }

    public Member getMemberByStudentId(String studentId) {
        refreshCacheIfNeeded();
        if (studentIdCache.containsKey(studentId)) return studentIdCache.get(studentId);
        String sql = "SELECT * FROM members WHERE student_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Member m = mapMember(rs);
                    studentIdCache.put(studentId, m);
                    return m;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching member by student ID: " + e.getMessage());
        }
        return null;
    }

    /** Lookup by structured code e.g. ST00000001 */
    public Member getMemberByCode(String memberCode) {
        String sql = "SELECT * FROM members WHERE member_code=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, memberCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMember(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching member by code: " + e.getMessage());
        }
        return null;
    }

    // ── Pagination — ordered by serial_no ────────────────────────────────────

    public List<Member> getAllMembers(int page, int pageSize) {
        PageRequest pr = PageRequest.of(page, pageSize);
        String sql = "SELECT * FROM members WHERE status != 'Archived' ORDER BY COALESCE(serial_no, std_id) LIMIT ? OFFSET ?";
        List<Member> members = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pr.limit);
            ps.setInt(2, pr.offset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapMember(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching members: " + e.getMessage());
        }
        return members;
    }

    public List<Member> getArchivedMembers(int page, int pageSize) {
        PageRequest pr = PageRequest.of(page, pageSize);
        String sql = "SELECT * FROM members WHERE status='Archived' ORDER BY COALESCE(serial_no, std_id) LIMIT ? OFFSET ?";
        List<Member> members = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pr.limit);
            ps.setInt(2, pr.offset());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapMember(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching archived members: " + e.getMessage());
        }
        return members;
    }

    public int getTotalMembers() {
        String sql = "SELECT COUNT(*) FROM members WHERE status != 'Archived'";
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting members: " + e.getMessage());
        }
        return 0;
    }

    // ── Search — includes member_code in search ───────────────────────────────

    public List<Member> searchMembers(String query) {
        return searchMembers(query, false);
    }

    public List<Member> searchMembers(String query, boolean includeArchived) {
        int limit = Math.min(AppConfig.getInstance().getDefaultLimit(), PageRequest.MAX_LIMIT);
        String statusClause = includeArchived ? "" : "status != 'Archived' AND ";
        // Search by member_code (ST00000001), student_id, name, email, contact
        String sql = "SELECT * FROM members WHERE " + statusClause +
                     "(name LIKE ? OR student_id LIKE ? OR email LIKE ? OR contact LIKE ? OR COALESCE(member_code,'') LIKE ?) " +
                     "ORDER BY COALESCE(serial_no, std_id) LIMIT ?";
        List<Member> members = new ArrayList<>();
        String p = "%" + query + "%";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p); ps.setString(2, p);
            ps.setString(3, p); ps.setString(4, p);
            ps.setString(5, p); ps.setInt(6, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapMember(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching members: " + e.getMessage());
        }
        return members;
    }

    // ── Archive — resequences both lists ─────────────────────────────────────

    public boolean archiveMember(int stdId) {
        String sql = "UPDATE members SET status='Archived', archived_date=? WHERE std_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, stdId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceMembers();
                SerialNumberService.getInstance().resequenceArchivedMembers();
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error archiving member: " + e.getMessage());
            return false;
        }
    }

    public boolean unarchiveMember(int stdId) {
        String sql = "UPDATE members SET status='Active', archived_date=NULL WHERE std_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stdId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                invalidateCache();
                SerialNumberService.getInstance().resequenceMembers();
                SerialNumberService.getInstance().resequenceArchivedMembers();
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error unarchiving member: " + e.getMessage());
            return false;
        }
    }

    // ── Fine ──────────────────────────────────────────────────────────────────

    public boolean addFine(int stdId, double amount) {
        String sql = "UPDATE members SET fine_balance = fine_balance + ? WHERE std_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, amount); ps.setInt(2, stdId);
            invalidateCache();
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding fine: " + e.getMessage());
            return false;
        }
    }

    public boolean clearFine(int stdId) {
        String sql = "UPDATE members SET fine_balance = 0 WHERE std_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stdId);
            invalidateCache();
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error clearing fine: " + e.getMessage());
            return false;
        }
    }

    public List<Member> getMembersWithOutstandingFines() {
        int limit = Math.min(AppConfig.getInstance().getDefaultLimit(), PageRequest.MAX_LIMIT);
        String sql = "SELECT * FROM members WHERE fine_balance > 0 ORDER BY fine_balance DESC LIMIT ?";
        List<Member> members = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapMember(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching members with fines: " + e.getMessage());
        }
        return members;
    }

    public List<Member> filterByDepartment(String department) {
        int limit = Math.min(AppConfig.getInstance().getDefaultLimit(), PageRequest.MAX_LIMIT);
        String sql = "SELECT * FROM members WHERE department=? AND status != 'Archived' ORDER BY COALESCE(serial_no, std_id) LIMIT ?";
        List<Member> members = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, department); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapMember(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error filtering by department: " + e.getMessage());
        }
        return members;
    }

    // ── Student ID Generation ─────────────────────────────────────────────────

    public String generateStudentId() {
        int year = LocalDate.now().getYear();
        String prefix = "LIB-" + year + "-";
        String sql = "SELECT MAX(student_id) FROM members WHERE student_id LIKE ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) {
                    String last = rs.getString(1);
                    int seq = Integer.parseInt(last.substring(last.lastIndexOf('-') + 1));
                    return prefix + String.format("%04d", seq + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating student ID: " + e.getMessage());
        }
        return prefix + "0001";
    }

    public int getActiveBookCount(int stdId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE member_id=? AND status='Issued'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stdId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting active books: " + e.getMessage());
        }
        return 0;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Member mapMember(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setStdId(rs.getInt("std_id"));
        m.setStudentId(rs.getString("student_id"));
        m.setName(rs.getString("name"));
        m.setFname(rs.getString("fname"));
        m.setCnic(rs.getString("cnic"));
        String dob = rs.getString("date_of_birth");
        if (dob != null) m.setDateOfBirth(LocalDate.parse(dob));
        m.setGender(rs.getString("gender"));
        m.setContact(rs.getString("contact"));
        m.setEmail(rs.getString("email"));
        m.setEmergencyContact(rs.getString("emergency_contact"));
        m.setBloodGroup(rs.getString("blood_group"));
        m.setAddress(rs.getString("address"));
        m.setCity(rs.getString("city"));
        m.setProvince(rs.getString("province"));
        m.setPostalCode(rs.getString("postal_code"));
        m.setCountry(rs.getString("country"));
        m.setDepartment(rs.getString("department"));
        m.setProgram(rs.getString("program"));
        m.setSemester(rs.getString("semester"));
        m.setSession(rs.getString("session"));
        String adm = rs.getString("admission_date");
        if (adm != null) m.setAdmissionDate(LocalDate.parse(adm));
        m.setStatus(rs.getString("status"));
        m.setLibraryCardNumber(rs.getString("library_card_no"));
        m.setBookLimit(rs.getInt("book_limit"));
        m.setMembershipType(rs.getString("membership_type"));
        String exp = rs.getString("membership_expiry");
        if (exp != null) m.setMembershipExpiry(LocalDate.parse(exp));
        m.setFineBalance(rs.getDouble("fine_balance"));
        m.setNotes(rs.getString("notes"));
        m.setProfilePic(rs.getBytes("profile_pic"));
        String reg = rs.getString("registration_date");
        if (reg != null) m.setRegistrationDate(LocalDate.parse(reg));
        // Structured ID and serial number (added via migration)
        try { m.setMemberCode(rs.getString("member_code")); } catch (SQLException ignored) {}
        try { m.setSerialNo(rs.getInt("serial_no")); }       catch (SQLException ignored) {}
        return m;
    }

    private void refreshCacheIfNeeded() {
        if (System.currentTimeMillis() - lastCacheRefresh > CACHE_TTL) {
            studentIdCache.clear();
            lastCacheRefresh = System.currentTimeMillis();
        }
    }

    private void invalidateCache() {
        studentIdCache.clear();
        lastCacheRefresh = 0;
    }
}
