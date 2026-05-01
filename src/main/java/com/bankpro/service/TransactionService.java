package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.Transaction;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class TransactionService {
    private static TransactionService instance;
    private final AuditService audit = AuditService.getInstance();

    private TransactionService() {}
    public static synchronized TransactionService getInstance() {
        if (instance == null) instance = new TransactionService();
        return instance;
    }

    public Transaction deposit(int accountId, double amount, String description) throws Exception {
        if (!SessionManager.getInstance().hasPermission(1))
            throw new SecurityException("Insufficient permissions for deposits");
        if (amount <= 0) throw new Exception("Deposit amount must be positive");
        if (amount > 10_00_00_000) throw new Exception("Single deposit cannot exceed ₹10 Crore. Use RTGS for large amounts.");

        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            // Lock and fetch account
            PreparedStatement lock = conn.prepareStatement(
                "SELECT * FROM accounts WHERE id=? AND status='ACTIVE'");
            lock.setInt(1, accountId);
            ResultSet rs = lock.executeQuery();
            if (!rs.next()) throw new Exception("Account not found or not active");

            double oldBalance = rs.getDouble("balance");
            double newBalance = oldBalance + amount;

            // Update balance
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE accounts SET balance=?, last_transaction_at=CURRENT_TIMESTAMP WHERE id=?");
            upd.setDouble(1, newBalance);
            upd.setInt(2, accountId);
            upd.executeUpdate();

            // Record transaction
            Transaction txn = insertTransaction(conn, null, accountId, "DEPOSIT",
                amount, "INR", 1.0, description, newBalance, 0);

            conn.commit();
            audit.log("DEPOSIT", "TRANSACTION", txn.getTransactionId(), null,
                BankUtil.formatCurrency(amount),
                String.format("Deposited %s to account %s", BankUtil.formatCurrency(amount),
                    rs.getString("account_number")));
            return txn;
        } catch (Exception e) {
            conn.rollback();
            audit.logFailure("DEPOSIT", "ACCOUNT", String.valueOf(accountId), e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public Transaction withdraw(int accountId, double amount, String description) throws Exception {
        if (!SessionManager.getInstance().hasPermission(1))
            throw new SecurityException("Insufficient permissions for withdrawals");
        if (amount <= 0) throw new Exception("Withdrawal amount must be positive");

        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            PreparedStatement lock = conn.prepareStatement(
                "SELECT * FROM accounts WHERE id=? AND status='ACTIVE'");
            lock.setInt(1, accountId);
            ResultSet rs = lock.executeQuery();
            if (!rs.next()) throw new Exception("Account not found or not active");

            double oldBalance = rs.getDouble("balance");
            double minBalance = rs.getDouble("minimum_balance");
            double overdraft = rs.getDouble("overdraft_limit");

            if (oldBalance - amount < minBalance - overdraft)
                throw new Exception(String.format(
                    "Insufficient funds. Available balance: %s (Minimum balance: %s)",
                    BankUtil.formatCurrency(oldBalance - minBalance + overdraft),
                    BankUtil.formatCurrency(minBalance)));

            if (amount > 2_00_000 && !SessionManager.getInstance().hasPermission(4))
                throw new SecurityException("Withdrawals above ₹2,00,000 require Supervisor approval (level 4+)");

            double newBalance = oldBalance - amount;
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE accounts SET balance=?, last_transaction_at=CURRENT_TIMESTAMP WHERE id=?");
            upd.setDouble(1, newBalance);
            upd.setInt(2, accountId);
            upd.executeUpdate();

            Transaction txn = insertTransaction(conn, accountId, null, "WITHDRAWAL",
                amount, "INR", 1.0, description, newBalance, 0);

            conn.commit();
            audit.log("WITHDRAWAL", "TRANSACTION", txn.getTransactionId(), null,
                BankUtil.formatCurrency(amount),
                String.format("Withdrew %s from account %s", BankUtil.formatCurrency(amount),
                    rs.getString("account_number")));
            return txn;
        } catch (Exception e) {
            conn.rollback();
            audit.logFailure("WITHDRAWAL", "ACCOUNT", String.valueOf(accountId), e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public Transaction transfer(int fromAccountId, int toAccountId, double amount,
                                 String type, String description) throws Exception {
        if (!SessionManager.getInstance().hasPermission(1))
            throw new SecurityException("Insufficient permissions for transfers");
        if (fromAccountId == toAccountId)
            throw new Exception("Cannot transfer to the same account");
        if (amount <= 0) throw new Exception("Transfer amount must be positive");

        // RTGS minimum
        if ("RTGS".equals(type) && amount < 2_00_000)
            throw new Exception("RTGS minimum amount is ₹2,00,000");
        // IMPS limit
        if ("IMPS".equals(type) && amount > 5_00_000)
            throw new Exception("IMPS maximum limit is ₹5,00,000");
        // NEFT limit check
        if ("NEFT".equals(type) && amount > 10_00_00_000)
            throw new Exception("NEFT maximum is ₹10 Crore");

        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            // From account
            PreparedStatement ps1 = conn.prepareStatement(
                "SELECT * FROM accounts WHERE id=? AND status='ACTIVE'");
            ps1.setInt(1, fromAccountId);
            ResultSet from = ps1.executeQuery();
            if (!from.next()) throw new Exception("Source account not found or not active");

            double fromBalance = from.getDouble("balance");
            double minBalance = from.getDouble("minimum_balance");
            double overdraft = from.getDouble("overdraft_limit");

            if (fromBalance - amount < minBalance - overdraft)
                throw new Exception(String.format("Insufficient funds. Available: %s",
                    BankUtil.formatCurrency(fromBalance - minBalance + overdraft)));

            // To account
            PreparedStatement ps2 = conn.prepareStatement(
                "SELECT * FROM accounts WHERE id=? AND status='ACTIVE'");
            ps2.setInt(1, toAccountId);
            ResultSet to = ps2.executeQuery();
            if (!to.next()) throw new Exception("Destination account not found or not active");

            double fromNew = fromBalance - amount;
            double toNew = to.getDouble("balance") + amount;

            PreparedStatement upd1 = conn.prepareStatement(
                "UPDATE accounts SET balance=?, last_transaction_at=CURRENT_TIMESTAMP WHERE id=?");
            upd1.setDouble(1, fromNew); upd1.setInt(2, fromAccountId); upd1.executeUpdate();

            PreparedStatement upd2 = conn.prepareStatement(
                "UPDATE accounts SET balance=?, last_transaction_at=CURRENT_TIMESTAMP WHERE id=?");
            upd2.setDouble(1, toNew); upd2.setInt(2, toAccountId); upd2.executeUpdate();

            Transaction txn = insertTransaction(conn, fromAccountId, toAccountId, type,
                amount, "INR", 1.0, description, fromNew, toNew);

            conn.commit();
            audit.log(type + "_TRANSFER", "TRANSACTION", txn.getTransactionId(), null,
                BankUtil.formatCurrency(amount),
                String.format("%s of %s from %s to %s", type,
                    BankUtil.formatCurrency(amount),
                    from.getString("account_number"),
                    to.getString("account_number")));
            return txn;
        } catch (Exception e) {
            conn.rollback();
            audit.logFailure("TRANSFER", "ACCOUNT", String.valueOf(fromAccountId), e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public Transaction internationalTransfer(int fromAccountId, double amount,
                                              String currency, String beneficiaryName,
                                              String beneficiaryAccount, String swiftCode,
                                              String purpose) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("International transfers require level 5+ permission");
        if (amount <= 0) throw new Exception("Amount must be positive");

        double rateToInr = getCurrencyRate(currency);
        if (rateToInr <= 0) throw new Exception("Currency not supported: " + currency);
        double amountInr = amount * rateToInr;

        // LRS limit - $250,000 per year equivalent
        double usdEquivalent = amountInr / getCurrencyRate("USD");
        if (usdEquivalent > 250000)
            throw new Exception("Exceeds LRS limit of USD 250,000 per year");

        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM accounts WHERE id=? AND status='ACTIVE'");
            ps.setInt(1, fromAccountId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new Exception("Account not found or not active");

            double balance = rs.getDouble("balance");
            double charges = amountInr * 0.005 + 500; // 0.5% + ₹500 SWIFT charges
            double totalDeduction = amountInr + charges;

            if (balance < totalDeduction)
                throw new Exception(String.format(
                    "Insufficient funds. Required: %s (includes SWIFT charges: %s)",
                    BankUtil.formatCurrency(totalDeduction),
                    BankUtil.formatCurrency(charges)));

            double newBalance = balance - totalDeduction;
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE accounts SET balance=?, last_transaction_at=CURRENT_TIMESTAMP WHERE id=?");
            upd.setDouble(1, newBalance); upd.setInt(2, fromAccountId); upd.executeUpdate();

            String narration = String.format("SWIFT to %s (%s) @ %.4f | Purpose: %s | Charges: %s",
                beneficiaryName, beneficiaryAccount, rateToInr, purpose,
                BankUtil.formatCurrency(charges));

            Transaction txn = insertTransaction(conn, fromAccountId, null, "SWIFT",
                amount, currency, rateToInr, narration, newBalance, 0);

            conn.commit();
            audit.log("SWIFT_TRANSFER", "TRANSACTION", txn.getTransactionId(), null,
                amount + " " + currency,
                "International SWIFT transfer: " + narration);
            return txn;
        } catch (Exception e) {
            conn.rollback();
            audit.logFailure("SWIFT", "ACCOUNT", String.valueOf(fromAccountId), e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public double getCurrencyRate(String code) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT rate_to_inr FROM currency_rates WHERE currency_code=?")) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public Map<String, Double> getAllRates() throws SQLException {
        Map<String, Double> rates = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT currency_code, rate_to_inr FROM currency_rates ORDER BY currency_code");
            while (rs.next())
                rates.put(rs.getString(1), rs.getDouble(2));
        }
        return rates;
    }

    public void updateCurrencyRate(String code, double rate) throws Exception {
        if (!SessionManager.getInstance().hasPermission(7))
            throw new SecurityException("Updating currency rates requires level 7+ permission");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE currency_rates SET rate_to_inr=?, last_updated=CURRENT_TIMESTAMP WHERE currency_code=?")) {
            ps.setDouble(1, rate);
            ps.setString(2, code);
            ps.executeUpdate();
            audit.log("UPDATE_RATE", "CURRENCY", code, null, String.valueOf(rate),
                "Currency rate updated: " + code + " = " + rate);
        }
    }

    public List<Transaction> getTransactionsByAccount(int accountId, int limit) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, " +
            "fa.account_number AS from_acct, ta.account_number AS to_acct, " +
            "u.full_name AS performed_by_name " +
            "FROM transactions t " +
            "LEFT JOIN accounts fa ON t.from_account_id = fa.id " +
            "LEFT JOIN accounts ta ON t.to_account_id = ta.id " +
            "LEFT JOIN users u ON t.performed_by = u.id " +
            "WHERE t.from_account_id=? OR t.to_account_id=? " +
            "ORDER BY t.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId); ps.setInt(2, accountId); ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapTransaction(rs));
        }
        return list;
    }

    public List<Transaction> getAllTransactions(int limit) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, " +
            "fa.account_number AS from_acct, ta.account_number AS to_acct, " +
            "u.full_name AS performed_by_name " +
            "FROM transactions t " +
            "LEFT JOIN accounts fa ON t.from_account_id = fa.id " +
            "LEFT JOIN accounts ta ON t.to_account_id = ta.id " +
            "LEFT JOIN users u ON t.performed_by = u.id " +
            "ORDER BY t.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapTransaction(rs));
        }
        return list;
    }

    public double getTodayTransactionVolume() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT SUM(amount_inr) FROM transactions WHERE DATE(created_at) = DATE('now')");
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private Transaction insertTransaction(Connection conn, Integer fromId, Integer toId,
                                           String type, double amount, String currency,
                                           double rate, String description,
                                           double balFrom, double balTo) throws SQLException {
        String txnId = BankUtil.generateTransactionId();
        double amountInr = amount * rate;

        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO transactions (transaction_id,from_account_id,to_account_id,transaction_type," +
            "amount,currency,amount_inr,exchange_rate,description,performed_by,balance_after_from," +
            "balance_after_to,narration) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
            new String[]{"id"});
        ps.setString(1, txnId);
        if (fromId != null) ps.setInt(2, fromId); else ps.setNull(2, Types.INTEGER);
        if (toId != null) ps.setInt(3, toId); else ps.setNull(3, Types.INTEGER);
        ps.setString(4, type);
        ps.setDouble(5, amount);
        ps.setString(6, currency);
        ps.setDouble(7, amountInr);
        ps.setDouble(8, rate);
        ps.setString(9, description);
        ps.setInt(10, SessionManager.getInstance().getCurrentUser().getId());
        ps.setDouble(11, balFrom);
        ps.setDouble(12, balTo);
        ps.setString(13, description);
        ps.executeUpdate();

        Transaction txn = new Transaction();
        txn.setTransactionId(txnId);
        txn.setTransactionType(type);
        txn.setAmount(amount);
        txn.setCurrency(currency);
        txn.setAmountInr(amountInr);
        txn.setExchangeRate(rate);
        txn.setDescription(description);
        txn.setStatus("COMPLETED");
        txn.setCreatedAt(LocalDateTime.now());
        return txn;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("id"));
        t.setTransactionId(rs.getString("transaction_id"));
        t.setTransactionType(rs.getString("transaction_type"));
        t.setAmount(rs.getDouble("amount"));
        t.setCurrency(rs.getString("currency"));
        t.setAmountInr(rs.getDouble("amount_inr"));
        t.setExchangeRate(rs.getDouble("exchange_rate"));
        t.setStatus(rs.getString("status"));
        t.setDescription(rs.getString("description"));
        t.setBalanceAfterFrom(rs.getDouble("balance_after_from"));
        t.setBalanceAfterTo(rs.getDouble("balance_after_to"));
        try { t.setFromAccountNumber(rs.getString("from_acct")); } catch (SQLException ignored) {}
        try { t.setToAccountNumber(rs.getString("to_acct")); } catch (SQLException ignored) {}
        try { t.setPerformedByName(rs.getString("performed_by_name")); } catch (SQLException ignored) {}
        String ca = rs.getString("created_at");
        if (ca != null) {
            try { t.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        return t;
    }
}
