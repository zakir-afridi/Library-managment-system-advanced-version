package com.library.students;

import com.library.model.Member;
import com.library.service.MemberService;

import java.util.*;

/**
 * STUDENTS BRANCH — service layer.
 * Delegates to MemberService / members schema.
 */
public class StudentService {

    private final MemberService memberService = new MemberService();

    public int getTotalCount() {
        return memberService.getTotalMembers();
    }

    public List<Map<String, String>> search(String query) {
        List<Map<String, String>> results = new ArrayList<>();
        List<Member> members = memberService.searchMembers(query);
        for (Member m : members) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("student_id", m.getStudentId() != null ? m.getStudentId() : "");
            row.put("full_name",  m.getName() != null ? m.getName() : "");
            row.put("department", m.getDepartment() != null ? m.getDepartment() : "");
            row.put("year",       m.getSemester() != null ? m.getSemester() : "1");
            row.put("email",      m.getEmail() != null ? m.getEmail() : "");
            row.put("phone",      m.getContact() != null ? m.getContact() : "");
            results.add(row);
        }
        return results;
    }

    public boolean add(String studentId, String fullName, String department,
                       int year, String email, String phone) {
        Member m = new Member();
        m.setStudentId(studentId);
        m.setName(fullName);
        m.setDepartment(department);
        m.setSemester(String.valueOf(year));
        m.setEmail(email);
        m.setContact(phone);
        m.setStatus(Member.STATUS_ACTIVE);
        return memberService.addMember(m);
    }

    public boolean archive(String studentId) {
        Member m = memberService.getMemberByStudentId(studentId);
        if (m != null) {
            return memberService.archiveMember(m.getStdId());
        }
        return false;
    }

    public boolean unarchive(String studentId) {
        Member m = memberService.getMemberByStudentId(studentId);
        if (m != null) {
            return memberService.unarchiveMember(m.getStdId());
        }
        return false;
    }
}
