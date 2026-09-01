package com.library.model;

import java.io.Serializable;

/**
 * Represents an Author entity in the library catalog.
 */
public class Author implements Serializable {

    private static final long serialVersionUID = 1L;

    private int    authorId;
    private String name;
    private String biography;
    private String status;

    public Author() {
        this.status = "Active";
    }

    public Author(int authorId, String name, String biography, String status) {
        this.authorId  = authorId;
        this.name      = name;
        this.biography = biography;
        this.status    = status != null ? status : "Active";
    }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getBiography() { return biography != null ? biography : ""; }
    public void setBiography(String biography) { this.biography = biography; }

    public String getStatus() { return status != null ? status : "Active"; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return name;
    }
}
