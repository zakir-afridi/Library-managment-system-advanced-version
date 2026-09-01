package com.library.model;

public class Category {
    private int categoryId;
    private String name;
    private String description;
    private String status; // Active, Inactive

    public Category() {
        this.status = "Active";
    }

    public Category(int categoryId, String name, String description, String status) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.status = status != null ? status : "Active";
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return "Active".equalsIgnoreCase(status); }

    @Override
    public String toString() { return name; }
}
