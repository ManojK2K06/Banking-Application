package com.bankpro.model;

import java.time.LocalDateTime;

public class Account {
    private int id;
    private String accountNumber;
    private int partyId;
    private String partyName;
    private String glCode;
    private String internalName;
    private String accountCategory;
    private String accountType;
    private double balance;
    private String currency;
    private String status;
    private double interestRate;
    private double minimumBalance;
    private LocalDateTime createdAt;
    private LocalDateTime lastTransactionAt;
    private String branchCode;
    private String ifscCode;
    private double overdraftLimit;

    public Account() {}

    public String getAccountTypeLabel() {
        return switch (accountType) {
            case "SAVINGS" -> "Savings Account";
            case "CURRENT" -> "Current Account";
            case "SALARY" -> "Salary Account";
            case "NRI" -> "NRI Account";
            case "FIXED_DEPOSIT" -> "Fixed Deposit";
            case "RECURRING" -> "Recurring Deposit";
            default -> accountType;
        };
    }

    public boolean isActive() { return "ACTIVE".equals(status); }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public int getPartyId() { return partyId; }
    public void setPartyId(int partyId) { this.partyId = partyId; }
    public String getPartyName() { return partyName; }
    public String getCustomerName() { return partyName; }
    public void setCustomerName(String n) { this.partyName = n; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    public String getGlCode() { return glCode; }
    public void setGlCode(String glCode) { this.glCode = glCode; }
    public String getInternalName() { return internalName; }
    public void setInternalName(String internalName) { this.internalName = internalName; }
    public String getAccountCategory() { return accountCategory; }
    public void setAccountCategory(String accountCategory) { this.accountCategory = accountCategory; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public double getMinimumBalance() { return minimumBalance; }
    public void setMinimumBalance(double minimumBalance) { this.minimumBalance = minimumBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastTransactionAt() { return lastTransactionAt; }
    public void setLastTransactionAt(LocalDateTime lastTransactionAt) { this.lastTransactionAt = lastTransactionAt; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public double getOverdraftLimit() { return overdraftLimit; }
    public void setOverdraftLimit(double overdraftLimit) { this.overdraftLimit = overdraftLimit; }
}
