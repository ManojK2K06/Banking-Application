package com.bankpro.ui.panels;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.*;
import com.bankpro.security.SessionManager;
import com.bankpro.service.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

// ═══════════════════════════════════════════════════════════════════════════
//  FIXED DEPOSIT PANEL
// ═══════════════════════════════════════════════════════════════════════════
class FixedDepositPanelImpl extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;

    private static final String[] COLS = {
        "FD Number","Customer","Principal","Rate%","Tenure","Maturity Amt","Start","Maturity","Status"
    };

    FixedDepositPanelImpl() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        loadFDs();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.titleLabel("📈  Fixed Deposits"), BorderLayout.WEST);
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acts.setOpaque(false);
        JButton newBtn = Theme.successButton("+ Create FD");
        JButton refreshBtn = Theme.ghostButton("⟳");
        acts.add(newBtn); acts.add(refreshBtn);
        header.add(acts, BorderLayout.EAST);

        tableModel = new DefaultTableModel(COLS, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel);
        Theme.styleTable(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setOpaque(false);
        JButton breakBtn = Theme.warningButton("⚡ Premature Break");
        bottom.add(breakBtn);

        main.add(header, BorderLayout.NORTH);
        main.add(Theme.styledScroll(table), BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        add(main);

        newBtn.addActionListener(e -> showCreateFD());
        refreshBtn.addActionListener(e -> loadFDs());
        breakBtn.addActionListener(e -> breakFD());
    }

    private void loadFDs() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT fd.*, c.first_name||' '||c.last_name AS cname FROM fixed_deposits fd " +
                "LEFT JOIN customers c ON fd.customer_id=c.id ORDER BY fd.created_at DESC");
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("fd_number"), rs.getString("cname"),
                    BankUtil.formatCurrency(rs.getDouble("principal_amount")),
                    rs.getDouble("interest_rate") + "%",
                    rs.getInt("tenure_months") + "M",
                    BankUtil.formatCurrency(rs.getDouble("maturity_amount")),
                    rs.getString("start_date"), rs.getString("maturity_date"), rs.getString("status")
                });
            }
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void showCreateFD() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create Fixed Deposit", true);
        dlg.setSize(480, 480);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(7, 4, 7, 4);

        JTextField custSearch  = Theme.styledField();
        JTextField accountNo   = Theme.styledField();
        JTextField amount      = Theme.styledField();
        JComboBox<String> tenor = Theme.styledCombo();
        for (String t : new String[]{"6","12","24","36","60","84","120"}) tenor.addItem(t);
        JTextField customTenure = Theme.styledField(); customTenure.setToolTipText("Or enter custom months");
        JLabel maturityLabel   = new JLabel("Maturity: —"); maturityLabel.setForeground(Theme.ACCENT_GOLD);
        JLabel custInfo        = Theme.label("—");
        JLabel acctInfo        = Theme.label("—");
        final int[] cId = {-1}, aId = {-1};

        int[] row = {0};
        addRow(p, gc, row, "Customer ID/Name", custSearch);
        addRow(p, gc, row, "Account Number", accountNo);
        addRow(p, gc, row, "Principal (₹)", amount);
        addRow(p, gc, row, "Tenure (months)", tenor);
        addRow(p, gc, row, "Custom tenure", customTenure);

        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2;
        p.add(custInfo, gc); row[0]++;
        gc.gridy=row[0]; p.add(acctInfo, gc); row[0]++;
        gc.gridy=row[0]; p.add(maturityLabel, gc); row[0]++;

        JButton calcBtn = Theme.ghostButton("Preview Maturity");
        gc.gridy=row[0]; p.add(calcBtn, gc); row[0]++;
        JButton createBtn = Theme.successButton("Create FD");
        gc.gridy=row[0]; gc.insets=new Insets(16,4,4,4); p.add(createBtn, gc);

        custSearch.addActionListener(e -> {
            try {
                List<Customer> res = CustomerService.getInstance().searchCustomers(custSearch.getText().trim());
                if (!res.isEmpty()) {
                    Customer c = res.get(0); cId[0] = c.getId();
                    custInfo.setText("✓ " + c.getFullName() + " [" + c.getCustomerId() + "]");
                    custInfo.setForeground(Theme.ACCENT_GREEN);
                } else custInfo.setText("❌ Not found");
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });
        accountNo.addActionListener(e -> {
            try {
                Account a = AccountService.getInstance().getByAccountNumber(accountNo.getText().trim());
                if (a != null) {
                    aId[0] = a.getId();
                    acctInfo.setText("✓ " + a.getAccountNumber() + " Bal: " + BankUtil.formatCurrency(a.getBalance()));
                    acctInfo.setForeground(Theme.ACCENT_GREEN);
                } else acctInfo.setText("❌ Not found");
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });
        calcBtn.addActionListener(e -> {
            try {
                double pr = Double.parseDouble(amount.getText().trim());
                int ten = customTenure.getText().trim().isEmpty()
                    ? Integer.parseInt((String) tenor.getSelectedItem())
                    : Integer.parseInt(customTenure.getText().trim());
                double rate = ten <= 12 ? 5.5 : ten <= 24 ? 6.25 : ten <= 36 ? 6.75 : ten <= 60 ? 7.0 : 7.25;
                double mat = BankUtil.calculateFDMaturity(pr, rate, ten);
                maturityLabel.setText(String.format("Rate: %.2f%%  |  Maturity: %s  |  Interest Earned: %s",
                    rate, BankUtil.formatCurrency(mat), BankUtil.formatCurrency(mat - pr)));
            } catch (Exception ex) { maturityLabel.setText("Invalid input"); }
        });
        createBtn.addActionListener(e -> {
            if (cId[0] < 0 || aId[0] < 0) { Theme.showError(dlg, "Select customer & account"); return; }
            if (!BankUtil.isValidAmount(amount.getText())) { Theme.showError(dlg, "Invalid amount"); return; }
            try {
                double pr = Double.parseDouble(amount.getText().trim());
                int ten = customTenure.getText().trim().isEmpty()
                    ? Integer.parseInt((String) tenor.getSelectedItem())
                    : Integer.parseInt(customTenure.getText().trim());
                double rate = ten <= 12 ? 5.5 : ten <= 24 ? 6.25 : ten <= 36 ? 6.75 : ten <= 60 ? 7.0 : 7.25;
                double mat = BankUtil.calculateFDMaturity(pr, rate, ten);
                String fdNo = BankUtil.generateFDNumber();
                String start = LocalDate.now().toString();
                String matDate = LocalDate.now().plusMonths(ten).toString();

                // Deduct from account
                Account a = AccountService.getInstance().getById(aId[0]);
                if (a.getBalance() < pr) { Theme.showError(dlg, "Insufficient balance: " + BankUtil.formatCurrency(a.getBalance())); return; }

                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO fixed_deposits (fd_number,customer_id,account_id,principal_amount," +
                         "interest_rate,tenure_months,maturity_amount,start_date,maturity_date,created_by) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, fdNo); ps.setInt(2, cId[0]); ps.setInt(3, aId[0]);
                    ps.setDouble(4, pr); ps.setDouble(5, rate); ps.setInt(6, ten);
                    ps.setDouble(7, mat); ps.setString(8, start); ps.setString(9, matDate);
                    ps.setInt(10, SessionManager.getInstance().getCurrentUser().getId());
                    ps.executeUpdate();
                }
                TransactionService.getInstance().withdraw(aId[0], pr, "FD Created: " + fdNo);
                dlg.dispose(); loadFDs();
                Theme.showSuccess(this, "FD Created!\nFD No: " + fdNo + "\nMaturity: " + BankUtil.formatCurrency(mat) + " on " + matDate);
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        dlg.add(new JScrollPane(p) {{ setBackground(Theme.BG_CARD); getViewport().setBackground(Theme.BG_CARD); setBorder(null); }});
        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gc, int[] row, String label, Component comp) {
        gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.35; gc.gridwidth=1;
        p.add(Theme.label(label), gc);
        gc.gridx=1; gc.weightx=0.65;
        p.add(comp, gc);
        row[0]++;
    }

    private void breakFD() {
        int row = table.getSelectedRow();
        if (row < 0) { Theme.showError(this, "Select an FD"); return; }
        String fdNo = tableModel.getValueAt(row, 0).toString();
        if (!Theme.showConfirm(this, "Break FD " + fdNo + "? A 1% penalty applies on premature withdrawal.")) return;
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE fixed_deposits SET status='BROKEN' WHERE fd_number=? AND status='ACTIVE'");
            ps.setString(1, fdNo); ps.executeUpdate();
            loadFDs();
            Theme.showSuccess(this, "FD broken. Please manually credit maturity amount minus penalty.");
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  CARDS PANEL
// ═══════════════════════════════════════════════════════════════════════════
class CardsPanelImpl extends JPanel {
    private DefaultTableModel tableModel;
    private static final String[] COLS = {"Card Number","Holder","Type","Network","Account","Expiry","Status","Limit"};

    CardsPanelImpl() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI(); loadCards();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.titleLabel("💳  Card Management"), BorderLayout.WEST);
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acts.setOpaque(false);
        JButton issueBtn = Theme.successButton("+ Issue Card");
        JButton refreshBtn = Theme.ghostButton("⟳");
        acts.add(issueBtn); acts.add(refreshBtn);
        header.add(acts, BorderLayout.EAST);

        tableModel = new DefaultTableModel(COLS, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(tableModel);
        Theme.styleTable(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setOpaque(false);
        JButton blockBtn  = Theme.dangerButton("🔒 Block Card");
        JButton hotlistBtn = Theme.warningButton("🚨 Hotlist");
        bottom.add(blockBtn); bottom.add(hotlistBtn);

        main.add(header, BorderLayout.NORTH);
        main.add(Theme.styledScroll(table), BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        add(main);

        issueBtn.addActionListener(e -> issueCard(table));
        refreshBtn.addActionListener(e -> loadCards());
        blockBtn.addActionListener(e -> updateCardStatus(table, "BLOCKED"));
        hotlistBtn.addActionListener(e -> updateCardStatus(table, "HOTLISTED"));
    }

    private void loadCards() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT c.*, a.account_number FROM cards c LEFT JOIN accounts a ON c.account_id=a.id ORDER BY c.issued_at DESC");
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    BankUtil.maskCardNumber(rs.getString("card_number")),
                    rs.getString("card_holder_name"), rs.getString("card_type"),
                    rs.getString("network"), rs.getString("account_number"),
                    rs.getString("expiry_date"), rs.getString("status"),
                    rs.getDouble("credit_limit") > 0 ? BankUtil.formatCurrency(rs.getDouble("credit_limit")) : "—"
                });
            }
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void issueCard(JTable table) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Issue Card", true);
        dlg.setSize(420, 380);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(7, 4, 7, 4);

        JTextField accountNo   = Theme.styledField();
        JTextField holderName  = Theme.styledField();
        JComboBox<String> type = Theme.styledCombo();
        for (String t : new String[]{"DEBIT","CREDIT","PREPAID"}) type.addItem(t);
        JComboBox<String> network = Theme.styledCombo();
        for (String n : new String[]{"VISA","MASTERCARD","RUPAY"}) network.addItem(n);
        JTextField creditLimit = Theme.styledField(); creditLimit.setText("0");
        JLabel acctInfo = Theme.label("—");
        final int[] aId = {-1};

        int[] row = {0};
        Object[][] rows = {{"Account No *", accountNo}, {"Card Holder Name *", holderName},
            {"Card Type", type}, {"Network", network}, {"Credit Limit (for credit card)", creditLimit}};
        for (Object[] r : rows) {
            gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.4; gc.gridwidth=1; p.add(Theme.label((String)r[0]), gc);
            gc.gridx=1; gc.weightx=0.6; p.add((Component)r[1], gc); row[0]++;
        }
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2; p.add(acctInfo, gc); row[0]++;

        accountNo.addActionListener(e -> {
            try {
                Account a = AccountService.getInstance().getByAccountNumber(accountNo.getText().trim());
                if (a != null) { aId[0]=a.getId(); holderName.setText(a.getCustomerName());
                    acctInfo.setText("✓ " + a.getCustomerName()); acctInfo.setForeground(Theme.ACCENT_GREEN); }
                else { acctInfo.setText("❌ Not found"); aId[0]=-1; }
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        JButton issueBtn = Theme.successButton("Issue Card");
        gc.gridy=row[0]; gc.insets=new Insets(16,4,4,4); p.add(issueBtn, gc);

        issueBtn.addActionListener(e -> {
            if (aId[0] < 0) { Theme.showError(dlg, "Verify account first"); return; }
            if (holderName.getText().trim().isEmpty()) { Theme.showError(dlg, "Enter card holder name"); return; }
            try {
                String cardNo  = BankUtil.generateCardNumber();
                String expiry  = BankUtil.generateExpiryDate();
                String cvv     = BankUtil.generateCVV();
                String cvvHash = com.bankpro.security.PasswordUtil.hashPassword(cvv,
                    com.bankpro.security.PasswordUtil.generateSalt());
                double limit   = Double.parseDouble(creditLimit.getText().trim());
                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO cards (card_number,account_id,card_type,card_holder_name,expiry_date,cvv_hash,credit_limit,network) VALUES (?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, cardNo); ps.setInt(2, aId[0]);
                    ps.setString(3, (String)type.getSelectedItem()); ps.setString(4, holderName.getText().trim());
                    ps.setString(5, expiry); ps.setString(6, cvvHash);
                    ps.setDouble(7, limit); ps.setString(8, (String)network.getSelectedItem());
                    ps.executeUpdate();
                }
                dlg.dispose(); loadCards();
                Theme.showSuccess(this, "Card Issued!\nCard: " + BankUtil.formatCardNumber(cardNo)
                    + "\nExpiry: " + expiry + "\nCVV: " + cvv + " (shown once — store securely)");
                AuditService.getInstance().log("ISSUE_CARD","CARD",cardNo,null,"ACTIVE","Card issued to "+holderName.getText());
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });
        dlg.add(p); dlg.setVisible(true);
    }

    private void updateCardStatus(JTable table, String status) {
        int row = table.getSelectedRow();
        if (row < 0) { Theme.showError(this, "Select a card"); return; }
        String masked = tableModel.getValueAt(row, 0).toString();
        if (!Theme.showConfirm(this, "Set card " + masked + " to " + status + "?")) return;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE cards SET status=? WHERE card_number LIKE ?")) {
            ps.setString(1, status);
            ps.setString(2, "%" + masked.substring(masked.length()-4));
            ps.executeUpdate();
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        loadCards();
        Theme.showSuccess(this, "Card status updated to " + status);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  FOREX PANEL
// ═══════════════════════════════════════════════════════════════════════════
class ForexPanelImpl extends JPanel {
    ForexPanelImpl() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));
        main.add(Theme.titleLabel("🌐  Forex / International Transfer"), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);
        center.add(buildConverterCard());
        center.add(buildSwiftCard());
        main.add(center, BorderLayout.CENTER);
        main.add(buildRatesTable(), BorderLayout.SOUTH);
        add(main);
    }

    private JPanel buildConverterCard() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(8, 4, 8, 4);

        gc.gridx=0; gc.gridy=0; gc.gridwidth=2;
        card.add(Theme.heading("💱 Currency Converter"), gc);

        JTextField amtField    = Theme.styledField(); amtField.setText("1000");
        JComboBox<String> from = Theme.styledCombo();
        JComboBox<String> to   = Theme.styledCombo();
        JLabel result          = new JLabel("—"); result.setFont(Theme.FONT_AMOUNT); result.setForeground(Theme.ACCENT_GOLD);

        try {
            Map<String, Double> rates = TransactionService.getInstance().getAllRates();
            for (String code : rates.keySet()) { from.addItem(code); to.addItem(code); }
            from.setSelectedItem("USD"); to.setSelectedItem("INR");
        } catch (Exception ignored) {}

        gc.gridwidth=1;
        int[] row = {1};
        Object[][] rows = {{"Amount", amtField}, {"From", from}, {"To", to}};
        for (Object[] r : rows) {
            gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.4; card.add(Theme.label((String)r[0]), gc);
            gc.gridx=1; gc.weightx=0.6; card.add((Component)r[1], gc); row[0]++;
        }
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2;
        card.add(result, gc); row[0]++;

        JButton convertBtn = Theme.primaryButton("Convert");
        gc.gridy=row[0]; gc.insets=new Insets(16,4,4,4);
        card.add(convertBtn, gc);

        convertBtn.addActionListener(e -> {
            try {
                double amt    = Double.parseDouble(amtField.getText().trim());
                String f      = (String) from.getSelectedItem();
                String t      = (String) to.getSelectedItem();
                double fromR  = TransactionService.getInstance().getCurrencyRate(f);
                double toR    = TransactionService.getInstance().getCurrencyRate(t);
                double inr    = amt * fromR;
                double conv   = inr / toR;
                result.setText(String.format("%.4f %s  =  %.4f %s", amt, f, conv, t));
                result.setForeground(Theme.ACCENT_GREEN);
            } catch (Exception ex) { result.setText("Error: " + ex.getMessage()); result.setForeground(Theme.ACCENT_RED); }
        });
        return card;
    }

    private JPanel buildSwiftCard() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(8, 4, 8, 4);

        gc.gridx=0; gc.gridy=0; gc.gridwidth=2;
        card.add(Theme.heading("🌍 SWIFT / International Wire"), gc);

        JTextField acctField   = Theme.styledField();
        JTextField amtField    = Theme.styledField();
        JComboBox<String> curCombo = Theme.styledCombo();
        JTextField benefName   = Theme.styledField();
        JTextField benefAcct   = Theme.styledField();
        JTextField swiftCode   = Theme.styledField();
        JTextField purpose     = Theme.styledField();
        JLabel inrEquiv        = Theme.label("INR Equivalent: —");

        try {
            Map<String, Double> rates = TransactionService.getInstance().getAllRates();
            for (String code : rates.keySet()) if (!"INR".equals(code)) curCombo.addItem(code);
        } catch (Exception ignored) {}

        int[] row = {1};
        gc.gridwidth=1;
        Object[][] rows = {
            {"From Account *", acctField}, {"Amount *", amtField}, {"Currency *", curCombo},
            {"Beneficiary Name *", benefName}, {"Beneficiary Acct *", benefAcct},
            {"SWIFT Code *", swiftCode}, {"Purpose *", purpose}
        };
        for (Object[] r : rows) {
            gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.4; card.add(Theme.label((String)r[0]), gc);
            gc.gridx=1; gc.weightx=0.6; card.add((Component)r[1], gc); row[0]++;
        }
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2;
        card.add(inrEquiv, gc); row[0]++;

        amtField.addActionListener(e -> {
            try {
                double a = Double.parseDouble(amtField.getText().trim());
                double r = TransactionService.getInstance().getCurrencyRate((String)curCombo.getSelectedItem());
                inrEquiv.setText("≈ " + BankUtil.formatCurrency(a * r) + " + SWIFT charges");
            } catch (Exception ignored) {}
        });

        JButton sendBtn = Theme.primaryButton("Send Wire Transfer");
        gc.gridy=row[0]; gc.insets=new Insets(16,4,4,4);
        card.add(sendBtn, gc);

        sendBtn.addActionListener(e -> {
            try {
                Account a = AccountService.getInstance().getByAccountNumber(acctField.getText().trim());
                if (a == null) { Theme.showError(card, "Account not found"); return; }
                if (!BankUtil.isValidAmount(amtField.getText())) { Theme.showError(card, "Invalid amount"); return; }
                double amt = Double.parseDouble(amtField.getText().trim());
                if (!Theme.showConfirm(card, "Send " + amt + " " + curCombo.getSelectedItem() + " internationally?")) return;
                Transaction t = TransactionService.getInstance().internationalTransfer(
                    a.getId(), amt, (String)curCombo.getSelectedItem(),
                    benefName.getText().trim(), benefAcct.getText().trim(),
                    swiftCode.getText().trim(), purpose.getText().trim());
                Theme.showSuccess(card, "SWIFT transfer initiated!\nTxn: " + t.getTransactionId());
            } catch (Exception ex) { Theme.showError(card, ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()); }
        });
        return card;
    }

    private JScrollPane buildRatesTable() {
        String[] cols = {"Currency","Name","Rate (1 unit → ₹)","Symbol"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.setPreferredScrollableViewportSize(new Dimension(0, 160));
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM currency_rates ORDER BY currency_code");
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("currency_code"), rs.getString("currency_name"),
                    "₹" + String.format("%.4f", rs.getDouble("rate_to_inr")), rs.getString("symbol")
                });
            }
        } catch (Exception ignored) {}
        return Theme.styledScroll(table);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  AUDIT LOG PANEL
// ═══════════════════════════════════════════════════════════════════════════
class AuditLogPanelImpl extends JPanel {
    private DefaultTableModel model;
    private static final String[] COLS = {"#","User","Action","Entity","Entity ID","Status","Timestamp","Details"};

    AuditLogPanelImpl() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI(); load();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.titleLabel("📜  Audit Log"), BorderLayout.WEST);
        JButton refreshBtn = Theme.ghostButton("⟳  Refresh");
        refreshBtn.addActionListener(e -> load());
        header.add(refreshBtn, BorderLayout.EAST);

        model = new DefaultTableModel(COLS, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String s = v != null ? v.toString() : "";
                setForeground("SUCCESS".equals(s) ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
                return this;
            }
        });

        main.add(header, BorderLayout.NORTH);
        main.add(Theme.styledScroll(table), BorderLayout.CENTER);
        add(main);
    }

    private void load() {
        model.setRowCount(0);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT a.*, u.full_name FROM audit_log a LEFT JOIN users u ON a.user_id=u.id ORDER BY a.timestamp DESC LIMIT 500");
            int i = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    i++, rs.getString("full_name"), rs.getString("action"),
                    rs.getString("entity_type"), rs.getString("entity_id"),
                    rs.getString("status"), rs.getString("timestamp"),
                    rs.getString("details")
                });
            }
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  USER MANAGEMENT PANEL
// ═══════════════════════════════════════════════════════════════════════════
class UserManagementPanelImpl extends JPanel {
    private DefaultTableModel model;
    private List<User> users;
    private static final String[] COLS = {"Emp ID","Username","Name","Email","Level","Role","Dept","Status","Last Login"};

