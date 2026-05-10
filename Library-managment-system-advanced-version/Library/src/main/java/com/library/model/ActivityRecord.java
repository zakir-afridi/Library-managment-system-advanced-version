package com.library.model;

public class ActivityRecord {
    private String memberName;
    private String bookTitle;
    private String action;
    private String date;
    private String status;
    
    public ActivityRecord() {}
    
    public ActivityRecord(String memberName, String bookTitle, String action, String date, String status) {
        this.memberName = memberName;
        this.bookTitle = bookTitle;
        this.action = action;
        this.date = date;
        this.status = status;
    }
    
    // Getters and Setters
    public String getMemberName() {
        return memberName;
    }
    
    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
    
    public String getBookTitle() {
        return bookTitle;
    }
    
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "ActivityRecord{" +
                "memberName='" + memberName + '\'' +
                ", bookTitle='" + bookTitle + '\'' +
                ", action='" + action + '\'' +
                ", date='" + date + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}