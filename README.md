# BankPro — Banking Management System v2.0

## Default Login

Username: `admin` Password: `Admin@1234`

## Run

Windows: go run the Main.java file
Requires Java 17+

## New in v2.0

- **Parties** replaces Customers — supports Individual, Corporate, Partnership, Trust, Government
- **Internal Accounts** — Cash Vault, Nostro, Clearing, Suspense linked to GL codes
- **General Ledger** — full double-entry bookkeeping, Balance Sheet, P&L, Chart of Accounts
- **Interest Engine** — 30-day cycle auto-processing, manager-configurable rules, per-account overrides

## Navigation

| Page                 | Min Level | Description                                             |
| -------------------- | --------- | ------------------------------------------------------- |
| Dashboard            | 1         | Live AUM, parties, transactions, loans                  |
| Parties              | 2         | Individual & Corporate party management                 |
| Accounts             | 2         | Party + internal accounts, statements, interest history |
| Deposit / Withdrawal | 1         | Cash operations with GL posting                         |
| Transfer             | 1         | NEFT / RTGS / IMPS                                      |
| Loans                | 3         | Apply, approve, disburse, repay                         |
| Fixed Deposits       | 2         | Create, manage, premature break                         |
| Cards                | 3         | Issue Debit/Credit/Prepaid                              |
| Forex / SWIFT        | 5         | Currency converter + international wire                 |
| Interest Mgmt        | 4         | View/edit rules, process 30-day cycle, account override |
| General Ledger       | 4         | Balance sheet, P&L, journal entries, drill-down         |
| Audit Log            | 4         | Complete action trail                                   |
| User Mgmt            | 8         | Staff creation, permission management                   |
| Settings             | 1         | Password change, FX rate update (level 7+)              |

## Permission Levels

1=Junior Clerk 2=Senior Clerk 3=Teller 4=Supervisor 5=Asst Manager
6=Branch Manager 7=Regional Manager 8=Senior Manager 9=Director 10=CTO/Admin

## Architecture

Java 21 + Swing | SQLite (embedded) | Double-entry General Ledger
SHA-256+salt password hashing | Role-based access on every operation
Full audit trail | Binary search on party lists | Luhn card numbers | EMI formula
