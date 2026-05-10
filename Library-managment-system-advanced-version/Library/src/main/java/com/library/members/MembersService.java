package com.library.members;

import com.library.model.Member;
import com.library.service.MemberService;

import java.util.List;

/**
 * MEMBERS BRANCH — service layer.
 */
public class MembersService {

    private final MemberService delegate = new MemberService();

    public boolean add(Member m)                               { return delegate.addMember(m); }
    public boolean update(Member m)                            { return delegate.updateMember(m); }
    public boolean delete(int stdId)                           { return delegate.deleteMember(stdId); }
    public Member getById(int stdId)                           { return delegate.getMemberById(stdId); }
    public Member getByStudentId(String sid)                   { return delegate.getMemberByStudentId(sid); }
    public List<Member> getPage(int page, int size)            { return delegate.getAllMembers(page, size); }
    public List<Member> search(String query)                   { return delegate.searchMembers(query); }
    public boolean archive(int stdId)                          { return delegate.archiveMember(stdId); }
    public boolean unarchive(int stdId)                        { return delegate.unarchiveMember(stdId); }
    public boolean addFine(int stdId, double amount)           { return delegate.addFine(stdId, amount); }
    public boolean clearFine(int stdId)                        { return delegate.clearFine(stdId); }
    public int getTotalCount()                                 { return delegate.getTotalMembers(); }
    public int getActiveBookCount(int stdId)                   { return delegate.getActiveBookCount(stdId); }
}
