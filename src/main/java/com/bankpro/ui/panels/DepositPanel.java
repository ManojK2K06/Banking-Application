package com.bankpro.ui.panels;

import com.bankpro.model.*;
import com.bankpro.service.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

// ───────────────────────────────────────────────────────────────────────────
public class DepositPanel extends JPanel {
    private JTextField accountField, amountField, descField;
    private JLabel balanceLabel, customerLabel;

    public DepositPanel() {
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
        gc.gridx=0; gc.gridy=row; gc.gridwidth=2;
        JLabel title = Theme.titleLabel("💰  Cash Deposit");
        card.add(title, gc); row++;

        JLabel sub = Theme.label("Accept cash deposit and credit to customer account");
        gc.gridy=row;
        card.add(sub, gc); row++;

        gc.gridwidth=1; gc.gridy=row++;

        Object[][] fields = {
            {"Account Number *", accountField = Theme.styledField()},
            {"Amount (₹) *", amountField = Theme.styledField()},
            {"Description / Narration", descField = Theme.styledField()},
        };

        for (Object[] f : fields) {
            gc.gridx=0; gc.gridy=row; gc.weightx=0.35;
            card.add(Theme.label((String)f[0]), gc);
            gc.gridx=1; gc.weightx=0.65;
            card.add((JTextField)f[1], gc);
            row++;
        }

        // Info display
        gc.gridx=0; gc.gridy=row; gc.gridwidth=2;
        customerLabel = Theme.label("Customer: —");
        card.add(customerLabel, gc); row++;

        balanceLabel = Theme.label("Current Balance: —");
        gc.gridy=row;
        card.add(balanceLabel, gc); row++;

        JButton lookupBtn = Theme.ghostButton("🔍 Lookup Account");
        JButton depositBtn = Theme.successButton("💰 Process Deposit");

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        btnRow.add(lookupBtn); btnRow.add(depositBtn);
        gc.gridy=row; gc.insets=new Insets(16, 8, 8, 8);
        card.add(btnRow, gc);

        add(card);

        lookupBtn.addActionListener(e -> lookupAccount());
        depositBtn.addActionListener(e -> processDeposit());
        accountField.addActionListener(e -> lookupAccount());
    }

    private void lookupAccount() {
        String acctNo = accountField.getText().trim();
        if (acctNo.isEmpty()) { Theme.showError(this, "Enter account number"); return; }
        try {
            Account a = AccountService.getInstance().getByAccountNumber(acctNo);
            if (a == null) { customerLabel.setText("❌ Account not found"); balanceLabel.setText(""); return; }
            if (!"ACTIVE".equals(a.getStatus())) {
                customerLabel.setText("⚠️ Account is " + a.getStatus());
                balanceLabel.setText("");
                return;
            }
            customerLabel.setText("✓ Customer: " + a.getCustomerName() + "  |  " + a.getAccountTypeLabel());
            customerLabel.setForeground(Theme.ACCENT_GREEN);
            balanceLabel.setText("Current Balance: " + BankUtil.formatCurrency(a.getBalance()));
            balanceLabel.setForeground(Theme.ACCENT_GOLD);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void processDeposit() {
        String acctNo = accountField.getText().trim();
        String amtStr = amountField.getText().trim();
        if (acctNo.isEmpty() || amtStr.isEmpty()) { Theme.showError(this, "Fill all required fields"); return; }
        if (!BankUtil.isValidAmount(amtStr)) { Theme.showError(this, "Invalid amount"); return; }

        double amount = Double.parseDouble(amtStr);
        if (!Theme.showConfirm(this, String.format("Deposit %s to account %s?",
                BankUtil.formatCurrency(amount), acctNo))) return;

        new SwingWorker<Transaction, Void>() {
            Account account;
            @Override protected Transaction doInBackground() throws Exception {
                account = AccountService.getInstance().getByAccountNumber(acctNo);
                if (account == null) throw new Exception("Account not found");
                return TransactionService.getInstance().deposit(account.getId(), amount,
                    descField.getText().trim().isEmpty() ? "Cash Deposit" : descField.getText().trim());
            }
            @Override protected void done() {
                try {
                    Transaction t = get();
                    double newBal = account.getBalance() + amount;
                    Theme.showSuccess(DepositPanel.this,
                        "✅ Deposit Successful!\n\nTransaction ID: " + t.getTransactionId() +
                        "\nAmount: " + BankUtil.formatCurrency(amount) +
                        "\nNew Balance: " + BankUtil.formatCurrency(newBal));
                    clearForm();
                } catch (Exception ex) { Theme.showError(DepositPanel.this, ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()); }
            }
        }.execute();
    }

    private void clearForm() {
        accountField.setText(""); amountField.setText(""); descField.setText("");
        customerLabel.setText("Customer: —"); customerLabel.setForeground(Theme.TEXT_SECONDARY);
        balanceLabel.setText("Current Balance: —"); balanceLabel.setForeground(Theme.TEXT_SECONDARY);
    }
}

// ───────────────────────────────────────────────────────────────────────────
class WithdrawalPanelBase extends JPanel {
    protected JTextField accountField, amountField, descField;
    protected JLabel balanceLabel, customerLabel;
    protected int currentAccountId = -1;

    protected void lookupAccount() {
        String acctNo = accountField.getText().trim();
        if (acctNo.isEmpty()) { Theme.showError(this, "Enter account number"); return; }
        try {
            Account a = AccountService.getInstance().getByAccountNumber(acctNo);
            if (a == null) { customerLabel.setText("❌ Account not found"); return; }
            if (!"ACTIVE".equals(a.getStatus())) {
                customerLabel.setText("⚠️ Account is " + a.getStatus()); return;
            }
            currentAccountId = a.getId();
            customerLabel.setText("✓ " + a.getCustomerName() + "  |  " + a.getAccountTypeLabel());
            customerLabel.setForeground(Theme.ACCENT_GREEN);
            balanceLabel.setText("Available Balance: " + BankUtil.formatCurrency(
                Math.max(0, a.getBalance() - a.getMinimumBalance())));
            balanceLabel.setForeground(Theme.ACCENT_GOLD);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    protected void clearForm() {
        accountField.setText(""); amountField.setText(""); descField.setText("");
        customerLabel.setText("Customer: —"); customerLabel.setForeground(Theme.TEXT_SECONDARY);
        balanceLabel.setText("Available Balance: —"); balanceLabel.setForeground(Theme.TEXT_SECONDARY);
        currentAccountId = -1;
    }
}
