package com.bankpro.model;

import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private String transactionId;
    private int fromAccountId;
    private int toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String transactionType;
    private double amount;
    private String currency;
    private double amountInr;
    private double exchangeRate;
    private String status;
    private String description;
    private String referenceNumber;
    private int performedBy;
    private String performedByName;
    private LocalDateTime createdAt;
    private String channel;
    private double balanceAfterFrom;
    private double balanceAfterTo;
    private String narration;

    public Transaction() {}

    public String getTypeLabel() {
        return switch (transactionType) {
            case "DEPOSIT" -> "💰 Deposit";
            case "WITHDRAWAL" -> "💸 Withdrawal";
            case "TRANSFER" -> "🔄 Transfer";
            case "NEFT" -> "🏦 NEFT";
            case "RTGS" -> "⚡ RTGS";
            case "IMPS" -> "📱 IMPS";
            case "SWIFT" -> "🌐 SWIFT";
            case "LOAN_DISBURSEMENT" -> "📋 Loan Disbursement";
            case "LOAN_REPAYMENT" -> "💳 Loan Repayment";
            case "FD_CREATE" -> "📊 FD Created";
            case "FD_MATURITY" -> "✅ FD Maturity";
            case "CHEQUE" -> "📄 Cheque";
            case "SERVICE_CHARGE" -> "🔧 Service Charge";
            default -> transactionType;
        };
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public int getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(int fromAccountId) { this.fromAccountId = fromAccountId; }
    public int getToAccountId() { return toAccountId; }
    public void setToAccountId(int toAccountId) { this.toAccountId = toAccountId; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public void setFromAccountNumber(String fromAccountNumber) { this.fromAccountNumber = fromAccountNumber; }
    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public double getAmountInr() { return amountInr; }
    public void setAmountInr(double amountInr) { this.amountInr = amountInr; }
    public double getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public int getPerformedBy() { return performedBy; }
    public void setPerformedBy(int performedBy) { this.performedBy = performedBy; }
    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public double getBalanceAfterFrom() { return balanceAfterFrom; }
    public void setBalanceAfterFrom(double balanceAfterFrom) { this.balanceAfterFrom = balanceAfterFrom; }
    public double getBalanceAfterTo() { return balanceAfterTo; }
    public void setBalanceAfterTo(double balanceAfterTo) { this.balanceAfterTo = balanceAfterTo; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
}
