package com.bankpro.model;

import java.time.LocalDateTime;

public class Customer {
    private int id;
    private String customerId;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String aadharNumber;
    private String panNumber;
    private String kycStatus;
    private LocalDateTime createdAt;
    private int createdBy;
    private boolean active;
    private int creditScore;

    public Customer() {}

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getKycStatusLabel() {
        return switch (kycStatus) {
            case "APPROVED" -> "✓ KYC Approved";
            case "PENDING" -> "⏳ KYC Pending";
            case "REJECTED" -> "✗ KYC Rejected";
            default -> kycStatus;
        };
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }
}
