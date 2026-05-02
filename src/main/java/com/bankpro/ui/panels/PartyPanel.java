package com.bankpro.ui.panels;

import com.bankpro.model.Party;
import com.bankpro.service.*;
import com.bankpro.ui.Theme;
import com.bankpro.util.BankUtil;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class PartyPanel extends JPanel {

    private JTextField       searchField;
    private JComboBox<String> typeFilter;
    private JTable           table;
    private DefaultTableModel tableModel;
    private List<Party>      currentList;

    private static final String[] COLS = {
        "Party ID","Name","Type","Segment","Phone","Email","City","KYC","Credit","Created"
    };

    public PartyPanel() {
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        javax.swing.SwingUtilities.invokeLater(() -> loadParties("","ALL"));
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(Theme.BG_DARK);
        main.setBorder(new EmptyBorder(24, 24, 24, 24));

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(Theme.titleLabel("🏢  Party Management"), BorderLayout.WEST);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acts.setOpaque(false);
        searchField = Theme.styledField();
        searchField.setPreferredSize(new Dimension(240, 36));

        typeFilter = Theme.styledCombo();
        for (String t : new String[]{"ALL","INDIVIDUAL","CORPORATE","PARTNERSHIP","TRUST","GOVERNMENT"})
            typeFilter.addItem(t);

        JButton searchBtn  = Theme.primaryButton("🔍");
        JButton newIndvBtn = Theme.successButton("+ Individual");
        JButton newCorpBtn = Theme.warningButton("+ Corporate");
        JButton refreshBtn = Theme.ghostButton("⟳");

        acts.add(searchField); acts.add(typeFilter);
        acts.add(searchBtn); acts.add(newIndvBtn); acts.add(newCorpBtn); acts.add(refreshBtn);
        header.add(acts, BorderLayout.EAST);

        // ── Table ────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COLS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {120,180,100,80,110,200,100,110,80,110};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Colour KYC column
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String s = v != null ? v.toString() : "";
                if      (s.contains("Approved")) setForeground(Theme.ACCENT_GREEN);
                else if (s.contains("Pending"))  setForeground(Theme.ACCENT_GOLD);
                else                             setForeground(Theme.ACCENT_RED);
                return this;
            }
        });

        // ── Bottom bar ───────────────────────────────────────────────────────
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomBar.setOpaque(false);
        JButton viewBtn     = Theme.ghostButton("👁 Details");
        JButton editBtn     = Theme.ghostButton("✏ Edit");
        JButton kycBtn      = Theme.warningButton("✓ KYC");
        JButton accountsBtn = Theme.primaryButton("🏦 Accounts");
        JButton deactivBtn  = Theme.dangerButton("⊘ Deactivate");
        bottomBar.add(viewBtn); bottomBar.add(editBtn); bottomBar.add(kycBtn);
        bottomBar.add(accountsBtn); bottomBar.add(deactivBtn);

        main.add(header, BorderLayout.NORTH);
        main.add(Theme.styledScroll(table), BorderLayout.CENTER);
        main.add(bottomBar, BorderLayout.SOUTH);
        add(main);

        // ── Wiring ───────────────────────────────────────────────────────────
        searchBtn.addActionListener(e  -> loadParties(searchField.getText().trim(),(String)typeFilter.getSelectedItem()));
        searchField.addActionListener(e-> loadParties(searchField.getText().trim(),(String)typeFilter.getSelectedItem()));
        typeFilter.addActionListener(e -> loadParties(searchField.getText().trim(),(String)typeFilter.getSelectedItem()));
        refreshBtn.addActionListener(e -> loadParties("","ALL"));
        newIndvBtn.addActionListener(e -> showPartyDialog(null, Party.INDIVIDUAL));
        newCorpBtn.addActionListener(e -> showPartyDialog(null, Party.CORPORATE));
        viewBtn.addActionListener(e    -> viewSelected());
        editBtn.addActionListener(e    -> editSelected());
        kycBtn.addActionListener(e     -> updateKyc());
        accountsBtn.addActionListener(e-> viewAccounts());
        deactivBtn.addActionListener(e -> deactivateSelected());
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadParties(String query, String typeF) {
        new SwingWorker<List<Party>, Void>() {
            @Override protected List<Party> doInBackground() throws Exception {
                List<Party> all = query.isEmpty()
                    ? PartyService.getInstance().getAllParties()
                    : PartyService.getInstance().searchParties(query);
                if (!"ALL".equals(typeF))
                    all = all.stream().filter(p -> typeF.equals(p.getPartyType())).toList();
                return all;
            }
            @Override protected void done() {
                try {
                    currentList = get();
                    tableModel.setRowCount(0);
                    for (Party p : currentList) {
                        tableModel.addRow(new Object[]{
                            p.getPartyId(), p.getDisplayName(), p.getShortType(),
                            p.getSegmentLabel(), p.getPhone(), p.getEmail(),
                            p.getCity() != null ? p.getCity() : "—",
                            p.getKycStatusLabel(), p.getCreditScore(),
                            p.getCreatedAt() != null ? BankUtil.formatDate(p.getCreatedAt()) : "—"
                        });
                    }
                } catch (Exception ex) { Theme.showError(PartyPanel.this, ex.getMessage()); }
            }
        }.execute();
    }

    private Party getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || currentList == null) { Theme.showError(this,"Select a party first"); return null; }
        return currentList.get(row);
    }

    // ── Detail view ───────────────────────────────────────────────────────────

    private void viewSelected() {
        Party p = getSelected(); if (p == null) return;
        StringBuilder sb = new StringBuilder("<html><table cellpadding='5'>");
        sb.append(row("Party ID", p.getPartyId()));
        sb.append(row("Type", p.getShortType()));
        sb.append(row("Segment", p.getSegmentLabel()));
        if (Party.INDIVIDUAL.equals(p.getPartyType())) {
            sb.append(row("Name", p.getDisplayName()));
            sb.append(row("DOB", p.getDateOfBirth() != null ? p.getDateOfBirth() : "—"));
            sb.append(row("Gender", p.getGender() != null ? p.getGender() : "—"));
            sb.append(row("Aadhar", p.getAadharNumber() != null
                ? "XXXX-XXXX-" + p.getAadharNumber().substring(Math.max(0,p.getAadharNumber().length()-4)) : "—"));
        } else {
            sb.append(row("Company", p.getCompanyName()));
            sb.append(row("Business Type", p.getBusinessType() != null ? p.getBusinessType() : "—"));
            sb.append(row("Reg No.", p.getRegistrationNumber() != null ? p.getRegistrationNumber() : "—"));
            sb.append(row("CIN", p.getCinNumber() != null ? p.getCinNumber() : "—"));
            sb.append(row("GST", p.getGstNumber() != null ? p.getGstNumber() : "—"));
            sb.append(row("Contact", p.getContactPersonName() != null ? p.getContactPersonName() : "—"));
        }
        sb.append(row("PAN", p.getPanNumber() != null ? p.getPanNumber() : "—"));
        sb.append(row("Email", p.getEmail()));
        sb.append(row("Phone", p.getPhone()));
        sb.append(row("Alt Phone", p.getAltPhone() != null ? p.getAltPhone() : "—"));
        sb.append(row("Address", (p.getAddress()!=null?p.getAddress():"") + ", " +
            (p.getCity()!=null?p.getCity():"") + " " + (p.getState()!=null?p.getState():"") +
            " - " + (p.getPincode()!=null?p.getPincode():"")));
        sb.append(row("KYC", p.getKycStatusLabel()));
        sb.append(row("Credit Score", String.valueOf(p.getCreditScore())));
        sb.append(row("Created", p.getCreatedAt() != null ? BankUtil.formatDateTime(p.getCreatedAt()) : "—"));
        sb.append("</table></html>");
        JOptionPane.showMessageDialog(this, new JLabel(sb.toString()),
            "Party Details — " + p.getDisplayName(), JOptionPane.INFORMATION_MESSAGE);
    }

    private String row(String k, String v) {
        return "<tr><td><b>" + k + "</b></td><td>" + v + "</td></tr>";
    }

    // ── Party dialog (create / edit) ──────────────────────────────────────────

    private void editSelected() {
        Party p = getSelected(); if (p == null) return;
        showPartyDialog(p, p.getPartyType());
    }

    private void showPartyDialog(Party existing, String partyType) {
        boolean isEdit = existing != null;
        boolean isCorp = Party.CORPORATE.equals(partyType) || Party.PARTNERSHIP.equals(partyType)
            || Party.TRUST.equals(partyType) || Party.GOVERNMENT.equals(partyType);

        JDialog dlg = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
            (isEdit ? "Edit" : "New") + " Party — " + partyType, true);
        dlg.setSize(620, isCorp ? 760 : 700);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_CARD);
        p.setBorder(new EmptyBorder(20,30,20,30));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5,4,5,4);

        // Common fields
        JTextField email    = Theme.styledField();
        JTextField phone    = Theme.styledField();
        JTextField altPhone = Theme.styledField();
        JTextField address  = Theme.styledField();
        JTextField city     = Theme.styledField();
        JTextField state    = Theme.styledField();
        JTextField pincode  = Theme.styledField();
        JTextField pan      = Theme.styledField();
        JComboBox<String> segmentBox = Theme.styledCombo();
        for (String s : new String[]{"RETAIL","SME","CORPORATE","HNI"}) segmentBox.addItem(s);

        // Individual-specific
        JTextField firstName = Theme.styledField();
        JTextField lastName  = Theme.styledField();
        JTextField dob       = Theme.styledField();
        JComboBox<String> genderBox = Theme.styledCombo();
        for (String g : new String[]{"Male","Female","Other","Not Disclosed"}) genderBox.addItem(g);
        JTextField aadhar    = Theme.styledField();

        // Corporate-specific
        JTextField companyName   = Theme.styledField();
        JTextField regNo         = Theme.styledField();
        JTextField incDate       = Theme.styledField();
        JTextField gst           = Theme.styledField();
        JTextField cin           = Theme.styledField();
        JTextField contactPerson = Theme.styledField();
        JTextField contactDesig  = Theme.styledField();
        JComboBox<String> bizType = Theme.styledCombo();
        for (String b : new String[]{"Private Limited","Public Limited","LLP","OPC","Sole Proprietorship","Partnership","Trust","NGO","Government"})
            bizType.addItem(b);

        if (isEdit) {
            email.setText(existing.getEmail()); phone.setText(existing.getPhone());
            if (existing.getAltPhone() != null) altPhone.setText(existing.getAltPhone());
            if (existing.getAddress() != null) address.setText(existing.getAddress());
            if (existing.getCity() != null) city.setText(existing.getCity());
            if (existing.getState() != null) state.setText(existing.getState());
            if (existing.getPincode() != null) pincode.setText(existing.getPincode());
            if (existing.getPanNumber() != null) pan.setText(existing.getPanNumber());
            if (existing.getSegment() != null) segmentBox.setSelectedItem(existing.getSegment());
            if (!isCorp) {
                if (existing.getFirstName() != null) firstName.setText(existing.getFirstName());
                if (existing.getLastName() != null)  lastName.setText(existing.getLastName());
                if (existing.getDateOfBirth() != null) dob.setText(existing.getDateOfBirth());
                if (existing.getGender() != null) genderBox.setSelectedItem(existing.getGender());
                if (existing.getAadharNumber() != null) aadhar.setText(existing.getAadharNumber());
            } else {
                if (existing.getCompanyName() != null) companyName.setText(existing.getCompanyName());
                if (existing.getRegistrationNumber() != null) regNo.setText(existing.getRegistrationNumber());
                if (existing.getIncorporationDate() != null) incDate.setText(existing.getIncorporationDate());
                if (existing.getGstNumber() != null) gst.setText(existing.getGstNumber());
                if (existing.getCinNumber() != null) cin.setText(existing.getCinNumber());
                if (existing.getContactPersonName() != null) contactPerson.setText(existing.getContactPersonName());
                if (existing.getContactPersonDesig() != null) contactDesig.setText(existing.getContactPersonDesig());
            }
        }

        int[] row = {0};
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2;
        p.add(Theme.heading((isEdit ? "Edit " : "Register ") + partyType), gc); row[0]++;
        gc.gridwidth=1;

        if (!isCorp) {
            addRow(p,gc,row,"First Name *",firstName);
            addRow(p,gc,row,"Last Name *",lastName);
            addRow(p,gc,row,"Date of Birth (YYYY-MM-DD)",dob);
            addRow(p,gc,row,"Gender",genderBox);
            addRow(p,gc,row,"Aadhar No. (12 digits)",aadhar);
        } else {
            addRow(p,gc,row,"Company Name *",companyName);
            addRow(p,gc,row,"Business Type",bizType);
            addRow(p,gc,row,"Reg. No. / ROC",regNo);
            addRow(p,gc,row,"Incorporation Date (YYYY-MM-DD)",incDate);
            addRow(p,gc,row,"CIN",cin);
            addRow(p,gc,row,"GST No.",gst);
            addRow(p,gc,row,"Contact Person",contactPerson);
            addRow(p,gc,row,"Designation",contactDesig);
        }
        addRow(p,gc,row,"PAN *",pan);
        addRow(p,gc,row,"Email *",email);
        addRow(p,gc,row,"Phone * (10 digit)",phone);
        addRow(p,gc,row,"Alternate Phone",altPhone);
        addRow(p,gc,row,"Address",address);
        addRow(p,gc,row,"City",city);
        addRow(p,gc,row,"State",state);
        addRow(p,gc,row,"Pincode",pincode);
        addRow(p,gc,row,"Segment",segmentBox);

        JButton saveBtn = Theme.successButton(isEdit ? "Update Party" : "Register Party");
        gc.gridx=0; gc.gridy=row[0]; gc.gridwidth=2; gc.insets=new Insets(16,4,4,4);
        p.add(saveBtn, gc);

        JScrollPane sp = new JScrollPane(p);
        sp.setBackground(Theme.BG_CARD); sp.getViewport().setBackground(Theme.BG_CARD); sp.setBorder(null);
        dlg.add(sp);

        saveBtn.addActionListener(e -> {
            try {
                Party party = isEdit ? existing : new Party();
                party.setPartyType(partyType);
                party.setEmail(email.getText().trim());
                party.setPhone(phone.getText().trim());
                party.setAltPhone(altPhone.getText().trim());
                party.setAddress(address.getText().trim());
                party.setCity(city.getText().trim());
                party.setState(state.getText().trim());
                party.setPincode(pincode.getText().trim());
                party.setPanNumber(pan.getText().trim().toUpperCase());
                party.setSegment((String)segmentBox.getSelectedItem());
                if (!isCorp) {
                    party.setFirstName(firstName.getText().trim());
                    party.setLastName(lastName.getText().trim());
                    party.setDateOfBirth(dob.getText().trim().isEmpty() ? null : dob.getText().trim());
                    party.setGender((String)genderBox.getSelectedItem());
                    party.setAadharNumber(aadhar.getText().trim().isEmpty() ? null : aadhar.getText().trim());
                } else {
                    party.setCompanyName(companyName.getText().trim());
                    party.setBusinessType((String)bizType.getSelectedItem());
                    party.setRegistrationNumber(regNo.getText().trim());
                    party.setIncorporationDate(incDate.getText().trim().isEmpty() ? null : incDate.getText().trim());
                    party.setGstNumber(gst.getText().trim().isEmpty() ? null : gst.getText().trim());
                    party.setCinNumber(cin.getText().trim().isEmpty() ? null : cin.getText().trim());
                    party.setContactPersonName(contactPerson.getText().trim());
                    party.setContactPersonDesig(contactDesig.getText().trim());
                }
                if (isEdit) PartyService.getInstance().updateParty(party);
                else        PartyService.getInstance().createParty(party);
                dlg.dispose();
                loadParties("","ALL");
                Theme.showSuccess(this, isEdit
                    ? "Party updated!"
                    : "Party registered!\nID: " + party.getPartyId());
            } catch (Exception ex) { Theme.showError(dlg, ex.getMessage()); }
        });

        dlg.setVisible(true);
    }

    private void addRow(JPanel p, GridBagConstraints gc, int[] row, String label, Component comp) {
        gc.gridx=0; gc.gridy=row[0]; gc.weightx=0.35; gc.gridwidth=1;
        p.add(Theme.label(label), gc);
        gc.gridx=1; gc.weightx=0.65;
        p.add(comp, gc);
        row[0]++;
    }

    private void updateKyc() {
        Party p = getSelected(); if (p == null) return;
        String[] opts = {"APPROVED","PENDING","REJECTED"};
        String sel = (String)JOptionPane.showInputDialog(this,
            "KYC status for " + p.getDisplayName(), "Update KYC",
            JOptionPane.PLAIN_MESSAGE, null, opts, p.getKycStatus());
        if (sel != null) {
            try {
                PartyService.getInstance().updateKycStatus(p.getId(), sel);
                loadParties("","ALL");
                Theme.showSuccess(this, "KYC updated to " + sel);
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }

    private void viewAccounts() {
        Party p = getSelected(); if (p == null) return;
        try {
            var accounts = AccountService.getInstance().getAccountsByParty(p.getId());
            if (accounts.isEmpty()) { Theme.showError(this,"No accounts found for "+p.getDisplayName()); return; }
            StringBuilder sb = new StringBuilder("<html><b>Accounts — " + p.getDisplayName() + "</b><br><br>");
            for (var a : accounts)
                sb.append("• ").append(a.getAccountNumber()).append("  [")
                  .append(a.getAccountTypeLabel()).append("]  ")
                  .append(BankUtil.formatCurrency(a.getBalance()))
                  .append("  ").append(a.getStatus()).append("<br>");
            sb.append("</html>");
            JOptionPane.showMessageDialog(this, new JLabel(sb.toString()), "Accounts", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
    }

    private void deactivateSelected() {
        Party p = getSelected(); if (p == null) return;
        if (Theme.showConfirm(this,"Deactivate party "+p.getDisplayName()+"?")) {
            try {
                PartyService.getInstance().deactivateParty(p.getId());
                loadParties("","ALL");
                Theme.showSuccess(this,"Party deactivated.");
            } catch (Exception ex) { Theme.showError(this, ex.getMessage()); }
        }
    }
}