    UserManagementPanelImpl() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI(); loadUsers();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.titleLabel("🔒  User Management"), BorderLayout.WEST);
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acts.setOpaque(false);
        JButton newBtn  = Theme.successButton("+ New User");
        JButton refresh = Theme.ghostButton("⟳");
        acts.add(newBtn); acts.add(refresh);
        header.add(acts, BorderLayout.EAST);

        model = new DefaultTableModel(COLS, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(model);
        Theme.styleTable(table);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottom.setOpaque(false);
        JButton changeLevel = Theme.warningButton("🔑 Change Permission");
        JButton resetPwd    = Theme.primaryButton("🔒 Reset Password");
        JButton deactivate  = Theme.dangerButton("⊘ Deactivate");
        bottom.add(changeLevel); bottom.add(resetPwd); bottom.add(deactivate);

        main.add(header, BorderLayout.NORTH);
        main.add(Theme.styledScroll(table), BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);
        add(main);

        newBtn.addActionListener(e -> showNewUserDialog());
        refresh.addActionListener(e -> loadUsers());
        changeLevel.addActionListener(e -> changePermission(table));
        resetPwd.addActionListener(e -> resetPassword(table));
        deactivate.addActionListener(e -> deactivateUser(table));
    }

    private void loadUsers() {
        model.setRowCount(0);
        try {
            users = UserService.getInstance().getAllUsers();
            for (User u : users) {
                model.addRow(new Object[]{
                    u.getEmployeeId(), u.getUsername(), u.getFullName(), u.getEmail(),
                    "L" + u.getPermissionLevel(), u.getPermissionLabel(),
                    u.getDepartment() != null ? u.getDepartment() : "—",
                    u.isActive() ? "Active" : "Inactive",
                    u.getLastLogin() != null ? BankUtil.formatDateTime(u.getLastLogin()) : "Never"
                });
            }
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void showNewUserDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create New User", true);
        dlg.setSize(480, 500);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(8,4,8,4);

        JTextField username  = Theme.styledField();
        JPasswordField pwd   = Theme.styledPasswordField();
        JTextField fullName  = Theme.styledField();
        JTextField email     = Theme.styledField();
        JTextField phone     = Theme.styledField();
        JTextField dept      = Theme.styledField();
        JSpinner level       = new JSpinner(new SpinnerNumberModel(1, 1,
            Math.min(9, SessionManager.getInstance().getPermissionLevel()-1), 1));
        level.setBackground(Theme.BG_INPUT);

        int maxLevel = SessionManager.getInstance().getPermissionLevel() - 1;
        int[] row = {0};
        Object[][] rows = {
            {"Username *", username}, {"Password *", pwd}, {"Full Name *", fullName},
            {"Email *", email}, {"Phone", phone}, {"Department", dept},
            {"Permission Level (1-" + maxLevel + ")", level}
        };
        for (Object[] r : rows) {
            gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.4; gc.gridwidth=1;
            p.add(Theme.label((String)r[0]), gc);
            gc.gridx=1; gc.weightx=0.6;
            p.add((Component)r[1], gc); row[0]++;
        }

        JLabel hint = Theme.label("Password must have 8+ chars, upper, lower, digit, special char");
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2;
        p.add(hint, gc); row[0]++;

        JButton createBtn = Theme.successButton("Create User");
        gc.gridy=row[0]; gc.insets=new Insets(16,4,4,4);
        p.add(createBtn, gc);

        createBtn.addActionListener(e -> {
            try {
                UserService.getInstance().createUser(
                    username.getText().trim(), new String(pwd.getPassword()),
                    fullName.getText().trim(), email.getText().trim(),
                    phone.getText().trim(), (Integer) level.getValue(), dept.getText().trim());
                dlg.dispose(); loadUsers();
                Theme.showSuccess(this, "User created: " + username.getText().trim());
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        dlg.add(p); dlg.setVisible(true);
    }

    private User getSelected(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0 || users == null) { Theme.showError(this, "Select a user"); return null; }
        return users.get(row);
    }

    private void changePermission(JTable table) {
        User u = getSelected(table);
        if (u == null) return;
        String res = JOptionPane.showInputDialog(this, "New permission level (1–10) for " + u.getFullName() + ":",
            u.getPermissionLevel());
        if (res == null) return;
        try {
            int lvl = Integer.parseInt(res.trim());
            UserService.getInstance().updatePermission(u.getId(), lvl);
            loadUsers();
            Theme.showSuccess(this, "Permission updated to Level " + lvl);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void resetPassword(JTable table) {
        User u = getSelected(table);
        if (u == null) return;
        JPasswordField pwdField = Theme.styledPasswordField();
        int res = JOptionPane.showConfirmDialog(this, pwdField, "New password for " + u.getUsername(), JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                UserService.getInstance().resetPassword(u.getId(), new String(pwdField.getPassword()));
                Theme.showSuccess(this, "Password reset for " + u.getUsername());
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void deactivateUser(JTable table) {
        User u = getSelected(table);
        if (u == null) return;
        if (Theme.showConfirm(this, "Deactivate user " + u.getUsername() + "?")) {
            try {
                UserService.getInstance().deactivateUser(u.getId());
                loadUsers();
                Theme.showSuccess(this, "User deactivated.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SETTINGS PANEL
// ═══════════════════════════════════════════════════════════════════════════
class SettingsPanelImpl extends JPanel {
    SettingsPanelImpl() {
        setBackground(Theme.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(560, 500));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(8, 8, 8, 8);

        gc.gridx=0; gc.gridy=0; gc.gridwidth=2;
        card.add(Theme.titleLabel("⚙️  Settings"), gc);

        gc.gridy=1;
        card.add(Theme.heading("Change Password"), gc);

        JPasswordField curr = Theme.styledPasswordField();
        JPasswordField newP = Theme.styledPasswordField();
        JPasswordField conf = Theme.styledPasswordField();

        gc.gridwidth=1;
        int[] row = {2};
        Object[][] rows = {{"Current Password", curr}, {"New Password", newP}, {"Confirm Password", conf}};
        for (Object[] r : rows) {
            gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.35; card.add(Theme.label((String)r[0]), gc);
            gc.gridx=1; gc.weightx=0.65; card.add((Component)r[1], gc); row[0]++;
        }

        JButton changePwdBtn = Theme.primaryButton("Update Password");
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2; gc.insets=new Insets(12,8,8,8);
        card.add(changePwdBtn, gc); row[0]++;

        // Separator
        JSeparator sep = new JSeparator(); sep.setForeground(Theme.BORDER_COLOR);
        gc.gridy=row[0]; gc.insets=new Insets(16,8,8,8); card.add(sep, gc); row[0]++;

        // Rate update
        if (SessionManager.getInstance().hasPermission(7)) {
            gc.gridy=row[0]; gc.insets=new Insets(8,8,8,8);
            card.add(Theme.heading("Update Currency Rate"), gc); row[0]++;

            JComboBox<String> codeCombo = Theme.styledCombo();
            try {
                for (String code : TransactionService.getInstance().getAllRates().keySet()) codeCombo.addItem(code);
            } catch (Exception ignored) {}
            JTextField rateField = Theme.styledField();
            JButton updateRateBtn = Theme.warningButton("Update Rate");

            gc.gridwidth=1;
            gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.35; card.add(Theme.label("Currency Code"), gc);
            gc.gridx=1; gc.weightx=0.65; card.add(codeCombo, gc); row[0]++;
            gc.gridx=0; gc.gridy=row[0]; card.add(Theme.label("Rate to INR"), gc);
            gc.gridx=1; card.add(rateField, gc); row[0]++;
            gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2;
            card.add(updateRateBtn, gc);

            updateRateBtn.addActionListener(e -> {
                try {
                    double r = Double.parseDouble(rateField.getText().trim());
                    TransactionService.getInstance().updateCurrencyRate((String)codeCombo.getSelectedItem(), r);
                    Theme.showSuccess(card, "Rate updated.");
                } catch (Exception ex) { Theme.showError(card, ex.getMessage()); }
            });
        }

        changePwdBtn.addActionListener(e -> {
            String c = new String(curr.getPassword());
            String n = new String(newP.getPassword());
            String cf = new String(conf.getPassword());
            if (!n.equals(cf)) { Theme.showError(card, "Passwords do not match"); return; }
            try {
                User me = SessionManager.getInstance().getCurrentUser();
                if (!com.bankpro.security.PasswordUtil.verifyPassword(c, me.getSalt(), me.getPasswordHash()))
                    throw new Exception("Current password is incorrect");
                UserService.getInstance().resetPassword(me.getId(), n);
                Theme.showSuccess(card, "Password updated successfully.");
                curr.setText(""); newP.setText(""); conf.setText("");
            } catch (Exception ex) { Theme.showError(card, ex.getMessage()); }
        });

        add(card);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  PUBLIC WRAPPER CLASSES  (referenced from MainFrame)
// ═══════════════════════════════════════════════════════════════════════════
public class FixedDepositPanel  extends FixedDepositPanelImpl  { public FixedDepositPanel()  { super(); } }
class CardPanelInner            extends CardsPanelImpl          { CardPanelInner()             { super(); } }
class ForexPanelInner           extends ForexPanelImpl          { ForexPanelInner()            { super(); } }
class AuditLogPanelInner        extends AuditLogPanelImpl       { AuditLogPanelInner()         { super(); } }
class UserManagementPanelInner  extends UserManagementPanelImpl { UserManagementPanelInner()   { super(); } }
class SettingsPanelInner        extends SettingsPanelImpl       { SettingsPanelInner()         { super(); } }
