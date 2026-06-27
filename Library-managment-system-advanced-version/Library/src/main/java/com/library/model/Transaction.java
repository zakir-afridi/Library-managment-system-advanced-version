package com.library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.library.config.AppConfig;

/**
 * Represents a book issue/return transaction.
 * Replaces the old IssueRecord with fine calculation and status tracking.
 */
public class Transaction {

    public static final String STATUS_ISSUED   = "Issued";
    public static final String STATUS_RETURNED = "Returned";
    public static final String STATUS_OVERDUE  = "Overdue";
    public static final String STATUS_LOST     = "Lost";

    public static final String CONDITION_GOOD    = "Good";
    public static final String CONDITION_DAMAGED = "Damaged";
    public static final String CONDITION_LOST    = "Lost";

    private int       transactionId;
    private int       bookId;
    private int       memberId;
    private String    bookName;       // denormalised for display
    private String    memberName;     // denormalised for display
    private String    studentId;      // denormalised for display
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double    fineAmount;
    private boolean   finePaid;
    private String    status;
    private String    returnCondition;
    private String    issuedBy;       // username of librarian

    public Transaction() {
        this.issueDate  = LocalDate.now();
        this.status     = STATUS_ISSUED;
        this.fineAmount = 0.0;
        this.finePaid   = false;
    }

    // ── Fine calculation ──────────────────────────────────────────────────────

    /**
     * Calculates the fine based on overdue days, grace period, and daily rate.
     * Uses AppConfig for rate and grace period.
     */
    public double calculateFine() {
        if (returnDate == null && dueDate == null) return 0.0;

        LocalDate checkDate = returnDate != null ? returnDate : LocalDate.now();
        long overdueDays = ChronoUnit.DAYS.between(dueDate, checkDate);

        int grace = AppConfig.getInstance().getGracePeriod();
        if (overdueDays <= grace) return 0.0;

        double rate = AppConfig.getInstance().getFineRate();
        return (overdueDays - grace) * rate;
    }

    public boolean isOverdue() {
        if (returnDate != null) return false;
        return dueDate != null && LocalDate.now().isAfter(dueDate);
    }

    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int       getTransactionId()          { return transactionId; }
    public void      setTransactionId(int v)     { this.transactionId = v; }

    public int       getBookId()                 { return bookId; }
    public void      setBookId(int v)            { this.bookId = v; }

    public int       getMemberId()               { return memberId; }
    public void      setMemberId(int v)          { this.memberId = v; }

    public String    getBookName()               { return bookName; }
    public void      setBookName(String v)       { this.bookName = v; }

    public String    getMemberName()             { return memberName; }
    public void      setMemberName(String v)     { this.memberName = v; }

    public String    getStudentId()              { return studentId; }
    public void      setStudentId(String v)      { this.studentId = v; }

    public LocalDate getIssueDate()              { return issueDate; }
    public void      setIssueDate(LocalDate v)   { this.issueDate = v; }

    public LocalDate getDueDate()                { return dueDate; }
    public void      setDueDate(LocalDate v)     { this.dueDate = v; }

    public LocalDate getReturnDate()             { return returnDate; }
    public void      setReturnDate(LocalDate v)  { this.returnDate = v; }

    public double    getFineAmount()             { return fineAmount; }
    public void      setFineAmount(double v)     { this.fineAmount = v; }

    public boolean   isFinePaid()                { return finePaid; }
    public void      setFinePaid(boolean v)      { this.finePaid = v; }

    public String    getStatus()                 { return status; }
    public void      setStatus(String v)         { this.status = v; }

    public String    getReturnCondition()        { return returnCondition; }
    public void      setReturnCondition(String v){ this.returnCondition = v; }

    public String    getIssuedBy()               { return issuedBy; }
    public void      setIssuedBy(String v)       { this.issuedBy = v; }

    // Legacy alias for old code that used IssueRecord
    public int       getBorrowId()               { return transactionId; }
    public void      setBorrowId(int v)          { this.transactionId = v; }
    public int       getStdId()                  { return memberId; }
    public void      setStdId(int v)             { this.memberId = v; }
}
