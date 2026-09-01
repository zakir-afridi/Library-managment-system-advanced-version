package com.library.model;

public class Publisher {
    private int publisherId;
    private String name;
    private String contact;
    private String address;
    private String status;

    public Publisher() {
        this.status = "Active";
    }

    public Publisher(int publisherId, String name, String contact, String address, String status) {
        this.publisherId = publisherId;
        this.name = name;
        this.contact = contact;
        this.address = address;
        this.status = status != null ? status : "Active";
    }

    public int getPublisherId() { return publisherId; }
    public void setPublisherId(int publisherId) { this.publisherId = publisherId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return "Active".equalsIgnoreCase(status); }

    @Override
    public String toString() { return name; }
}
