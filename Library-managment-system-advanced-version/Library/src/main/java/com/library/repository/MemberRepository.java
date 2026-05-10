package com.library.repository;

import com.library.model.Member;
import com.library.service.MemberService;

import java.util.List;

/**
 * Legacy repository — delegates to MemberService.
 * Kept for backward compatibility with any old code that imports this class.
 */
public class MemberRepository {

    private final MemberService service = new MemberService();

    public List<Member> getAllMembers()         { return service.getAllMembers(1, 10000); }
    public boolean      addMember(Member m)     { return service.addMember(m); }
    public boolean      updateMember(Member m)  { return service.updateMember(m); }
    public boolean      deleteMember(int id)    { return service.deleteMember(id); }
    public Member       getMemberById(int id)   { return service.getMemberById(id); }
    public int          getTotalMembers()       { return service.getTotalMembers(); }
}
