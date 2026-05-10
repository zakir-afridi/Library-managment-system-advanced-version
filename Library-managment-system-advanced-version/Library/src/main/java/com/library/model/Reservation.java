package com.library.model;

import java.time.LocalDate;

/**
 * Represents a book reservation in the waiting queue.
 * Queue position is managed by the ReservationService (FIFO).
 */
public class Reservation {

    public static final String STATUS_PENDING   = "Pending";
    public static final String STATUS_READY     = "Ready";
    public static final String STATUS_FULFILLED = "Fulfilled";
    public static final String STATUS_CANCELLED = "Cancelled";

    private int       reservationId;
    private int       bookId;
    private int       memberId;
    private String    bookName;       // denormalised for display
    private String    memberName;     // denormalised for display
    private String    studentId;      // denormalised for display
    private LocalDate reservationDate;
    private String    status;
    private int       queuePosition;
    private LocalDate notifiedDate;

    public Reservation() {
        this.reservationDate = LocalDate.now();
        this.status          = STATUS_PENDING;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int       getReservationId()          { return reservationId; }
    public void      setReservationId(int v)     { this.reservationId = v; }

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

    public LocalDate getReservationDate()        { return reservationDate; }
    public void      setReservationDate(LocalDate v){ this.reservationDate = v; }

    public String    getStatus()                 { return status; }
    public void      setStatus(String v)         { this.status = v; }

    public int       getQueuePosition()          { return queuePosition; }
    public void      setQueuePosition(int v)     { this.queuePosition = v; }

    public LocalDate getNotifiedDate()           { return notifiedDate; }
    public void      setNotifiedDate(LocalDate v){ this.notifiedDate = v; }
}
