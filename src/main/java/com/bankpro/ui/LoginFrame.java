package com.bankpro.ui;

import com.bankpro.model.User;
import com.bankpro.security.SessionManager;
import com.bankpro.service.UserService;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JLabel statusLabel;
    private int failedAttempts = 0;
    private long lockoutUntil = 0;

    public LoginFrame() {
        setTitle("BankPro — Employee Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 620);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BG_DARK);
        setContentPane(root);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(Theme.BORDER_COLOR, 1),
            new EmptyBorder(40, 48, 40, 48)));
        card.setMaximumSize(new Dimension(380, 500));

        // Logo / Bank name
        JLabel logo = new JLabel("🏛", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bankName = new JLabel("BankPro", SwingConstants.CENTER);
        bankName.setFont(new Font("Segoe UI", Font.BOLD, 28));
        bankName.setForeground(Theme.ACCENT_GOLD);
        bankName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Employee Banking Portal", SwingConstants.CENTER);
        tagline.setFont(Theme.FONT_BODY);
        tagline.setForeground(Theme.TEXT_MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Fields
        JLabel userLbl = Theme.label("Username");
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField = Theme.styledField();
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = Theme.label("Password");
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField = Theme.styledPasswordField();
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginBtn = Theme.primaryButton("Sign In");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.ACCENT_RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Default: admin / Admin@1234", SwingConstants.CENTER);
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(8));
        card.add(bankName);
        card.add(Box.createVerticalStrut(4));
        card.add(tagline);
        card.add(Box.createVerticalStrut(32));
        card.add(userLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));
        card.add(passLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(24));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(hint);

        root.add(card);

        // Actions
        loginBtn.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        getRootPane().setDefaultButton(loginBtn);
    }

    private void doLogin() {
        long now = System.currentTimeMillis();
        if (now < lockoutUntil) {
            long remaining = (lockoutUntil - now) / 1000;
            statusLabel.setText("Account locked. Try again in " + remaining + "s");
            return;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password are required");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Signing in...");
        statusLabel.setText("");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                return UserService.getInstance().authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    failedAttempts = 0;
                    SessionManager.getInstance().login(user);
                    dispose();
                    SwingUtilities.invokeLater(() -> {
                        MainFrame mainFrame = new MainFrame();
                        mainFrame.setVisible(true);
                    });
                } catch (Exception ex) {
                    failedAttempts++;
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    if (failedAttempts >= 5) {
                        lockoutUntil = System.currentTimeMillis() + 30_000;
                        statusLabel.setText("5 failed attempts. Locked for 30 seconds.");
                        failedAttempts = 0;
                    } else {
                        statusLabel.setText(msg + " (" + (5 - failedAttempts) + " attempts left)");
                    }
                    passwordField.setText("");
                    passwordField.requestFocus();
                } finally {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Sign In");
                }
            }
        };
        worker.execute();
    }
}
