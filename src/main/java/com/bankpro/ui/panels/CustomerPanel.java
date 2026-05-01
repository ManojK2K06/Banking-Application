package com.bankpro.ui.panels;

import com.bankpro.model.Customer;
import com.bankpro.service.CustomerService;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class CustomerPanel extends JPanel {

    private JTextField searchField;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Customer> currentList;

    private static final String[] COLS = {
        "Customer ID","Name","Phone","Email","City","KYC","Credit Score","Created"
    };

    public CustomerPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        javax.swing.SwingUtilities.invokeLater(() -> loadCustomers(""));
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Header bar
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(Theme.titleLabel("👥  Customer Management"), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        searchField = Theme.styledField();
        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, ID, phone...");
        JButton searchBtn = Theme.primaryButton("🔍 Search");
        JButton newBtn = Theme.successButton("+ New Customer");
        JButton refreshBtn = Theme.ghostButton("⟳");

        actions.add(searchField);
        actions.add(searchBtn);
        actions.add(newBtn);
        actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);

        // Table
        tableModel = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // column widths
        int[] widths = {120,160,110,200,100,110,100,120};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = Theme.styledScroll(table);

        // Bottom action bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomBar.setOpaque(false);
        JButton viewBtn = Theme.ghostButton("👁 View Details");
        JButton editBtn = Theme.ghostButton("✏ Edit");
        JButton kycBtn = Theme.warningButton("✓ Update KYC");
        JButton accountsBtn = Theme.primaryButton("🏦 View Accounts");
        JButton deactivateBtn = Theme.dangerButton("⊘ Deactivate");

        bottomBar.add(viewBtn);
        bottomBar.add(editBtn);
        bottomBar.add(kycBtn);
        bottomBar.add(accountsBtn);
        bottomBar.add(deactivateBtn);

        main.add(header, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        main.add(bottomBar, BorderLayout.SOUTH);
        add(main);

        // Event handlers
        searchBtn.addActionListener(e -> loadCustomers(searchField.getText().trim()));
        searchField.addActionListener(e -> loadCustomers(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> loadCustomers(""));
        newBtn.addActionListener(e -> showNewCustomerDialog());
        viewBtn.addActionListener(e -> viewSelected());
        editBtn.addActionListener(e -> editSelected());
        kycBtn.addActionListener(e -> updateKyc());
        accountsBtn.addActionListener(e -> viewAccounts());
        deactivateBtn.addActionListener(e -> deactivateSelected());
    }

    private void loadCustomers(String query) {
        SwingWorker<List<Customer>, Void> w = new SwingWorker<>() {
            @Override protected List<Customer> doInBackground() throws Exception {
                if (query.isEmpty()) return CustomerService.getInstance().getAllCustomers();
                return CustomerService.getInstance().searchCustomers(query);
            }
            @Override protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (Customer c : currentList) {
                        tableModel.addRow(new Object[]{
                            c.getCustomerId(), c.getFullName(), c.getPhone(),
                            c.getEmail(), c.getCity() != null ? c.getCity() : "",
                            c.getKycStatus(), c.getCreditScore(),
                            c.getCreatedAt() != null ? BankUtil.formatDate(c.getCreatedAt()) : ""
                        });
                    }
                } catch (Exception ex) {
                    Theme.showError(CustomerPanel.this, "Load failed: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private Customer getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null || row >= currentList.size()) {
            Theme.showError(this, "Please select a customer first");
            return null;
        }
        return currentList.get(row);
    }

    private void viewSelected() {
        Customer c = getSelected();
        if (c == null) return;
        String info = String.format(
            "<html><b>Customer ID:</b> %s<br>" +
            "<b>Name:</b> %s<br>" +
            "<b>DOB:</b> %s<br>" +
            "<b>Email:</b> %s<br>" +
            "<b>Phone:</b> %s<br>" +
            "<b>Address:</b> %s, %s, %s - %s<br>" +
            "<b>Aadhar:</b> %s<br>" +
            "<b>PAN:</b> %s<br>" +
            "<b>KYC:</b> %s<br>" +
            "<b>Credit Score:</b> %d<br>" +
            "<b>Created:</b> %s</html>",
            c.getCustomerId(), c.getFullName(), c.getDateOfBirth() != null ? c.getDateOfBirth() : "N/A",
            c.getEmail(), c.getPhone(),
            c.getAddress() != null ? c.getAddress() : "",
            c.getCity() != null ? c.getCity() : "",
            c.getState() != null ? c.getState() : "",
            c.getPincode() != null ? c.getPincode() : "",
            c.getAadharNumber() != null ? "XXXX-XXXX-" + c.getAadharNumber().substring(8) : "N/A",
            c.getPanNumber() != null ? c.getPanNumber() : "N/A",
            c.getKycStatusLabel(), c.getCreditScore(),
            c.getCreatedAt() != null ? BankUtil.formatDateTime(c.getCreatedAt()) : "N/A"
        );
        JLabel msg = new JLabel(info);
        msg.setFont(Theme.FONT_BODY);
        JOptionPane.showMessageDialog(this, msg, "Customer Details — " + c.getFullName(),
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void editSelected() {
        Customer c = getSelected();
        if (c == null) return;
        showCustomerDialog(c, true);
    }

    private void showNewCustomerDialog() {
        showCustomerDialog(new Customer(), false);
    }

    private void showCustomerDialog(Customer existing, boolean isEdit) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isEdit ? "Edit Customer" : "New Customer Registration", true);
        dlg.setSize(600, 720);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(24, 32, 24, 32));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 4, 4, 4);

        JTextField firstName = Theme.styledField();
        JTextField lastName  = Theme.styledField();
        JTextField dob       = Theme.styledField();
        JTextField email     = Theme.styledField();
        JTextField phone     = Theme.styledField();
        JTextField address   = Theme.styledField();
        JTextField city      = Theme.styledField();
        JTextField state     = Theme.styledField();
        JTextField pincode   = Theme.styledField();
        JTextField aadhar    = Theme.styledField();
        JTextField pan       = Theme.styledField();

        if (isEdit) {
            firstName.setText(existing.getFirstName());
            lastName.setText(existing.getLastName());
            dob.setText(existing.getDateOfBirth() != null ? existing.getDateOfBirth() : "");
            email.setText(existing.getEmail());
            phone.setText(existing.getPhone());
            address.setText(existing.getAddress() != null ? existing.getAddress() : "");
            city.setText(existing.getCity() != null ? existing.getCity() : "");
            state.setText(existing.getState() != null ? existing.getState() : "");
            pincode.setText(existing.getPincode() != null ? existing.getPincode() : "");
            aadhar.setText(existing.getAadharNumber() != null ? existing.getAadharNumber() : "");
            pan.setText(existing.getPanNumber() != null ? existing.getPanNumber() : "");
        }

        Object[][] fields = {
            {"First Name *", firstName}, {"Last Name *", lastName},
            {"Date of Birth (YYYY-MM-DD)", dob}, {"Email *", email},
            {"Phone (10-digit) *", phone}, {"Address", address},
            {"City", city}, {"State", state},
            {"Pincode", pincode}, {"Aadhar No.", aadhar},
            {"PAN No.", pan}
        };

        int row = 0;
        for (Object[] field : fields) {
            gc.gridx=0; gc.gridy=row; gc.weightx=0.3;
            JLabel lbl = Theme.label((String)field[0]);
            p.add(lbl, gc);
            gc.gridx=1; gc.weightx=0.7;
            p.add((Component)field[1], gc);
            row++;
        }

        JButton saveBtn = Theme.successButton(isEdit ? "Update Customer" : "Register Customer");
        gc.gridx=0; gc.gridy=row; gc.gridwidth=2; gc.insets=new Insets(16,4,4,4);
        p.add(saveBtn, gc);

        JScrollPane sp = new JScrollPane(p);
        sp.setBackground(Theme.BG_CARD);
        sp.getViewport().setBackground(Theme.BG_CARD);
        sp.setBorder(null);
        dlg.add(sp);

        saveBtn.addActionListener(e -> {
            try {
                existing.setFirstName(firstName.getText().trim());
                existing.setLastName(lastName.getText().trim());
                existing.setDateOfBirth(dob.getText().trim().isEmpty() ? null : dob.getText().trim());
                existing.setEmail(email.getText().trim());
                existing.setPhone(phone.getText().trim());
                existing.setAddress(address.getText().trim());
                existing.setCity(city.getText().trim());
                existing.setState(state.getText().trim());
                existing.setPincode(pincode.getText().trim());
                existing.setAadharNumber(aadhar.getText().trim().isEmpty() ? null : aadhar.getText().trim());
                existing.setPanNumber(pan.getText().trim().isEmpty() ? null : pan.getText().trim().toUpperCase());

                if (isEdit) CustomerService.getInstance().updateCustomer(existing);
                else CustomerService.getInstance().createCustomer(existing);

                dlg.dispose();
                loadCustomers("");
                Theme.showSuccess(this,
                    (isEdit ? "Customer updated!" : "Customer registered! ID: " + existing.getCustomerId()));
            } catch (Exception ex) {
                Theme.showError(dlg, ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    private void updateKyc() {
        Customer c = getSelected();
        if (c == null) return;
        String[] options = {"APPROVED", "PENDING", "REJECTED"};
        String sel = (String)JOptionPane.showInputDialog(this,
            "Update KYC status for " + c.getFullName(),
            "KYC Update", JOptionPane.PLAIN_MESSAGE, null, options, c.getKycStatus());
        if (sel != null) {
            try {
                CustomerService.getInstance().updateKycStatus(c.getId(), sel);
                loadCustomers("");
                Theme.showSuccess(this, "KYC status updated to " + sel);
            } catch (Exception ex) {
                Theme.showError(this, ex.getMessage());
            }
        }
    }

    private void viewAccounts() {
        Customer c = getSelected();
        if (c == null) return;
        try {
            var accounts = com.bankpro.service.AccountService.getInstance()
                .getAccountsByCustomer(c.getId());
            if (accounts.isEmpty()) {
                Theme.showError(this, "No accounts found for " + c.getFullName());
                return;
            }
            StringBuilder sb = new StringBuilder("<html><b>Accounts for " + c.getFullName() + "</b><br><br>");
            for (var a : accounts) {
                sb.append("• ").append(a.getAccountNumber()).append("  [")
                  .append(a.getAccountType()).append("]  ")
                  .append(BankUtil.formatCurrency(a.getBalance()))
                  .append("  Status: ").append(a.getStatus()).append("<br>");
            }
            sb.append("</html>");
            JOptionPane.showMessageDialog(this, new JLabel(sb.toString()),
                "Accounts", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Theme.showError(this, ex.getMessage());
        }
    }

    private void deactivateSelected() {
        Customer c = getSelected();
        if (c == null) return;
        if (Theme.showConfirm(this, "Deactivate customer " + c.getFullName() + "? This cannot be undone easily.")) {
            try {
                CustomerService.getInstance().deactivateCustomer(c.getId());
                loadCustomers("");
                Theme.showSuccess(this, "Customer deactivated.");
            } catch (Exception ex) {
                Theme.showError(this, ex.getMessage());
            }
        }
    }
}
