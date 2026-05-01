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

public class LoanPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Loan> currentList;
    private JComboBox<String> filterCombo;

    private static final String[] COLS = {
        "Loan ID","Customer","Type","Principal","Outstanding","EMI","Rate%","Tenure","Status","Next EMI"
    };

    public LoanPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        javax.swing.SwingUtilities.invokeLater(() -> loadLoans("ALL"));
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(Theme.titleLabel("📋  Loan Management"), BorderLayout.WEST);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acts.setOpaque(false);
        filterCombo = Theme.styledCombo();
        for (String s : new String[]{"ALL","PENDING","APPROVED","ACTIVE","CLOSED","REJECTED"}) filterCombo.addItem(s);
        JButton applyBtn   = Theme.successButton("+ Apply Loan");
        JButton refreshBtn = Theme.ghostButton("⟳");
        acts.add(new JLabel("Filter: ") {{ setForeground(Theme.TEXT_SECONDARY); }});
        acts.add(filterCombo); acts.add(applyBtn); acts.add(refreshBtn);
        header.add(acts, BorderLayout.EAST);

        tableModel = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Color status column
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String s = v != null ? v.toString() : "";
                if (s.contains("ACTIVE"))   setForeground(Theme.ACCENT_GREEN);
                else if (s.contains("PEND")) setForeground(Theme.ACCENT_GOLD);
                else if (s.contains("CLOS")) setForeground(Theme.TEXT_MUTED);
                else if (s.contains("REJ"))  setForeground(Theme.ACCENT_RED);
                else                         setForeground(Theme.TEXT_PRIMARY);
                return this;
            }
        });

        // Bottom bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomBar.setOpaque(false);
        JButton viewBtn    = Theme.ghostButton("👁 Details");
        JButton approveBtn = Theme.successButton("✅ Approve");
        JButton rejectBtn  = Theme.dangerButton("✗ Reject");
        JButton disburseBtn = Theme.warningButton("💰 Disburse");
        JButton repayBtn   = Theme.primaryButton("💳 Repay EMI");
        JButton calcBtn    = Theme.ghostButton("🧮 EMI Calc");
        bottomBar.add(viewBtn); bottomBar.add(approveBtn); bottomBar.add(rejectBtn);
        bottomBar.add(disburseBtn); bottomBar.add(repayBtn); bottomBar.add(calcBtn);

        main.add(header, BorderLayout.NORTH);
        main.add(Theme.styledScroll(table), BorderLayout.CENTER);
        main.add(bottomBar, BorderLayout.SOUTH);
        add(main);

        filterCombo.addActionListener(e -> loadLoans((String) filterCombo.getSelectedItem()));
        refreshBtn.addActionListener(e -> loadLoans((String) filterCombo.getSelectedItem()));
        applyBtn.addActionListener(e -> showApplyDialog());
        viewBtn.addActionListener(e -> viewDetails());
        approveBtn.addActionListener(e -> approveLoan());
        rejectBtn.addActionListener(e -> rejectLoan());
        disburseBtn.addActionListener(e -> disburseLoan());
        repayBtn.addActionListener(e -> repayLoan());
        calcBtn.addActionListener(e -> showEmiCalculator());
    }

    private void loadLoans(String filter) {
        new SwingWorker<List<Loan>, Void>() {
            @Override protected List<Loan> doInBackground() throws Exception {
                List<Loan> all = LoanService.getInstance().getAllLoans();
                if ("ALL".equals(filter)) return all;
                return all.stream().filter(l -> l.getStatus().equals(filter)).toList();
            }
            @Override protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (Loan l : currentList) {
                        tableModel.addRow(new Object[]{
                            l.getLoanId(), l.getCustomerName(), l.getLoanType(),
                            BankUtil.formatCurrency(l.getPrincipalAmount()),
                            BankUtil.formatCurrency(l.getOutstandingAmount()),
                            BankUtil.formatCurrency(l.getEmiAmount()),
                            l.getInterestRate() + "%",
                            l.getTenureMonths() + "M",
                            l.getStatusLabel(),
                            l.getNextEmiDate() != null ? l.getNextEmiDate() : "—"
                        });
                    }
                } catch (Exception ex) { Theme.showError(LoanPanel.this, ex.getMessage()); }
            }
        }.execute();
    }

    private Loan getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null) { Theme.showError(this, "Select a loan first"); return null; }
        return currentList.get(row);
    }

    private void showApplyDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Apply for Loan", true);
        dlg.setSize(580, 640);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 4, 6, 4);

        JTextField customerSearch = Theme.styledField();
        JLabel customerFound      = Theme.label("Search and select customer...");
        JButton searchBtn         = Theme.ghostButton("Search");
        JComboBox<String> typeBox = Theme.styledCombo();
        for (String t : new String[]{"PERSONAL","HOME","CAR","EDUCATION","BUSINESS","GOLD","MORTGAGE"}) typeBox.addItem(t);
        JTextField amountField    = Theme.styledField();
        JTextField tenureField    = Theme.styledField();
        JTextField rateField      = Theme.styledField();
        JTextField purposeField   = Theme.styledField();
        JTextField collateralField= Theme.styledField();
        JLabel emiPreview         = Theme.label("EMI: —");
        emiPreview.setForeground(Theme.ACCENT_GOLD);
        emiPreview.setFont(Theme.FONT_HEADING);
        JTextField accountField   = Theme.styledField();
        JLabel accountInfo        = Theme.label("—");

        final int[] cId = {-1}, aId = {-1};

        // Default rates
        typeBox.addActionListener(e -> {
            String t = (String) typeBox.getSelectedItem();
            rateField.setText(switch (t) {
                case "HOME"      -> "8.50";
                case "CAR"       -> "9.75";
                case "EDUCATION" -> "10.50";
                case "PERSONAL"  -> "13.00";
                case "BUSINESS"  -> "11.50";
                case "GOLD"      -> "9.00";
                default          -> "12.00";
            });
            updateEmiPreview(amountField, tenureField, rateField, emiPreview);
        });
        amountField.addActionListener(e -> updateEmiPreview(amountField, tenureField, rateField, emiPreview));
        tenureField.addActionListener(e -> updateEmiPreview(amountField, tenureField, rateField, emiPreview));
        rateField.addActionListener(e -> updateEmiPreview(amountField, tenureField, rateField, emiPreview));

        int row = 0;
        Object[][] fields = {
            {"Customer Search", customerSearch}, {"", searchBtn}, {"Account Number *", accountField},
            {"Loan Type *", typeBox}, {"Amount (₹) *", amountField},
            {"Tenure (months) *", tenureField}, {"Interest Rate % *", rateField},
            {"Purpose", purposeField}, {"Collateral", collateralField},
        };
        for (Object[] f : fields) {
            gc.gridx = 0; gc.gridy = row; gc.weightx = 0.35;
            p.add(Theme.label((String) f[0]), gc);
            gc.gridx = 1; gc.weightx = 0.65;
            p.add(Theme.label((String) f[0]), gc);
            gc.gridx = 1; gc.weightx = 0.65;
            p.add((Component) f[1], gc);
            row++;
        }

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        p.add(customerFound, gc); row++;
        gc.gridy = row; p.add(accountInfo, gc); row++;
        gc.gridy = row; p.add(emiPreview, gc); row++;

        JButton submitBtn = Theme.successButton("Submit Loan Application");
        gc.gridy = row; gc.insets = new Insets(16, 4, 4, 4);
        p.add(submitBtn, gc);

        searchBtn.addActionListener(e -> {
            try {
                List<Customer> res = CustomerService.getInstance().searchCustomers(customerSearch.getText().trim());
                if (res.isEmpty()) { customerFound.setText("❌ Not found"); return; }
                Customer c = res.size() == 1 ? res.get(0) : selectFromList(dlg, res);
                if (c == null) return;
                cId[0] = c.getId();
                customerFound.setText("✓ " + c.getFullName() + " [" + c.getCustomerId() + "] — Credit: " + c.getCreditScore());
                customerFound.setForeground(Theme.ACCENT_GREEN);
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        accountField.addActionListener(e -> {
            try {
                Account a = AccountService.getInstance().getByAccountNumber(accountField.getText().trim());
                if (a == null) { accountInfo.setText("❌ Account not found"); aId[0] = -1; return; }
                aId[0] = a.getId();
                accountInfo.setText("✓ " + a.getAccountNumber() + " — " + a.getAccountTypeLabel());
                accountInfo.setForeground(Theme.ACCENT_GREEN);
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        submitBtn.addActionListener(e -> {
            if (cId[0] < 0) { Theme.showError(dlg, "Search and select a customer first"); return; }
            if (aId[0] < 0) { Theme.showError(dlg, "Enter valid account number (press Enter to verify)"); return; }
            if (!BankUtil.isValidAmount(amountField.getText())) { Theme.showError(dlg, "Invalid loan amount"); return; }
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                int tenure = Integer.parseInt(tenureField.getText().trim());
                double rate = Double.parseDouble(rateField.getText().trim());
                Loan loan = LoanService.getInstance().applyLoan(
                    cId[0], aId[0], (String) typeBox.getSelectedItem(),
                    amount, tenure, rate, purposeField.getText().trim(), collateralField.getText().trim());
                dlg.dispose();
                loadLoans("ALL");
                Theme.showSuccess(this, "Loan application submitted!\nLoan ID: " + loan.getLoanId()
                    + "\nEMI: " + BankUtil.formatCurrency(loan.getEmiAmount()) + "/month"
                    + "\nPending approval from Manager.");
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        JScrollPane sp = new JScrollPane(p);
        sp.setBackground(Theme.BG_CARD); sp.getViewport().setBackground(Theme.BG_CARD); sp.setBorder(null);
        dlg.add(sp);
        rateField.setText("13.00");
        dlg.setVisible(true);
    }

    private void updateEmiPreview(JTextField amt, JTextField tenure, JTextField rate, JLabel label) {
        try {
            double a = Double.parseDouble(amt.getText().trim());
            int t    = Integer.parseInt(tenure.getText().trim());
            double r = Double.parseDouble(rate.getText().trim());
            double emi = BankUtil.calculateEMI(a, r, t);
            label.setText("EMI: " + BankUtil.formatCurrency(emi) + "/month  |  Total: " + BankUtil.formatCurrency(emi * t));
        } catch (Exception ignored) { label.setText("EMI: —"); }
    }

    private Customer selectFromList(JDialog parent, List<Customer> list) {
        String[] names = list.stream().map(c -> c.getCustomerId() + " — " + c.getFullName()).toArray(String[]::new);
        String sel = (String) JOptionPane.showInputDialog(parent, "Select:", "Multiple Found",
            JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (sel == null) return null;
        int idx = java.util.Arrays.asList(names).indexOf(sel);
        return list.get(idx);
    }

    private void viewDetails() {
        Loan l = getSelected();
        if (l == null) return;
        double totalInterest = l.getEmiAmount() * l.getTenureMonths() - l.getPrincipalAmount();
        String info = String.format(
            "<html><table cellpadding='4'>" +
            "<tr><td><b>Loan ID</b></td><td>%s</td></tr>" +
            "<tr><td><b>Customer</b></td><td>%s</td></tr>" +
            "<tr><td><b>Type</b></td><td>%s</td></tr>" +
            "<tr><td><b>Principal</b></td><td>%s</td></tr>" +
            "<tr><td><b>Outstanding</b></td><td>%s</td></tr>" +
            "<tr><td><b>Interest Rate</b></td><td>%.2f%%</td></tr>" +
            "<tr><td><b>Tenure</b></td><td>%d months</td></tr>" +
            "<tr><td><b>EMI</b></td><td>%s/month</td></tr>" +
            "<tr><td><b>Total Interest</b></td><td>%s</td></tr>" +
            "<tr><td><b>Total Paid</b></td><td>%s</td></tr>" +
            "<tr><td><b>Penalty</b></td><td>%s</td></tr>" +
            "<tr><td><b>Status</b></td><td>%s</td></tr>" +
            "<tr><td><b>Next EMI</b></td><td>%s</td></tr>" +
            "<tr><td><b>Purpose</b></td><td>%s</td></tr>" +
            "</table></html>",
            l.getLoanId(), l.getCustomerName(), l.getLoanTypeLabel(),
            BankUtil.formatCurrency(l.getPrincipalAmount()),
            BankUtil.formatCurrency(l.getOutstandingAmount()),
            l.getInterestRate(), l.getTenureMonths(),
            BankUtil.formatCurrency(l.getEmiAmount()),
            BankUtil.formatCurrency(totalInterest),
            BankUtil.formatCurrency(l.getTotalPaid()),
            BankUtil.formatCurrency(l.getPenaltyCharges()),
            l.getStatusLabel(),
            l.getNextEmiDate() != null ? l.getNextEmiDate() : "—",
            l.getPurpose() != null ? l.getPurpose() : "—"
        );
        JOptionPane.showMessageDialog(this, new JLabel(info), "Loan Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void approveLoan() {
        Loan l = getSelected();
        if (l == null) return;
        if (!"PENDING".equals(l.getStatus())) { Theme.showError(this, "Only PENDING loans can be approved"); return; }
        if (Theme.showConfirm(this, "Approve loan " + l.getLoanId() + "?")) {
            try {
                LoanService.getInstance().approveLoan(l.getId());
                loadLoans((String) filterCombo.getSelectedItem());
                Theme.showSuccess(this, "Loan approved! Proceed to disburse when ready.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void rejectLoan() {
        Loan l = getSelected();
        if (l == null) return;
        if (!"PENDING".equals(l.getStatus())) { Theme.showError(this, "Only PENDING loans can be rejected"); return; }
        String reason = JOptionPane.showInputDialog(this, "Rejection reason:");
        if (reason != null && !reason.trim().isEmpty()) {
            try {
                LoanService.getInstance().rejectLoan(l.getId(), reason);
                loadLoans((String) filterCombo.getSelectedItem());
                Theme.showSuccess(this, "Loan rejected.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void disburseLoan() {
        Loan l = getSelected();
        if (l == null) return;
        if (!"APPROVED".equals(l.getStatus())) { Theme.showError(this, "Only APPROVED loans can be disbursed"); return; }
        if (Theme.showConfirm(this, "Disburse " + BankUtil.formatCurrency(l.getPrincipalAmount())
                + " to account " + l.getAccountNumber() + "?")) {
            try {
                LoanService.getInstance().disburseLoan(l.getId());
                loadLoans((String) filterCombo.getSelectedItem());
                Theme.showSuccess(this, "Loan disbursed! Amount credited to customer account.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void repayLoan() {
        Loan l = getSelected();
        if (l == null) return;
        if (!"ACTIVE".equals(l.getStatus())) { Theme.showError(this, "Only ACTIVE loans can be repaid"); return; }

        String amtStr = JOptionPane.showInputDialog(this,
            "Outstanding: " + BankUtil.formatCurrency(l.getOutstandingAmount())
            + "\nEMI: " + BankUtil.formatCurrency(l.getEmiAmount())
            + "\nEnter repayment amount:",
            String.format("%.2f", l.getEmiAmount()));
        if (amtStr == null || amtStr.trim().isEmpty()) return;
        if (!BankUtil.isValidAmount(amtStr)) { Theme.showError(this, "Invalid amount"); return; }

        double amount = Double.parseDouble(amtStr.trim());
        if (Theme.showConfirm(this, "Repay " + BankUtil.formatCurrency(amount) + " for loan " + l.getLoanId() + "?")) {
            try {
                LoanService.getInstance().repayLoan(l.getId(), amount);
                loadLoans((String) filterCombo.getSelectedItem());
                Theme.showSuccess(this, "Repayment of " + BankUtil.formatCurrency(amount) + " processed.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void showEmiCalculator() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "EMI Calculator", true);
        dlg.setSize(420, 340);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 4, 8, 4);

        JTextField principal = Theme.styledField(); principal.setText("500000");
        JTextField rate      = Theme.styledField(); rate.setText("12.00");
        JTextField tenure    = Theme.styledField(); tenure.setText("60");
        JLabel result        = new JLabel(); result.setFont(Theme.FONT_HEADING); result.setForeground(Theme.ACCENT_GOLD);

        int[] row = {0};
        Object[][] rows = {
            {"Principal (₹)", principal}, {"Annual Rate (%)", rate}, {"Tenure (months)", tenure}
        };
        for (Object[] r : rows) {
            gc.gridx = 0; gc.gridy = row[0]; gc.weightx = 0.4; p.add(Theme.label((String) r[0]), gc);
            gc.gridx = 1; gc.weightx = 0.6; p.add((Component) r[1], gc); row[0]++;
        }
        JButton calcBtn = Theme.primaryButton("Calculate");
        gc.gridx = 0; gc.gridy = row[0]; gc.gridwidth = 2; p.add(calcBtn, gc); row[0]++;
        gc.gridy = row[0]; p.add(result, gc);

        calcBtn.addActionListener(e -> {
            try {
                double pr = Double.parseDouble(principal.getText().trim());
                double ra = Double.parseDouble(rate.getText().trim());
                int te    = Integer.parseInt(tenure.getText().trim());
                double emi = BankUtil.calculateEMI(pr, ra, te);
                double total = emi * te;
                double interest = total - pr;
                result.setText("<html>EMI: <b>" + BankUtil.formatCurrency(emi) + "</b>/month<br>"
                    + "Total Payment: " + BankUtil.formatCurrency(total) + "<br>"
                    + "Total Interest: " + BankUtil.formatCurrency(interest) + "</html>");
            } catch (Exception ex) { result.setText("Invalid input"); result.setForeground(Theme.ACCENT_RED); }
        });

        dlg.add(p);
        dlg.setVisible(true);
    }

}
