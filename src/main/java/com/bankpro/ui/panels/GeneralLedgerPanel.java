package com.bankpro.ui.panels;

import com.bankpro.model.*;
import com.bankpro.service.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GeneralLedgerPanel extends JPanel {

    private JTabbedPane tabs;
    private DefaultTableModel assetModel, liabModel, incomeModel, expenseModel;
    private JLabel assetTotal, liabTotal, incomeTotal, expenseTotal, netProfitLabel;

    public GeneralLedgerPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(Theme.titleLabel("📒  General Ledger"), BorderLayout.WEST);
        JButton refreshBtn = Theme.ghostButton("⟳  Refresh All");
        refreshBtn.addActionListener(e -> refreshAll());
        header.add(refreshBtn, BorderLayout.EAST);

        tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_CARD);
        tabs.setForeground(Theme.TEXT_PRIMARY);
        tabs.setFont(Theme.FONT_BODY);

        tabs.addTab("📊 Balance Sheet", buildBalanceSheetTab());
        tabs.addTab("📈 P & L", buildPnLTab());
        tabs.addTab("📋 Chart of Accounts", buildCoaTab());
        tabs.addTab("📔 Journal Entries", buildJournalTab());
        tabs.addTab("🔎 GL Drill-Down", buildDrillDownTab());

        main.add(header, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);
        add(main);

        javax.swing.SwingUtilities.invokeLater(this::refreshAll);
    }

    // ── Balance Sheet Tab ─────────────────────────────────────────────────────

    private JPanel buildBalanceSheetTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        assetModel = new DefaultTableModel(new String[] { "Code", "Account", "Balance" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        liabModel = new DefaultTableModel(new String[] { "Code", "Account", "Balance" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable assetTable = new JTable(assetModel);
        Theme.styleTable(assetTable);
        assetTable.getColumnModel().getColumn(2).setCellRenderer(amountRenderer());
        JTable liabTable = new JTable(liabModel);
        Theme.styleTable(liabTable);
        liabTable.getColumnModel().getColumn(2).setCellRenderer(amountRenderer());

        assetTotal = new JLabel("Total: —");
        assetTotal.setFont(Theme.FONT_HEADING);
        assetTotal.setForeground(Theme.ACCENT_GOLD);
        liabTotal = new JLabel("Total: —");
        liabTotal.setFont(Theme.FONT_HEADING);
        liabTotal.setForeground(Theme.ACCENT_GOLD);

        JPanel leftCard = Theme.card();
        leftCard.setLayout(new BorderLayout(0, 8));
        leftCard.add(Theme.heading("📦  Assets"), BorderLayout.NORTH);
        leftCard.add(Theme.styledScroll(assetTable), BorderLayout.CENTER);
        leftCard.add(assetTotal, BorderLayout.SOUTH);

        JPanel rightCard = Theme.card();
        rightCard.setLayout(new BorderLayout(0, 8));
        rightCard.add(Theme.heading("📄  Liabilities & Equity"), BorderLayout.NORTH);
        rightCard.add(Theme.styledScroll(liabTable), BorderLayout.CENTER);
        rightCard.add(liabTotal, BorderLayout.SOUTH);

        panel.add(leftCard);
        panel.add(rightCard);
        return panel;
    }

    private JPanel buildBSSide(String side) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 8));
        card.setName(side);
        String title = "ASSET".equals(side) ? "📦  Assets" : "📄  Liabilities & Equity";
        card.add(Theme.heading(title), BorderLayout.NORTH);
        String[] cols = { "GL Code", "Account", "Balance" };
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(m);
        Theme.styleTable(t);
        t.getColumnModel().getColumn(2).setCellRenderer(amountRenderer());
        card.add(Theme.styledScroll(t), BorderLayout.CENTER);

        JLabel total = new JLabel("Total: —");
        total.setFont(Theme.FONT_HEADING);
        total.setForeground(Theme.ACCENT_GOLD);
        card.add(total, BorderLayout.SOUTH);

        card.putClientProperty("model", m);
        card.putClientProperty("total", total);
        return card;
    }

    private JPanel buildPnLTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        incomeModel = new DefaultTableModel(new String[] { "Code", "Account", "Balance" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        expenseModel = new DefaultTableModel(new String[] { "Code", "Account", "Balance" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable incTable = new JTable(incomeModel);
        Theme.styleTable(incTable);
        incTable.getColumnModel().getColumn(2).setCellRenderer(amountRenderer());
        JTable expTable = new JTable(expenseModel);
        Theme.styleTable(expTable);
        expTable.getColumnModel().getColumn(2).setCellRenderer(amountRenderer());

        incomeTotal = new JLabel("Total: —");
        incomeTotal.setFont(Theme.FONT_HEADING);
        incomeTotal.setForeground(Theme.ACCENT_GREEN);
        expenseTotal = new JLabel("Total: —");
        expenseTotal.setFont(Theme.FONT_HEADING);
        expenseTotal.setForeground(Theme.ACCENT_RED);
        netProfitLabel = new JLabel("");
        netProfitLabel.setFont(Theme.FONT_HEADING);
        netProfitLabel.setForeground(Theme.ACCENT_GOLD);

        JPanel incCard = Theme.card();
        incCard.setLayout(new BorderLayout(0, 8));
        incCard.add(Theme.heading("💰  Income"), BorderLayout.NORTH);
        incCard.add(Theme.styledScroll(incTable), BorderLayout.CENTER);
        incCard.add(incomeTotal, BorderLayout.SOUTH);

        JPanel expCard = Theme.card();
        expCard.setLayout(new BorderLayout(0, 8));
        expCard.add(Theme.heading("💸  Expenses"), BorderLayout.NORTH);
        expCard.add(Theme.styledScroll(expTable), BorderLayout.CENTER);
        JPanel expBottom = new JPanel(new BorderLayout());
        expBottom.setOpaque(false);
        expBottom.add(expenseTotal, BorderLayout.WEST);
        expBottom.add(netProfitLabel, BorderLayout.EAST);
        expCard.add(expBottom, BorderLayout.SOUTH);

        panel.add(incCard);
        panel.add(expCard);
        return panel;
    }

    private JPanel buildPLSide(String side) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 8));
        card.setName(side);
        String title = "INCOME".equals(side) ? "💰  Income" : "💸  Expenses";
        card.add(Theme.heading(title), BorderLayout.NORTH);
        String[] cols = { "GL Code", "Account", "Balance" };
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(m);
        Theme.styleTable(t);
        t.getColumnModel().getColumn(2).setCellRenderer(amountRenderer());
        card.add(Theme.styledScroll(t), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        JLabel total = new JLabel("Total: —");
        total.setFont(Theme.FONT_HEADING);
        total.setForeground("INCOME".equals(side) ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
        JLabel netProfit = new JLabel("");
        netProfit.setFont(Theme.FONT_HEADING);
        netProfit.setForeground(Theme.ACCENT_GOLD);
        bottomPanel.add(total, BorderLayout.WEST);
        if ("EXPENSE".equals(side))
            bottomPanel.add(netProfit, BorderLayout.EAST);
        card.add(bottomPanel, BorderLayout.SOUTH);

        card.putClientProperty("model", m);
        card.putClientProperty("total", total);
        card.putClientProperty("net", netProfit);
        return card;
    }

    private JPanel buildCoaTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        String[] cols = { "Code", "Name", "Category", "Normal Bal", "Balance", "Internal", "Active" };
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(m);
        Theme.styleTable(t);
        t.getColumnModel().getColumn(4).setCellRenderer(amountRenderer());
        panel.setName("COA");
        panel.putClientProperty("model", m);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        top.setOpaque(false);
        JButton addGLBtn = Theme.successButton("+ Add GL Account");
        top.add(addGLBtn);
        addGLBtn.addActionListener(e -> showAddGLDialog());

        panel.add(top, BorderLayout.NORTH);
        panel.add(Theme.styledScroll(t), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildJournalTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        String[] cols = { "Journal ID", "GL Code", "GL Name", "Dr/Cr", "Amount", "Reference", "Period", "Date", "By" };
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(m);
        Theme.styleTable(t);
        t.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean sel, boolean foc, int r,
                    int c) {
                super.getTableCellRendererComponent(tbl, v, sel, foc, r, c);
                String s = v != null ? v.toString() : "";
                setForeground("DEBIT".equals(s) ? Theme.ACCENT_RED : Theme.ACCENT_GREEN);
                return this;
            }
        });
        t.getColumnModel().getColumn(4).setCellRenderer(amountRenderer());
        panel.setName("JNL");
        panel.putClientProperty("model", m);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        top.setOpaque(false);
        JButton postBtn = Theme.warningButton("📝 Post Manual Journal");
        top.add(postBtn);
        postBtn.addActionListener(e -> showManualJournalDialog());

        panel.add(top, BorderLayout.NORTH);
        panel.add(Theme.styledScroll(t), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDrillDownTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        JTextField glCodeField = Theme.styledField();
        glCodeField.setPreferredSize(new Dimension(120, 36));
        JButton loadBtn = Theme.primaryButton("Load Entries");
        JLabel balLabel = new JLabel("");
        balLabel.setForeground(Theme.ACCENT_GOLD);
        balLabel.setFont(Theme.FONT_HEADING);
        searchBar.add(Theme.label("GL Code:"));
        searchBar.add(glCodeField);
        searchBar.add(loadBtn);
        searchBar.add(balLabel);

        String[] cols = { "Journal ID", "Dr/Cr", "Amount", "Description", "Reference", "Date", "Running Balance" };
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable t = new JTable(m);
        Theme.styleTable(t);
        t.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean sel, boolean foc, int r,
                    int c) {
                super.getTableCellRendererComponent(tbl, v, sel, foc, r, c);
                String s = v != null ? v.toString() : "";
                setForeground("DEBIT".equals(s) ? Theme.ACCENT_RED : Theme.ACCENT_GREEN);
                return this;
            }
        });

        loadBtn.addActionListener(e -> {
            String code = glCodeField.getText().trim();
            if (code.isEmpty()) {
                Theme.showError(panel, "Enter a GL code");
                return;
            }
            try {
                List<LedgerEntry> entries = GeneralLedgerService.getInstance().getLedgerEntries(code, 200);
                m.setRowCount(0);
                for (LedgerEntry en : entries) {
                    m.addRow(new Object[] {
                            en.getJournalId(), en.getEntryType(),
                            BankUtil.formatCurrency(en.getAmount()),
                            en.getDescription(), en.getReferenceId(),
                            en.getCreatedAt() != null ? BankUtil.formatDateTime(en.getCreatedAt()) : "—",
                            BankUtil.formatCurrency(en.getRunningBalance())
                    });
                }
                // Show GL balance
                List<GeneralLedger> coa = GeneralLedgerService.getInstance().getChartOfAccounts();
                coa.stream().filter(g -> code.equals(g.getGlCode())).findFirst().ifPresent(g -> balLabel
                        .setText("Balance: " + BankUtil.formatCurrency(g.getBalance()) + "  (" + g.getGlName() + ")"));
            } catch (Exception ex) {
                Theme.showError(panel, ex.getMessage());
            }
        });

        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(Theme.styledScroll(t), BorderLayout.CENTER);
        return panel;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void refreshAll() {
        new SwingWorker<Void, Void>() {
            Map<String, List<GeneralLedger>> bs, pl;
            List<GeneralLedger> coa;
            List<LedgerEntry> jnl;
            double netProfit;

            @Override
            protected Void doInBackground() throws Exception {
                bs = GeneralLedgerService.getInstance().getBalanceSheet();
                pl = GeneralLedgerService.getInstance().getProfitAndLoss();
                coa = GeneralLedgerService.getInstance().getChartOfAccounts();
                jnl = GeneralLedgerService.getInstance().getAllJournalEntries(200);
                netProfit = GeneralLedgerService.getInstance().getNetProfit();
                return null;
            }

            @Override
            protected void done() {
                try {
                    populateBS(bs);
                    populatePL(pl, netProfit);
                    populateCOA(coa);
                    populateJournal(jnl);
                } catch (Exception ex) {
                    Theme.showError(GeneralLedgerPanel.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateBS(Map<String, List<GeneralLedger>> bs) {
        assetModel.setRowCount(0);
        liabModel.setRowCount(0);
        double at = 0, lt = 0;
        for (GeneralLedger gl : bs.getOrDefault("ASSET", List.of())) {
            assetModel
                    .addRow(new Object[] { gl.getGlCode(), gl.getGlName(), BankUtil.formatCurrency(gl.getBalance()) });
            at += gl.getBalance();
        }
        for (String cat : List.of("LIABILITY", "EQUITY")) {
            for (GeneralLedger gl : bs.getOrDefault(cat, List.of())) {
                liabModel.addRow(
                        new Object[] { gl.getGlCode(), gl.getGlName(), BankUtil.formatCurrency(gl.getBalance()) });
                lt += gl.getBalance();
            }
        }
        assetTotal.setText("Total Assets: " + BankUtil.formatCurrency(at));
        liabTotal.setText("Total Liabilities + Equity: " + BankUtil.formatCurrency(lt));
    }

    private void populatePL(Map<String, List<GeneralLedger>> pl, double netProfit) {
        incomeModel.setRowCount(0);
        expenseModel.setRowCount(0);
        double it = 0, et = 0;
        for (GeneralLedger gl : pl.getOrDefault("INCOME", List.of())) {
            incomeModel
                    .addRow(new Object[] { gl.getGlCode(), gl.getGlName(), BankUtil.formatCurrency(gl.getBalance()) });
            it += gl.getBalance();
        }
        for (GeneralLedger gl : pl.getOrDefault("EXPENSE", List.of())) {
            expenseModel
                    .addRow(new Object[] { gl.getGlCode(), gl.getGlName(), BankUtil.formatCurrency(gl.getBalance()) });
            et += gl.getBalance();
        }
        incomeTotal.setText("Total Income: " + BankUtil.formatCurrency(it));
        expenseTotal.setText("Total Expenses: " + BankUtil.formatCurrency(et));
        netProfitLabel.setText("Net Profit: " + BankUtil.formatCurrency(netProfit));
        netProfitLabel.setForeground(netProfit >= 0 ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
    }

    private void populateCOA(List<GeneralLedger> coa) {
        JPanel coaTab = (JPanel) tabs.getComponentAt(2);
        DefaultTableModel m = (DefaultTableModel) coaTab.getClientProperty("model");
        if (m == null)
            return;
        m.setRowCount(0);
        for (GeneralLedger gl : coa) {
            m.addRow(new Object[] {
                    gl.getGlCode(), gl.getGlName(), gl.getCategory(), gl.getNormalBalance(),
                    BankUtil.formatCurrency(gl.getBalance()),
                    gl.isInternal() ? "Yes" : "No",
                    gl.isActive() ? "Active" : "Inactive"
            });
        }
    }

    private void populateJournal(List<LedgerEntry> jnl) {
        JPanel jnlTab = (JPanel) tabs.getComponentAt(3);
        DefaultTableModel m = (DefaultTableModel) jnlTab.getClientProperty("model");
        if (m == null)
            return;
        m.setRowCount(0);
        for (LedgerEntry e : jnl) {
            m.addRow(new Object[] {
                    e.getJournalId(), e.getGlCode(),
                    e.getGlName() != null ? e.getGlName() : "—",
                    e.getEntryType(), BankUtil.formatCurrency(e.getAmount()),
                    e.getReferenceId() != null ? e.getReferenceType() + "/" + e.getReferenceId() : "—",
                    e.getPeriod(),
                    e.getCreatedAt() != null ? BankUtil.formatDateTime(e.getCreatedAt()) : "—",
                    e.getPerformedByName() != null ? e.getPerformedByName() : "System"
            });
        }
    }

    // ── Add GL Account dialog ─────────────────────────────────────────────────

    private void showAddGLDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add GL Account", true);
        dlg.setSize(420, 360);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 4, 8, 4);

        JTextField codeField = Theme.styledField();
        JTextField nameField = Theme.styledField();
        JComboBox<String> catBox = Theme.styledCombo();
        for (String c : new String[] { "ASSET", "LIABILITY", "EQUITY", "INCOME", "EXPENSE" })
            catBox.addItem(c);
        JComboBox<String> normBox = Theme.styledCombo();
        for (String n : new String[] { "DEBIT", "CREDIT" })
            normBox.addItem(n);
        JTextField descField = Theme.styledField();
        JCheckBox internalCb = new JCheckBox("Internal Account");
        internalCb.setBackground(Theme.BG_CARD);
        internalCb.setForeground(Theme.TEXT_PRIMARY);

        catBox.addActionListener(e -> {
            String cat = (String) catBox.getSelectedItem();
            normBox.setSelectedItem("ASSET".equals(cat) || "EXPENSE".equals(cat) ? "DEBIT" : "CREDIT");
        });

        int[] r = { 0 };
        Object[][] rows = { { "GL Code *", codeField }, { "Name *", nameField },
                { "Category *", catBox }, { "Normal Balance", normBox }, { "Description", descField },
                { "", internalCb } };
        for (Object[] row : rows) {
            gc.gridx = 0;
            gc.gridy = r[0];
            gc.weightx = 0.4;
            gc.gridwidth = 1;
            p.add(Theme.label((String) row[0]), gc);
            gc.gridx = 1;
            gc.weightx = 0.6;
            p.add((Component) row[1], gc);
            r[0]++;
        }

        JButton saveBtn = Theme.successButton("Add GL Account");
        gc.gridx = 0;
        gc.gridy = r[0];
        gc.gridwidth = 2;
        gc.insets = new Insets(16, 4, 4, 4);
        p.add(saveBtn, gc);

        saveBtn.addActionListener(e -> {
            if (codeField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()) {
                Theme.showError(dlg, "Code and Name are required");
                return;
            }
            try {
                GeneralLedger gl = new GeneralLedger();
                gl.setGlCode(codeField.getText().trim());
                gl.setGlName(nameField.getText().trim());
                gl.setCategory((String) catBox.getSelectedItem());
                gl.setNormalBalance((String) normBox.getSelectedItem());
                gl.setDescription(descField.getText().trim());
                gl.setInternal(internalCb.isSelected());
                gl.setActive(true);
                GeneralLedgerService.getInstance().upsertGLAccount(gl);
                dlg.dispose();
                refreshAll();
                Theme.showSuccess(this, "GL account " + gl.getGlCode() + " added.");
            } catch (Exception ex) {
                Theme.showError(dlg, ex.getMessage());
            }
        });

        dlg.add(p);
        dlg.setVisible(true);
    }

    // ── Manual Journal Entry dialog ───────────────────────────────────────────

    private void showManualJournalDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Post Manual Journal", true);
        dlg.setSize(560, 420);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 24, 24, 24));

        p.add(Theme.heading("Post Manual Double-Entry Journal"), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(Theme.BG_CARD);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 4, 6, 4);

        JTextField drCodeField = Theme.styledField();
        drCodeField.setToolTipText("e.g. 1001");
        JTextField drAmtField = Theme.styledField();
        JTextField drDescField = Theme.styledField();
        JTextField crCodeField = Theme.styledField();
        JTextField crAmtField = Theme.styledField();
        JTextField crDescField = Theme.styledField();
        JTextField refField = Theme.styledField();
        refField.setText("MANUAL");

        Object[][] rows = {
                { "DEBIT GL Code *", drCodeField }, { "DEBIT Amount *", drAmtField },
                { "DEBIT Description", drDescField },
                { "CREDIT GL Code *", crCodeField }, { "CREDIT Amount *", crAmtField },
                { "CREDIT Description", crDescField },
                { "Reference", refField },
        };
        int[] row = { 0 };
        for (Object[] r : rows) {
            gc.gridx = 0;
            gc.gridy = row[0];
            gc.weightx = 0.35;
            gc.gridwidth = 1;
            fields.add(Theme.label((String) r[0]), gc);
            gc.gridx = 1;
            gc.weightx = 0.65;
            fields.add((Component) r[1], gc);
            row[0]++;
        }

        JLabel balanceCheck = Theme.label("Enter amounts to check balance");
        gc.gridx = 0;
        gc.gridy = row[0];
        gc.gridwidth = 2;
        fields.add(balanceCheck, gc);

        java.awt.event.KeyAdapter checker = new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                try {
                    double dr = Double.parseDouble(drAmtField.getText().trim());
                    double cr = Double.parseDouble(crAmtField.getText().trim());
                    if (Math.abs(dr - cr) < 0.005) {
                        balanceCheck.setText("✅ Balanced — " + BankUtil.formatCurrency(dr));
                        balanceCheck.setForeground(Theme.ACCENT_GREEN);
                    } else {
                        balanceCheck.setText("⚠ Not balanced  Dr:" + BankUtil.formatCurrency(dr) + "  Cr:"
                                + BankUtil.formatCurrency(cr));
                        balanceCheck.setForeground(Theme.ACCENT_RED);
                    }
                } catch (Exception ignored) {
                }
            }
        };
        drAmtField.addKeyListener(checker);
        crAmtField.addKeyListener(checker);

        JButton postBtn = Theme.warningButton("📝 Post Journal");
        p.add(fields, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setOpaque(false);
        btnRow.add(postBtn);
        p.add(btnRow, BorderLayout.SOUTH);

        postBtn.addActionListener(e -> {
            try {
                double drAmt = Double.parseDouble(drAmtField.getText().trim());
                double crAmt = Double.parseDouble(crAmtField.getText().trim());
                java.util.List<String[]> drList = new java.util.ArrayList<>();
                drList.add(new String[] { drCodeField.getText().trim(), String.valueOf(drAmt),
                        drDescField.getText().trim() });
                java.util.List<String[]> crList = new java.util.ArrayList<>();
                crList.add(new String[] { crCodeField.getText().trim(), String.valueOf(crAmt),
                        crDescField.getText().trim() });
                String jnlId = GeneralLedgerService.getInstance().postJournal(drList, crList,
                        "MANUAL", refField.getText().trim());
                dlg.dispose();
                refreshAll();
                Theme.showSuccess(this, "Journal posted: " + jnlId);
            } catch (Exception ex) {
                Theme.showError(dlg, ex.getMessage());
            }
        });

        dlg.add(p);
        dlg.setVisible(true);
    }

    // ── Correct double-entry journal wrappers ────────────────────────────────

    /**
     * DEPOSIT: Cash comes in (Asset ↑), Customer Deposit liability increases
     * (Liability ↑)
     * DR 1001 Cash in Hand
     * CR 2001 Customer Deposits
     */
    public String journalDeposit(int accountId, double amount, String txnId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        String glCode = getAccountGLCode(accountId, "2001");
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "1001", String.valueOf(amount), "Cash received - " + acctNo });
        cr.add(new String[] { glCode, String.valueOf(amount), "Deposit credit  - " + acctNo });
        return postJournal(dr, cr, "TRANSACTION", txnId);
    }

    /**
     * WITHDRAWAL: Cash goes out (Asset ↓), Customer Deposit liability decreases
     * (Liability ↓)
     * DR 2001 Customer Deposits
     * CR 1001 Cash in Hand
     */
    public String journalWithdrawal(int accountId, double amount, String txnId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        String glCode = getAccountGLCode(accountId, "2001");
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { glCode, String.valueOf(amount), "Withdrawal debit - " + acctNo });
        cr.add(new String[] { "1001", String.valueOf(amount), "Cash paid out   - " + acctNo });
        return postJournal(dr, cr, "TRANSACTION", txnId);
    }

    /**
     * TRANSFER (internal): Move liability from one deposit account to another.
     * DR 2001 Customer Deposits (source party's deposit balance reduces)
     * CR 2001 Customer Deposits (dest party's deposit balance increases)
     * Net effect on total liabilities = zero. Cash stays in vault.
     */
    public String journalTransfer(int fromId, int toId, double amount, String txnId) throws Exception {
        String from = getAccountNumber(fromId);
        String to = getAccountNumber(toId);
        String fromGL = getAccountGLCode(fromId, "2001");
        String toGL = getAccountGLCode(toId, "2001");
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { fromGL, String.valueOf(amount), "Transfer out - " + from + " -> " + to });
        cr.add(new String[] { toGL, String.valueOf(amount), "Transfer in  - " + from + " -> " + to });
        return postJournal(dr, cr, "TRANSACTION", txnId);
    }

    /**
     * INTEREST CREDIT: Bank pays interest to depositor.
     * DR 5001 Interest Expense (expense increases — bank pays out)
     * CR 2001 Customer Deposits (liability increases — depositor's balance goes up)
     * This correctly hits the P&L expense side AND increases the liability.
     */
    public String journalInterestCredit(int accountId, double interest, String txnId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        String glCode = getAccountGLCode(accountId, "2001");
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "5001", String.valueOf(interest), "Interest expense   - " + acctNo });
        cr.add(new String[] { glCode, String.valueOf(interest), "Interest credited  - " + acctNo });
        return postJournal(dr, cr, "INTEREST", txnId);
    }

    /**
     * LOAN DISBURSEMENT: Bank lends money out.
     * DR 1101 Loans Receivable (asset increases — bank is owed money)
     * CR 1001 Cash in Hand (asset decreases — cash leaves vault)
     */
    public String journalLoanDisbursement(int accountId, double amount, String loanId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "1101", String.valueOf(amount), "Loan receivable    - " + loanId });
        cr.add(new String[] { "1001", String.valueOf(amount), "Cash disbursed     - " + acctNo });
        return postJournal(dr, cr, "LOAN", loanId);
    }

    /**
     * LOAN REPAYMENT: Borrower pays back principal + interest.
     * DR 1001 Cash in Hand (asset up — cash received)
     * CR 1101 Loans Receivable (asset down — loan balance reduces)
     * CR 4001 Interest Income (income up — bank earns interest)
     */
    public String journalLoanRepayment(double principal, double interest, String loanId) throws Exception {
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "1001", String.valueOf(principal + interest), "Repayment received - " + loanId });
        cr.add(new String[] { "1101", String.valueOf(principal), "Principal recovered- " + loanId });
        cr.add(new String[] { "4001", String.valueOf(interest), "Interest income    - " + loanId });
        return postJournal(dr, cr, "LOAN", loanId);
    }

    /**
     * FD CREATION: Customer locks money in FD.
     * DR 2001 Customer Deposits (reduce current/savings liability)
     * CR 2004 FD Deposits (increase FD liability)
     */
    public String journalFDCreate(int fromAccountId, double amount, String fdNo) throws Exception {
        String acctNo = getAccountNumber(fromAccountId);
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "2001", String.valueOf(amount), "FD debit from  - " + acctNo });
        cr.add(new String[] { "2004", String.valueOf(amount), "FD created     - " + fdNo });
        return postJournal(dr, cr, "FD", fdNo);
    }

    /**
     * FD MATURITY: FD matures and pays back principal + interest.
     * DR 2004 FD Deposits (FD liability reduces)
     * DR 5001 Interest Expense (bank pays out FD interest)
     * CR 2001 Customer Deposits (customer's account credited)
     */
    public String journalFDMaturity(int accountId, double principal, double interest, String fdNo) throws Exception {
        String acctNo = getAccountNumber(accountId);
        double total = principal + interest;
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "2004", String.valueOf(principal), "FD principal returned - " + fdNo });
        dr.add(new String[] { "5001", String.valueOf(interest), "FD interest expense   - " + fdNo });
        cr.add(new String[] { "2001", String.valueOf(total), "FD maturity credit    - " + acctNo });
        return postJournal(dr, cr, "FD", fdNo);
    }

    /**
     * FEE / CHARGE: Bank collects a service fee.
     * DR 2001 Customer Deposits (reduce customer balance)
     * CR 4002 Fee & Commission (bank earns fee income)
     */
    public String journalFeeCharge(int accountId, double fee, String ref) throws Exception {
        String acctNo = getAccountNumber(accountId);
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "2001", String.valueOf(fee), "Fee charged     - " + acctNo });
        cr.add(new String[] { "4002", String.valueOf(fee), "Fee income      - " + ref });
        return postJournal(dr, cr, "FEE", ref);
    }

    /**
     * SWIFT / FOREX: International transfer out.
     * DR 2001 Customer Deposits (customer balance reduces)
     * CR 1002 Nostro Account (bank's foreign correspondent account reduces)
     * CR 4003 Forex Income (bank earns spread/charges)
     */
    public String journalSwift(int accountId, double amountInr, double charges, String txnId) throws Exception {
        String acctNo = getAccountNumber(accountId);
        List<String[]> dr = new ArrayList<>();
        List<String[]> cr = new ArrayList<>();
        dr.add(new String[] { "2001", String.valueOf(amountInr + charges), "SWIFT debit     - " + acctNo });
        cr.add(new String[] { "1002", String.valueOf(amountInr), "Nostro transfer - " + txnId });
        cr.add(new String[] { "4003", String.valueOf(charges), "SWIFT charges   - " + txnId });
        return postJournal(dr, cr, "SWIFT", txnId);
    }

    // ── Renderer ──────────────────────────────────────────────────────────────

    private DefaultTableCellRenderer amountRenderer() {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setForeground(Theme.ACCENT_GOLD);
                return this;
            }
        };
    }
}