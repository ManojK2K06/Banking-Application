package com.bankpro;

import com.bankpro.db.DatabaseManager;
import com.bankpro.ui.LoginFrame;
import com.bankpro.ui.Theme;
import javax.swing.*;
import java.util.logging.*;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Configure logging
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        StreamHandler handler = new StreamHandler(System.out, new SimpleFormatter());
        handler.setLevel(Level.INFO);
        root.addHandler(handler);

        logger.info("BankPro starting...");

        // Init DB
        try {
            DatabaseManager.getInstance().initializeDatabase();
            logger.info("Database ready.");
        } catch (Exception e) {
            logger.severe("Database init failed: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                "Failed to initialize database:\n" + e.getMessage(),
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Launch UI on EDT
        SwingUtilities.invokeLater(() -> {
            Theme.applyGlobalDefaults();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            LoginFrame login = new LoginFrame();
            login.setVisible(true);
            logger.info("BankPro UI launched. Default login: admin / Admin@1234");
        });
    }
}
