package com.library.service;

import com.library.model.Book;
import com.library.model.IssueRecord;
import com.library.model.Member;
import com.library.model.Transaction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Legacy facade kept for backward compatibility.
 * All calls delegate to the new service layer.
 */
public class LibraryService {

    private final BookService        bookService   = new BookService();
    private final MemberService      memberService = new MemberService();
    private final TransactionService txService     = new TransactionService();
    private final UserService        userService   = new UserService();

    // ── Books ─────────────────────────────────────────────────────────────────
    public List<Book> getAllBooks()              { return bookService.getAllBooks(1, 10000); }
    public boolean    addBook(Book b)            { return bookService.addBook(b); }
    public boolean    updateBook(Book b)         { return bookService.updateBook(b); }
    public boolean    deleteBook(int id)         { return bookService.deleteBook(id); }
    public Book       getBookById(int id)        { return bookService.getBookById(id); }
    public int        getTotalBooks()            { return bookService.getTotalBooks(); }

    // ── Members ───────────────────────────────────────────────────────────────
    public List<Member> getAllMembers()           { return memberService.getAllMembers(1, 10000); }
    public boolean      addMember(Member m)      { return memberService.addMember(m); }
    public boolean      updateMember(Member m)   { return memberService.updateMember(m); }
    public boolean      deleteMember(int id)     { return memberService.deleteMember(id); }
    public Member       getMemberById(int id)    { return memberService.getMemberById(id); }
    public int          getTotalMembers()        { return memberService.getTotalMembers(); }

    // ── Issue / Return ────────────────────────────────────────────────────────
    public boolean issueBook(int stdId, int bookId) {
        return txService.issueBook(bookId, stdId, "admin").isEmpty();
    }

    public boolean returnBook(int stdId, int bookId) {
        List<Transaction> active = txService.getMemberTransactions(stdId)
                .stream()
                .filter(t -> t.getBookId() == bookId
                          && Transaction.STATUS_ISSUED.equals(t.getStatus()))
                .toList();
        if (active.isEmpty()) return false;
        return txService.returnBook(active.get(0).getTransactionId(),
                Transaction.CONDITION_GOOD, "admin") >= 0;
    }

    public List<IssueRecord> getIssuedBooks() {
        return txService.getActiveTransactions()
                .stream()
                .map(t -> {
                    IssueRecord r = new IssueRecord();
                    r.setTransactionId(t.getTransactionId());
                    r.setStdId(t.getMemberId());
                    r.setBookId(t.getBookId());
                    r.setIssueDate(t.getIssueDate());
                    r.setDueDate(t.getDueDate());
                    return r;
                })
                .collect(Collectors.toList());
    }

    public int getTotalIssuedBooks() {
        return txService.getActiveTransactions().size();
    }

    // ── Authentication ────────────────────────────────────────────────────────
    public boolean authenticate(String username, String password) {
        return userService.authenticate(username, password) != null;
    }
}
