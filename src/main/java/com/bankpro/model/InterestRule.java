package com.bankpro.model;

import java.time.LocalDateTime;

public class InterestRule {
    private int    id;
    private String accountType;       // SAVINGS, CURRENT, NRI, SALARY, FD
    private String ruleCode;
    private double annualRate;        // % per annum
    private double minBalance;        // minimum balance to earn interest
    private String calculationMethod; // DAILY_BALANCE, MONTHLY_BALANCE, MINIMUM_BALANCE
    private String creditFrequency;   // MONTHLY, QUARTERLY, ANNUALLY
    private boolean isActive;
    private int     updatedBy;
    private String  updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String  notes;

    public InterestRule() {}

    public String getMethodLabel() {
        return switch (calculationMethod != null ? calculationMethod : "") {
            case "DAILY_BALANCE"   -> "Daily Balance Avg";
            case "MONTHLY_BALANCE" -> "Monthly Closing Balance";
            case "MINIMUM_BALANCE" -> "Minimum Monthly Balance";
            default                -> calculationMethod;
        };
    }

    public String getFrequencyLabel() {
        return switch (creditFrequency != null ? creditFrequency : "") {
            case "MONTHLY"   -> "Monthly (30-day cycle)";
            case "QUARTERLY" -> "Quarterly";
            case "ANNUALLY"  -> "Annually";
            default          -> creditFrequency;
        };
    }

    // Getters / Setters
    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }
    public String getAccountType()                 { return accountType; }
    public void   setAccountType(String v)         { this.accountType = v; }
    public String getRuleCode()                    { return ruleCode; }
    public void   setRuleCode(String v)            { this.ruleCode = v; }
    public double getAnnualRate()                  { return annualRate; }
    public void   setAnnualRate(double v)          { this.annualRate = v; }
    public double getMinBalance()                  { return minBalance; }
    public void   setMinBalance(double v)          { this.minBalance = v; }
    public String getCalculationMethod()           { return calculationMethod; }
    public void   setCalculationMethod(String v)   { this.calculationMethod = v; }
    public String getCreditFrequency()             { return creditFrequency; }
    public void   setCreditFrequency(String v)     { this.creditFrequency = v; }
    public boolean isActive()                      { return isActive; }
    public void   setActive(boolean v)             { this.isActive = v; }
    public int    getUpdatedBy()                   { return updatedBy; }
    public void   setUpdatedBy(int v)              { this.updatedBy = v; }
    public String getUpdatedByName()               { return updatedByName; }
    public void   setUpdatedByName(String v)       { this.updatedByName = v; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void   setCreatedAt(LocalDateTime v)    { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void   setUpdatedAt(LocalDateTime v)    { this.updatedAt = v; }
    public String getNotes()                       { return notes; }
    public void   setNotes(String v)               { this.notes = v; }
}
