package com.bankpro.db;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final String DB_PATH = "bankpro.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;
    private static DatabaseManager instance;
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    public void initializeDatabase() {
        // Set WAL mode first on its own connection
        try (Connection walConn = DriverManager.getConnection(URL);
             Statement ws = walConn.createStatement()) {
            ws.execute("PRAGMA journal_mode = WAL");
            ws.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException ignored) {}

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Users/Employees table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    phone TEXT,
                    permission_level INTEGER NOT NULL DEFAULT 1,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP,
                    created_by INTEGER,
                    department TEXT,
                    employee_id TEXT UNIQUE NOT NULL,
                    FOREIGN KEY (created_by) REFERENCES users(id)
                )
            """);

            // Customers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id TEXT UNIQUE NOT NULL,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    date_of_birth TEXT,
                    email TEXT UNIQUE NOT NULL,
                    phone TEXT NOT NULL,
                    address TEXT,
                    city TEXT,
                    state TEXT,
                    pincode TEXT,
                    country TEXT DEFAULT 'India',
                    aadhar_number TEXT UNIQUE,
                    pan_number TEXT UNIQUE,
                    kyc_status TEXT DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_by INTEGER,
                    is_active INTEGER DEFAULT 1,
                    credit_score INTEGER DEFAULT 650,
                    FOREIGN KEY (created_by) REFERENCES users(id)
                )
            """);

            // Accounts table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_number TEXT UNIQUE NOT NULL,
                    customer_id INTEGER NOT NULL,
                    account_type TEXT NOT NULL,
                    balance REAL NOT NULL DEFAULT 0.0,
                    currency TEXT NOT NULL DEFAULT 'INR',
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    interest_rate REAL DEFAULT 0.0,
                    minimum_balance REAL DEFAULT 0.0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_transaction_at TIMESTAMP,
                    branch_code TEXT DEFAULT 'MAIN001',
                    ifsc_code TEXT DEFAULT 'BPRO0001',
                    overdraft_limit REAL DEFAULT 0.0,
                    FOREIGN KEY (customer_id) REFERENCES customers(id)
                )
            """);

            // Cards table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    card_number TEXT UNIQUE NOT NULL,
                    account_id INTEGER NOT NULL,
                    card_type TEXT NOT NULL,
                    card_holder_name TEXT NOT NULL,
                    expiry_date TEXT NOT NULL,
                    cvv_hash TEXT NOT NULL,
                    pin_hash TEXT,
                    status TEXT DEFAULT 'ACTIVE',
                    credit_limit REAL DEFAULT 0.0,
                    current_balance REAL DEFAULT 0.0,
                    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_used TIMESTAMP,
                    network TEXT DEFAULT 'VISA',
                    FOREIGN KEY (account_id) REFERENCES accounts(id)
                )
            """);

            // Transactions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    transaction_id TEXT UNIQUE NOT NULL,
                    from_account_id INTEGER,
                    to_account_id INTEGER,
                    transaction_type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    currency TEXT DEFAULT 'INR',
                    amount_inr REAL,
                    exchange_rate REAL DEFAULT 1.0,
                    status TEXT DEFAULT 'COMPLETED',
                    description TEXT,
                    reference_number TEXT,
                    performed_by INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    channel TEXT DEFAULT 'BRANCH',
                    balance_after_from REAL,
                    balance_after_to REAL,
                    narration TEXT,
                    FOREIGN KEY (from_account_id) REFERENCES accounts(id),
                    FOREIGN KEY (to_account_id) REFERENCES accounts(id),
                    FOREIGN KEY (performed_by) REFERENCES users(id)
                )
            """);

            // Loans table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS loans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    loan_id TEXT UNIQUE NOT NULL,
                    customer_id INTEGER NOT NULL,
                    account_id INTEGER NOT NULL,
                    loan_type TEXT NOT NULL,
                    principal_amount REAL NOT NULL,
                    outstanding_amount REAL NOT NULL,
                    interest_rate REAL NOT NULL,
                    tenure_months INTEGER NOT NULL,
                    emi_amount REAL NOT NULL,
                    status TEXT DEFAULT 'PENDING',
                    approved_by INTEGER,
                    disbursed_at TIMESTAMP,
                    next_emi_date TEXT,
                    total_paid REAL DEFAULT 0.0,
                    penalty_charges REAL DEFAULT 0.0,
                    purpose TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    collateral TEXT,
                    FOREIGN KEY (customer_id) REFERENCES customers(id),
                    FOREIGN KEY (account_id) REFERENCES accounts(id),
                    FOREIGN KEY (approved_by) REFERENCES users(id)
                )
            """);

            // Loan repayments
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS loan_repayments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    loan_id INTEGER NOT NULL,
                    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    amount_paid REAL NOT NULL,
                    principal_paid REAL NOT NULL,
                    interest_paid REAL NOT NULL,
                    penalty_paid REAL DEFAULT 0.0,
                    balance_after REAL NOT NULL,
                    transaction_id TEXT,
                    performed_by INTEGER,
                    FOREIGN KEY (loan_id) REFERENCES loans(id),
                    FOREIGN KEY (performed_by) REFERENCES users(id)
                )
            """);

            // Fixed Deposits
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS fixed_deposits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fd_number TEXT UNIQUE NOT NULL,
                    customer_id INTEGER NOT NULL,
                    account_id INTEGER NOT NULL,
                    principal_amount REAL NOT NULL,
                    interest_rate REAL NOT NULL,
                    tenure_months INTEGER NOT NULL,
                    maturity_amount REAL NOT NULL,
                    start_date TEXT NOT NULL,
                    maturity_date TEXT NOT NULL,
                    status TEXT DEFAULT 'ACTIVE',
                    auto_renew INTEGER DEFAULT 0,
                    created_by INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    premature_withdrawal_penalty REAL DEFAULT 1.0,
                    FOREIGN KEY (customer_id) REFERENCES customers(id),
                    FOREIGN KEY (account_id) REFERENCES accounts(id)
                )
            """);

            // Audit log
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    entity_type TEXT,
                    entity_id TEXT,
                    old_value TEXT,
                    new_value TEXT,
                    ip_address TEXT DEFAULT 'localhost',
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    session_id TEXT,
                    status TEXT DEFAULT 'SUCCESS',
                    details TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            // Currency rates
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS currency_rates (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    currency_code TEXT UNIQUE NOT NULL,
                    currency_name TEXT NOT NULL,
                    rate_to_inr REAL NOT NULL,
                    symbol TEXT NOT NULL,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Beneficiaries
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS beneficiaries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    beneficiary_name TEXT NOT NULL,
                    account_number TEXT NOT NULL,
                    bank_name TEXT,
                    ifsc_code TEXT,
                    is_active INTEGER DEFAULT 1,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES customers(id)
                )
            """);

            // Notifications/Alerts
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER,
                    user_id INTEGER,
                    alert_type TEXT NOT NULL,
                    message TEXT NOT NULL,
                    is_read INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES customers(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            // Session table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    session_id TEXT UNIQUE NOT NULL,
                    login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    logout_at TIMESTAMP,
                    ip_address TEXT DEFAULT 'localhost',
                    is_active INTEGER DEFAULT 1,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            // Cheques table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cheques (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cheque_number TEXT NOT NULL,
                    account_id INTEGER NOT NULL,
                    payee_name TEXT NOT NULL,
                    amount REAL NOT NULL,
                    issue_date TEXT,
                    clearing_date TEXT,
                    status TEXT DEFAULT 'ISSUED',
                    processed_by INTEGER,
                    FOREIGN KEY (account_id) REFERENCES accounts(id),
                    FOREIGN KEY (processed_by) REFERENCES users(id)
                )
            """);

            insertDefaultData(conn);
            logger.info("Database initialized successfully");

        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private void insertDefaultData(Connection conn) throws SQLException {
        // Insert default currency rates
        String[] currencies = {
            "('USD','US Dollar',83.50,'$')",
            "('EUR','Euro',90.25,'€')",
            "('GBP','British Pound',105.80,'£')",
            "('JPY','Japanese Yen',0.56,'¥')",
            "('AUD','Australian Dollar',54.20,'A$')",
            "('CAD','Canadian Dollar',61.50,'C$')",
            "('CHF','Swiss Franc',93.40,'Fr')",
            "('SGD','Singapore Dollar',61.80,'S$')",
            "('AED','UAE Dirham',22.74,'د.إ')",
            "('INR','Indian Rupee',1.00,'₹')"
        };

        for (String cur : currencies) {
            try {
                conn.createStatement().execute(
                    "INSERT OR IGNORE INTO currency_rates (currency_code, currency_name, rate_to_inr, symbol) VALUES " + cur
                );
            } catch (SQLException ignored) {}
        }

        // Create admin user if not exists
        PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = 'admin'");
        ResultSet rs = check.executeQuery();
        if (rs.next() && rs.getInt(1) == 0) {
            // Salt and hash for admin/Admin@1234
            String salt = com.bankpro.security.PasswordUtil.generateSalt();
            String hash = com.bankpro.security.PasswordUtil.hashPassword("Admin@1234", salt);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, salt, full_name, email, phone, permission_level, employee_id, department) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, "admin");
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, "System Administrator");
            ps.setString(5, "admin@bankpro.com");
            ps.setString(6, "9999999999");
            ps.setInt(7, 10);
            ps.setString(8, "EMP00001");
            ps.setString(9, "IT Administration");
            ps.executeUpdate();
            logger.info("Admin user created: admin / Admin@1234");
        }
    }
}
