package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.InterestRule;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

public class InterestService {
    private static final Logger logger = Logger.getLogger(InterestService.class.getName());
    private static InterestService instance;
    private final AuditService            audit  = AuditService.getInstance();
    private final GeneralLedgerService    gl     = GeneralLedgerService.getInstance();

    private InterestService() {}
    public static synchronized InterestService getInstance() {
        if (instance == null) instance = new InterestService();
        return instance;
    }

    // ── Rule Management ──────────────────────────────────────────────────────

    public List<InterestRule> getAllRules() throws SQLException {
        List<InterestRule> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT ir.*, u.full_name AS uname FROM interest_rules ir " +
                "LEFT JOIN users u ON ir.updated_by=u.id ORDER BY ir.account_type, ir.rule_code");
            while (rs.next()) list.add(mapRule(rs));
        }
        return list;
    }

    public InterestRule getRuleForAccountType(String accountType) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT ir.*, u.full_name AS uname FROM interest_rules ir " +
                "LEFT JOIN users u ON ir.updated_by=u.id " +
                "WHERE ir.account_type=? AND ir.is_active=1 ORDER BY ir.id LIMIT 1")) {
            ps.setString(1, accountType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRule(rs);
        }
        return null;
    }

    public void updateRule(int ruleId, double newRate, double minBalance,
                            String method, String frequency, String notes) throws Exception {
        if (!SessionManager.getInstance().hasPermission(6))
            throw new SecurityException("Updating interest rules requires Level 6+ (Branch Manager)");
        if (newRate < 0 || newRate > 25)
            throw new Exception("Interest rate must be between 0% and 25%");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE interest_rules SET annual_rate=?,min_balance=?,calculation_method=?," +
                "credit_frequency=?,notes=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            ps.setDouble(1, newRate); ps.setDouble(2, minBalance);
            ps.setString(3, method);  ps.setString(4, frequency);
            ps.setString(5, notes);
            ps.setInt(6, SessionManager.getInstance().getCurrentUser().getId());
            ps.setInt(7, ruleId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new Exception("Interest rule not found");
            audit.log("UPDATE_INTEREST_RULE","INTEREST_RULE",String.valueOf(ruleId),
                null, newRate + "%", "Rate updated to " + newRate + "% by manager");
        }
    }

    public void createRule(String accountType, double rate, double minBalance,
                            String method, String frequency, String notes) throws Exception {
        if (!SessionManager.getInstance().hasPermission(6))
            throw new SecurityException("Creating interest rules requires Level 6+");
        String code = "IR-" + accountType.substring(0,Math.min(3,accountType.length()))
            + "-" + System.currentTimeMillis() % 10000;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO interest_rules (rule_code,account_type,annual_rate,min_balance," +
                "calculation_method,credit_frequency,notes,updated_by) VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, code); ps.setString(2, accountType);
            ps.setDouble(3, rate); ps.setDouble(4, minBalance);
            ps.setString(5, method); ps.setString(6, frequency);
            ps.setString(7, notes);
            ps.setInt(8, SessionManager.getInstance().getCurrentUser().getId());
            ps.executeUpdate();
            audit.log("CREATE_INTEREST_RULE","INTEREST_RULE",code,null,rate+"%","New rule created");
        }
    }

    public void overrideAccountRate(int accountId, double newRate) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Overriding account interest rate requires Level 5+");
        if (newRate < 0 || newRate > 25)
            throw new Exception("Rate must be 0–25%");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE accounts SET interest_rate=? WHERE id=?")) {
            ps.setDouble(1, newRate); ps.setInt(2, accountId); ps.executeUpdate();
            audit.log("OVERRIDE_ACCOUNT_RATE","ACCOUNT",String.valueOf(accountId),
                null, newRate+"%", "Manager overrode interest rate to "+newRate+"%");
        }
    }

    // ── 30-Day Interest Processing ───────────────────────────────────────────

    /**
     * Process interest for ALL eligible accounts.
     * Called manually by a manager (level 5+) or can be scheduled.
     * Returns number of accounts processed.
     */
    public int processMonthlyInterest() throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Processing interest requires Level 5+ (Asst Manager)");

        String today = LocalDate.now().toString();
        String thirtyDaysAgo = LocalDate.now().minusDays(30).toString();

        List<Map<String,Object>> eligible = getEligibleAccounts(thirtyDaysAgo);
        int processed = 0;

        for (Map<String,Object> acct : eligible) {
            try {
                int    accountId   = (int) acct.get("id");
                double balance     = (double) acct.get("balance");
                double rate        = (double) acct.get("interest_rate");
                String acctType    = (String) acct.get("account_type");
                String lastDate    = (String) acct.get("last_interest_date");
                double minBalance  = (double) acct.get("minimum_balance");

                if (rate <= 0) continue;
                if (balance < minBalance) continue;   // below minimum — no interest

                // Calculate 30-day interest
                double interest = calculateInterest(balance, rate, 30);
                if (interest < 0.01) continue;

                creditInterest(accountId, interest, thirtyDaysAgo, today, rate);
                processed++;
            } catch (Exception e) {
                logger.warning("Interest failed for account " + acct.get("id") + ": " + e.getMessage());
            }
        }

        audit.log("PROCESS_INTEREST","SYSTEM","ALL",null,String.valueOf(processed),
            "Monthly interest processed for " + processed + " accounts");
        return processed;
    }

    /**
     * Process interest for a single account immediately (manager override).
     */
    public void processInterestForAccount(int accountId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Requires Level 5+");

        String today        = LocalDate.now().toString();
        String thirtyDaysAgo = LocalDate.now().minusDays(30).toString();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM accounts WHERE id=? AND status='ACTIVE' AND account_category='PARTY'")) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new Exception("Account not found or not active");

            double balance    = rs.getDouble("balance");
            double rate       = rs.getDouble("interest_rate");
            double minBalance = rs.getDouble("minimum_balance");

            if (rate <= 0)            throw new Exception("Account has 0% interest rate");
            if (balance < minBalance) throw new Exception("Balance below minimum — interest not applicable");

            double interest = calculateInterest(balance, rate, 30);
            creditInterest(accountId, interest, thirtyDaysAgo, today, rate);
            audit.log("MANUAL_INTEREST","ACCOUNT",String.valueOf(accountId),
                null, BankUtil.formatCurrency(interest), "Manual interest credit by manager");
        }
    }

    public List<Map<String,Object>> getAccrualHistory(int accountId, int limit) throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT ia.*, u.full_name AS uname FROM interest_accrual ia " +
                "LEFT JOIN users u ON ia.processed_by=u.id " +
                "WHERE ia.account_id=? ORDER BY ia.created_at DESC LIMIT ?")) {
            ps.setInt(1, accountId); ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("date",     rs.getString("accrual_date"));
                m.put("from",     rs.getString("period_from"));
                m.put("to",       rs.getString("period_to"));
                m.put("balance",  rs.getDouble("avg_balance"));
                m.put("rate",     rs.getDouble("rate_applied"));
                m.put("interest", rs.getDouble("interest_amount"));
                m.put("status",   rs.getString("status"));
                m.put("by",       rs.getString("uname"));
                list.add(m);
            }
        }
        return list;
    }

    public double getAccruedInterestTotal() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COALESCE(SUM(interest_amount),0) FROM interest_accrual WHERE status='CREDITED'");
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private double calculateInterest(double balance, double annualRate, int days) {
        // Simple interest: P × R × T / (365 × 100)
        return balance * annualRate * days / (365.0 * 100.0);
    }

    private void creditInterest(int accountId, double interest, String periodFrom,
                                 String periodTo, double rate) throws Exception {
        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            // Fetch current balance
            PreparedStatement balPs = conn.prepareStatement(
                "SELECT balance FROM accounts WHERE id=?");
            balPs.setInt(1, accountId);
            ResultSet balRs = balPs.executeQuery();
            if (!balRs.next()) throw new Exception("Account not found");
            double balance = balRs.getDouble(1);

            // Credit interest to account
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE accounts SET balance=balance+?, last_interest_date=CURRENT_DATE, " +
                "last_transaction_at=CURRENT_TIMESTAMP WHERE id=?");
            upd.setDouble(1, interest); upd.setInt(2, accountId); upd.executeUpdate();

            // Record accrual
            int userId = SessionManager.getInstance().isLoggedIn()
                ? SessionManager.getInstance().getCurrentUser().getId() : 0;
            PreparedStatement accrual = conn.prepareStatement(
                "INSERT INTO interest_accrual (account_id,accrual_date,period_from,period_to," +
                "avg_balance,rate_applied,interest_amount,processed_by) VALUES (?,?,?,?,?,?,?,?)");
            accrual.setInt(1, accountId);
            accrual.setString(2, LocalDate.now().toString());
            accrual.setString(3, periodFrom); accrual.setString(4, periodTo);
            accrual.setDouble(5, balance); accrual.setDouble(6, rate);
            accrual.setDouble(7, interest);
            if (userId > 0) accrual.setInt(8, userId); else accrual.setNull(8, Types.INTEGER);
            accrual.executeUpdate();

            // Record transaction
            String txnId = BankUtil.generateTransactionId();
            PreparedStatement txn = conn.prepareStatement(
                "INSERT INTO transactions (transaction_id,to_account_id,transaction_type,amount," +
                "currency,amount_inr,exchange_rate,description,performed_by,balance_after_to) " +
                "VALUES (?,?,'INTEREST_CREDIT',?,?,?,1.0,?,?,?)");
            txn.setString(1, txnId); txn.setInt(2, accountId);
            txn.setDouble(3, interest); txn.setString(4, "INR");
            txn.setDouble(5, interest);
            txn.setString(6, "Interest @ "+rate+"% for "+periodFrom+" to "+periodTo);
            if (userId > 0) txn.setInt(7, userId); else txn.setNull(7, Types.INTEGER);
            txn.setDouble(8, balance + interest);
            txn.executeUpdate();

            // GL journal
            conn.commit();
            conn.setAutoCommit(true);
            try { gl.journalInterestCredit(accountId, interest, txnId); }
            catch (Exception e) { logger.warning("GL interest journal failed: " + e.getMessage()); }

        } catch (Exception e) {
            conn.rollback(); conn.setAutoCommit(true);
            throw e;
        } finally {
            try { conn.close(); } catch (Exception ignored) {}
        }
    }

    private List<Map<String,Object>> getEligibleAccounts(String cutoffDate) throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        String sql = "SELECT id,balance,interest_rate,account_type,last_interest_date,minimum_balance " +
            "FROM accounts WHERE status='ACTIVE' AND account_category='PARTY' " +
            "AND interest_rate > 0 " +
            "AND (last_interest_date IS NULL OR last_interest_date <= ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cutoffDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String,Object> m = new HashMap<>();
                m.put("id",           rs.getInt("id"));
                m.put("balance",      rs.getDouble("balance"));
                m.put("interest_rate",rs.getDouble("interest_rate"));
                m.put("account_type", rs.getString("account_type"));
                m.put("last_interest_date", rs.getString("last_interest_date"));
                m.put("minimum_balance", rs.getDouble("minimum_balance"));
                list.add(m);
            }
        }
        return list;
    }

    private InterestRule mapRule(ResultSet rs) throws SQLException {
        InterestRule r = new InterestRule();
        r.setId(rs.getInt("id"));
        r.setRuleCode(rs.getString("rule_code"));
        r.setAccountType(rs.getString("account_type"));
        r.setAnnualRate(rs.getDouble("annual_rate"));
        r.setMinBalance(rs.getDouble("min_balance"));
        r.setCalculationMethod(rs.getString("calculation_method"));
        r.setCreditFrequency(rs.getString("credit_frequency"));
        r.setActive(rs.getInt("is_active") == 1);
        r.setNotes(rs.getString("notes"));
        try { r.setUpdatedByName(rs.getString("uname")); } catch (Exception ignored) {}
        String ua = rs.getString("updated_at");
        if (ua != null) {
            try { r.setUpdatedAt(LocalDateTime.parse(ua.replace(" ","T"))); } catch (Exception ignored) {}
        }
        return r;
    }
}
