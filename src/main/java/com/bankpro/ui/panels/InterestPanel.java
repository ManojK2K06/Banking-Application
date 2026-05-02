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

public class InterestPanel extends JPanel {

    private DefaultTableModel rulesModel;
    private List<InterestRule> currentRules;

    private static final String[] RULE_COLS = {
        "Code","Account Type","Rate %","Min Balance","Method","Frequency","Updated By","Last Updated"
    };

    public InterestPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        javax.swing.SwingUtilities.invokeLater(this::loadRules);
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0,16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24,24,24,24));

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.titleLabel("📐  Interest Management"), BorderLayout.WEST);
        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        acts.setOpaque(false);
        JButton newRuleBtn   = Theme.successButton("+ New Rule");
        JButton processBtn   = Theme.warningButton("⚙ Process 30-Day Interest");
        JButton refreshBtn   = Theme.ghostButton("⟳");
        acts.add(newRuleBtn); acts.add(processBtn); acts.add(refreshBtn);
        header.add(acts, BorderLayout.EAST);

        // ── Split: Rules top, accrual history + account override bottom ──────
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerLocation(300);
        split.setBorder(null);
        split.setBackground(Theme.BG_DARK);

        // Rules table
        rulesModel = new DefaultTableModel(RULE_COLS,0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable rulesTable = new JTable(rulesModel);
        Theme.styleTable(rulesTable);

        JPanel topPanel = new JPanel(new BorderLayout(0,8));
        topPanel.setBackground(Theme.BG_DARK);
        topPanel.add(Theme.heading("Interest Rules (Chart)"), BorderLayout.NORTH);
        topPanel.add(Theme.styledScroll(rulesTable), BorderLayout.CENTER);

        JPanel ruleActions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        ruleActions.setOpaque(false);
        JButton editRuleBtn = Theme.primaryButton("✏ Edit Rate");
        JButton acctOverBtn = Theme.ghostButton("🔧 Override Account Rate");
        ruleActions.add(editRuleBtn); ruleActions.add(acctOverBtn);
        topPanel.add(ruleActions, BorderLayout.SOUTH);
        split.setTopComponent(topPanel);

        // Bottom: stats + accrual log
        JPanel bottomPanel = new JPanel(new GridLayout(1,2,16,0));
        bottomPanel.setBackground(Theme.BG_DARK);
        bottomPanel.add(buildStatsCard());
        bottomPanel.add(buildAccrualLogCard());
        split.setBottomComponent(bottomPanel);

        main.add(header, BorderLayout.NORTH);
        main.add(split, BorderLayout.CENTER);
        add(main);

        // ── Wiring ───────────────────────────────────────────────────────────
        newRuleBtn.addActionListener(e  -> showNewRuleDialog());
        refreshBtn.addActionListener(e  -> loadRules());
        processBtn.addActionListener(e  -> processMonthlyInterest());
        editRuleBtn.addActionListener(e -> editSelectedRule(rulesTable));
        acctOverBtn.addActionListener(e -> showAccountOverrideDialog());
    }

    private JPanel buildStatsCard() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(8,8,8,8);

        gc.gridx=0; gc.gridy=0; gc.gridwidth=2;
        card.add(Theme.heading("💡 Interest Summary"), gc);

        JLabel totalPaidLbl  = new JLabel("—"); totalPaidLbl.setForeground(Theme.ACCENT_GOLD); totalPaidLbl.setFont(Theme.FONT_AMOUNT);
        JLabel nextRunLbl    = new JLabel("Manual trigger only"); nextRunLbl.setForeground(Theme.TEXT_SECONDARY);
        JLabel eligibleLbl   = new JLabel("—"); eligibleLbl.setForeground(Theme.ACCENT_BLUE);

        int[] row = {1};
        Object[][] rows = {{"Total Interest Paid (all time)", totalPaidLbl},
                           {"Eligible Accounts (pending 30d)", eligibleLbl},
                           {"Interest Processing", nextRunLbl}};
        for (Object[] r : rows) {
            gc.gridwidth=1; gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.5;
            card.add(Theme.label((String)r[0]), gc);
            gc.gridx=1; card.add((Component)r[1], gc); row[0]++;
        }

        JButton calcBtn = Theme.ghostButton("🔄 Refresh Stats");
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2; gc.insets=new Insets(16,8,4,8);
        card.add(calcBtn, gc);

        calcBtn.addActionListener(e -> new SwingWorker<double[],Void>() {
            @Override protected double[] doInBackground() throws Exception {
                double total = InterestService.getInstance().getAccruedInterestTotal();
                return new double[]{total};
            }
            @Override protected void done() {
                try {
                    double[] d = get();
                    totalPaidLbl.setText(BankUtil.formatCurrency(d[0]));
                } catch (Exception ex) { totalPaidLbl.setText("Error"); }
            }
        }.execute());

        return card;
    }

    private JPanel buildAccrualLogCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0,8));

        card.add(Theme.heading("📋 Recent Interest Credits"), BorderLayout.NORTH);

        String[] cols = {"Account","Date","Period","Balance","Rate%","Interest"};
        DefaultTableModel model = new DefaultTableModel(cols,0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.setFont(Theme.FONT_SMALL);
        table.setRowHeight(28);

        // Load recent accruals
        try (var conn = com.bankpro.db.DatabaseManager.getInstance().getConnection();
             var stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                "SELECT ia.*, a.account_number FROM interest_accrual ia " +
                "LEFT JOIN accounts a ON ia.account_id=a.id " +
                "ORDER BY ia.created_at DESC LIMIT 30");
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("account_number"),
                    rs.getString("accrual_date"),
                    rs.getString("period_from")+" → "+rs.getString("period_to"),
                    BankUtil.formatCurrency(rs.getDouble("avg_balance")),
                    rs.getDouble("rate_applied")+"%",
                    BankUtil.formatCurrency(rs.getDouble("interest_amount"))
                });
            }
        } catch (Exception ignored) {}

        card.add(Theme.styledScroll(table), BorderLayout.CENTER);
        return card;
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadRules() {
        new SwingWorker<List<InterestRule>,Void>() {
            @Override protected List<InterestRule> doInBackground() throws Exception {
                return InterestService.getInstance().getAllRules();
            }
            @Override protected void done() {
                try {
                    currentRules = get();
                    rulesModel.setRowCount(0);
                    for (InterestRule r : currentRules) {
                        rulesModel.addRow(new Object[]{
                            r.getRuleCode(), r.getAccountType(),
                            r.getAnnualRate()+"%", BankUtil.formatCurrency(r.getMinBalance()),
                            r.getMethodLabel(), r.getFrequencyLabel(),
                            r.getUpdatedByName() != null ? r.getUpdatedByName() : "System",
                            r.getUpdatedAt() != null ? BankUtil.formatDate(r.getUpdatedAt()) : "—"
                        });
                    }
                } catch (Exception ex) { Theme.showError(InterestPanel.this, ex.getMessage()); }
            }
        }.execute();
    }

    private void editSelectedRule(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0 || currentRules == null) { Theme.showError(this,"Select a rule first"); return; }
        InterestRule rule = currentRules.get(row);

        JDialog dlg = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"Edit Interest Rule",true);
        dlg.setSize(480,380);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24,32,24,32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(8,4,8,4);

        JTextField rateField   = Theme.styledField(); rateField.setText(String.valueOf(rule.getAnnualRate()));
        JTextField minBalField = Theme.styledField(); minBalField.setText(String.valueOf(rule.getMinBalance()));
        JComboBox<String> methodBox = Theme.styledCombo();
        for (String m : new String[]{"DAILY_BALANCE","MONTHLY_BALANCE","MINIMUM_BALANCE"}) methodBox.addItem(m);
        methodBox.setSelectedItem(rule.getCalculationMethod());
        JComboBox<String> freqBox = Theme.styledCombo();
        for (String f : new String[]{"MONTHLY","QUARTERLY","ANNUALLY"}) freqBox.addItem(f);
        freqBox.setSelectedItem(rule.getCreditFrequency());
        JTextArea notesArea = Theme.styledTextArea(); notesArea.setText(rule.getNotes() != null ? rule.getNotes() : "");
        notesArea.setRows(3);

        int[] r = {0};
        gc.gridx=0; gc.gridy=r[0]; gc.gridwidth=2;
        p.add(Theme.heading("Rule: "+rule.getRuleCode()+" — "+rule.getAccountType()), gc); r[0]++;
        gc.gridwidth=1;
        Object[][] rows = {
            {"Annual Rate % *", rateField}, {"Min Balance (₹)", minBalField},
            {"Calculation Method", methodBox}, {"Credit Frequency", freqBox},
        };
        for (Object[] row2 : rows) {
            gc.gridx=0; gc.gridy=r[0]; gc.weightx=0.4; p.add(Theme.label((String)row2[0]), gc);
            gc.gridx=1; gc.weightx=0.6; p.add((Component)row2[1], gc); r[0]++;
        }
        gc.gridx=0; gc.gridy=r[0]; gc.gridwidth=2;
        p.add(Theme.label("Notes"), gc); r[0]++;
        gc.gridy=r[0]; p.add(new JScrollPane(notesArea), gc); r[0]++;

        JButton saveBtn = Theme.successButton("Update Rule");
        gc.gridy=r[0]; gc.insets=new Insets(16,4,4,4); p.add(saveBtn, gc);

        saveBtn.addActionListener(e -> {
            try {
                double rate = Double.parseDouble(rateField.getText().trim());
                double minB = Double.parseDouble(minBalField.getText().trim());
                InterestService.getInstance().updateRule(rule.getId(), rate, minB,
                    (String)methodBox.getSelectedItem(), (String)freqBox.getSelectedItem(),
                    notesArea.getText().trim());
                dlg.dispose(); loadRules();
                Theme.showSuccess(this,"Interest rule updated to "+rate+"%");
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        dlg.add(p); dlg.setVisible(true);
    }

    private void showNewRuleDialog() {
        JDialog dlg = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),"New Interest Rule",true);
        dlg.setSize(440,380);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24,32,24,32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(8,4,8,4);

        JComboBox<String> acctType = Theme.styledCombo();
        for (String t : new String[]{"SAVINGS","CURRENT","SALARY","NRI","FIXED_DEPOSIT"}) acctType.addItem(t);
        JTextField rateField   = Theme.styledField(); rateField.setText("3.50");
        JTextField minBalField = Theme.styledField(); minBalField.setText("0");
        JComboBox<String> methodBox = Theme.styledCombo();
        for (String m : new String[]{"DAILY_BALANCE","MONTHLY_BALANCE","MINIMUM_BALANCE"}) methodBox.addItem(m);
        JComboBox<String> freqBox = Theme.styledCombo();
        for (String f : new String[]{"MONTHLY","QUARTERLY","ANNUALLY"}) freqBox.addItem(f);
        JTextField notesField  = Theme.styledField();

        int[] r = {0};
        gc.gridx=0; gc.gridy=r[0]; gc.gridwidth=2;
        p.add(Theme.heading("Create New Interest Rule"), gc); r[0]++;
        gc.gridwidth=1;
        Object[][] rows = {
            {"Account Type", acctType}, {"Annual Rate %", rateField},
            {"Min Balance (₹)", minBalField}, {"Method", methodBox},
            {"Frequency", freqBox}, {"Notes", notesField}
        };
        for (Object[] row : rows) {
            gc.gridx=0; gc.gridy=r[0]; gc.weightx=0.4; p.add(Theme.label((String)row[0]), gc);
            gc.gridx=1; gc.weightx=0.6; p.add((Component)row[1], gc); r[0]++;
        }

        JButton createBtn = Theme.successButton("Create Rule");
        gc.gridx=0; gc.gridy=r[0]; gc.gridwidth=2; gc.insets=new Insets(16,4,4,4);
        p.add(createBtn, gc);

        createBtn.addActionListener(e -> {
            try {
                InterestService.getInstance().createRule(
                    (String)acctType.getSelectedItem(),
                    Double.parseDouble(rateField.getText().trim()),
                    Double.parseDouble(minBalField.getText().trim()),
                    (String)methodBox.getSelectedItem(),
                    (String)freqBox.getSelectedItem(),
                    notesField.getText().trim());
                dlg.dispose(); loadRules();
                Theme.showSuccess(this, "Interest rule created.");
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        dlg.add(p); dlg.setVisible(true);
    }

    private void showAccountOverrideDialog() {
        JTextField acctField = Theme.styledField();
        JTextField rateField = Theme.styledField();
        JLabel acctInfo      = Theme.label("—");
        final int[] aId      = {-1};

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(16,24,16,24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(8,4,8,4);

        gc.gridx=0; gc.gridy=0; gc.gridwidth=2;
        p.add(Theme.heading("Override Account Interest Rate"), gc);
        gc.gridwidth=1; gc.gridy=1; gc.gridx=0; gc.weightx=0.4; p.add(Theme.label("Account Number"), gc);
        gc.gridx=1; gc.weightx=0.6; p.add(acctField, gc);
        gc.gridy=2; gc.gridx=0; p.add(Theme.label("New Annual Rate %"), gc);
        gc.gridx=1; p.add(rateField, gc);
        gc.gridy=3; gc.gridx=0; gc.gridwidth=2; p.add(acctInfo, gc);

        acctField.addActionListener(e -> {
            try {
                Account a = AccountService.getInstance().getByAccountNumber(acctField.getText().trim());
                if (a != null) {
                    aId[0] = a.getId();
                    rateField.setText(String.valueOf(a.getInterestRate()));
                    acctInfo.setText("✓ " + a.getPartyName() + " — " + a.getAccountTypeLabel() +
                        "  Current rate: " + a.getInterestRate() + "%");
                    acctInfo.setForeground(Theme.ACCENT_GREEN);
                } else { acctInfo.setText("❌ Account not found"); aId[0]=-1; }
            } catch (Exception ex) { Theme.showError(null, ex.getMessage()); }
        });

        int res = JOptionPane.showConfirmDialog(this, p, "Override Rate", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            if (aId[0] < 0) { Theme.showError(this,"Verify account first (press Enter in Account field)"); return; }
            try {
                double rate = Double.parseDouble(rateField.getText().trim());
                InterestService.getInstance().overrideAccountRate(aId[0], rate);
                Theme.showSuccess(this,"Account rate overridden to "+rate+"%");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void processMonthlyInterest() {
        if (!Theme.showConfirm(this,
            "Process 30-day interest for ALL eligible accounts?\n" +
            "This will credit interest to all accounts due for their monthly cycle.")) return;

        JDialog prog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Processing...", true);
        JLabel lbl = new JLabel("  Processing interest... please wait", SwingConstants.CENTER);
        lbl.setFont(Theme.FONT_HEADING); lbl.setForeground(Theme.TEXT_PRIMARY);
        prog.setContentPane(new JPanel(new BorderLayout()) {{
            setBackground(Theme.BG_CARD); add(lbl);
        }});
        prog.setSize(360, 100);
        prog.setLocationRelativeTo(this);

        new SwingWorker<Integer,Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return InterestService.getInstance().processMonthlyInterest();
            }
            @Override protected void done() {
                prog.dispose();
                try {
                    int count = get();
                    Theme.showSuccess(InterestPanel.this,
                        "✅ Interest processed for " + count + " accounts.\n" +
                        "All eligible accounts have been credited.");
                    loadRules();
                } catch (Exception ex) {
                    Theme.showError(InterestPanel.this,
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();

        SwingUtilities.invokeLater(() -> prog.setVisible(true));
    }
}
