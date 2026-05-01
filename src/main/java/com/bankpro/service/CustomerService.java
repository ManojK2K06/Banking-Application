package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.Customer;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CustomerService {
    private static CustomerService instance;
    private final AuditService audit = AuditService.getInstance();

    private CustomerService() {}
    public static synchronized CustomerService getInstance() {
        if (instance == null) instance = new CustomerService();
        return instance;
    }

    public Customer createCustomer(Customer c) throws Exception {
        if (!SessionManager.getInstance().hasPermission(2))
            throw new SecurityException("Insufficient permissions to create customers (requires level 2+)");
        validateCustomer(c);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO customers (customer_id,first_name,last_name,date_of_birth,email,phone," +
                 "address,city,state,pincode,country,aadhar_number,pan_number,kyc_status,created_by) " +
                 "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {

            String cid = BankUtil.generateCustomerId();
            ps.setString(1, cid);
            ps.setString(2, c.getFirstName());
            ps.setString(3, c.getLastName());
            ps.setString(4, c.getDateOfBirth());
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getPhone());
            ps.setString(7, c.getAddress());
            ps.setString(8, c.getCity());
            ps.setString(9, c.getState());
            ps.setString(10, c.getPincode());
            ps.setString(11, c.getCountry() != null ? c.getCountry() : "India");
            ps.setString(12, c.getAadharNumber());
            ps.setString(13, c.getPanNumber());
            ps.setString(14, "PENDING");
            ps.setInt(15, SessionManager.getInstance().getCurrentUser().getId());
            ps.executeUpdate();

            ResultSet rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()");
            if (rs.next()) {
                c.setId(rs.getInt(1));
                c.setCustomerId(cid);
            }
            audit.log("CREATE_CUSTOMER", "CUSTOMER", cid, null,
                c.getFullName(), "Created customer: " + c.getFullName());
            return c;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                if (e.getMessage() != null && e.getMessage().contains("email"))
                    throw new Exception("Email already registered: " + c.getEmail());
                if (e.getMessage() != null && e.getMessage().contains("aadhar"))
                    throw new Exception("Aadhar number already registered");
                if (e.getMessage() != null && e.getMessage().contains("pan"))
                    throw new Exception("PAN number already registered");
            }
            throw new Exception("Database error: " + e.getMessage());
        }
    }

    public void updateCustomer(Customer c) throws Exception {
        if (!SessionManager.getInstance().hasPermission(2))
            throw new SecurityException("Insufficient permissions");
        validateCustomer(c);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE customers SET first_name=?,last_name=?,date_of_birth=?,email=?,phone=?," +
                 "address=?,city=?,state=?,pincode=?,country=?,aadhar_number=?,pan_number=? WHERE id=?")) {
            ps.setString(1, c.getFirstName());
            ps.setString(2, c.getLastName());
            ps.setString(3, c.getDateOfBirth());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getPhone());
            ps.setString(6, c.getAddress());
            ps.setString(7, c.getCity());
            ps.setString(8, c.getState());
            ps.setString(9, c.getPincode());
            ps.setString(10, c.getCountry());
            ps.setString(11, c.getAadharNumber());
            ps.setString(12, c.getPanNumber());
            ps.setInt(13, c.getId());
            ps.executeUpdate();
            audit.log("UPDATE_CUSTOMER", "CUSTOMER", c.getCustomerId(), null,
                c.getFullName(), "Updated customer details");
        }
    }

    public void updateKycStatus(int customerId, String status) throws Exception {
        if (!SessionManager.getInstance().hasPermission(4))
            throw new SecurityException("KYC update requires level 4+ permission");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE customers SET kyc_status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, customerId);
            ps.executeUpdate();
            audit.log("UPDATE_KYC", "CUSTOMER", String.valueOf(customerId),
                null, status, "KYC status updated to " + status);
        }
    }

    public void deactivateCustomer(int customerId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(6))
            throw new SecurityException("Deactivating customers requires level 6+ permission");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE customers SET is_active=0 WHERE id=?")) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
            audit.log("DEACTIVATE_CUSTOMER", "CUSTOMER", String.valueOf(customerId),
                "ACTIVE", "INACTIVE", "Customer deactivated");
        }
    }

    public Customer getById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM customers WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapCustomer(rs);
        }
        return null;
    }

    public Customer getByCustomerId(String customerId) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM customers WHERE customer_id=?")) {
            ps.setString(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapCustomer(rs);
        }
        return null;
    }

    // Binary search on pre-sorted customer list by customer_id
    public Customer binarySearchByCustomerId(List<Customer> sortedList, String customerId) {
        int idx = BankUtil.binarySearchById(sortedList, customerId, Customer::getCustomerId);
        return idx >= 0 ? sortedList.get(idx) : null;
    }

    public List<Customer> searchCustomers(String query) throws SQLException {
        List<Customer> results = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE is_active=1 AND (" +
                     "customer_id LIKE ? OR first_name LIKE ? OR last_name LIKE ? OR " +
                     "phone LIKE ? OR email LIKE ? OR pan_number LIKE ?) ORDER BY customer_id";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String q = "%" + query + "%";
            for (int i = 1; i <= 6; i++) ps.setString(i, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(mapCustomer(rs));
        }
        return results;
    }

    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM customers WHERE is_active=1 ORDER BY customer_id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapCustomer(rs));
        }
        return list;
    }

    public int getTotalCustomerCount() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers WHERE is_active=1");
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void validateCustomer(Customer c) throws Exception {
        if (c.getFirstName() == null || c.getFirstName().trim().isEmpty())
            throw new Exception("First name is required");
        if (c.getLastName() == null || c.getLastName().trim().isEmpty())
            throw new Exception("Last name is required");
        if (!BankUtil.isValidEmail(c.getEmail()))
            throw new Exception("Invalid email address: " + c.getEmail());
        if (!BankUtil.isValidPhone(c.getPhone()))
            throw new Exception("Invalid Indian phone number (must be 10 digits starting with 6-9)");
        if (c.getAadharNumber() != null && !c.getAadharNumber().isEmpty()
                && !BankUtil.isValidAadhar(c.getAadharNumber()))
            throw new Exception("Invalid Aadhar number (must be 12 digits)");
        if (c.getPanNumber() != null && !c.getPanNumber().isEmpty()
                && !BankUtil.isValidPAN(c.getPanNumber()))
            throw new Exception("Invalid PAN number format (e.g. ABCDE1234F)");
        if (c.getPincode() != null && !c.getPincode().isEmpty()
                && !BankUtil.isValidPincode(c.getPincode()))
            throw new Exception("Invalid pincode (must be 6 digits)");
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setCustomerId(rs.getString("customer_id"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setDateOfBirth(rs.getString("date_of_birth"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setCity(rs.getString("city"));
        c.setState(rs.getString("state"));
        c.setPincode(rs.getString("pincode"));
        c.setCountry(rs.getString("country"));
        c.setAadharNumber(rs.getString("aadhar_number"));
        c.setPanNumber(rs.getString("pan_number"));
        c.setKycStatus(rs.getString("kyc_status"));
        c.setActive(rs.getInt("is_active") == 1);
        c.setCreditScore(rs.getInt("credit_score"));
        String ca = rs.getString("created_at");
        if (ca != null) {
            try { c.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T"))); }
            catch (Exception ignored) {}
        }
        return c;
    }
}
