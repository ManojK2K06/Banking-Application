package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.Account;
import com.bankpro.model.InterestRule;
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

    // ── Party Accounts ───────────────────────────────────────────────────────

    public Account openAccount(int partyId, String accountType, double initialDeposit,
                               String currency) throws Exception {
        if (!SessionManager.getInstance().hasPermission(2))
            throw new SecurityException("Opening accounts requires level 2+");
        if (initialDeposit < 0)
            throw new Exception("Initial deposit cannot be negative");

        double minBalance    = getMinimumBalance(accountType);
        double interestRate  = getDefaultInterestRate(accountType);

        if (initialDeposit < minBalance)
            throw new Exception(String.format("Minimum balance for %s is %s",
                accountType, BankUtil.formatCurrency(minBalance)));

        String glCode = getGLCode(accountType);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String accountNumber = BankUtil.generateAccountNumber();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO accounts (account_number,party_id,account_type,account_category," +
                "balance,currency,interest_rate,minimum_balance,gl_code) VALUES (?,?,?,'PARTY',?,?,?,?,?)");
            ps.setString(1, accountNumber);
            ps.setInt(2, partyId);
            ps.setString(3, accountType);
            ps.setDouble(4, initialDeposit);
            ps.setString(5, currency != null ? currency : "INR");
            ps.setDouble(6, interestRate);
            ps.setDouble(7, minBalance);
            ps.setString(8, glCode);
            ps.executeUpdate();

            ResultSet rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()");
            Account account = new Account();
            if (rs.next()) account.setId(rs.getInt(1));
            account.setAccountNumber(accountNumber);
            account.setPartyId(partyId);
            account.setAccountType(accountType);
            account.setBalance(initialDeposit);
            account.setCurrency(currency != null ? currency : "INR");
            account.setStatus("ACTIVE");
            account.setInterestRate(interestRate);
            account.setMinimumBalance(minBalance);

            // GL journal for initial deposit
            if (initialDeposit > 0) {
                try {
                    GeneralLedgerService.getInstance().journalDeposit(
                        account.getId(), initialDeposit, "OPEN-" + accountNumber);
                } catch (Exception e) { /* non-fatal */ }
            }

            audit.log("OPEN_ACCOUNT","ACCOUNT",accountNumber,null,accountType,
                String.format("Opened %s account with %s initial deposit",
                    accountType, BankUtil.formatCurrency(initialDeposit)));
            return account;
        }
    }

    // ── Internal Accounts ────────────────────────────────────────────────────

    public List<Account> getInternalAccounts() throws SQLException {
        List<Account> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM accounts WHERE account_category='INTERNAL' ORDER BY account_number");
            while (rs.next()) list.add(mapAccount(rs));
        }
        return list;
    }

    // ── Account Operations ───────────────────────────────────────────────────

    public void closeAccount(int accountId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(6))
            throw new SecurityException("Closing accounts requires level 6+");
        Account acc = getById(accountId);
        if (acc == null) throw new Exception("Account not found");
        if (acc.getBalance() > 0.01)
            throw new Exception("Account has balance " + BankUtil.formatCurrency(acc.getBalance()) +
                ". Please withdraw before closing.");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE accounts SET status='CLOSED' WHERE id=?")) {
            ps.setInt(1, accountId); ps.executeUpdate();
            audit.log("CLOSE_ACCOUNT","ACCOUNT",acc.getAccountNumber(),"ACTIVE","CLOSED","Account closed");
        }
    }

    public void freezeAccount(int accountId, String reason) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Freezing accounts requires level 5+");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE accounts SET status='FROZEN' WHERE id=?")) {
            ps.setInt(1, accountId); ps.executeUpdate();
            audit.log("FREEZE_ACCOUNT","ACCOUNT",String.valueOf(accountId),"ACTIVE","FROZEN","Reason: "+reason);
        }
    }

    public void unfreezeAccount(int accountId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Unfreezing accounts requires level 5+");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE accounts SET status='ACTIVE' WHERE id=?")) {
            ps.setInt(1, accountId); ps.executeUpdate();
            audit.log("UNFREEZE_ACCOUNT","ACCOUNT",String.valueOf(accountId),"FROZEN","ACTIVE","Account unfrozen");
        }
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public Account getById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT a.*, COALESCE(p.first_name||' '||p.last_name, p.company_name, a.internal_name) AS party_name " +
                "FROM accounts a LEFT JOIN parties p ON a.party_id=p.id WHERE a.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapAccount(rs);
        }
        return null;
    }

    public Account getByAccountNumber(String accountNumber) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT a.*, COALESCE(p.first_name||' '||p.last_name, p.company_name, a.internal_name) AS party_name " +
                "FROM accounts a LEFT JOIN parties p ON a.party_id=p.id WHERE a.account_number=?")) {
            ps.setString(1, accountNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapAccount(rs);
        }
        return null;
    }

    public List<Account> getAccountsByCustomer(int id) throws SQLException { return getAccountsByParty(id); }
    public List<Account> getAccountsByParty(int partyId) throws SQLException {
        List<Account> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT a.*, COALESCE(p.first_name||' '||p.last_name, p.company_name) AS party_name " +
                "FROM accounts a LEFT JOIN parties p ON a.party_id=p.id " +
                "WHERE a.party_id=? ORDER BY a.created_at DESC")) {
            ps.setInt(1, partyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapAccount(rs));
        }
        return list;
    }

    public List<Account> getAllAccounts() throws SQLException {
        List<Account> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT a.*, COALESCE(p.first_name||' '||p.last_name, p.company_name, a.internal_name) AS party_name " +
                "FROM accounts a LEFT JOIN parties p ON a.party_id=p.id " +
                "WHERE a.account_category='PARTY' ORDER BY a.account_number");
            while (rs.next()) list.add(mapAccount(rs));
        }
        return list;
    }

    public double getTotalDeposits() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COALESCE(SUM(balance),0) FROM accounts WHERE status='ACTIVE' AND account_category='PARTY'");
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public int getTotalAccountCount() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM accounts WHERE status='ACTIVE' AND account_category='PARTY'");
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private double getMinimumBalance(String type) {
        return switch (type) {
            case "SAVINGS"  -> 1000.0;
            case "CURRENT"  -> 10000.0;
            case "SALARY"   -> 0.0;
            case "NRI"      -> 75000.0;
            default         -> 0.0;
        };
    }

    private double getDefaultInterestRate(String type) {
        // Tries to read from interest_rules, falls back to hardcoded
        try {
            InterestRule rule = InterestService.getInstance().getRuleForAccountType(type);
            if (rule != null) return rule.getAnnualRate();
        } catch (Exception ignored) {}
        return switch (type) {
            case "SAVINGS" -> 3.5;
            case "NRI"     -> 4.0;
            default        -> 0.0;
        };
    }

    private String getGLCode(String type) {
        return switch (type) {
            case "SAVINGS"  -> "2002";
            case "CURRENT"  -> "2003";
            case "NRI"      -> "2005";
            default         -> "2001";
        };
    }


    private Account mapAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("id"));
        a.setAccountNumber(rs.getString("account_number"));
        try { a.setPartyId(rs.getInt("party_id")); } catch (Exception ignored) {}
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getDouble("balance"));
        a.setCurrency(rs.getString("currency"));
        a.setStatus(rs.getString("status"));
        a.setInterestRate(rs.getDouble("interest_rate"));
        a.setMinimumBalance(rs.getDouble("minimum_balance"));
        a.setBranchCode(rs.getString("branch_code"));
        a.setIfscCode(rs.getString("ifsc_code"));
        a.setOverdraftLimit(rs.getDouble("overdraft_limit"));
        try { a.setPartyName(rs.getString("party_name")); } catch (Exception ignored) {}
        try { a.setGlCode(rs.getString("gl_code")); } catch (Exception ignored) {}
        try { a.setInternalName(rs.getString("internal_name")); } catch (Exception ignored) {}
        String ca = rs.getString("created_at");
        if (ca != null) {
            try { a.setCreatedAt(LocalDateTime.parse(ca.replace(" ","T"))); } catch (Exception ignored) {}
        }
        return a;
    }
}
