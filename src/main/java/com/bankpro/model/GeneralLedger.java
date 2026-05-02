package com.bankpro.model;

import java.time.LocalDateTime;

public class GeneralLedger {
    // Account categories (Chart of Accounts)
    public static final String ASSET     = "ASSET";
    public static final String LIABILITY = "LIABILITY";
    public static final String EQUITY    = "EQUITY";
    public static final String INCOME    = "INCOME";
    public static final String EXPENSE   = "EXPENSE";

    // Standard GL account codes
    public static final String GL_CASH_IN_HAND      = "1001";
    public static final String GL_NOSTRO             = "1002";
    public static final String GL_CUSTOMER_DEPOSITS  = "2001";
    public static final String GL_SAVINGS_DEPOSITS   = "2002";
    public static final String GL_CURRENT_DEPOSITS   = "2003";
    public static final String GL_FD_DEPOSITS        = "2004";
    public static final String GL_LOANS_RECEIVABLE   = "1101";
    public static final String GL_INTEREST_PAYABLE   = "2101";
    public static final String GL_INTEREST_RECEIVABLE= "1201";
    public static final String GL_INTEREST_INCOME    = "4001";
    public static final String GL_INTEREST_EXPENSE   = "5001";
    public static final String GL_FEE_INCOME         = "4002";
    public static final String GL_FOREX_INCOME       = "4003";
    public static final String GL_SHARE_CAPITAL      = "3001";
    public static final String GL_RETAINED_EARNINGS  = "3002";
    public static final String GL_OPERATING_EXPENSE  = "5101";
    public static final String GL_SUSPENSE           = "9001";
    public static final String GL_INTERNAL_CLEARING  = "9002";

    private int    id;
    private String glCode;
    private String glName;
    private String category;      // ASSET / LIABILITY / EQUITY / INCOME / EXPENSE
    private String normalBalance; // DEBIT / CREDIT
    private double balance;
    private String description;
    private boolean isActive;
    private boolean isInternal;   // internal bank accounts (nostro, clearing, etc.)
    private String  parentCode;   // for hierarchical COA
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GeneralLedger() {}

    public GeneralLedger(String code, String name, String category, String normalBalance) {
        this.glCode = code;
        this.glName = name;
        this.category = category;
        this.normalBalance = normalBalance;
        this.isActive = true;
    }

    public String getCategoryIcon() {
        return switch (category) {
            case ASSET     -> "📦";
            case LIABILITY -> "📄";
            case EQUITY    -> "💼";
            case INCOME    -> "💰";
            case EXPENSE   -> "💸";
            default        -> "❓";
        };
    }

    // Getters / Setters
    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }
    public String getGlCode()                    { return glCode; }
    public void   setGlCode(String v)            { this.glCode = v; }
    public String getGlName()                    { return glName; }
    public void   setGlName(String v)            { this.glName = v; }
    public String getCategory()                  { return category; }
    public void   setCategory(String v)          { this.category = v; }
    public String getNormalBalance()             { return normalBalance; }
    public void   setNormalBalance(String v)     { this.normalBalance = v; }
    public double getBalance()                   { return balance; }
    public void   setBalance(double v)           { this.balance = v; }
    public String getDescription()               { return description; }
    public void   setDescription(String v)       { this.description = v; }
    public boolean isActive()                    { return isActive; }
    public void   setActive(boolean v)           { this.isActive = v; }
    public boolean isInternal()                  { return isInternal; }
    public void   setInternal(boolean v)         { this.isInternal = v; }
    public String getParentCode()                { return parentCode; }
    public void   setParentCode(String v)        { this.parentCode = v; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void   setCreatedAt(LocalDateTime v)  { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
    public void   setUpdatedAt(LocalDateTime v)  { this.updatedAt = v; }
}
