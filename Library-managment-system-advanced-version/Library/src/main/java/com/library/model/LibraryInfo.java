package com.library.model;

public class LibraryInfo {
    private String libraryName;
    private String institutionName;
    private String email;
    private String contactNumber;
    private String address;
    private String website;

    public LibraryInfo() {
        this.libraryName = "Central Library";
        this.institutionName = "University of Engineering & Technology Peshawar";
        this.email = "library@uetpeshawar.edu.pk";
        this.contactNumber = "+92 91 9216796";
        this.address = "University Campus, Peshawar, Khyber Pakhtunkhwa";
        this.website = "https://uetpeshawar.edu.pk";
    }

    public LibraryInfo(String libraryName, String institutionName, String email, String contactNumber, String address, String website) {
        this.libraryName = libraryName;
        this.institutionName = institutionName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.address = address;
        this.website = website;
    }

    public String getLibraryName() { return libraryName != null ? libraryName : "Central Library"; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }

    public String getInstitutionName() { return institutionName != null ? institutionName : ""; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }

    public String getContactNumber() { return contactNumber != null ? contactNumber : ""; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address != null ? address : ""; }
    public void setAddress(String address) { this.address = address; }

    public String getWebsite() { return website != null ? website : ""; }
    public void setWebsite(String website) { this.website = website; }
}
