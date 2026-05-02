package com.bankpro.db;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final String DB_PATH = "bankpro.db";
    private static final String URL     = "jdbc:sqlite:" + DB_PATH;
    private static DatabaseManager instance;
    private static final Logger logger  = Logger.getLogger(DatabaseManager.class.getName());

    static {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("SQLite JDBC driver not found", e); }
    }

    private DatabaseManager() {}
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    public void initializeDatabase() {
        try (Connection wc = DriverManager.getConnection(URL); Statement ws = wc.createStatement()) {
            ws.execute("PRAGMA journal_mode = WAL");
            ws.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException ignored) {}

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            createTables(stmt);
            insertDefaultData(conn);
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private void createTables(Statement stmt) throws SQLException {
        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "username TEXT UNIQUE NOT NULL," +
            "password_hash TEXT NOT NULL," +
            "salt TEXT NOT NULL," +
            "full_name TEXT NOT NULL," +
            "email TEXT UNIQUE NOT NULL," +
            "phone TEXT," +
            "permission_level INTEGER NOT NULL DEFAULT 1," +
            "is_active INTEGER NOT NULL DEFAULT 1," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "last_login TIMESTAMP," +
            "created_by INTEGER," +
            "department TEXT," +
            "employee_id TEXT UNIQUE NOT NULL," +
            "FOREIGN KEY (created_by) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS parties (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "party_id TEXT UNIQUE NOT NULL," +
            "party_type TEXT NOT NULL DEFAULT 'INDIVIDUAL'," +
            "first_name TEXT," +
            "last_name TEXT," +
            "date_of_birth TEXT," +
            "gender TEXT," +
            "company_name TEXT," +
            "registration_number TEXT," +
            "incorporation_date TEXT," +
            "business_type TEXT," +
            "contact_person_name TEXT," +
            "contact_person_desig TEXT," +
            "email TEXT UNIQUE NOT NULL," +
            "phone TEXT NOT NULL," +
            "alt_phone TEXT," +
            "address TEXT," +
            "city TEXT," +
            "state TEXT," +
            "pincode TEXT," +
            "country TEXT DEFAULT 'India'," +
            "aadhar_number TEXT," +
            "pan_number TEXT UNIQUE," +
            "gst_number TEXT," +
            "cin_number TEXT," +
            "kyc_status TEXT DEFAULT 'PENDING'," +
            "kyc_documents TEXT," +
            "credit_score INTEGER DEFAULT 650," +
            "segment TEXT DEFAULT 'RETAIL'," +
            "is_active INTEGER DEFAULT 1," +
            "created_by INTEGER," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (created_by) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS accounts (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "account_number TEXT UNIQUE NOT NULL," +
            "party_id INTEGER," +
            "account_type TEXT NOT NULL," +
            "account_category TEXT NOT NULL DEFAULT 'PARTY'," +
            "balance REAL NOT NULL DEFAULT 0.0," +
            "currency TEXT NOT NULL DEFAULT 'INR'," +
            "status TEXT NOT NULL DEFAULT 'ACTIVE'," +
            "interest_rate REAL DEFAULT 0.0," +
            "minimum_balance REAL DEFAULT 0.0," +
            "overdraft_limit REAL DEFAULT 0.0," +
            "last_interest_date TEXT," +
            "accrued_interest REAL DEFAULT 0.0," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "last_transaction_at TIMESTAMP," +
            "branch_code TEXT DEFAULT 'MAIN001'," +
            "ifsc_code TEXT DEFAULT 'BPRO0001'," +
            "gl_code TEXT," +
            "internal_name TEXT," +
            "FOREIGN KEY (party_id) REFERENCES parties(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS interest_rules (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "rule_code TEXT UNIQUE NOT NULL," +
            "account_type TEXT NOT NULL," +
            "annual_rate REAL NOT NULL," +
            "min_balance REAL DEFAULT 0.0," +
            "calculation_method TEXT DEFAULT 'DAILY_BALANCE'," +
            "credit_frequency TEXT DEFAULT 'MONTHLY'," +
            "is_active INTEGER DEFAULT 1," +
            "updated_by INTEGER," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "notes TEXT," +
            "FOREIGN KEY (updated_by) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS interest_accrual (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "account_id INTEGER NOT NULL," +
            "accrual_date TEXT NOT NULL," +
            "period_from TEXT NOT NULL," +
            "period_to TEXT NOT NULL," +
            "avg_balance REAL NOT NULL," +
            "rate_applied REAL NOT NULL," +
            "interest_amount REAL NOT NULL," +
            "status TEXT DEFAULT 'CREDITED'," +
            "journal_id TEXT," +
            "processed_by INTEGER," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (account_id) REFERENCES accounts(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS gl_accounts (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "gl_code TEXT UNIQUE NOT NULL," +
            "gl_name TEXT NOT NULL," +
            "category TEXT NOT NULL," +
            "normal_balance TEXT NOT NULL DEFAULT 'DEBIT'," +
            "balance REAL DEFAULT 0.0," +
            "description TEXT," +
            "parent_code TEXT," +
            "is_active INTEGER DEFAULT 1," +
            "is_internal INTEGER DEFAULT 0," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        stmt.execute("CREATE TABLE IF NOT EXISTS ledger_entries (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "journal_id TEXT NOT NULL," +
            "gl_code TEXT NOT NULL," +
            "entry_type TEXT NOT NULL," +
            "amount REAL NOT NULL," +
            "currency TEXT DEFAULT 'INR'," +
            "description TEXT," +
            "reference_type TEXT," +
            "reference_id TEXT," +
            "performed_by INTEGER," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "period TEXT," +
            "running_balance REAL DEFAULT 0.0," +
            "FOREIGN KEY (gl_code) REFERENCES gl_accounts(gl_code)," +
            "FOREIGN KEY (performed_by) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "transaction_id TEXT UNIQUE NOT NULL," +
            "from_account_id INTEGER," +
            "to_account_id INTEGER," +
            "transaction_type TEXT NOT NULL," +
            "amount REAL NOT NULL," +
            "currency TEXT DEFAULT 'INR'," +
            "amount_inr REAL," +
            "exchange_rate REAL DEFAULT 1.0," +
            "status TEXT DEFAULT 'COMPLETED'," +
            "description TEXT," +
            "reference_number TEXT," +
            "performed_by INTEGER," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "channel TEXT DEFAULT 'BRANCH'," +
            "balance_after_from REAL," +
            "balance_after_to REAL," +
            "narration TEXT," +
            "journal_id TEXT," +
            "FOREIGN KEY (from_account_id) REFERENCES accounts(id)," +
            "FOREIGN KEY (to_account_id) REFERENCES accounts(id)," +
            "FOREIGN KEY (performed_by) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS loans (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "loan_id TEXT UNIQUE NOT NULL," +
            "party_id INTEGER NOT NULL," +
            "account_id INTEGER NOT NULL," +
            "loan_type TEXT NOT NULL," +
            "principal_amount REAL NOT NULL," +
            "outstanding_amount REAL NOT NULL," +
            "interest_rate REAL NOT NULL," +
            "tenure_months INTEGER NOT NULL," +
            "emi_amount REAL NOT NULL," +
            "status TEXT DEFAULT 'PENDING'," +
            "approved_by INTEGER," +
            "disbursed_at TIMESTAMP," +
            "next_emi_date TEXT," +
            "total_paid REAL DEFAULT 0.0," +
            "penalty_charges REAL DEFAULT 0.0," +
            "purpose TEXT," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "collateral TEXT," +
            "FOREIGN KEY (party_id) REFERENCES parties(id)," +
            "FOREIGN KEY (account_id) REFERENCES accounts(id)," +
            "FOREIGN KEY (approved_by) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS loan_repayments (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "loan_id INTEGER NOT NULL," +
            "payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "amount_paid REAL NOT NULL," +
            "principal_paid REAL NOT NULL," +
            "interest_paid REAL NOT NULL," +
            "penalty_paid REAL DEFAULT 0.0," +
            "balance_after REAL NOT NULL," +
            "transaction_id TEXT," +
            "performed_by INTEGER," +
            "FOREIGN KEY (loan_id) REFERENCES loans(id)," +
            "FOREIGN KEY (performed_by) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS fixed_deposits (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "fd_number TEXT UNIQUE NOT NULL," +
            "party_id INTEGER NOT NULL," +
            "account_id INTEGER NOT NULL," +
            "principal_amount REAL NOT NULL," +
            "interest_rate REAL NOT NULL," +
            "tenure_months INTEGER NOT NULL," +
            "maturity_amount REAL NOT NULL," +
            "start_date TEXT NOT NULL," +
            "maturity_date TEXT NOT NULL," +
            "status TEXT DEFAULT 'ACTIVE'," +
            "auto_renew INTEGER DEFAULT 0," +
            "created_by INTEGER," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "premature_withdrawal_penalty REAL DEFAULT 1.0," +
            "FOREIGN KEY (party_id) REFERENCES parties(id)," +
            "FOREIGN KEY (account_id) REFERENCES accounts(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS cards (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "card_number TEXT UNIQUE NOT NULL," +
            "account_id INTEGER NOT NULL," +
            "card_type TEXT NOT NULL," +
            "card_holder_name TEXT NOT NULL," +
            "expiry_date TEXT NOT NULL," +
            "cvv_hash TEXT NOT NULL," +
            "pin_hash TEXT," +
            "status TEXT DEFAULT 'ACTIVE'," +
            "credit_limit REAL DEFAULT 0.0," +
            "current_balance REAL DEFAULT 0.0," +
            "issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "last_used TIMESTAMP," +
            "network TEXT DEFAULT 'VISA'," +
            "FOREIGN KEY (account_id) REFERENCES accounts(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS currency_rates (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "currency_code TEXT UNIQUE NOT NULL," +
            "currency_name TEXT NOT NULL," +
            "rate_to_inr REAL NOT NULL," +
            "symbol TEXT NOT NULL," +
            "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        stmt.execute("CREATE TABLE IF NOT EXISTS audit_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER," +
            "action TEXT NOT NULL," +
            "entity_type TEXT," +
            "entity_id TEXT," +
            "old_value TEXT," +
            "new_value TEXT," +
            "ip_address TEXT DEFAULT 'localhost'," +
            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "session_id TEXT," +
            "status TEXT DEFAULT 'SUCCESS'," +
            "details TEXT," +
            "FOREIGN KEY (user_id) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS sessions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id INTEGER NOT NULL," +
            "session_id TEXT UNIQUE NOT NULL," +
            "login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "logout_at TIMESTAMP," +
            "ip_address TEXT DEFAULT 'localhost'," +
            "is_active INTEGER DEFAULT 1," +
            "FOREIGN KEY (user_id) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS beneficiaries (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "party_id INTEGER NOT NULL," +
            "beneficiary_name TEXT NOT NULL," +
            "account_number TEXT NOT NULL," +
            "bank_name TEXT," +
            "ifsc_code TEXT," +
            "is_active INTEGER DEFAULT 1," +
            "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (party_id) REFERENCES parties(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS cheques (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "cheque_number TEXT NOT NULL," +
            "account_id INTEGER NOT NULL," +
            "payee_name TEXT NOT NULL," +
            "amount REAL NOT NULL," +
            "issue_date TEXT," +
            "clearing_date TEXT," +
            "status TEXT DEFAULT 'ISSUED'," +
            "processed_by INTEGER," +
            "FOREIGN KEY (account_id) REFERENCES accounts(id)," +
            "FOREIGN KEY (processed_by) REFERENCES users(id))");
    }

    private void insertDefaultData(Connection conn) throws SQLException {
        String[] currencies = {
            "('USD','US Dollar',83.50,'$')", "('EUR','Euro',90.25,'€')",
            "('GBP','British Pound',105.80,'£')", "('JPY','Japanese Yen',0.56,'¥')",
            "('AUD','Australian Dollar',54.20,'A$')", "('CAD','Canadian Dollar',61.50,'C$')",
            "('CHF','Swiss Franc',93.40,'Fr')", "('SGD','Singapore Dollar',61.80,'S$')",
            "('AED','UAE Dirham',22.74,'د.إ')", "('INR','Indian Rupee',1.00,'₹')"
        };
        for (String c : currencies)
            try { conn.createStatement().execute(
                "INSERT OR IGNORE INTO currency_rates (currency_code,currency_name,rate_to_inr,symbol) VALUES " + c);
            } catch (SQLException ignored) {}

        ResultSet ck = conn.createStatement().executeQuery("SELECT COUNT(*) FROM users WHERE username='admin'");
        if (ck.next() && ck.getInt(1) == 0) {
            String salt = com.bankpro.security.PasswordUtil.generateSalt();
            String hash = com.bankpro.security.PasswordUtil.hashPassword("Admin@1234", salt);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username,password_hash,salt,full_name,email,phone,permission_level,employee_id,department) VALUES (?,?,?,?,?,?,?,?,?)");
            ps.setString(1,"admin"); ps.setString(2,hash); ps.setString(3,salt);
            ps.setString(4,"System Administrator"); ps.setString(5,"admin@bankpro.com");
            ps.setString(6,"9999999999"); ps.setInt(7,10);
            ps.setString(8,"EMP00001"); ps.setString(9,"IT Administration");
            ps.executeUpdate();
            logger.info("Admin user created: admin / Admin@1234");
        }

        String[][] rules = {
            {"IR-SAV","SAVINGS","3.50","1000","DAILY_BALANCE","MONTHLY","Savings account standard rate"},
            {"IR-CUR","CURRENT","0.00","10000","DAILY_BALANCE","MONTHLY","Current account - no interest"},
            {"IR-SAL","SALARY","3.50","0","DAILY_BALANCE","MONTHLY","Salary account rate"},
            {"IR-NRI","NRI","4.00","75000","DAILY_BALANCE","MONTHLY","NRI savings rate"},
        };
        for (String[] r : rules) {
            try { conn.createStatement().execute(String.format(
                "INSERT OR IGNORE INTO interest_rules (rule_code,account_type,annual_rate,min_balance,calculation_method,credit_frequency,notes) VALUES ('%s','%s',%s,%s,'%s','%s','%s')",
                r[0],r[1],r[2],r[3],r[4],r[5],r[6])); } catch (SQLException ignored) {}
        }

        String[][] gl = {
            {"1001","Cash in Hand","ASSET","DEBIT","1"},
            {"1002","Nostro / Correspondent","ASSET","DEBIT","1"},
            {"1003","Investments","ASSET","DEBIT","0"},
            {"1101","Loans Receivable","ASSET","DEBIT","0"},
            {"1201","Interest Receivable","ASSET","DEBIT","0"},
            {"1301","Fixed Assets","ASSET","DEBIT","0"},
            {"2001","Customer Deposits","LIABILITY","CREDIT","0"},
            {"2002","Savings Deposits","LIABILITY","CREDIT","0"},
            {"2003","Current Deposits","LIABILITY","CREDIT","0"},
            {"2004","Fixed Deposits Liability","LIABILITY","CREDIT","0"},
            {"2005","NRI Deposits","LIABILITY","CREDIT","0"},
            {"2101","Interest Payable","LIABILITY","CREDIT","1"},
            {"2201","Other Liabilities","LIABILITY","CREDIT","0"},
            {"3001","Share Capital","EQUITY","CREDIT","1"},
            {"3002","Retained Earnings","EQUITY","CREDIT","1"},
            {"3003","General Reserves","EQUITY","CREDIT","1"},
            {"4001","Interest Income","INCOME","CREDIT","0"},
            {"4002","Fee & Commission Income","INCOME","CREDIT","0"},
            {"4003","Forex Income","INCOME","CREDIT","0"},
            {"4004","Other Income","INCOME","CREDIT","0"},
            {"5001","Interest Expense","EXPENSE","DEBIT","0"},
            {"5101","Operating Expense","EXPENSE","DEBIT","0"},
            {"5201","Provision for NPA","EXPENSE","DEBIT","0"},
            {"9001","Suspense Account","ASSET","DEBIT","1"},
            {"9002","Internal Clearing","ASSET","DEBIT","1"},
        };
        for (String[] g : gl) {
            try { conn.createStatement().execute(String.format(
                "INSERT OR IGNORE INTO gl_accounts (gl_code,gl_name,category,normal_balance,is_internal) VALUES ('%s','%s','%s','%s',%s)",
                g[0],g[1],g[2],g[3],g[4])); } catch (SQLException ignored) {}
        }

        String[][] ia = {
            {"INTERNAL-CASH-001","INTERNAL","1001","Cash Vault - Main Branch","10000000"},
            {"INTERNAL-NOSTRO-001","INTERNAL","1002","Nostro Account - RBI","0"},
            {"INTERNAL-CLR-001","INTERNAL","9002","Internal Clearing Account","0"},
            {"INTERNAL-SUSP-001","INTERNAL","9001","Suspense Account","0"},
            {"INTERNAL-EQUITY-001","INTERNAL","3001","Share Capital Account","50000000"},
            {"INTERNAL-INT-PAY-001","INTERNAL","2101","Interest Payable Pool","0"},
        };
        for (String[] a : ia) {
            try { conn.createStatement().execute(String.format(
                "INSERT OR IGNORE INTO accounts (account_number,account_type,account_category,balance,gl_code,internal_name) VALUES ('%s','%s','INTERNAL',%s,'%s','%s')",
                a[0],a[1],a[4],a[2],a[3])); } catch (SQLException ignored) {}
        }
    }
}
