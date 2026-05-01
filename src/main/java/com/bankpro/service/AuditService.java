package com.bankpro.service;

import com.bankpro.db.DatabaseManager;
import com.bankpro.security.SessionManager;
import java.sql.*;
import java.util.logging.Logger;

public class AuditService {
    private static final Logger logger = Logger.getLogger(AuditService.class.getName());
    private static AuditService instance;

    private AuditService() {}

    public static synchronized AuditService getInstance() {
        if (instance == null) instance = new AuditService();
        return instance;
    }

    public void log(String action, String entityType, String entityId, String oldValue, String newValue, String details) {
        int userId = SessionManager.getInstance().isLoggedIn()
            ? SessionManager.getInstance().getCurrentUser().getId() : 0;
        String sessionId = SessionManager.getInstance().getSessionId();
        logWithUser(userId, sessionId, action, entityType, entityId, oldValue, newValue, "SUCCESS", details);
    }

    public void logFailure(String action, String entityType, String entityId, String details) {
        int userId = SessionManager.getInstance().isLoggedIn()
            ? SessionManager.getInstance().getCurrentUser().getId() : 0;
        String sessionId = SessionManager.getInstance().getSessionId();
        logWithUser(userId, sessionId, action, entityType, entityId, null, null, "FAILURE", details);
    }

    public void logLogin(int userId, boolean success, String details) {
        logWithUser(userId, null, "LOGIN", "USER", String.valueOf(userId),
            null, null, success ? "SUCCESS" : "FAILURE", details);
    }

    public void logLogout(int userId, String sessionId) {
        logWithUser(userId, sessionId, "LOGOUT", "USER", String.valueOf(userId),
            null, null, "SUCCESS", "User logged out");
    }

    private void logWithUser(int userId, String sessionId, String action, String entityType,
                              String entityId, String oldValue, String newValue,
                              String status, String details) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO audit_log (user_id, action, entity_type, entity_id, old_value, new_value, " +
                 "session_id, status, details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            // Use NULL for system/pre-login actions to avoid FK violation
            if (userId > 0) ps.setInt(1, userId);
            else            ps.setNull(1, java.sql.Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setString(4, entityId);
            ps.setString(5, oldValue);
            ps.setString(6, newValue);
            ps.setString(7, sessionId);
            ps.setString(8, status);
            ps.setString(9, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Audit log failed: " + e.getMessage());
        }
    }
}
