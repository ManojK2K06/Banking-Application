package com.bankpro.security;

import com.bankpro.model.User;
import com.bankpro.db.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private String sessionId;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        this.sessionId = PasswordUtil.generateSecureToken();
        this.loginTime = LocalDateTime.now();
        persistSession();
    }

    public void logout() {
        if (sessionId != null) {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE sessions SET logout_at = CURRENT_TIMESTAMP, is_active = 0 WHERE session_id = ?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            } catch (SQLException e) { /* log silently */ }
        }
        this.currentUser = null;
        this.sessionId = null;
        this.loginTime = null;
    }

    private void persistSession() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO sessions (user_id, session_id) VALUES (?, ?)")) {
            ps.setInt(1, currentUser.getId());
            ps.setString(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) { /* log silently */ }
    }

    public boolean isLoggedIn() { return currentUser != null; }
    public User getCurrentUser() { return currentUser; }
    public String getSessionId() { return sessionId; }
    public LocalDateTime getLoginTime() { return loginTime; }

    public boolean hasPermission(int requiredLevel) {
        return currentUser != null && currentUser.getPermissionLevel() >= requiredLevel;
    }

    public int getPermissionLevel() {
        return currentUser != null ? currentUser.getPermissionLevel() : 0;
    }
}
