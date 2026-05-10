package com.library.model;

import java.time.LocalDate;

public class Employee {

    public static final String STATUS_ACTIVE   = "Active";
    public static final String STATUS_INACTIVE = "Inactive";
    public static final String STATUS_ARCHIVED = "Archived";

    private int       empId;
    private String    employeeCode;  // EP00000001 — auto-generated structured ID
    private int       serialNo;      // display order, resequenced on add/remove
    private String    name;
    private String    designation;
    private String    department;
    private String    contact;
    private String    email;
    private String    cnic;
    private String    address;
    private LocalDate joinDate;
    private String    status;
    private double    salary;
    private String    notes;
    private byte[]    profilePic;
    private LocalDate archivedDate;

    public Employee() {
        this.joinDate = LocalDate.now();
        this.status   = STATUS_ACTIVE;
        this.salary   = 0.0;
    }

    public int       getEmpId()              { return empId; }
    public void      setEmpId(int v)         { this.empId = v; }

    public String    getEmployeeCode()       { return employeeCode; }
    public void      setEmployeeCode(String v){ this.employeeCode = v; }

    public int       getSerialNo()           { return serialNo; }
    public void      setSerialNo(int v)      { this.serialNo = v; }

    public String    getName()               { return name; }
    public void      setName(String v)       { this.name = v; }

    public String    getDesignation()        { return designation; }
    public void      setDesignation(String v){ this.designation = v; }

    public String    getDepartment()         { return department; }
    public void      setDepartment(String v) { this.department = v; }

    public String    getContact()            { return contact; }
    public void      setContact(String v)    { this.contact = v; }

    public String    getEmail()              { return email; }
    public void      setEmail(String v)      { this.email = v; }

    public String    getCnic()               { return cnic; }
    public void      setCnic(String v)       { this.cnic = v; }

    public String    getAddress()            { return address; }
    public void      setAddress(String v)    { this.address = v; }

    public LocalDate getJoinDate()           { return joinDate; }
    public void      setJoinDate(LocalDate v){ this.joinDate = v; }

    public String    getStatus()             { return status; }
    public void      setStatus(String v)     { this.status = v; }

    public double    getSalary()             { return salary; }
    public void      setSalary(double v)     { this.salary = v; }

    public String    getNotes()              { return notes; }
    public void      setNotes(String v)      { this.notes = v; }

    public byte[]    getProfilePic()         { return profilePic; }
    public void      setProfilePic(byte[] v) { this.profilePic = v; }

    public LocalDate getArchivedDate()       { return archivedDate; }
    public void      setArchivedDate(LocalDate v){ this.archivedDate = v; }

    @Override
    public String toString() { return name + " [" + employeeCode + "]"; }
}
