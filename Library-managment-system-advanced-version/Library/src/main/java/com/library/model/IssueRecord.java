package com.library.model;

import java.time.LocalDate;

/**
 * Legacy alias kept for backward compatibility.
 * New code should use Transaction instead.
 */
public class IssueRecord extends Transaction {

    public IssueRecord() { super(); }

    public IssueRecord(int borrowId, int stdId, int bookId,
                       LocalDate issueDate, LocalDate returnDate) {
        super();
        setBorrowId(borrowId);
        setStdId(stdId);
        setBookId(bookId);
        setIssueDate(issueDate);
        setReturnDate(returnDate);
    }
}