package com.bankpro.model;

import java.time.LocalDateTime;

public class LedgerEntry {
    private int    id;
    private String journalId;      // groups debit+credit pair
    private String glCode;
    private String glName;
    private String entryType;      // DEBIT / CREDIT
    private double amount;
    private String currency;
    private String description;
    private String referenceType;  // TRANSACTION / LOAN / FD / INTEREST / MANUAL
    private String referenceId;
    private int    performedBy;
    private String performedByName;
    private LocalDateTime createdAt;
    private String period;         // YYYY-MM  for period reporting
    private double runningBalance; // GL running balance after this entry

    public LedgerEntry() {}

    public LedgerEntry(String journalId, String glCode, String entryType,
                       double amount, String description, String refType, String refId) {
        this.journalId    = journalId;
        this.glCode       = glCode;
        this.entryType    = entryType;
        this.amount       = amount;
        this.description  = description;
        this.referenceType = refType;
        this.referenceId  = refId;
        this.currency     = "INR";
        this.createdAt    = LocalDateTime.now();
        this.period       = String.format("%d-%02d",
                               createdAt.getYear(), createdAt.getMonthValue());
    }

    // Getters / Setters
    public int    getId()                           { return id; }
    public void   setId(int id)                     { this.id = id; }
    public String getJournalId()                    { return journalId; }
    public void   setJournalId(String v)            { this.journalId = v; }
    public String getGlCode()                       { return glCode; }
    public void   setGlCode(String v)               { this.glCode = v; }
    public String getGlName()                       { return glName; }
    public void   setGlName(String v)               { this.glName = v; }
    public String getEntryType()                    { return entryType; }
    public void   setEntryType(String v)            { this.entryType = v; }
    public double getAmount()                       { return amount; }
    public void   setAmount(double v)               { this.amount = v; }
    public String getCurrency()                     { return currency; }
    public void   setCurrency(String v)             { this.currency = v; }
    public String getDescription()                  { return description; }
    public void   setDescription(String v)          { this.description = v; }
    public String getReferenceType()                { return referenceType; }
    public void   setReferenceType(String v)        { this.referenceType = v; }
    public String getReferenceId()                  { return referenceId; }
    public void   setReferenceId(String v)          { this.referenceId = v; }
    public int    getPerformedBy()                  { return performedBy; }
    public void   setPerformedBy(int v)             { this.performedBy = v; }
    public String getPerformedByName()              { return performedByName; }
    public void   setPerformedByName(String v)      { this.performedByName = v; }
    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void   setCreatedAt(LocalDateTime v)     { this.createdAt = v; }
    public String getPeriod()                       { return period; }
    public void   setPeriod(String v)               { this.period = v; }
    public double getRunningBalance()               { return runningBalance; }
    public void   setRunningBalance(double v)       { this.runningBalance = v; }
}
