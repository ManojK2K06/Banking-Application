package com.bankpro.model;

import java.time.LocalDateTime;

public class Loan {
    private int id;
    private String loanId;
    private int partyId;
    private String partyName;
    private int accountId;
    private String accountNumber;
    private String loanType;
    private double principalAmount;
    private double outstandingAmount;
    private double interestRate;
    private int tenureMonths;
    private double emiAmount;
    private String status;
    private int approvedBy;
    private String approvedByName;
    private LocalDateTime disbursedAt;
    private String nextEmiDate;
    private double totalPaid;
    private double penaltyCharges;
    private String purpose;
    private LocalDateTime createdAt;
    private String collateral;

    public Loan() {}

    public String getLoanTypeLabel() {
        return switch (loanType) {
            case "HOME" -> "🏠 Home Loan";
            case "PERSONAL" -> "👤 Personal Loan";
            case "CAR" -> "🚗 Car/Auto Loan";
            case "EDUCATION" -> "🎓 Education Loan";
            case "BUSINESS" -> "💼 Business Loan";
            case "GOLD" -> "✨ Gold Loan";
            case "MORTGAGE" -> "🏢 Mortgage Loan";
            default -> loanType;
        };
    }

    public String getStatusLabel() {
        return switch (status) {
            case "PENDING" -> "⏳ Pending Approval";
            case "APPROVED" -> "✅ Approved";
            case "DISBURSED" -> "💰 Disbursed";
            case "ACTIVE" -> "📋 Active";
            case "CLOSED" -> "✓ Closed";
            case "REJECTED" -> "✗ Rejected";
            case "NPA" -> "⚠️ NPA";
            default -> status;
        };
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }
    public int getPartyId() { return partyId; }
    public void setPartyId(int partyId) { this.partyId = partyId; }
    public String getCustomerName() { return partyName; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public double getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(double principalAmount) { this.principalAmount = principalAmount; }
    public double getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(double outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }
    public double getEmiAmount() { return emiAmount; }
    public void setEmiAmount(double emiAmount) { this.emiAmount = emiAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getApprovedBy() { return approvedBy; }
    public void setApprovedBy(int approvedBy) { this.approvedBy = approvedBy; }
    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }
    public String getNextEmiDate() { return nextEmiDate; }
    public void setNextEmiDate(String nextEmiDate) { this.nextEmiDate = nextEmiDate; }
    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }
    public double getPenaltyCharges() { return penaltyCharges; }
    public void setPenaltyCharges(double penaltyCharges) { this.penaltyCharges = penaltyCharges; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCollateral() { return collateral; }
    public void setCollateral(String collateral) { this.collateral = collateral; }
}
