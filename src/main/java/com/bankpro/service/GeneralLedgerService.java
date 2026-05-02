package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.GeneralLedger;
import com.bankpro.model.LedgerEntry;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Arrays;

public class GeneralLedgerService {
    private static GeneralLedgerService instance;
    private final AuditService audit = AuditService.getInstance();

    private GeneralLedgerService() {}
    public static synchronized GeneralLedgerService getInstance() {
        if (instance == null) instance = new GeneralLedgerService();
        return instance;
    }

    // ── Journal posting (double-entry) ───────────────────────────────────────

    /**
     * Post a balanced journal entry.
     * debits  = list of {glCode, amount, description}
     * credits = list of {glCode, amount, description}
     * Total debits must equal total credits.
     */
    public String postJournal(List<String[]> debits, List<String[]> credits,
                               String referenceType, String referenceId) throws Exception {
        double totalDebit  = debits.stream().mapToDouble(r -> Double.parseDouble(r[1])).sum();
        double totalCredit = credits.stream().mapToDouble(r -> Double.parseDouble(r[1])).sum();
        if (Math.abs(totalDebit - totalCredit) > 0.005)
            throw new Exception(String.format(
                "Journal not balanced — Debit: %s  Credit: %s",
                BankUtil.formatCurrency(totalDebit), BankUtil.formatCurrency(totalCredit)));

        String journalId = "JNL" + System.currentTimeMillis();
        int userId = SessionManager.getInstance().isLoggedIn()
            ? SessionManager.getInstance().getCurrentUser().getId() : 0;
        String period = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String[] d : debits)
                    postLine(conn, journalId, d[0], "DEBIT",
                        Double.parseDouble(d[1]), d[2], referenceType, referenceId, userId, period);
                for (String[] c : credits)
                    postLine(conn, journalId, c[0], "CREDIT",
                        Double.parseDouble(c[1]), c[2], referenceType, referenceId, userId, period);
                conn.commit();
                audit.log("POST_JOURNAL","GL",journalId,null,null,
                    referenceType + "/" + referenceId + " Dr:" + BankUtil.formatCurrency(totalDebit));
                return journalId;
            } catch (Exception e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void postLine(Connection conn, String journalId, String glCode,
                           String entryType, double amount, String desc,
                           String refType, String refId, int userId, String period)
            throws SQLException {
        // Update GL balance
        String balUpdate = "DEBIT".equals(entryType)
            ? "UPDATE gl_accounts SET balance = CASE WHEN normal_balance='DEBIT' THEN balance+? ELSE balance-? END, updated_at=CURRENT_TIMESTAMP WHERE gl_code=?"
            : "UPDATE gl_accounts SET balance = CASE WHEN normal_balance='CREDIT' THEN balance+? ELSE balance-? END, updated_at=CURRENT_TIMESTAMP WHERE gl_code=?";
        PreparedStatement upd = conn.prepareStatement(balUpdate);
        upd.setDouble(1, amount); upd.setDouble(2, amount); upd.setString(3, glCode);
        upd.executeUpdate();

        // Fetch new running balance
        PreparedStatement rb = conn.prepareStatement("SELECT balance FROM gl_accounts WHERE gl_code=?");
        rb.setString(1, glCode);
        ResultSet rbRs = rb.executeQuery();
        double running = rbRs.next() ? rbRs.getDouble(1) : 0;

        // Insert ledger entry
        PreparedStatement ins = conn.prepareStatement(
            "INSERT INTO ledger_entries (journal_id,gl_code,entry_type,amount,description," +
            "reference_type,reference_id,performed_by,period,running_balance) VALUES (?,?,?,?,?,?,?,?,?,?)");
        ins.setString(1, journalId); ins.setString(2, glCode); ins.setString(3, entryType);
        ins.setDouble(4, amount); ins.setString(5, desc);
        ins.setString(6, refType); ins.setString(7, refId);
        if (userId > 0) ins.setInt(8, userId); else ins.setNull(8, Types.INTEGER);
        ins.setString(9, period); ins.setDouble(10, running);
        ins.executeUpdate();
    }

    // ── Convenience wrappers for common bank transactions ────────────────────

    public String journalDeposit(int accountId, double amount, String txnId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        List<String[]> dr = new ArrayList<>(); dr.add(new String[]{"1001", String.valueOf(amount), "Cash Deposit - " + acctNo});
        List<String[]> cr = new ArrayList<>(); cr.add(new String[]{"2001", String.valueOf(amount), "Deposit credit - " + acctNo});
        return postJournal(dr, cr, "TRANSACTION", txnId);
    }

    public String journalWithdrawal(int accountId, double amount, String txnId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        List<String[]> dr = new ArrayList<>(); dr.add(new String[]{"2001", String.valueOf(amount), "Withdrawal debit - " + acctNo});
        List<String[]> cr = new ArrayList<>(); cr.add(new String[]{"1001", String.valueOf(amount), "Cash paid - " + acctNo});
        return postJournal(dr, cr, "TRANSACTION", txnId);
    }

    public String journalTransfer(int fromId, int toId, double amount, String txnId) throws Exception {
        String from = getAccountNumber(fromId), to = getAccountNumber(toId);
        List<String[]> dr = new ArrayList<>(); dr.add(new String[]{"9002", String.valueOf(amount), "Transfer from " + from});
        List<String[]> cr = new ArrayList<>(); cr.add(new String[]{"9002", String.valueOf(amount), "Transfer to " + to});
        return postJournal(dr, cr, "TRANSACTION", txnId);
    }

    public String journalInterestCredit(int accountId, double interest,
                                         String accrualId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        List<String[]> dr = new ArrayList<>(); dr.add(new String[]{"5001", String.valueOf(interest), "Interest expense - " + acctNo});
        List<String[]> cr = new ArrayList<>(); cr.add(new String[]{"2001", String.valueOf(interest), "Interest credited - " + acctNo});
        return postJournal(dr, cr, "INTEREST", accrualId);
    }

    public String journalLoanDisbursement(int accountId, double amount,
                                           String loanId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        List<String[]> dr = new ArrayList<>(); dr.add(new String[]{"1101", String.valueOf(amount), "Loan disbursed - " + loanId});
        List<String[]> cr = new ArrayList<>(); cr.add(new String[]{"1001", String.valueOf(amount), "Paid to " + acctNo});
        return postJournal(dr, cr, "LOAN", loanId);
    }

    public String journalLoanRepayment(double principal, double interest,
                                        String loanId) throws Exception {
        List<String[]> debits = new ArrayList<>();
        List<String[]> credits = new ArrayList<>();
        debits.add(new String[]{"1001", String.valueOf(principal+interest), "Loan repayment - "+loanId});
        credits.add(new String[]{"1101", String.valueOf(principal), "Principal recovered - "+loanId});
        credits.add(new String[]{"4001", String.valueOf(interest), "Interest income - "+loanId});
        return postJournal(debits, credits, "LOAN", loanId);
    }

    // ── Reporting ────────────────────────────────────────────────────────────

    public List<GeneralLedger> getChartOfAccounts() throws SQLException {
        List<GeneralLedger> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM gl_accounts WHERE is_active=1 ORDER BY gl_code");
            while (rs.next()) list.add(mapGL(rs));
        }
        return list;
    }

    /** Balance Sheet — Assets vs Liabilities+Equity */
    public Map<String, List<GeneralLedger>> getBalanceSheet() throws SQLException {
        Map<String, List<GeneralLedger>> bs = new LinkedHashMap<>();
        bs.put("ASSET",     new ArrayList<>());
        bs.put("LIABILITY", new ArrayList<>());
        bs.put("EQUITY",    new ArrayList<>());

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM gl_accounts WHERE is_active=1 AND category IN ('ASSET','LIABILITY','EQUITY') ORDER BY gl_code");
            while (rs.next()) {
                GeneralLedger gl = mapGL(rs);
                bs.get(gl.getCategory()).add(gl);
            }
        }
        return bs;
    }

    /** Profit & Loss — Income vs Expense */
    public Map<String, List<GeneralLedger>> getProfitAndLoss() throws SQLException {
        Map<String, List<GeneralLedger>> pl = new LinkedHashMap<>();
        pl.put("INCOME",  new ArrayList<>());
        pl.put("EXPENSE", new ArrayList<>());
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM gl_accounts WHERE is_active=1 AND category IN ('INCOME','EXPENSE') ORDER BY gl_code");
            while (rs.next()) {
                GeneralLedger gl = mapGL(rs);
                pl.get(gl.getCategory()).add(gl);
            }
        }
        return pl;
    }

    public List<LedgerEntry> getLedgerEntries(String glCode, int limit) throws SQLException {
        List<LedgerEntry> list = new ArrayList<>();
        String sql = "SELECT le.*, u.full_name AS uname " +
            "FROM ledger_entries le LEFT JOIN users u ON le.performed_by=u.id " +
            "WHERE le.gl_code=? ORDER BY le.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, glCode); ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapEntry(rs));
        }
        return list;
    }

    public List<LedgerEntry> getAllJournalEntries(int limit) throws SQLException {
        List<LedgerEntry> list = new ArrayList<>();
        String sql = "SELECT le.*, ga.gl_name, u.full_name AS uname " +
            "FROM ledger_entries le " +
            "LEFT JOIN gl_accounts ga ON le.gl_code=ga.gl_code " +
            "LEFT JOIN users u ON le.performed_by=u.id " +
            "ORDER BY le.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapEntry(rs));
        }
        return list;
    }

    public double getTotalAssets() throws SQLException {
        return sumCategory("ASSET");
    }
    public double getTotalLiabilities() throws SQLException {
        return sumCategory("LIABILITY");
    }
    public double getTotalEquity() throws SQLException {
        return sumCategory("EQUITY");
    }
    public double getTotalIncome() throws SQLException {
        return sumCategory("INCOME");
    }
    public double getTotalExpense() throws SQLException {
        return sumCategory("EXPENSE");
    }
    public double getNetProfit() throws SQLException {
        return getTotalIncome() - getTotalExpense();
    }

    /** Add or update a GL account */
    public void upsertGLAccount(GeneralLedger gl) throws Exception {
        if (!SessionManager.getInstance().hasPermission(8))
            throw new SecurityException("Managing GL accounts requires level 8+");
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            ResultSet ex = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM gl_accounts WHERE gl_code='" + gl.getGlCode() + "'");
            if (ex.next() && ex.getInt(1) > 0) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE gl_accounts SET gl_name=?,category=?,normal_balance=?," +
                    "description=?,is_active=?,updated_at=CURRENT_TIMESTAMP WHERE gl_code=?");
                ps.setString(1,gl.getGlName()); ps.setString(2,gl.getCategory());
                ps.setString(3,gl.getNormalBalance()); ps.setString(4,gl.getDescription());
                ps.setInt(5,gl.isActive()?1:0); ps.setString(6,gl.getGlCode());
                ps.executeUpdate();
            } else {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO gl_accounts (gl_code,gl_name,category,normal_balance,description,is_internal) VALUES (?,?,?,?,?,?)");
                ps.setString(1,gl.getGlCode()); ps.setString(2,gl.getGlName());
                ps.setString(3,gl.getCategory()); ps.setString(4,gl.getNormalBalance());
                ps.setString(5,gl.getDescription()); ps.setInt(6,gl.isInternal()?1:0);
                ps.executeUpdate();
            }
            audit.log("UPSERT_GL","GL",gl.getGlCode(),null,gl.getGlName(),"GL account saved");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getAccountNumber(int accountId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT account_number FROM accounts WHERE id=?")) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : String.valueOf(accountId);
        } catch (Exception e) { return String.valueOf(accountId); }
    }

    private double sumCategory(String category) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(balance),0) FROM gl_accounts WHERE category=? AND is_active=1")) {
            ps.setString(1, category);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private GeneralLedger mapGL(ResultSet rs) throws SQLException {
        GeneralLedger gl = new GeneralLedger();
        gl.setId(rs.getInt("id"));
        gl.setGlCode(rs.getString("gl_code"));
        gl.setGlName(rs.getString("gl_name"));
        gl.setCategory(rs.getString("category"));
        gl.setNormalBalance(rs.getString("normal_balance"));
        gl.setBalance(rs.getDouble("balance"));
        gl.setDescription(rs.getString("description"));
        gl.setActive(rs.getInt("is_active") == 1);
        gl.setInternal(rs.getInt("is_internal") == 1);
        try { gl.setParentCode(rs.getString("parent_code")); } catch (Exception ignored) {}
        return gl;
    }

    private LedgerEntry mapEntry(ResultSet rs) throws SQLException {
        LedgerEntry e = new LedgerEntry();
        e.setId(rs.getInt("id"));
        e.setJournalId(rs.getString("journal_id"));
        e.setGlCode(rs.getString("gl_code"));
        e.setEntryType(rs.getString("entry_type"));
        e.setAmount(rs.getDouble("amount"));
        e.setCurrency(rs.getString("currency"));
        e.setDescription(rs.getString("description"));
        e.setReferenceType(rs.getString("reference_type"));
        e.setReferenceId(rs.getString("reference_id"));
        e.setRunningBalance(rs.getDouble("running_balance"));
        e.setPeriod(rs.getString("period"));
        try { e.setGlName(rs.getString("gl_name")); } catch (Exception ignored) {}
        try { e.setPerformedByName(rs.getString("uname")); } catch (Exception ignored) {}
        String ca = rs.getString("created_at");
        if (ca != null) { try { e.setCreatedAt(LocalDateTime.parse(ca.replace(" ","T"))); } catch (Exception ignored) {} }
        return e;
    }
}
