package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.Loan;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class LoanService {
    private static LoanService instance;
    private final AuditService audit = AuditService.getInstance();

    private LoanService() {}
    public static synchronized LoanService getInstance() {
        if (instance == null) instance = new LoanService();
        return instance;
    }

    public Loan applyLoan(int customerId, int accountId, String loanType,
                           double amount, int tenureMonths, double interestRate,
                           String purpose, String collateral) throws Exception {
        if (!SessionManager.getInstance().hasPermission(3))
            throw new SecurityException("Loan applications require level 3+ permission");
        if (amount <= 0) throw new Exception("Loan amount must be positive");
        if (tenureMonths <= 0 || tenureMonths > 360)
            throw new Exception("Tenure must be 1 to 360 months");
        if (interestRate <= 0 || interestRate > 36)
            throw new Exception("Interest rate must be between 0.01% and 36%");

        validateLoanLimits(loanType, amount);

        double emi = BankUtil.calculateEMI(amount, interestRate, tenureMonths);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO loans (loan_id,customer_id,account_id,loan_type,principal_amount," +
                 "outstanding_amount,interest_rate,tenure_months,emi_amount,status,purpose,collateral) " +
                 "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
            String loanId = BankUtil.generateLoanId();
            ps.setString(1, loanId);
            ps.setInt(2, customerId);
            ps.setInt(3, accountId);
            ps.setString(4, loanType);
            ps.setDouble(5, amount);
            ps.setDouble(6, amount);
            ps.setDouble(7, interestRate);
            ps.setInt(8, tenureMonths);
            ps.setDouble(9, emi);
            ps.setString(10, "PENDING");
            ps.setString(11, purpose);
            ps.setString(12, collateral);
            ps.executeUpdate();

            Loan loan = new Loan();
            ResultSet rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()");
            if (rs.next()) loan.setId(rs.getInt(1));
            loan.setLoanId(loanId);
            loan.setCustomerId(customerId);
            loan.setAccountId(accountId);
            loan.setLoanType(loanType);
            loan.setPrincipalAmount(amount);
            loan.setOutstandingAmount(amount);
            loan.setInterestRate(interestRate);
            loan.setTenureMonths(tenureMonths);
            loan.setEmiAmount(emi);
            loan.setStatus("PENDING");

            audit.log("APPLY_LOAN", "LOAN", loanId, null, loanType,
                String.format("Loan applied: %s %s for customer %d",
                    loanType, BankUtil.formatCurrency(amount), customerId));
            return loan;
        }
    }

    public void approveLoan(int loanId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Loan approval requires level 5+ permission");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE loans SET status='APPROVED', approved_by=? WHERE id=? AND status='PENDING'")) {
            ps.setInt(1, SessionManager.getInstance().getCurrentUser().getId());
            ps.setInt(2, loanId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new Exception("Loan not found or already processed");
            audit.log("APPROVE_LOAN", "LOAN", String.valueOf(loanId),
                "PENDING", "APPROVED", "Loan approved");
        }
    }

    public void rejectLoan(int loanId, String reason) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Loan rejection requires level 5+ permission");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE loans SET status='REJECTED' WHERE id=? AND status='PENDING'")) {
            ps.setInt(1, loanId);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new Exception("Loan not found or already processed");
            audit.log("REJECT_LOAN", "LOAN", String.valueOf(loanId),
                "PENDING", "REJECTED", "Reason: " + reason);
        }
    }

    public void disburseLoan(int loanId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(5))
            throw new SecurityException("Loan disbursement requires level 5+ permission");

        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM loans WHERE id=? AND status='APPROVED'");
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new Exception("Loan not found or not approved");

            double amount = rs.getDouble("principal_amount");
            int accountId = rs.getInt("account_id");
            String nextEmiDate = LocalDate.now().plusMonths(1).toString();

            // Credit loan amount to account
            PreparedStatement upd = conn.prepareStatement(
                "UPDATE accounts SET balance=balance+?, last_transaction_at=CURRENT_TIMESTAMP WHERE id=?");
            upd.setDouble(1, amount); upd.setInt(2, accountId); upd.executeUpdate();

            // Record transaction - get new balance first
            ResultSet balRs = conn.createStatement().executeQuery(
                "SELECT balance FROM accounts WHERE id=" + accountId);
            double newAcctBal = balRs.next() ? balRs.getDouble(1) : amount;
            PreparedStatement txn = conn.prepareStatement(
                "INSERT INTO transactions (transaction_id,to_account_id,transaction_type,amount," +
                "currency,amount_inr,exchange_rate,description,performed_by,balance_after_to) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)");
            txn.setString(1, BankUtil.generateTransactionId());
            txn.setInt(2, accountId);
            txn.setString(3, "LOAN_DISBURSEMENT");
            txn.setDouble(4, amount); txn.setString(5, "INR");
            txn.setDouble(6, amount); txn.setDouble(7, 1.0);
            txn.setString(8, "Loan disbursement: " + rs.getString("loan_id"));
            txn.setInt(9, SessionManager.getInstance().getCurrentUser().getId());
            txn.setDouble(10, newAcctBal);
            txn.executeUpdate();

            // Update loan status
            PreparedStatement updLoan = conn.prepareStatement(
                "UPDATE loans SET status='ACTIVE', disbursed_at=CURRENT_TIMESTAMP, " +
                "next_emi_date=? WHERE id=?");
            updLoan.setString(1, nextEmiDate); updLoan.setInt(2, loanId); updLoan.executeUpdate();

            conn.commit();
            audit.log("DISBURSE_LOAN", "LOAN", String.valueOf(loanId),
                "APPROVED", "ACTIVE",
                "Loan disbursed: " + BankUtil.formatCurrency(amount));
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public void repayLoan(int loanId, double amount) throws Exception {
        if (!SessionManager.getInstance().hasPermission(1))
            throw new SecurityException("Insufficient permissions");

        Connection conn = DatabaseManager.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM loans WHERE id=? AND status='ACTIVE'");
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new Exception("Loan not found or not active");

            double outstanding = rs.getDouble("outstanding_amount");
            double interestRate = rs.getDouble("interest_rate");
            double penalty = rs.getDouble("penalty_charges");
            int accountId = rs.getInt("account_id");

            if (amount <= 0) throw new Exception("Repayment amount must be positive");
            if (amount > outstanding + penalty + 1)
                throw new Exception(String.format("Payment %s exceeds outstanding %s",
                    BankUtil.formatCurrency(amount),
                    BankUtil.formatCurrency(outstanding + penalty)));

            // Check account balance
            PreparedStatement acct = conn.prepareStatement(
                "SELECT balance FROM accounts WHERE id=? AND status='ACTIVE'");
            acct.setInt(1, accountId);
            ResultSet acctRs = acct.executeQuery();
            if (!acctRs.next()) throw new Exception("Account not found or not active");
            double balance = acctRs.getDouble("balance");
            if (balance < amount)
                throw new Exception(String.format("Insufficient account balance. Available: %s",
                    BankUtil.formatCurrency(balance)));

            // Calculate interest and principal portions
            double monthlyRate = interestRate / (12 * 100);
            double interestPortion = outstanding * monthlyRate;
            double penaltyPaid = Math.min(penalty, amount);
            double remainingAfterPenalty = amount - penaltyPaid;
            double interestPaid = Math.min(interestPortion, remainingAfterPenalty);
            double principalPaid = remainingAfterPenalty - interestPaid;
            double newOutstanding = Math.max(0, outstanding - principalPaid);
            double newBalance = balance - amount;

            // Deduct from account
            conn.prepareStatement(
                "UPDATE accounts SET balance=" + newBalance + ", last_transaction_at=CURRENT_TIMESTAMP WHERE id=" + accountId
            ).executeUpdate();

            // Update loan
            String newStatus = newOutstanding <= 0.01 ? "CLOSED" : "ACTIVE";
            String nextEmiDate = newOutstanding > 0 ? LocalDate.now().plusMonths(1).toString() : null;
            PreparedStatement updLoan = conn.prepareStatement(
                "UPDATE loans SET outstanding_amount=?,total_paid=total_paid+?,penalty_charges=?," +
                "status=?,next_emi_date=? WHERE id=?");
            updLoan.setDouble(1, newOutstanding);
            updLoan.setDouble(2, amount);
            updLoan.setDouble(3, Math.max(0, penalty - penaltyPaid));
            updLoan.setString(4, newStatus);
            if (nextEmiDate != null) updLoan.setString(5, nextEmiDate); else updLoan.setNull(5, Types.VARCHAR);
            updLoan.setInt(6, loanId);
            updLoan.executeUpdate();

            // Record repayment
            String txnId = BankUtil.generateTransactionId();
            PreparedStatement repay = conn.prepareStatement(
                "INSERT INTO loan_repayments (loan_id,amount_paid,principal_paid,interest_paid," +
                "penalty_paid,balance_after,transaction_id,performed_by) VALUES (?,?,?,?,?,?,?,?)");
            repay.setInt(1, loanId);
            repay.setDouble(2, amount);
            repay.setDouble(3, principalPaid);
            repay.setDouble(4, interestPaid);
            repay.setDouble(5, penaltyPaid);
            repay.setDouble(6, newOutstanding);
            repay.setString(7, txnId);
            repay.setInt(8, SessionManager.getInstance().getCurrentUser().getId());
            repay.executeUpdate();

            // Record in transactions table
            conn.prepareStatement(
                "INSERT INTO transactions (transaction_id,from_account_id,transaction_type,amount," +
                "currency,amount_inr,exchange_rate,description,performed_by,balance_after_from) VALUES ('" +
                txnId + "'," + accountId + ",'LOAN_REPAYMENT'," + amount + ",'INR'," + amount + ",1.0," +
                "'Loan repayment: LID-" + loanId + "'," + SessionManager.getInstance().getCurrentUser().getId() +
                "," + newBalance + ")"
            ).executeUpdate();

            conn.commit();
            audit.log("LOAN_REPAYMENT", "LOAN", String.valueOf(loanId), null,
                BankUtil.formatCurrency(amount),
                String.format("Repaid %s. Outstanding: %s. Status: %s",
                    BankUtil.formatCurrency(amount),
                    BankUtil.formatCurrency(newOutstanding), newStatus));
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public List<Loan> getLoansByCustomer(int customerId) throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT l.*, c.first_name||' '||c.last_name AS cname, a.account_number, " +
                 "u.full_name AS approved_by_name FROM loans l " +
                 "LEFT JOIN customers c ON l.customer_id=c.id " +
                 "LEFT JOIN accounts a ON l.account_id=a.id " +
                 "LEFT JOIN users u ON l.approved_by=u.id " +
                 "WHERE l.customer_id=? ORDER BY l.created_at DESC")) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapLoan(rs));
        }
        return list;
    }

    public List<Loan> getAllLoans() throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT l.*, c.first_name||' '||c.last_name AS cname, a.account_number, " +
                 "u.full_name AS approved_by_name FROM loans l " +
                 "LEFT JOIN customers c ON l.customer_id=c.id " +
                 "LEFT JOIN accounts a ON l.account_id=a.id " +
                 "LEFT JOIN users u ON l.approved_by=u.id " +
                 "ORDER BY l.created_at DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapLoan(rs));
        }
        return list;
    }

    public List<Loan> getPendingLoans() throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT l.*, c.first_name||' '||c.last_name AS cname, a.account_number, " +
                 "u.full_name AS approved_by_name FROM loans l " +
                 "LEFT JOIN customers c ON l.customer_id=c.id " +
                 "LEFT JOIN accounts a ON l.account_id=a.id " +
                 "LEFT JOIN users u ON l.approved_by=u.id " +
                 "WHERE l.status='PENDING' ORDER BY l.created_at")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapLoan(rs));
        }
        return list;
    }

    public double getTotalLoanOutstanding() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT SUM(outstanding_amount) FROM loans WHERE status IN ('ACTIVE','DISBURSED')");
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private void validateLoanLimits(String type, double amount) throws Exception {
        double max = switch (type) {
            case "PERSONAL" -> 40_00_000;
            case "HOME" -> 10_00_00_000;
            case "CAR" -> 1_50_00_000;
            case "EDUCATION" -> 50_00_000;
            case "BUSINESS" -> 5_00_00_000;
            case "GOLD" -> 50_00_000;
            case "MORTGAGE" -> 10_00_00_000;
            default -> 1_00_00_000;
        };
        if (amount > max)
            throw new Exception(String.format("%s loan maximum is %s",
                type, BankUtil.formatCurrency(max)));
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        Loan l = new Loan();
        l.setId(rs.getInt("id"));
        l.setLoanId(rs.getString("loan_id"));
        l.setCustomerId(rs.getInt("customer_id"));
        l.setAccountId(rs.getInt("account_id"));
        l.setLoanType(rs.getString("loan_type"));
        l.setPrincipalAmount(rs.getDouble("principal_amount"));
        l.setOutstandingAmount(rs.getDouble("outstanding_amount"));
        l.setInterestRate(rs.getDouble("interest_rate"));
        l.setTenureMonths(rs.getInt("tenure_months"));
        l.setEmiAmount(rs.getDouble("emi_amount"));
        l.setStatus(rs.getString("status"));
        l.setNextEmiDate(rs.getString("next_emi_date"));
        l.setTotalPaid(rs.getDouble("total_paid"));
        l.setPenaltyCharges(rs.getDouble("penalty_charges"));
        l.setPurpose(rs.getString("purpose"));
        l.setCollateral(rs.getString("collateral"));
        try { l.setCustomerName(rs.getString("cname")); } catch (SQLException ignored) {}
        try { l.setAccountNumber(rs.getString("account_number")); } catch (SQLException ignored) {}
        try { l.setApprovedByName(rs.getString("approved_by_name")); } catch (SQLException ignored) {}
        String da = rs.getString("disbursed_at");
        if (da != null) {
            try { l.setDisbursedAt(LocalDateTime.parse(da.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        String ca = rs.getString("created_at");
        if (ca != null) {
            try { l.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        return l;
    }
}
