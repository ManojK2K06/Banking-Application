package com.bankpro.ui.panels;

import com.bankpro.service.*;
import com.bankpro.model.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel {

    private JLabel totalDepositsLbl, totalCustomersLbl, totalAccountsLbl, loanOutstandingLbl;
    private JLabel txnVolumeLbl, pendingLoansLbl;
    private JPanel recentTxnPanel;

    public DashboardPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        // Defer data load until after the frame is fully rendered
        javax.swing.SwingUtilities.invokeLater(this::refreshData);
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = Theme.titleLabel("📊  Dashboard");
        JButton refreshBtn = Theme.ghostButton("⟳  Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        // Stats grid
        JPanel stats = new JPanel(new GridLayout(2, 3, 16, 16));
        stats.setOpaque(false);

        totalDepositsLbl = new JLabel("...");
        totalCustomersLbl = new JLabel("...");
        totalAccountsLbl = new JLabel("...");
        loanOutstandingLbl = new JLabel("...");
        txnVolumeLbl = new JLabel("...");
        pendingLoansLbl = new JLabel("...");

        stats.add(makeStatCard("Total Deposits (AUM)", totalDepositsLbl, Theme.ACCENT_GREEN));
        stats.add(makeStatCard("Total Parties", totalCustomersLbl, Theme.ACCENT_BLUE));
        stats.add(makeStatCard("Active Accounts", totalAccountsLbl, Theme.ACCENT_GOLD));
        stats.add(makeStatCard("Loan Outstanding", loanOutstandingLbl, Theme.ACCENT_RED));
        stats.add(makeStatCard("Today's Txn Volume", txnVolumeLbl, Theme.ACCENT_ORANGE));
        stats.add(makeStatCard("Pending Loans", pendingLoansLbl, new Color(167, 139, 250)));

        // Recent transactions
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomRow.setOpaque(false);

        recentTxnPanel = new JPanel(new BorderLayout());
        recentTxnPanel.setBackground(Theme.BG_CARD);
        recentTxnPanel.setBorder(new CompoundBorder(
            new LineBorder(Theme.BORDER_COLOR),
            new EmptyBorder(16, 16, 16, 16)));

        JLabel txnTitle = Theme.heading("Recent Transactions");
        recentTxnPanel.add(txnTitle, BorderLayout.NORTH);
        recentTxnPanel.add(buildRecentTxnTable(), BorderLayout.CENTER);

        // Quick actions
        JPanel quickActions = new JPanel(new BorderLayout());
        quickActions.setBackground(Theme.BG_CARD);
        quickActions.setBorder(new CompoundBorder(
            new LineBorder(Theme.BORDER_COLOR),
            new EmptyBorder(16, 16, 16, 16)));

        JLabel qaTitle = Theme.heading("⚡ Quick Info");
        quickActions.add(qaTitle, BorderLayout.NORTH);
        quickActions.add(buildQuickInfo(), BorderLayout.CENTER);

        bottomRow.add(recentTxnPanel);
        bottomRow.add(quickActions);

        main.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(stats, BorderLayout.NORTH);
        center.add(bottomRow, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        add(main);
    }

    private JPanel makeStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(new CompoundBorder(
            new MatteBorder(0, 3, 0, 0, color),
            new EmptyBorder(16, 16, 16, 16)));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(Theme.FONT_SMALL);
        lTitle.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(color);

        card.add(lTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane buildRecentTxnTable() {
        String[] cols = {"Transaction ID", "Type", "Amount", "Date"};
        JTable table = new JTable(new Object[0][4], cols) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        Theme.styleTable(table);

        // Load data
        try {
            List<Transaction> txns = TransactionService.getInstance().getAllTransactions(10);
            Object[][] data = new Object[txns.size()][4];
            for (int i = 0; i < txns.size(); i++) {
                Transaction t = txns.get(i);
                data[i][0] = t.getTransactionId().substring(0, 16) + "...";
                data[i][1] = t.getTypeLabel();
                data[i][2] = BankUtil.formatCurrency(t.getAmountInr() > 0 ? t.getAmountInr() : t.getAmount());
                data[i][3] = t.getCreatedAt() != null ?
                    t.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm")) : "";
            }
            table.setModel(new javax.swing.table.DefaultTableModel(data, cols) {
                public boolean isCellEditable(int r, int c) { return false; }
            });
            Theme.styleTable(table);
        } catch (Exception ignored) {}

        return Theme.styledScroll(table);
    }

    private JPanel buildQuickInfo() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 8));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 0, 0, 0));

        String[][] info = {
            {"IFSC Code", "BPRO0001"},
            {"Branch", "Main Branch"},
            {"SWIFT Code", "BPROINBB"},
            {"Bank Email", "support@bankpro.com"},
            {"RBI Reg No.", "RBI/2024/001234"},
            {"Working Hours", "Mon-Sat  9:00-17:00"},
        };

        for (String[] row : info) {
            JPanel item = new JPanel(new BorderLayout());
            item.setOpaque(false);
            item.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_COLOR));

            JLabel key = new JLabel(row[0]);
            key.setFont(Theme.FONT_SMALL);
            key.setForeground(Theme.TEXT_MUTED);

            JLabel val = new JLabel(row[1]);
            val.setFont(Theme.FONT_BODY);
            val.setForeground(Theme.TEXT_PRIMARY);

            item.add(key, BorderLayout.WEST);
            item.add(val, BorderLayout.EAST);
            p.add(item);
        }
        return p;
    }

    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            double deposits, loans, txnVol;
            int customers, accounts;
            int pendingLoans;

            @Override
            protected Void doInBackground() throws Exception {
                deposits = AccountService.getInstance().getTotalDeposits();
                customers = PartyService.getInstance().getTotalPartyCount();
                accounts = AccountService.getInstance().getTotalAccountCount();
                loans = LoanService.getInstance().getTotalLoanOutstanding();
                txnVol = TransactionService.getInstance().getTodayTransactionVolume();
                pendingLoans = LoanService.getInstance().getPendingLoans().size();
                return null;
            }

            @Override
            protected void done() {
                totalDepositsLbl.setText(BankUtil.formatCurrency(deposits));
                totalCustomersLbl.setText(String.format("%,d", customers));
                totalAccountsLbl.setText(String.format("%,d", accounts));
                loanOutstandingLbl.setText(BankUtil.formatCurrency(loans));
                txnVolumeLbl.setText(BankUtil.formatCurrency(txnVol));
                pendingLoansLbl.setText(String.valueOf(pendingLoans));
                recentTxnPanel.removeAll();
                recentTxnPanel.add(Theme.heading("Recent Transactions"), BorderLayout.NORTH);
                recentTxnPanel.add(buildRecentTxnTable(), BorderLayout.CENTER);
                recentTxnPanel.revalidate();
                recentTxnPanel.repaint();
            }
        };
        worker.execute();
    }
}
