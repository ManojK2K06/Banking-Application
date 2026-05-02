package com.bankpro.ui.panels;

import com.bankpro.model.*;
import com.bankpro.service.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class TransferPanel extends JPanel {
    private JTextField fromField, toField, amountField, descField;
    private JLabel fromInfo, toInfo, feeLabel;
    private JComboBox<String> typeCombo;
    private int fromId = -1, toId = -1;

    public TransferPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(580, 580));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 8, 8, 8);

        int row = 0;
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2;
        card.add(Theme.titleLabel("🔄  Fund Transfer"), gc);
        gc.gridy = row++;
        card.add(Theme.label("Transfer funds between accounts — NEFT / RTGS / IMPS"), gc);

        // Transfer type
        typeCombo = Theme.styledCombo();
        for (String t : new String[]{"TRANSFER","NEFT","RTGS","IMPS"}) typeCombo.addItem(t);
        gc.gridwidth = 1; gc.gridx = 0; gc.gridy = row; gc.weightx = 0.35;
        card.add(Theme.label("Transfer Type *"), gc);
        gc.gridx = 1; gc.weightx = 0.65;
        card.add(typeCombo, gc); row++;

        fromField = Theme.styledField();
        toField   = Theme.styledField();
        amountField = Theme.styledField();
        descField   = Theme.styledField();

        Object[][] rows = {
            {"From Account *", fromField},
            {"To Account *",   toField},
            {"Amount (₹) *",   amountField},
            {"Remarks",        descField},
        };
        for (Object[] r : rows) {
            gc.gridx = 0; gc.gridy = row; gc.weightx = 0.35;
            card.add(Theme.label((String) r[0]), gc);
            gc.gridx = 1; gc.weightx = 0.65;
            card.add((JTextField) r[1], gc);
            row++;
        }

        fromInfo = Theme.label("—"); gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; card.add(fromInfo, gc); row++;
        toInfo   = Theme.label("—"); gc.gridy = row; card.add(toInfo, gc); row++;
        feeLabel = Theme.label("Charges: See below"); gc.gridy = row; card.add(feeLabel, gc); row++;

        JPanel feeNote = buildFeeNote();
        gc.gridy = row++; card.add(feeNote, gc);

        JButton lookupBtn   = Theme.ghostButton("🔍 Verify Accounts");
        JButton transferBtn = Theme.primaryButton("🔄 Transfer Now");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btns.setOpaque(false);
        btns.add(lookupBtn); btns.add(transferBtn);
        gc.gridy = row; gc.insets = new Insets(16, 8, 8, 8);
        card.add(btns, gc);

        add(card);

        lookupBtn.addActionListener(e -> verifyAccounts());
        transferBtn.addActionListener(e -> processTransfer());
        typeCombo.addActionListener(e -> updateFeeLabel());
        amountField.addActionListener(e -> updateFeeLabel());
    }

    private JPanel buildFeeNote() {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 2));
        p.setBackground(new Color(30, 41, 59));
        p.setBorder(new CompoundBorder(
            new LineBorder(Theme.BORDER_COLOR),
            new EmptyBorder(8, 12, 8, 12)));

        String[][] fees = {
            {"TRANSFER (Internal)", "Free"},
            {"NEFT", "₹2.50 – ₹25 (RBI slabs)"},
            {"RTGS (min ₹2L)", "₹25 – ₹50"},
            {"IMPS (max ₹5L)", "₹5 – ₹15"},
        };
        for (String[] f : fees) {
            JLabel k = Theme.label(f[0]); k.setForeground(Theme.TEXT_SECONDARY);
            JLabel v = Theme.label(f[1]); v.setForeground(Theme.ACCENT_GOLD);
            p.add(k); p.add(v);
        }
        return p;
    }

    private void verifyAccounts() {
        String fromNo = fromField.getText().trim();
        String toNo   = toField.getText().trim();
        try {
            if (!fromNo.isEmpty()) {
                Account a = AccountService.getInstance().getByAccountNumber(fromNo);
                if (a == null) { fromInfo.setText("❌ Source account not found"); fromInfo.setForeground(Theme.ACCENT_RED); fromId = -1; }
                else if (!"ACTIVE".equals(a.getStatus())) { fromInfo.setText("⚠️ Source account " + a.getStatus()); fromId = -1; }
                else {
                    fromId = a.getId();
                    fromInfo.setText("✓ FROM: " + a.getPartyName() + "  [" + a.getAccountTypeLabel() + "]  Bal: " + BankUtil.formatCurrency(a.getBalance()));
                    fromInfo.setForeground(Theme.ACCENT_GREEN);
                }
            }
            if (!toNo.isEmpty()) {
                Account b = AccountService.getInstance().getByAccountNumber(toNo);
                if (b == null) { toInfo.setText("❌ Dest account not found"); toInfo.setForeground(Theme.ACCENT_RED); toId = -1; }
                else if (!"ACTIVE".equals(b.getStatus())) { toInfo.setText("⚠️ Dest account " + b.getStatus()); toId = -1; }
                else {
                    toId = b.getId();
                    toInfo.setText("✓ TO: " + b.getPartyName() + "  [" + b.getAccountTypeLabel() + "]");
                    toInfo.setForeground(Theme.ACCENT_BLUE);
                }
            }
            updateFeeLabel();
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void updateFeeLabel() {
        String type = (String) typeCombo.getSelectedItem();
        String amtStr = amountField.getText().trim();
        double charge = 0;
        if (BankUtil.isValidAmount(amtStr)) {
            double amt = Double.parseDouble(amtStr);
            charge = switch (type) {
                case "NEFT" -> amt <= 10000 ? 2.5 : amt <= 100000 ? 5 : amt <= 200000 ? 15 : 25;
                case "RTGS" -> amt < 500000 ? 25 : 50;
                case "IMPS" -> amt <= 10000 ? 5 : amt <= 100000 ? 7 : 15;
                default -> 0;
            };
        }
        feeLabel.setText(charge > 0
            ? "Charges: ₹" + String.format("%.2f", charge) + " + GST"
            : "Charges: Free (internal transfer)");
        feeLabel.setForeground(charge > 0 ? Theme.ACCENT_ORANGE : Theme.ACCENT_GREEN);
    }

    private void processTransfer() {
        if (fromId < 0 || toId < 0) { Theme.showError(this, "Verify both accounts first"); return; }
        if (!BankUtil.isValidAmount(amountField.getText().trim())) { Theme.showError(this, "Invalid amount"); return; }

        double amount = Double.parseDouble(amountField.getText().trim());
        String type   = (String) typeCombo.getSelectedItem();
        String desc   = descField.getText().trim().isEmpty() ? type + " Transfer" : descField.getText().trim();

        if (!Theme.showConfirm(this,
            type + " transfer of " + BankUtil.formatCurrency(amount) + "\nFrom: " + fromField.getText() + "\nTo: " + toField.getText())) return;

        final int fId = fromId, tId = toId;
        new SwingWorker<Transaction, Void>() {
            @Override protected Transaction doInBackground() throws Exception {
                return TransactionService.getInstance().transfer(fId, tId, amount, type, desc);
            }
            @Override protected void done() {
                try {
                    Transaction t = get();
                    Theme.showSuccess(TransferPanel.this,
                        "✅ Transfer Successful!\nTxn ID: " + t.getTransactionId()
                        + "\nAmount: " + BankUtil.formatCurrency(amount)
                        + "\nRef: " + type);
                    clearForm();
                } catch (Exception ex) {
                    Theme.showError(TransferPanel.this,
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }

    private void clearForm() {
        fromField.setText(""); toField.setText(""); amountField.setText(""); descField.setText("");
        fromInfo.setText("—"); fromInfo.setForeground(Theme.TEXT_SECONDARY);
        toInfo.setText("—");   toInfo.setForeground(Theme.TEXT_SECONDARY);
        fromId = toId = -1;
        updateFeeLabel();
    }
}
