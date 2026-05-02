package com.bankpro.model;

import java.time.LocalDateTime;

public class Party {
    // Party types
    public static final String INDIVIDUAL  = "INDIVIDUAL";
    public static final String CORPORATE   = "CORPORATE";
    public static final String PARTNERSHIP = "PARTNERSHIP";
    public static final String TRUST       = "TRUST";
    public static final String GOVERNMENT  = "GOVERNMENT";

    private int    id;
    private String partyId;           // PARTY00000001
    private String partyType;         // INDIVIDUAL / CORPORATE / ...
    private String displayName;       // computed: full name or company name

    // --- Individual fields ---
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String gender;

    // --- Corporate fields ---
    private String companyName;
    private String registrationNumber;  // CIN / ROC
    private String incorporationDate;
    private String businessType;        // PVT LTD, LLP, etc.
    private String contactPersonName;
    private String contactPersonDesig;

    // --- Common fields ---
    private String email;
    private String phone;
    private String altPhone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String country;

    // --- KYC fields ---
    private String aadharNumber;   // Individual
    private String panNumber;      // Both
    private String gstNumber;      // Corporate
    private String cinNumber;      // Corporate
    private String kycStatus;      // PENDING / APPROVED / REJECTED
    private String kycDocuments;   // JSON or comma-separated doc refs

    // --- Meta ---
    private int     creditScore;
    private boolean active;
    private int     createdBy;
    private LocalDateTime createdAt;
    private String  segment;       // RETAIL / SME / CORPORATE / HNI

    public Party() {}

    // ── Computed helpers ────────────────────────────────────────────────────

    public String getDisplayName() {
        if (INDIVIDUAL.equals(partyType)) {
            return (firstName != null ? firstName : "") + " " +
                   (lastName  != null ? lastName  : "");
        }
        return companyName != null ? companyName : "";
    }

    public String getShortType() {
        return switch (partyType != null ? partyType : "") {
            case INDIVIDUAL  -> "Individual";
            case CORPORATE   -> "Corporate";
            case PARTNERSHIP -> "Partnership";
            case TRUST       -> "Trust";
            case GOVERNMENT  -> "Government";
            default          -> partyType;
        };
    }

    public String getKycStatusLabel() {
        return switch (kycStatus != null ? kycStatus : "") {
            case "APPROVED" -> "✓ KYC Approved";
            case "PENDING"  -> "⏳ KYC Pending";
            case "REJECTED" -> "✗ KYC Rejected";
            default         -> kycStatus;
        };
    }

    public String getSegmentLabel() {
        return switch (segment != null ? segment : "") {
            case "RETAIL"    -> "🏠 Retail";
            case "SME"       -> "🏭 SME";
            case "CORPORATE" -> "🏢 Corporate";
            case "HNI"       -> "💎 HNI";
            default          -> segment != null ? segment : "—";
        };
    }

    /** For display in combo-boxes / tables */
    @Override public String toString() {
        return partyId + " — " + getDisplayName() + " [" + getShortType() + "]";
    }

    // ── Getters / Setters ───────────────────────────────────────────────────
    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }
    public String getPartyId()                   { return partyId; }
    public void   setPartyId(String partyId)     { this.partyId = partyId; }
    public String getPartyType()                 { return partyType; }
    public void   setPartyType(String t)         { this.partyType = t; }
    public String getFirstName()                 { return firstName; }
    public void   setFirstName(String v)         { this.firstName = v; }
    public String getLastName()                  { return lastName; }
    public void   setLastName(String v)          { this.lastName = v; }
    public String getDateOfBirth()               { return dateOfBirth; }
    public void   setDateOfBirth(String v)       { this.dateOfBirth = v; }
    public String getGender()                    { return gender; }
    public void   setGender(String v)            { this.gender = v; }
    public String getCompanyName()               { return companyName; }
    public void   setCompanyName(String v)       { this.companyName = v; }
    public String getRegistrationNumber()        { return registrationNumber; }
    public void   setRegistrationNumber(String v){ this.registrationNumber = v; }
    public String getIncorporationDate()         { return incorporationDate; }
    public void   setIncorporationDate(String v) { this.incorporationDate = v; }
    public String getBusinessType()              { return businessType; }
    public void   setBusinessType(String v)      { this.businessType = v; }
    public String getContactPersonName()         { return contactPersonName; }
    public void   setContactPersonName(String v) { this.contactPersonName = v; }
    public String getContactPersonDesig()        { return contactPersonDesig; }
    public void   setContactPersonDesig(String v){ this.contactPersonDesig = v; }
    public String getEmail()                     { return email; }
    public void   setEmail(String v)             { this.email = v; }
    public String getPhone()                     { return phone; }
    public void   setPhone(String v)             { this.phone = v; }
    public String getAltPhone()                  { return altPhone; }
    public void   setAltPhone(String v)          { this.altPhone = v; }
    public String getAddress()                   { return address; }
    public void   setAddress(String v)           { this.address = v; }
    public String getCity()                      { return city; }
    public void   setCity(String v)              { this.city = v; }
    public String getState()                     { return state; }
    public void   setState(String v)             { this.state = v; }
    public String getPincode()                   { return pincode; }
    public void   setPincode(String v)           { this.pincode = v; }
    public String getCountry()                   { return country; }
    public void   setCountry(String v)           { this.country = v; }
    public String getAadharNumber()              { return aadharNumber; }
    public void   setAadharNumber(String v)      { this.aadharNumber = v; }
    public String getPanNumber()                 { return panNumber; }
    public void   setPanNumber(String v)         { this.panNumber = v; }
    public String getGstNumber()                 { return gstNumber; }
    public void   setGstNumber(String v)         { this.gstNumber = v; }
    public String getCinNumber()                 { return cinNumber; }
    public void   setCinNumber(String v)         { this.cinNumber = v; }
    public String getKycStatus()                 { return kycStatus; }
    public void   setKycStatus(String v)         { this.kycStatus = v; }
    public String getKycDocuments()              { return kycDocuments; }
    public void   setKycDocuments(String v)      { this.kycDocuments = v; }
    public int    getCreditScore()               { return creditScore; }
    public void   setCreditScore(int v)          { this.creditScore = v; }
    public boolean isActive()                    { return active; }
    public void   setActive(boolean v)           { this.active = v; }
    public int    getCreatedBy()                 { return createdBy; }
    public void   setCreatedBy(int v)            { this.createdBy = v; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void   setCreatedAt(LocalDateTime v)  { this.createdAt = v; }
    public String getSegment()                   { return segment; }
    public void   setSegment(String v)           { this.segment = v; }
}
