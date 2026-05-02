package com.bankpro.ui.panels;

import com.bankpro.model.*;
import com.bankpro.service.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class AccountPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Account> currentList;
    private JTabbedPane tabs;

    private static final String[] PARTY_COLS = {
        "Account No.","Party","Type","Balance","Currency","Status","Rate%","IFSC","Opened"
    };
    private static final String[] INTERNAL_COLS = {
        "Account No.","Name","GL Code","Balance","Status"
    };

    public AccountPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        javax.swing.SwingUtilities.invokeLater(this::loadAccounts);
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(Theme.titleLabel("🏦  Account Management"), BorderLayout.WEST);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acts.setOpaque(false);
        JButton openBtn    = Theme.successButton("+ Open Account");
        JButton refreshBtn = Theme.ghostButton("⟳  Refresh");
        acts.add(openBtn); acts.add(refreshBtn);
        header.add(acts, BorderLayout.EAST);

        // Tabbed: Party Accounts | Internal Accounts
        tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_CARD);
        tabs.setForeground(Theme.TEXT_PRIMARY);
        tabs.setFont(Theme.FONT_BODY);

        // Party accounts tab
        tableModel = new DefaultTableModel(PARTY_COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {160,160,110,140,70,70,60,100,100};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        tabs.addTab("👥 Party Accounts", Theme.styledScroll(table));

        // Internal accounts tab
        DefaultTableModel internalModel = new DefaultTableModel(INTERNAL_COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable internalTable = new JTable(internalModel);
        Theme.styleTable(internalTable);
        tabs.addTab("🏛 Internal Accounts", Theme.styledScroll(internalTable));
        tabs.putClientProperty("internalModel", internalModel);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) loadInternalAccounts(internalModel);
        });

        // Bottom action bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomBar.setOpaque(false);
        JButton stmtBtn     = Theme.primaryButton("📄 Statement");
        JButton interestBtn = Theme.ghostButton("📐 Interest History");
        JButton freezeBtn   = Theme.warningButton("❄ Freeze");
        JButton unfreezeBtn = Theme.ghostButton("♨ Unfreeze");
        JButton closeBtn    = Theme.dangerButton("⊘ Close");
        bottomBar.add(stmtBtn); bottomBar.add(interestBtn);
        bottomBar.add(freezeBtn); bottomBar.add(unfreezeBtn); bottomBar.add(closeBtn);

        main.add(header, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);
        main.add(bottomBar, BorderLayout.SOUTH);
        add(main);

        openBtn.addActionListener(e    -> showOpenAccountDialog());
        refreshBtn.addActionListener(e -> loadAccounts());
        stmtBtn.addActionListener(e    -> showStatement());
        interestBtn.addActionListener(e-> showInterestHistory());
        freezeBtn.addActionListener(e  -> freezeSelected());
        unfreezeBtn.addActionListener(e-> unfreezeSelected());
        closeBtn.addActionListener(e   -> closeSelected());
    }

    private void loadAccounts() {
        new SwingWorker<List<Account>, Void>() {
            @Override protected List<Account> doInBackground() throws Exception {
                return AccountService.getInstance().getAllAccounts();
            }
            @Override protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (Account a : currentList) {
                        tableModel.addRow(new Object[]{
                            a.getAccountNumber(),
                            a.getPartyName() != null ? a.getPartyName() : "—",
                            a.getAccountTypeLabel(),
                            BankUtil.formatCurrency(a.getBalance()),
                            a.getCurrency(),
                            a.getStatus(),
                            a.getInterestRate() + "%",
                            a.getIfscCode(),
                            a.getCreatedAt() != null ? BankUtil.formatDate(a.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) { Theme.showError(AccountPanel.this, ex.getMessage()); }
            }
        }.execute();
    }

    private void loadInternalAccounts(DefaultTableModel model) {
        new SwingWorker<List<Account>, Void>() {
            @Override protected List<Account> doInBackground() throws Exception {
                return AccountService.getInstance().getInternalAccounts();
            }
            @Override protected void done() {
                try {
                    model.setRowCount(0);
                    for (Account a : get()) {
                        model.addRow(new Object[]{
                            a.getAccountNumber(),
                            a.getInternalName() != null ? a.getInternalName() : a.getAccountNumber(),
                            a.getGlCode() != null ? a.getGlCode() : "—",
                            BankUtil.formatCurrency(a.getBalance()),
                            a.getStatus()
                        });
                    }
                } catch (Exception ex) { Theme.showError(AccountPanel.this, ex.getMessage()); }
            }
        }.execute();
    }

    private Account getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null) {
            Theme.showError(this, "Please select an account first");
            return null;
        }
        return currentList.get(row);
    }

    private void showOpenAccountDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Open New Account", true);
        dlg.setSize(500, 420);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 4, 6, 4);

        JTextField partySearch     = Theme.styledField();
        JLabel     partyFoundLabel = Theme.label("Search by party ID, name, or PAN...");
        JButton    searchBtn       = Theme.ghostButton("Search");
        JComboBox<String> typeCombo    = Theme.styledCombo();
        for (String t : new String[]{"SAVINGS","CURRENT","SALARY","NRI"}) typeCombo.addItem(t);
        JTextField initialDeposit  = Theme.styledField();
        JComboBox<String> currencyCombo = Theme.styledCombo();
        for (String c : new String[]{"INR","USD","EUR","GBP","AED","SGD"}) currencyCombo.addItem(c);

        final int[] selectedPartyId = {-1};

        int[] row = {0};
        Object[][] rows = {
            {"Party ID / Search *", partySearch}, {"", searchBtn},
            {"Account Type *",      typeCombo},
            {"Initial Deposit (₹)*",initialDeposit},
            {"Currency",           currencyCombo}
        };
        for (Object[] r : rows) {
            gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.35; gc.gridwidth=1;
            p.add(Theme.label((String)r[0]), gc);
            gc.gridx=1; gc.weightx=0.65;
            p.add((Component)r[1], gc);
            row[0]++;
        }

        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2;
        p.add(partyFoundLabel, gc); row[0]++;

        // Rate preview label
        JLabel ratePreview = Theme.label("Interest Rate: —");
        ratePreview.setForeground(Theme.ACCENT_GOLD);
        gc.gridy=row[0]; p.add(ratePreview, gc); row[0]++;

        typeCombo.addActionListener(e -> {
            try {
                InterestRule rule = InterestService.getInstance()
                    .getRuleForAccountType((String)typeCombo.getSelectedItem());
                if (rule != null)
                    ratePreview.setText("Interest Rate: " + rule.getAnnualRate() + "% p.a. (" + rule.getFrequencyLabel() + ")");
                else
                    ratePreview.setText("Interest Rate: 0% (no rule configured)");
            } catch (Exception ex) { ratePreview.setText("—"); }
        });
        // Fire once to populate
        typeCombo.getActionListeners()[0].actionPerformed(null);

        JButton openBtn = Theme.successButton("Open Account");
        gc.gridy=row[0]; gc.insets=new Insets(16,4,4,4); p.add(openBtn, gc);

        searchBtn.addActionListener(e -> {
            String query = partySearch.getText().trim();
            if (query.isEmpty()) { Theme.showError(dlg, "Enter party ID or name"); return; }
            try {
                List<Party> results = PartyService.getInstance().searchParties(query);
                if (results.isEmpty()) { partyFoundLabel.setText("❌ No party found"); return; }
                Party chosen = results.size() == 1 ? results.get(0) : pickParty(dlg, results);
                if (chosen == null) return;
                selectedPartyId[0] = chosen.getId();
                partyFoundLabel.setText("✓ " + chosen.getDisplayName() + "  [" + chosen.getPartyId() + "]  " + chosen.getShortType());
                partyFoundLabel.setForeground(Theme.ACCENT_GREEN);
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        openBtn.addActionListener(e -> {
            if (selectedPartyId[0] < 0) { Theme.showError(dlg,"Search and select a party first"); return; }
            if (!BankUtil.isValidAmount(initialDeposit.getText().trim())) { Theme.showError(dlg,"Invalid amount"); return; }
            try {
                double amt = Double.parseDouble(initialDeposit.getText().trim());
                Account acc = AccountService.getInstance().openAccount(
                    selectedPartyId[0],
                    (String) typeCombo.getSelectedItem(),
                    amt,
                    (String) currencyCombo.getSelectedItem());
                dlg.dispose();
                loadAccounts();
                Theme.showSuccess(this,
                    "Account Opened!\n" +
                    "Account No: " + acc.getAccountNumber() + "\n" +
                    "IFSC: " + acc.getIfscCode() + "\n" +
                    "Initial Deposit: " + BankUtil.formatCurrency(amt));
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        dlg.add(p);
        dlg.setVisible(true);
    }

    private Party pickParty(JDialog parent, List<Party> list) {
        String[] names = list.stream()
            .map(p -> p.getPartyId() + " — " + p.getDisplayName() + " [" + p.getShortType() + "]")
            .toArray(String[]::new);
        String sel = (String) JOptionPane.showInputDialog(parent, "Select party:",
            "Multiple Found", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (sel == null) return null;
        int idx = java.util.Arrays.asList(names).indexOf(sel);
        return list.get(idx);
    }

    private void showStatement() {
        Account a = getSelected();
        if (a == null) return;
        try {
            List<Transaction> txns = TransactionService.getInstance()
                .getTransactionsByAccount(a.getId(), 100);
            String[] cols = {"Date","Type","Amount","Direction","Balance After","By"};
            Object[][] data = new Object[txns.size()][6];
            for (int i = 0; i < txns.size(); i++) {
                Transaction t = txns.get(i);
                boolean isDebit = t.getFromAccountId() == a.getId();
                double bal = isDebit ? t.getBalanceAfterFrom() : t.getBalanceAfterTo();
                data[i] = new Object[]{
                    t.getCreatedAt() != null ? BankUtil.formatDateTime(t.getCreatedAt()) : "",
                    t.getTypeLabel(),
                    BankUtil.formatCurrency(t.getAmountInr() > 0 ? t.getAmountInr() : t.getAmount()),
                    isDebit ? "DEBIT ↓" : "CREDIT ↑",
                    BankUtil.formatCurrency(bal),
                    t.getPerformedByName() != null ? t.getPerformedByName() : "—"
                };
            }
            JTable stmtTable = Theme.styledTable(cols, data);
            stmtTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable t, Object v,
                        boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                    String val = v != null ? v.toString() : "";
                    setForeground(val.startsWith("DEBIT") ? Theme.ACCENT_RED : Theme.ACCENT_GREEN);
                    return this;
                }
            });

            JDialog dlg = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
                "Statement — " + a.getAccountNumber(), true);
            dlg.setSize(900, 500);
            dlg.setLocationRelativeTo(this);

            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(Theme.BG_CARD);
            top.setBorder(new EmptyBorder(12, 16, 12, 16));
            top.add(new JLabel("<html><b>" + a.getAccountNumber() + "</b>  " +
                a.getAccountTypeLabel() + "  |  Party: " + (a.getPartyName() != null ? a.getPartyName() : "—") +
                "  |  Balance: " + BankUtil.formatCurrency(a.getBalance()) +
                "  |  Rate: " + a.getInterestRate() + "%</html>"),
                BorderLayout.WEST);

            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(Theme.BG_DARK);
            body.add(top, BorderLayout.NORTH);
            body.add(Theme.styledScroll(stmtTable), BorderLayout.CENTER);
            dlg.add(body);
            dlg.setVisible(true);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void showInterestHistory() {
        Account a = getSelected();
        if (a == null) return;
        try {
            java.util.List<java.util.Map<String,Object>> history =
                InterestService.getInstance().getAccrualHistory(a.getId(), 50);
            if (history.isEmpty()) {
                Theme.showError(this, "No interest history for this account yet.");
                return;
            }
            String[] cols = {"Date","Period From","Period To","Balance","Rate%","Interest","Status","By"};
            Object[][] data = new Object[history.size()][8];
            int i = 0;
            for (java.util.Map<String,Object> row : history) {
                data[i++] = new Object[]{
                    row.get("date"), row.get("from"), row.get("to"),
                    BankUtil.formatCurrency((double)row.get("balance")),
                    row.get("rate") + "%",
                    BankUtil.formatCurrency((double)row.get("interest")),
                    row.get("status"),
                    row.getOrDefault("by","System")
                };
            }
            JTable t = Theme.styledTable(cols, data);
            JDialog dlg = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
                "Interest History — " + a.getAccountNumber(), true);
            dlg.setSize(860, 400);
            dlg.setLocationRelativeTo(this);
            dlg.add(Theme.styledScroll(t));
            dlg.setVisible(true);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void freezeSelected() {
        Account a = getSelected(); if (a == null) return;
        String reason = JOptionPane.showInputDialog(this, "Reason for freezing:");
        if (reason != null && !reason.trim().isEmpty()) {
            try { AccountService.getInstance().freezeAccount(a.getId(), reason); loadAccounts(); Theme.showSuccess(this,"Account frozen."); }
            catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void unfreezeSelected() {
        Account a = getSelected(); if (a == null) return;
        if (Theme.showConfirm(this,"Unfreeze account " + a.getAccountNumber() + "?")) {
            try { AccountService.getInstance().unfreezeAccount(a.getId()); loadAccounts(); Theme.showSuccess(this,"Account unfrozen."); }
            catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void closeSelected() {
        Account a = getSelected(); if (a == null) return;
        if (Theme.showConfirm(this,"Close account " + a.getAccountNumber() + "? This is irreversible.")) {
            try { AccountService.getInstance().closeAccount(a.getId()); loadAccounts(); Theme.showSuccess(this,"Account closed."); }
            catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }
}
