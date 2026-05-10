package com.library.model;

import java.time.LocalDate;

/**
 * Enhanced Member / Student model.
 * student_id is auto-generated in format LIB-YYYY-NNNN.
 */
public class Member {

    public static final String STATUS_ACTIVE    = "Active";
    public static final String STATUS_SUSPENDED = "Suspended";
    public static final String STATUS_EXPIRED   = "Expired";
    public static final String STATUS_ARCHIVED  = "Archived";

    private int       stdId;
    private String    studentId;          // LIB-2024-0001
    private String    name;
    private String    fname;              // father's name
    private String    cnic;
    private LocalDate dateOfBirth;
    private String    gender;
    private String    contact;
    private String    email;
    private String    emergencyContact;
    private String    bloodGroup;

    // Address
    private String    address;
    private String    city;
    private String    province;
    private String    postalCode;
    private String    country;

    // Academic
    private String    department;
    private String    program;
    private String    semester;
    private String    session;
    private LocalDate admissionDate;

    // Library
    private String    status;
    private String    libraryCardNumber;
    private int       bookLimit;
    private String    membershipType;
    private LocalDate membershipExpiry;
    private double    fineBalance;
    private String    memberCode;   // ST00000001 — auto-generated structured ID
    private int       serialNo;     // display order, resequenced on add/remove

    // Meta
    private String    notes;
    private String    photoPath;
    private byte[]    profilePic;
    private LocalDate registrationDate;

    public Member() {
        this.registrationDate = LocalDate.now();
        this.bookLimit        = 5;
        this.membershipType   = "Student";
        this.status           = STATUS_ACTIVE;
        this.country          = "Pakistan";
        this.fineBalance      = 0.0;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int       getStdId()              { return stdId; }
    public void      setStdId(int v)         { this.stdId = v; }

    public String    getStudentId()          { return studentId; }
    public void      setStudentId(String v)  { this.studentId = v; }

    public String    getName()               { return name; }
    public void      setName(String v)       { this.name = v; }

    public String    getFname()              { return fname; }
    public void      setFname(String v)      { this.fname = v; }
    public String    getFatherName()         { return fname; }
    public void      setFatherName(String v) { this.fname = v; }

    public String    getCnic()               { return cnic; }
    public void      setCnic(String v)       { this.cnic = v; }

    public LocalDate getDateOfBirth()        { return dateOfBirth; }
    public void      setDateOfBirth(LocalDate v){ this.dateOfBirth = v; }

    public String    getGender()             { return gender; }
    public void      setGender(String v)     { this.gender = v; }

    public String    getContact()            { return contact; }
    public void      setContact(String v)    { this.contact = v; }
    public String    getMobile()             { return contact; }
    public void      setMobile(String v)     { this.contact = v; }

    public String    getEmail()              { return email; }
    public void      setEmail(String v)      { this.email = v; }

    public String    getEmergencyContact()   { return emergencyContact; }
    public void      setEmergencyContact(String v){ this.emergencyContact = v; }

    public String    getBloodGroup()         { return bloodGroup; }
    public void      setBloodGroup(String v) { this.bloodGroup = v; }

    public String    getAddress()            { return address; }
    public void      setAddress(String v)    { this.address = v; }
    public String    getStreetAddress()      { return address; }
    public void      setStreetAddress(String v){ this.address = v; }

    public String    getCity()               { return city; }
    public void      setCity(String v)       { this.city = v; }

    public String    getProvince()           { return province; }
    public void      setProvince(String v)   { this.province = v; }

    public String    getPostalCode()         { return postalCode; }
    public void      setPostalCode(String v) { this.postalCode = v; }

    public String    getCountry()            { return country; }
    public void      setCountry(String v)    { this.country = v; }

    public String    getDepartment()         { return department; }
    public void      setDepartment(String v) { this.department = v; }

    public String    getProgram()            { return program; }
    public void      setProgram(String v)    { this.program = v; }

    public String    getSemester()           { return semester; }
    public void      setSemester(String v)   { this.semester = v; }

    public String    getSession()            { return session; }
    public void      setSession(String v)    { this.session = v; }

    public LocalDate getAdmissionDate()      { return admissionDate; }
    public void      setAdmissionDate(LocalDate v){ this.admissionDate = v; }

    public String    getStatus()             { return status; }
    public void      setStatus(String v)     { this.status = v; }

    public String    getLibraryCardNumber()  { return libraryCardNumber; }
    public void      setLibraryCardNumber(String v){ this.libraryCardNumber = v; }

    public int       getBookLimit()          { return bookLimit; }
    public void      setBookLimit(int v)     { this.bookLimit = v; }

    public String    getMembershipType()     { return membershipType; }
    public void      setMembershipType(String v){ this.membershipType = v; }

    public LocalDate getMembershipExpiry()   { return membershipExpiry; }
    public void      setMembershipExpiry(LocalDate v){ this.membershipExpiry = v; }

    public double    getFineBalance()        { return fineBalance; }
    public void      setFineBalance(double v){ this.fineBalance = v; }

    public String    getMemberCode()         { return memberCode; }
    public void      setMemberCode(String v) { this.memberCode = v; }

    public int       getSerialNo()           { return serialNo; }
    public void      setSerialNo(int v)      { this.serialNo = v; }

    public String    getNotes()              { return notes; }
    public void      setNotes(String v)      { this.notes = v; }

    public String    getPhotoPath()         { return photoPath; }
    public void      setPhotoPath(String v)  { this.photoPath = v; }

    public byte[]    getProfilePic()         { return profilePic; }
    public void      setProfilePic(byte[] v) { this.profilePic = v; }

    public LocalDate getRegistrationDate()   { return registrationDate; }
    public void      setRegistrationDate(LocalDate v){ this.registrationDate = v; }

    public boolean   hasOutstandingFine()    { return fineBalance > 0; }

    @Override
    public String toString() { return name + " [" + studentId + "]"; }
}
