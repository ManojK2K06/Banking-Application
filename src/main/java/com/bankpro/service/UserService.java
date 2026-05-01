package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.model.User;
import com.bankpro.security.PasswordUtil;
import com.bankpro.security.SessionManager;
import com.bankpro.util.BankUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UserService {
    private static UserService instance;
    private final AuditService audit = AuditService.getInstance();

    private UserService() {}
    public static synchronized UserService getInstance() {
        if (instance == null) instance = new UserService();
        return instance;
    }

    public User authenticate(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty())
            throw new Exception("Username is required");
        if (password == null || password.isEmpty())
            throw new Exception("Password is required");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM users WHERE username=? AND is_active=1")) {
            ps.setString(1, username.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                audit.logLogin(0, false, "Unknown user: " + username);
                throw new Exception("Invalid username or password");
            }

            String hash = rs.getString("password_hash");
            String salt = rs.getString("salt");

            if (!PasswordUtil.verifyPassword(password, salt, hash)) {
                audit.logLogin(rs.getInt("id"), false, "Wrong password for: " + username);
                throw new Exception("Invalid username or password");
            }

            User user = mapUser(rs);

            // Update last login
            conn.prepareStatement(
                "UPDATE users SET last_login=CURRENT_TIMESTAMP WHERE id=" + user.getId()
            ).executeUpdate();

            audit.logLogin(user.getId(), true, "Logged in: " + username);
            return user;
        }
    }

    public User createUser(String username, String password, String fullName, String email,
                            String phone, int permissionLevel, String department) throws Exception {
        if (!SessionManager.getInstance().hasPermission(8))
            throw new SecurityException("Creating users requires level 8+ permission");
        if (permissionLevel >= SessionManager.getInstance().getPermissionLevel())
            throw new SecurityException("Cannot create user with equal or higher permission than yourself");
        if (!PasswordUtil.isStrongPassword(password))
            throw new Exception("Password must be 8+ chars with uppercase, lowercase, digit, and special character");
        if (!BankUtil.isValidEmail(email))
            throw new Exception("Invalid email: " + email);

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username,password_hash,salt,full_name,email,phone," +
                 "permission_level,department,created_by,employee_id) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, fullName);
            ps.setString(5, email);
            ps.setString(6, phone);
            ps.setInt(7, permissionLevel);
            ps.setString(8, department);
            ps.setInt(9, SessionManager.getInstance().getCurrentUser().getId());
            ps.setString(10, "TEMP");
            ps.executeUpdate();

            ResultSet rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()");
            User user = new User();
            if (rs.next()) {
                int newId = rs.getInt(1);
                user.setId(newId);
                // Update with real employee ID
                String empId = BankUtil.generateEmployeeId(newId);
                conn.prepareStatement("UPDATE users SET employee_id='" + empId + "' WHERE id=" + newId).executeUpdate();
                user.setEmployeeId(empId);
            }
            user.setUsername(username);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPermissionLevel(permissionLevel);
            user.setDepartment(department);
            user.setActive(true);

            audit.log("CREATE_USER", "USER", username, null, "Level " + permissionLevel,
                "Created user: " + fullName);
            return user;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                if (e.getMessage() != null && e.getMessage().contains("username")) throw new Exception("Username already exists: " + username);
                if (e.getMessage() != null && e.getMessage().contains("email")) throw new Exception("Email already in use: " + email);
            }
            throw new Exception("Database error: " + e.getMessage());
        }
    }

    public void updatePermission(int userId, int newLevel) throws Exception {
        if (!SessionManager.getInstance().hasPermission(10))
            throw new SecurityException("Only level 10 (Admin/CTO) can change permissions");
        if (userId == SessionManager.getInstance().getCurrentUser().getId())
            throw new Exception("Cannot change your own permission level");
        if (newLevel < 1 || newLevel > 10)
            throw new Exception("Permission level must be 1 to 10");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE users SET permission_level=? WHERE id=?")) {
            ps.setInt(1, newLevel);
            ps.setInt(2, userId);
            ps.executeUpdate();
            audit.log("UPDATE_PERMISSION", "USER", String.valueOf(userId),
                null, "Level " + newLevel, "Permission updated to " + newLevel);
        }
    }

    public void resetPassword(int userId, String newPassword) throws Exception {
        if (!SessionManager.getInstance().hasPermission(8))
            throw new SecurityException("Password reset requires level 8+ permission");
        if (!PasswordUtil.isStrongPassword(newPassword))
            throw new Exception("Password must be 8+ chars with uppercase, lowercase, digit, and special character");

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newPassword, salt);

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE users SET password_hash=?, salt=? WHERE id=?")) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            ps.executeUpdate();
            audit.log("RESET_PASSWORD", "USER", String.valueOf(userId),
                null, null, "Password reset");
        }
    }

    public void deactivateUser(int userId) throws Exception {
        if (!SessionManager.getInstance().hasPermission(9))
            throw new SecurityException("Deactivating users requires level 9+ permission");
        if (userId == SessionManager.getInstance().getCurrentUser().getId())
            throw new Exception("Cannot deactivate yourself");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE users SET is_active=0 WHERE id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            audit.log("DEACTIVATE_USER", "USER", String.valueOf(userId),
                "ACTIVE", "INACTIVE", "User deactivated");
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM users ORDER BY permission_level DESC, full_name");
            while (rs.next()) list.add(mapUser(rs));
        }
        return list;
    }

    public User getById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setPermissionLevel(rs.getInt("permission_level"));
        u.setActive(rs.getInt("is_active") == 1);
        u.setDepartment(rs.getString("department"));
        u.setEmployeeId(rs.getString("employee_id"));
        String ll = rs.getString("last_login");
        if (ll != null) {
            try { u.setLastLogin(LocalDateTime.parse(ll.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        String ca = rs.getString("created_at");
        if (ca != null) {
            try { u.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        return u;
    }
}
