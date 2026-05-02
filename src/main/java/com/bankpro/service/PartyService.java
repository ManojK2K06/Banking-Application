package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.Party;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class PartyService {
    private static PartyService instance;
    private final AuditService audit = AuditService.getInstance();

    private PartyService() {}
    public static synchronized PartyService getInstance() {
        if (instance == null) instance = new PartyService();
        return instance;
    }

    public Party createParty(Party p) throws Exception {
        if (!SessionManager.getInstance().hasPermission(2))
            throw new SecurityException("Creating parties requires level 2+");
        validateParty(p);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String pid = generatePartyId(p.getPartyType(), conn);
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO parties (party_id,party_type,first_name,last_name,date_of_birth,gender," +
                "company_name,registration_number,incorporation_date,business_type," +
                "contact_person_name,contact_person_desig,email,phone,alt_phone," +
                "address,city,state,pincode,country,aadhar_number,pan_number," +
                "gst_number,cin_number,kyc_status,segment,created_by) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setString(1, pid);
            ps.setString(2, p.getPartyType());
            ps.setString(3, p.getFirstName());
            ps.setString(4, p.getLastName());
            ps.setString(5, p.getDateOfBirth());
            ps.setString(6, p.getGender());
            ps.setString(7, p.getCompanyName());
            ps.setString(8, p.getRegistrationNumber());
            ps.setString(9, p.getIncorporationDate());
            ps.setString(10, p.getBusinessType());
            ps.setString(11, p.getContactPersonName());
            ps.setString(12, p.getContactPersonDesig());
            ps.setString(13, p.getEmail());
            ps.setString(14, p.getPhone());
            ps.setString(15, p.getAltPhone());
            ps.setString(16, p.getAddress());
            ps.setString(17, p.getCity());
            ps.setString(18, p.getState());
            ps.setString(19, p.getPincode());
            ps.setString(20, p.getCountry() != null ? p.getCountry() : "India");
            ps.setString(21, p.getAadharNumber());
            ps.setString(22, p.getPanNumber() != null ? p.getPanNumber().toUpperCase() : null);
            ps.setString(23, p.getGstNumber());
            ps.setString(24, p.getCinNumber());
            ps.setString(25, "PENDING");
            ps.setString(26, p.getSegment() != null ? p.getSegment() : "RETAIL");
            ps.setInt(27, SessionManager.getInstance().getCurrentUser().getId());
            ps.executeUpdate();

            ResultSet rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()");
            if (rs.next()) { p.setId(rs.getInt(1)); p.setPartyId(pid); }

            audit.log("CREATE_PARTY", "PARTY", pid, null, p.getDisplayName(),
                "Created party: " + p.getDisplayName() + " [" + p.getPartyType() + "]");
            return p;
        } catch (SQLException e) {
            String m = e.getMessage() != null ? e.getMessage() : "";
            if (m.contains("UNIQUE")) {
                if (m.contains("email"))   throw new Exception("Email already registered: " + p.getEmail());
                if (m.contains("pan"))     throw new Exception("PAN already registered: " + p.getPanNumber());
                if (m.contains("aadhar"))  throw new Exception("Aadhar already registered");
            }
            throw new Exception("Database error: " + m);
        }
    }

    public void updateParty(Party p) throws Exception {
        if (!SessionManager.getInstance().hasPermission(2))
            throw new SecurityException("Insufficient permissions");
        validateParty(p);
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE parties SET first_name=?,last_name=?,date_of_birth=?,gender=?," +
                "company_name=?,registration_number=?,incorporation_date=?,business_type=?," +
                "contact_person_name=?,contact_person_desig=?,email=?,phone=?,alt_phone=?," +
                "address=?,city=?,state=?,pincode=?,country=?,aadhar_number=?,pan_number=?," +
                "gst_number=?,cin_number=?,segment=? WHERE id=?")) {
            ps.setString(1,p.getFirstName()); ps.setString(2,p.getLastName());
            ps.setString(3,p.getDateOfBirth()); ps.setString(4,p.getGender());
            ps.setString(5,p.getCompanyName()); ps.setString(6,p.getRegistrationNumber());
            ps.setString(7,p.getIncorporationDate()); ps.setString(8,p.getBusinessType());
            ps.setString(9,p.getContactPersonName()); ps.setString(10,p.getContactPersonDesig());
            ps.setString(11,p.getEmail()); ps.setString(12,p.getPhone());
            ps.setString(13,p.getAltPhone()); ps.setString(14,p.getAddress());
            ps.setString(15,p.getCity()); ps.setString(16,p.getState());
            ps.setString(17,p.getPincode()); ps.setString(18,p.getCountry());
            ps.setString(19,p.getAadharNumber());
            ps.setString(20,p.getPanNumber() != null ? p.getPanNumber().toUpperCase() : null);
            ps.setString(21,p.getGstNumber()); ps.setString(22,p.getCinNumber());
            ps.setString(23,p.getSegment()); ps.setInt(24,p.getId());
            ps.executeUpdate();
            audit.log("UPDATE_PARTY","PARTY",p.getPartyId(),null,p.getDisplayName(),"Party updated");
        }
    }

    public void updateKycStatus(int partyId, String status) throws Exception {
        if (!SessionManager.getInstance().hasPermission(4))
            throw new SecurityException("KYC update requires level 4+");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE parties SET kyc_status=? WHERE id=?")) {
            ps.setString(1, status); ps.setInt(2, partyId); ps.executeUpdate();
            audit.log("UPDATE_KYC","PARTY",String.valueOf(partyId),null,status,"KYC → "+status);
        }
    }

    public void deactivateParty(int partyId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(6))
            throw new SecurityException("Deactivating parties requires level 6+");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE parties SET is_active=0 WHERE id=?")) {
            ps.setInt(1, partyId); ps.executeUpdate();
            audit.log("DEACTIVATE_PARTY","PARTY",String.valueOf(partyId),"ACTIVE","INACTIVE","Party deactivated");
        }
    }

    public Party getById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM parties WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapParty(rs);
        }
        return null;
    }

    public Party getByPartyId(String pid) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM parties WHERE party_id=?")) {
            ps.setString(1, pid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapParty(rs);
        }
        return null;
    }

    public List<Party> searchParties(String query) throws SQLException {
        List<Party> list = new ArrayList<>();
        String sql = "SELECT * FROM parties WHERE is_active=1 AND (" +
            "party_id LIKE ? OR first_name LIKE ? OR last_name LIKE ? OR " +
            "company_name LIKE ? OR phone LIKE ? OR email LIKE ? OR pan_number LIKE ?) " +
            "ORDER BY party_id LIMIT 200";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String q = "%" + query + "%";
            for (int i = 1; i <= 7; i++) ps.setString(i, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapParty(rs));
        }
        return list;
    }

    public List<Party> getAllParties() throws SQLException {
        List<Party> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM parties WHERE is_active=1 ORDER BY party_id");
            while (rs.next()) list.add(mapParty(rs));
        }
        return list;
    }

    /** Binary search on a pre-sorted list by partyId */
    public Party binarySearchByPartyId(List<Party> sorted, String targetId) {
        int idx = BankUtil.binarySearchById(sorted, targetId, Party::getPartyId);
        return idx >= 0 ? sorted.get(idx) : null;
    }

    public int getTotalPartyCount() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM parties WHERE is_active=1");
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String generatePartyId(String type, Connection conn) throws SQLException {
        String prefix = switch (type) {
            case Party.CORPORATE   -> "CORP";
            case Party.PARTNERSHIP -> "PART";
            case Party.TRUST       -> "TRST";
            case Party.GOVERNMENT  -> "GOVT";
            default                -> "INDV";
        };
        ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM parties");
        int seq = rs.next() ? rs.getInt(1) + 1 : 1;
        return prefix + String.format("%08d", seq);
    }

    private void validateParty(Party p) throws Exception {
        if (!BankUtil.isValidEmail(p.getEmail()))
            throw new Exception("Invalid email address: " + p.getEmail());
        if (!BankUtil.isValidPhone(p.getPhone()))
            throw new Exception("Invalid phone number (must be 10 digits, starting 6-9)");

        if (Party.INDIVIDUAL.equals(p.getPartyType())) {
            if (p.getFirstName() == null || p.getFirstName().trim().isEmpty())
                throw new Exception("First name is required for individuals");
            if (p.getLastName() == null || p.getLastName().trim().isEmpty())
                throw new Exception("Last name is required for individuals");
            if (p.getAadharNumber() != null && !p.getAadharNumber().isEmpty()
                    && !BankUtil.isValidAadhar(p.getAadharNumber()))
                throw new Exception("Invalid Aadhar number (must be 12 digits)");
        } else {
            if (p.getCompanyName() == null || p.getCompanyName().trim().isEmpty())
                throw new Exception("Company name is required");
        }

        if (p.getPanNumber() != null && !p.getPanNumber().isEmpty()
                && !BankUtil.isValidPAN(p.getPanNumber()))
            throw new Exception("Invalid PAN format (e.g. ABCDE1234F)");
        if (p.getPincode() != null && !p.getPincode().isEmpty()
                && !BankUtil.isValidPincode(p.getPincode()))
            throw new Exception("Invalid pincode (must be 6 digits)");
    }

    private Party mapParty(ResultSet rs) throws SQLException {
        Party p = new Party();
        p.setId(rs.getInt("id"));
        p.setPartyId(rs.getString("party_id"));
        p.setPartyType(rs.getString("party_type"));
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));
        p.setDateOfBirth(rs.getString("date_of_birth"));
        p.setGender(rs.getString("gender"));
        p.setCompanyName(rs.getString("company_name"));
        p.setRegistrationNumber(rs.getString("registration_number"));
        p.setIncorporationDate(rs.getString("incorporation_date"));
        p.setBusinessType(rs.getString("business_type"));
        p.setContactPersonName(rs.getString("contact_person_name"));
        p.setContactPersonDesig(rs.getString("contact_person_desig"));
        p.setEmail(rs.getString("email"));
        p.setPhone(rs.getString("phone"));
        p.setAltPhone(rs.getString("alt_phone"));
        p.setAddress(rs.getString("address"));
        p.setCity(rs.getString("city"));
        p.setState(rs.getString("state"));
        p.setPincode(rs.getString("pincode"));
        p.setCountry(rs.getString("country"));
        p.setAadharNumber(rs.getString("aadhar_number"));
        p.setPanNumber(rs.getString("pan_number"));
        p.setGstNumber(rs.getString("gst_number"));
        p.setCinNumber(rs.getString("cin_number"));
        p.setKycStatus(rs.getString("kyc_status"));
        p.setKycDocuments(rs.getString("kyc_documents"));
        p.setCreditScore(rs.getInt("credit_score"));
        p.setSegment(rs.getString("segment"));
        p.setActive(rs.getInt("is_active") == 1);
        p.setCreatedBy(rs.getInt("created_by"));
        String ca = rs.getString("created_at");
        if (ca != null) {
            try { p.setCreatedAt(LocalDateTime.parse(ca.replace(" ","T"))); }
            catch (Exception ignored) {}
        }
        return p;
    }
}
