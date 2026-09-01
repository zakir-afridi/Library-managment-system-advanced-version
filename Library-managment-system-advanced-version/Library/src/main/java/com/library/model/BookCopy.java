package com.library.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a physical copy of a bibliographic book.
 * Each copy has its own unique copy code, barcode, physical condition, shelf location, and circulation status.
 */
public class BookCopy implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_AVAILABLE   = "Available";
    public static final String STATUS_ISSUED      = "Issued";
    public static final String STATUS_RESERVED    = "Reserved";
    public static final String STATUS_LOST        = "Lost";
    public static final String STATUS_DAMAGED     = "Damaged";
    public static final String STATUS_MAINTENANCE = "Maintenance";
    public static final String STATUS_WITHDRAWN   = "Withdrawn";

    private int           copyId;
    private int           bookId;
    private String        copyCode;
    private String        barcode;
    private String        status;
    private String        condition;
    private String        shelfLocation;
    private double        price;
    private LocalDate     acquisitionDate;
    private LocalDateTime createdAt;

    // Optional joined fields for UI/TableView display
    private String        bookTitle;
    private String        isbn;
    private String        author;

    public BookCopy() {
        this.status = STATUS_AVAILABLE;
        this.condition = "Good";
        this.acquisitionDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    public BookCopy(int copyId, int bookId, String copyCode, String barcode, String status,
                    String condition, String shelfLocation, double price, LocalDate acquisitionDate) {
        this.copyId = copyId;
        this.bookId = bookId;
        this.copyCode = copyCode;
        this.barcode = barcode;
        this.status = status != null ? status : STATUS_AVAILABLE;
        this.condition = condition != null ? condition : "Good";
        this.shelfLocation = shelfLocation;
        this.price = price;
        this.acquisitionDate = acquisitionDate != null ? acquisitionDate : LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equalsIgnoreCase(this.status);
    }

    // ── Getters and Setters ───────────────────────────────────────────────────

    public int getCopyId() { return copyId; }
    public void setCopyId(int copyId) { this.copyId = copyId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getCopyCode() { return copyCode != null ? copyCode : ""; }
    public void setCopyCode(String copyCode) { this.copyCode = copyCode; }

    public String getBarcode() { return barcode != null ? barcode : ""; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getStatus() { return status != null ? status : STATUS_AVAILABLE; }
    public void setStatus(String status) { this.status = status; }

    public String getCondition() { return condition != null ? condition : "Good"; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getShelfLocation() { return shelfLocation != null ? shelfLocation : ""; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getBookTitle() { return bookTitle != null ? bookTitle : ""; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getIsbn() { return isbn != null ? isbn : ""; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getAuthor() { return author != null ? author : ""; }
    public void setAuthor(String author) { this.author = author; }

    @Override
    public String toString() {
        return copyCode + " [" + status + "] " + (shelfLocation.isBlank() ? "" : "@ " + shelfLocation);
    }
}
