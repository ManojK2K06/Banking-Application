package com.bankpro.ui.panels;

import com.bankpro.model.*;
import com.bankpro.service.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class WithdrawalPanel extends JPanel {
    private JTextField accountField, amountField, descField;
    private JLabel balanceLabel, customerLabel;
    private int currentAccountId = -1;

    public WithdrawalPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(520, 480));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 8, 8, 8);

        int row = 0;
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        card.add(Theme.titleLabel("💸  Cash Withdrawal"), gc); row++;
        gc.gridy = row;
        card.add(Theme.label("Debit cash from customer account"), gc); row++;

        gc.gridwidth = 1;
        accountField = Theme.styledField();
        amountField  = Theme.styledField();
        descField    = Theme.styledField();
        Object[][] fields = {
            {"Account Number *", accountField},
            {"Amount (₹) *",     amountField},
            {"Description",      descField},
        };
        for (Object[] f : fields) {
            gc.gridx = 0; gc.gridy = row; gc.weightx = 0.35;
            card.add(Theme.label((String) f[0]), gc);
            gc.gridx = 1; gc.weightx = 0.65;
            card.add((JTextField) f[1], gc);
            row++;
        }

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        customerLabel = Theme.label("Customer: —");
        card.add(customerLabel, gc); row++;
        balanceLabel = Theme.label("Available Balance: —");
        gc.gridy = row;
        card.add(balanceLabel, gc); row++;

        JButton lookupBtn  = Theme.ghostButton("🔍 Lookup");
        JButton withdrawBtn = Theme.dangerButton("💸 Process Withdrawal");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btns.setOpaque(false);
        btns.add(lookupBtn); btns.add(withdrawBtn);
        gc.gridy = row; gc.insets = new Insets(16, 8, 8, 8);
        card.add(btns, gc);

        add(card);
        lookupBtn.addActionListener(e -> lookupAccount());
        accountField.addActionListener(e -> lookupAccount());
        withdrawBtn.addActionListener(e -> processWithdrawal());
    }

    private void lookupAccount() {
        String no = accountField.getText().trim();
        if (no.isEmpty()) { Theme.showError(this, "Enter account number"); return; }
        try {
            Account a = AccountService.getInstance().getByAccountNumber(no);
            if (a == null) { customerLabel.setText("❌ Not found"); balanceLabel.setText(""); return; }
            if (!"ACTIVE".equals(a.getStatus())) {
                customerLabel.setText("⚠️ Account is " + a.getStatus()); return;
            }
            currentAccountId = a.getId();
            customerLabel.setText("✓ " + a.getPartyName() + "  |  " + a.getAccountTypeLabel());
            customerLabel.setForeground(Theme.ACCENT_GREEN);
            double avail = Math.max(0, a.getBalance() - a.getMinimumBalance() + a.getOverdraftLimit());
            balanceLabel.setText("Available: " + BankUtil.formatCurrency(avail)
                + "  (Balance: " + BankUtil.formatCurrency(a.getBalance()) + ")");
            balanceLabel.setForeground(Theme.ACCENT_GOLD);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void processWithdrawal() {
        if (currentAccountId < 0) { Theme.showError(this, "Lookup account first"); return; }
        String amtStr = amountField.getText().trim();
        if (!BankUtil.isValidAmount(amtStr)) { Theme.showError(this, "Invalid amount"); return; }
        double amount = Double.parseDouble(amtStr);
        String desc = descField.getText().trim().isEmpty() ? "Cash Withdrawal" : descField.getText().trim();

        if (!Theme.showConfirm(this, "Withdraw " + BankUtil.formatCurrency(amount) + "?")) return;

        new SwingWorker<Transaction, Void>() {
            @Override protected Transaction doInBackground() throws Exception {
                return TransactionService.getInstance().withdraw(currentAccountId, amount, desc);
            }
            @Override protected void done() {
                try {
                    Transaction t = get();
                    Theme.showSuccess(WithdrawalPanel.this,
                        "✅ Withdrawal Successful!\nTxn ID: " + t.getTransactionId()
                        + "\nAmount: " + BankUtil.formatCurrency(amount));
                    clearForm();
                } catch (Exception ex) {
                    Theme.showError(WithdrawalPanel.this,
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }

    private void clearForm() {
        accountField.setText(""); amountField.setText(""); descField.setText("");
        customerLabel.setText("Customer: —"); customerLabel.setForeground(Theme.TEXT_SECONDARY);
        balanceLabel.setText("Available Balance: —"); balanceLabel.setForeground(Theme.TEXT_SECONDARY);
        currentAccountId = -1;
    }
}
