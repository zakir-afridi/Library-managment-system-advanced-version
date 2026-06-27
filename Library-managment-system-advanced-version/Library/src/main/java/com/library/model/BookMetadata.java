package com.library.model;

/**
 * Metadata fetched from Open Library API.
 * Used to auto-fill the Add Book form by ISBN.
 */
public class BookMetadata {

    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String publishDate;
    private int    pageCount;
    private String description;
    private String coverUrl;
    private String category;

    public BookMetadata() {}

    public String getIsbn()            { return isbn; }
    public void   setIsbn(String v)    { this.isbn = v; }

    public String getTitle()           { return title; }
    public void   setTitle(String v)   { this.title = v; }

    public String getAuthor()          { return author; }
    public void   setAuthor(String v)  { this.author = v; }

    public String getPublisher()            { return publisher; }
    public void   setPublisher(String v)    { this.publisher = v; }

    public String getPublishDate()          { return publishDate; }
    public void   setPublishDate(String v)  { this.publishDate = v; }

    public int    getPageCount()            { return pageCount; }
    public void   setPageCount(int v)       { this.pageCount = v; }

    public String getDescription()          { return description; }
    public void   setDescription(String v)  { this.description = v; }

    public String getCoverUrl()             { return coverUrl; }
    public void   setCoverUrl(String v)     { this.coverUrl = v; }

    public String getCategory()             { return category; }
    public void   setCategory(String v)     { this.category = v; }

    /**
     * Extract publication year from publish date string (e.g. "June 12, 2009" → 2009).
     */
    public int extractYear() {
        if (publishDate == null || publishDate.isBlank()) return 0;
        try {
            // Try plain 4-digit year
            java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d{4})").matcher(publishDate);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public String toString() {
        return title + " — " + author + " (" + isbn + ")";
    }
}
