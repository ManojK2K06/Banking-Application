package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.Account;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class AccountService {
    private static AccountService instance;
    private final AuditService audit = AuditService.getInstance();

    private AccountService() {}
    public static synchronized AccountService getInstance() {
        if (instance == null) instance = new AccountService();
        return instance;
    }

    public Account openAccount(int customerId, String accountType, double initialDeposit,
                                String currency) throws Exception {
        if (!SessionManager.getInstance().hasPermission(2))
            throw new SecurityException("Opening accounts requires level 2+ permission");
        if (initialDeposit < 0)
            throw new Exception("Initial deposit cannot be negative");

        double minBalance = getMinimumBalance(accountType);
        if (initialDeposit < minBalance)
            throw new Exception(String.format("Minimum balance for %s is %s",
                accountType, BankUtil.formatCurrency(minBalance)));

        double interestRate = getDefaultInterestRate(accountType);
        String accountNumber = BankUtil.generateAccountNumber();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO accounts (account_number,customer_id,account_type,balance,currency," +
                 "interest_rate,minimum_balance) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, accountNumber);
            ps.setInt(2, customerId);
            ps.setString(3, accountType);
            ps.setDouble(4, initialDeposit);
            ps.setString(5, currency != null ? currency : "INR");
            ps.setDouble(6, interestRate);
            ps.setDouble(7, minBalance);
            ps.executeUpdate();

            ResultSet rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()");
            Account account = new Account();
            if (rs.next()) account.setId(rs.getInt(1));
            account.setAccountNumber(accountNumber);
            account.setCustomerId(customerId);
            account.setAccountType(accountType);
            account.setBalance(initialDeposit);
            account.setCurrency(currency != null ? currency : "INR");
            account.setStatus("ACTIVE");
            account.setInterestRate(interestRate);
            account.setMinimumBalance(minBalance);

            audit.log("OPEN_ACCOUNT", "ACCOUNT", accountNumber, null,
                accountType, String.format("Opened %s account with initial deposit %s",
                    accountType, BankUtil.formatCurrency(initialDeposit)));
            return account;
        }
    }

    public void closeAccount(int accountId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(6))
            throw new SecurityException("Closing accounts requires level 6+ permission");

        Account acc = getById(accountId);
        if (acc == null) throw new Exception("Account not found");
        if (acc.getBalance() > 0)
            throw new Exception("Account has balance of " + BankUtil.formatCurrency(acc.getBalance()) +
                ". Please withdraw before closing.");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE accounts SET status='CLOSED' WHERE id=?")) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
            audit.log("CLOSE_ACCOUNT", "ACCOUNT", acc.getAccountNumber(),
                "ACTIVE", "CLOSED", "Account closed");
        }
    }

    public void freezeAccount(int accountId, String reason) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Freezing accounts requires level 5+ permission");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE accounts SET status='FROZEN' WHERE id=?")) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
            audit.log("FREEZE_ACCOUNT", "ACCOUNT", String.valueOf(accountId),
                "ACTIVE", "FROZEN", "Reason: " + reason);
        }
    }

    public void unfreezeAccount(int accountId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Unfreezing accounts requires level 5+ permission");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE accounts SET status='ACTIVE' WHERE id=?")) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
            audit.log("UNFREEZE_ACCOUNT", "ACCOUNT", String.valueOf(accountId),
                "FROZEN", "ACTIVE", "Account unfrozen");
        }
    }

    public Account getById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT a.*, c.first_name || ' ' || c.last_name AS customer_name " +
                 "FROM accounts a LEFT JOIN customers c ON a.customer_id = c.id WHERE a.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapAccount(rs);
        }
        return null;
    }

    public Account getByAccountNumber(String accountNumber) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT a.*, c.first_name || ' ' || c.last_name AS customer_name " +
                 "FROM accounts a LEFT JOIN customers c ON a.customer_id = c.id " +
                 "WHERE a.account_number=?")) {
            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapAccount(rs);
        }
        return null;
    }

    public List<Account> getAccountsByCustomer(int customerId) throws SQLException {
        List<Account> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT a.*, c.first_name || ' ' || c.last_name AS customer_name " +
                 "FROM accounts a LEFT JOIN customers c ON a.customer_id = c.id " +
                 "WHERE a.customer_id=? ORDER BY a.created_at DESC")) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAccount(rs));
        }
        return list;
    }

    public List<Account> getAllAccounts() throws SQLException {
        List<Account> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT a.*, c.first_name || ' ' || c.last_name AS customer_name " +
                 "FROM accounts a LEFT JOIN customers c ON a.customer_id = c.id " +
                 "ORDER BY a.account_number")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAccount(rs));
        }
        return list;
    }

    public double getTotalDeposits() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT SUM(balance) FROM accounts WHERE status='ACTIVE'");
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public int getTotalAccountCount() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts WHERE status='ACTIVE'");
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double getMinimumBalance(String type) {
        return switch (type) {
            case "SAVINGS" -> 1000.0;
            case "CURRENT" -> 10000.0;
            case "SALARY" -> 0.0;
            case "NRI" -> 75000.0;
            default -> 0.0;
        };
    }

    private double getDefaultInterestRate(String type) {
        return switch (type) {
            case "SAVINGS" -> 3.5;
            case "CURRENT" -> 0.0;
            case "SALARY" -> 3.5;
            case "NRI" -> 4.0;
            default -> 0.0;
        };
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setCustomerId(rs.getInt("customer_id"));
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getDouble("balance"));
        a.setCurrency(rs.getString("currency"));
        a.setStatus(rs.getString("status"));
        a.setInterestRate(rs.getDouble("interest_rate"));
        a.setMinimumBalance(rs.getDouble("minimum_balance"));
        a.setBranchCode(rs.getString("branch_code"));
        a.setIfscCode(rs.getString("ifsc_code"));
        a.setOverdraftLimit(rs.getDouble("overdraft_limit"));
        try { a.setCustomerName(rs.getString("customer_name")); } catch (SQLException ignored) {}
        String ca = rs.getString("created_at");
        if (ca != null) {
            try { a.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        return a;
    }
}
