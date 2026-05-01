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

    private static final String[] COLS = {
        "Account No.","Customer","Type","Balance","Currency","Status","IFSC","Opened"
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
        JButton openBtn   = Theme.successButton("+ Open Account");
        JButton refreshBtn = Theme.ghostButton("⟳  Refresh");
        acts.add(openBtn); acts.add(refreshBtn);
        header.add(acts, BorderLayout.EAST);

        tableModel = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {160,150,110,140,80,80,100,110};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomBar.setOpaque(false);
        JButton stmtBtn   = Theme.primaryButton("📄 Statement");
        JButton freezeBtn = Theme.warningButton("❄ Freeze");
        JButton unfreezeBtn = Theme.ghostButton("♨ Unfreeze");
        JButton closeBtn  = Theme.dangerButton("⊘ Close Account");
        bottomBar.add(stmtBtn); bottomBar.add(freezeBtn);
        bottomBar.add(unfreezeBtn); bottomBar.add(closeBtn);

        main.add(header, BorderLayout.NORTH);
        main.add(Theme.styledScroll(table), BorderLayout.CENTER);
        main.add(bottomBar, BorderLayout.SOUTH);
        add(main);

        openBtn.addActionListener(e -> showOpenAccountDialog());
        refreshBtn.addActionListener(e -> loadAccounts());
        stmtBtn.addActionListener(e -> showStatement());
        freezeBtn.addActionListener(e -> freezeSelected());
        unfreezeBtn.addActionListener(e -> unfreezeSelected());
        closeBtn.addActionListener(e -> closeSelected());
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
                            a.getAccountNumber(), a.getCustomerName(), a.getAccountTypeLabel(),
                            BankUtil.formatCurrency(a.getBalance()), a.getCurrency(),
                            a.getStatus(), a.getIfscCode(),
                            a.getCreatedAt() != null ? BankUtil.formatDate(a.getCreatedAt()) : ""
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
        JDialog dlg = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
            "Open New Account", true);
        dlg.setSize(480, 380);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 4, 6, 4);

        JTextField customerSearch = Theme.styledField();
        JLabel customerFound = Theme.label("Enter customer ID or search...");
        JButton searchBtn = Theme.ghostButton("Search");
        JComboBox<String> typeCombo = Theme.styledCombo();
        for (String t : new String[]{"SAVINGS","CURRENT","SALARY","NRI"}) typeCombo.addItem(t);
        JTextField initialDeposit = Theme.styledField();
        JComboBox<String> currencyCombo = Theme.styledCombo();
        for (String c : new String[]{"INR","USD","EUR","GBP","AED","SGD"}) currencyCombo.addItem(c);

        final int[] selectedCustomerId = {-1};

        int[] r = {0};
        Object[][] rows = {
            {"Customer ID / Search", customerSearch},
            {"", searchBtn},
            {"Account Type", typeCombo},
            {"Initial Deposit (₹)", initialDeposit},
            {"Currency", currencyCombo}
        };

        for (Object[] row : rows) {
            gc.gridx=0; gc.gridy=r[0]; gc.weightx=0.35;
            p.add(Theme.label((String)row[0]), gc);
            gc.gridx=1; gc.weightx=0.65;
            p.add((Component)row[1], gc);
            r[0]++;
        }

        gc.gridx=0; gc.gridy=r[0]; gc.gridwidth=2;
        p.add(customerFound, gc); r[0]++;

        JButton openBtn = Theme.successButton("Open Account");
        gc.gridy=r[0]; gc.insets=new Insets(16,4,4,4);
        p.add(openBtn, gc);

        searchBtn.addActionListener(e -> {
            String query = customerSearch.getText().trim();
            if (query.isEmpty()) { Theme.showError(dlg, "Enter customer ID or name"); return; }
            try {
                List<Customer> results = CustomerService.getInstance().searchCustomers(query);
                if (results.isEmpty()) { customerFound.setText("❌ No customer found"); return; }
                if (results.size() == 1) {
                    Customer c = results.get(0);
                    selectedCustomerId[0] = c.getId();
                    customerFound.setText("✓ " + c.getFullName() + "  [" + c.getCustomerId() + "]");
                    customerFound.setForeground(Theme.ACCENT_GREEN);
                } else {
                    String[] names = results.stream().map(c -> c.getCustomerId() + " — " + c.getFullName())
                        .toArray(String[]::new);
                    String sel = (String)JOptionPane.showInputDialog(dlg, "Select customer:",
                        "Multiple Found", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
                    if (sel != null) {
                        int idx = java.util.Arrays.asList(names).indexOf(sel);
                        Customer c = results.get(idx);
                        selectedCustomerId[0] = c.getId();
                        customerFound.setText("✓ " + c.getFullName() + "  [" + c.getCustomerId() + "]");
                        customerFound.setForeground(Theme.ACCENT_GREEN);
                    }
                }
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        openBtn.addActionListener(e -> {
            if (selectedCustomerId[0] < 0) { Theme.showError(dlg, "Please search and select a customer first"); return; }
            if (initialDeposit.getText().trim().isEmpty()) { Theme.showError(dlg, "Enter initial deposit amount"); return; }
            if (!BankUtil.isValidAmount(initialDeposit.getText().trim())) { Theme.showError(dlg, "Invalid amount"); return; }
            try {
                double amt = Double.parseDouble(initialDeposit.getText().trim());
                Account acc = AccountService.getInstance().openAccount(
                    selectedCustomerId[0],
                    (String)typeCombo.getSelectedItem(),
                    amt,
                    (String)currencyCombo.getSelectedItem()
                );
                dlg.dispose();
                loadAccounts();
                Theme.showSuccess(this, "Account opened!\nAccount No: " + acc.getAccountNumber());
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        dlg.add(p);
        dlg.setVisible(true);
    }

    private void showStatement() {
        Account a = getSelected();
        if (a == null) return;
        try {
            List<Transaction> txns = TransactionService.getInstance()
                .getTransactionsByAccount(a.getId(), 50);
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
                    t.getPerformedByName() != null ? t.getPerformedByName() : ""
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
            dlg.setBackground(Theme.BG_DARK);

            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(Theme.BG_CARD);
            top.setBorder(new EmptyBorder(12, 16, 12, 16));
            top.add(new JLabel("<html><b>" + a.getAccountNumber() + "</b>  " +
                a.getAccountTypeLabel() + "  |  Balance: " +
                BankUtil.formatCurrency(a.getBalance()) + "</html>"), BorderLayout.WEST);

            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(Theme.BG_DARK);
            p.add(top, BorderLayout.NORTH);
            p.add(Theme.styledScroll(stmtTable), BorderLayout.CENTER);
            dlg.add(p);
            dlg.setVisible(true);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void freezeSelected() {
        Account a = getSelected();
        if (a == null) return;
        String reason = JOptionPane.showInputDialog(this, "Reason for freezing account:");
        if (reason != null && !reason.trim().isEmpty()) {
            try {
                AccountService.getInstance().freezeAccount(a.getId(), reason);
                loadAccounts();
                Theme.showSuccess(this, "Account frozen.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void unfreezeSelected() {
        Account a = getSelected();
        if (a == null) return;
        if (Theme.showConfirm(this, "Unfreeze account " + a.getAccountNumber() + "?")) {
            try {
                AccountService.getInstance().unfreezeAccount(a.getId());
                loadAccounts();
                Theme.showSuccess(this, "Account unfrozen.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void closeSelected() {
        Account a = getSelected();
        if (a == null) return;
        if (Theme.showConfirm(this, "Close account " + a.getAccountNumber() + "? This is irreversible.")) {
            try {
                AccountService.getInstance().closeAccount(a.getId());
                loadAccounts();
                Theme.showSuccess(this, "Account closed.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }
}
