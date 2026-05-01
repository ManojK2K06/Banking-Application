# BankPro — Banking Management System

## Default Login
- Username: `admin`
- Password: `Admin@1234`

## Run
**Windows:** Double-click `run.bat` or: `java -jar BankPro.jar`
**Linux/Mac:** `./run.sh` or `java -jar BankPro.jar`

**Requires:** Java 17+ (Java 21 recommended)

## Features
| Module | Description |
|--------|-------------|
| Dashboard | Live stats: AUM, customers, txn volume, pending loans |
| Customers | Register, search (binary search), KYC, profile |
| Accounts | Savings/Current/Salary/NRI — open, freeze, statement |
| Deposit | Cash deposits with receipt |
| Withdrawal | Cash withdrawal with balance check |
| Transfer | NEFT / RTGS / IMPS between accounts |
| Loans | Apply, approve, disburse, repay EMI + calculator |
| Fixed Deposits | Create FD, premature break, maturity calculation |
| Cards | Issue Debit/Credit/Prepaid (Visa/Mastercard/RuPay) |
| Forex / SWIFT | Currency converter + international wire transfers |
| Audit Log | Full audit trail of every action |
| User Management | Create staff, assign permission levels 1–10 |
| Settings | Change password, update currency rates |

## Permission Levels
| Level | Role |
|-------|------|
| 1 | Junior Clerk |
| 2 | Senior Clerk (can create customers/accounts) |
| 3 | Teller (can process loans) |
| 4 | Supervisor (large withdrawals, KYC) |
| 5 | Asst Manager (approve/disburse loans, SWIFT) |
| 6 | Branch Manager (close/freeze accounts) |
| 7 | Regional Manager (update FX rates) |
| 8 | Senior Manager (create users, reset passwords) |
| 9 | Director (deactivate users) |
| 10 | CTO / Admin (all permissions, change any level) |

## Database
SQLite (`bankpro.db`) — created automatically on first run.
All transactions, audit logs, and customer data are persisted.

## Architecture
- **UI:** Java Swing (dark professional theme)
- **DB:** SQLite via JDBC (embedded, zero-config)
- **Security:** SHA-256 + salt (10,000 rounds), session management, role-based access
- **Algorithms:** Binary search for customer lookup, Luhn card generation, EMI formula
- **Audit:** Every action logged to `audit_log` table with user, timestamp, entity

