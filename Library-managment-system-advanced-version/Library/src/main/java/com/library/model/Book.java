package com.library.model;

import java.time.LocalDate;

/**
 * Enhanced Book model — supports ISBN, category, status tracking,
 * cover image (BLOB), and availability counts.
 */
public class Book {

    // ── Status constants ──────────────────────────────────────────────────────
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_ISSUED    = "Issued";
    public static final String STATUS_OVERDUE   = "Overdue";
    public static final String STATUS_RESERVED  = "Reserved";
    public static final String STATUS_LOST      = "Lost";
    public static final String STATUS_DAMAGED   = "Damaged";
    public static final String STATUS_ARCHIVED  = "Archived";

    private int       bookId;
    private String    bookCode;     // BK00000001 — auto-generated structured ID
    private int       serialNo;     // display order, resequenced on add/remove
    private String    isbn;
    private String    bookName;
    private String    author;
    private String    publisher;
    private int       publicationYear;
    private String    edition;
    private String    category;
    private String    description;
    private int       quantity;
    private int       availableQty;
    private String    status;
    private String    shelfLocation;
    private byte[]    coverImage;
    private String    coverUrl;        // v3: Open Library cover URL
    private LocalDate createdAt;

    public Book() {
        this.status       = STATUS_AVAILABLE;
        this.quantity     = 1;
        this.availableQty = 1;
        this.createdAt    = LocalDate.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int       getBookId()          { return bookId; }
    public void      setBookId(int v)     { this.bookId = v; }

    public String    getBookCode()        { return bookCode; }
    public void      setBookCode(String v){ this.bookCode = v; }

    public int       getSerialNo()        { return serialNo; }
    public void      setSerialNo(int v)   { this.serialNo = v; }

    public String    getIsbn()            { return isbn; }
    public void      setIsbn(String v)    { this.isbn = v; }

    public String    getBookName()        { return bookName; }
    public void      setBookName(String v){ this.bookName = v; }

    public String    getAuthor()          { return author; }
    public void      setAuthor(String v)  { this.author = v; }

    public String    getPublisher()       { return publisher; }
    public void      setPublisher(String v){ this.publisher = v; }

    public int       getPublicationYear() { return publicationYear; }
    public void      setPublicationYear(int v){ this.publicationYear = v; }

    public String    getEdition()         { return edition; }
    public void      setEdition(String v) { this.edition = v; }

    public String    getCategory()        { return category; }
    public void      setCategory(String v){ this.category = v; }

    public String    getDescription()     { return description; }
    public void      setDescription(String v){ this.description = v; }

    public int       getQuantity()        { return quantity; }
    public void      setQuantity(int v)   { this.quantity = v; }

    public int       getAvailableQty()    { return availableQty; }
    public void      setAvailableQty(int v){ this.availableQty = v; }

    public String    getStatus()          { return status; }
    public void      setStatus(String v)  { this.status = v; }

    public String    getShelfLocation()   { return shelfLocation; }
    public void      setShelfLocation(String v){ this.shelfLocation = v; }

    public byte[]    getCoverImage()      { return coverImage; }
    public void      setCoverImage(byte[] v){ this.coverImage = v; }

    public String    getCoverUrl()        { return coverUrl; }
    public void      setCoverUrl(String v){ this.coverUrl = v; }

    public LocalDate getCreatedAt()       { return createdAt; }
    public void      setCreatedAt(LocalDate v){ this.createdAt = v; }

    // Legacy alias kept for backward compatibility with existing repositories
    public byte[]    getProfilePic()      { return coverImage; }
    public void      setProfilePic(byte[] v){ this.coverImage = v; }

    public boolean   isAvailable()        { return availableQty > 0; }

    @Override
    public String toString() {
        return bookName + " — " + author + " [" + isbn + "]";
    }
}
