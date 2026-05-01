package com.bankpro.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private String email;
    private String phone;
    private int permissionLevel;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String department;
    private String employeeId;
    private int createdBy;

    public User() {}

    public User(int id, String username, String fullName, String email, int permissionLevel,
                String employeeId, String department, boolean active) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.permissionLevel = permissionLevel;
        this.employeeId = employeeId;
        this.department = department;
        this.active = active;
    }

    public String getPermissionLabel() {
        return switch (permissionLevel) {
            case 1 -> "Junior Clerk";
            case 2 -> "Senior Clerk";
            case 3 -> "Teller";
            case 4 -> "Supervisor";
            case 5 -> "Assistant Manager";
            case 6 -> "Branch Manager";
            case 7 -> "Regional Manager";
            case 8 -> "Senior Manager";
            case 9 -> "Director";
            case 10 -> "CTO / Admin";
            default -> "Unknown";
        };
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(int permissionLevel) { this.permissionLevel = permissionLevel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    @Override
    public String toString() {
        return fullName + " (" + username + ") - " + getPermissionLabel();
    }
}
