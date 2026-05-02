package com.bankpro.ui;

import com.bankpro.security.SessionManager;
import com.bankpro.ui.panels.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainFrame extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel clockLabel;
    private JButton activeNavBtn = null;

    // Panel keys
    private static final String[] PAGES = {
        "Dashboard","Customers","Accounts","Deposit","Withdrawal",
        "Transfer","Loans","FixedDeposit","Cards","FX","AuditLog","Users","Settings"
    };

    public MainFrame() {
        setTitle("BankPro — Banking Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1280, 720));
        Theme.applyGlobalDefaults();
        buildUI();
        startClock();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_DARK);
        setContentPane(root);

        // buildContent() MUST run first — it initialises cardLayout/contentPanel
        // which buildSidebar() depends on via setActivePage()
        JPanel content = buildContent();
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_CARD);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, Theme.BORDER_COLOR),
            new EmptyBorder(10, 20, 10, 20)));
        bar.setPreferredSize(new Dimension(0, 56));

        // Left: logo
        JLabel logo = new JLabel("🏛  BankPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setForeground(Theme.ACCENT_GOLD);

        // Right: user + clock + logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);

        clockLabel = new JLabel();
        clockLabel.setFont(Theme.FONT_MONO);
        clockLabel.setForeground(Theme.TEXT_MUTED);

        SessionManager s = SessionManager.getInstance();
        String userName = s.getCurrentUser().getFullName();
        String level = s.getCurrentUser().getPermissionLabel();

        JLabel userInfo = new JLabel("👤 " + userName + "  •  " + level);
        userInfo.setFont(Theme.FONT_BODY);
        userInfo.setForeground(Theme.TEXT_SECONDARY);

        JButton logoutBtn = Theme.ghostButton("⏻  Logout");
        logoutBtn.addActionListener(e -> {
            if (Theme.showConfirm(this, "Are you sure you want to logout?")) {
                SessionManager.getInstance().logout();
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        right.add(clockLabel);
        right.add(new JSeparator(SwingConstants.VERTICAL));
        right.add(userInfo);
        right.add(logoutBtn);

        bar.add(logo, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER_COLOR));
        sidebar.setPreferredSize(new Dimension(220, 0));

        String[][] navItems = {
            {"Dashboard",    "📊", "Dashboard"},
            {"Parties",      "🏢", "Parties"},
            {"Accounts",     "🏦", "Accounts"},
            {"Deposit",      "💰", "Deposit"},
            {"Withdrawal",   "💸", "Withdrawal"},
            {"Transfer",     "🔄", "Transfer"},
            {"Loans",        "📋", "Loans"},
            {"FixedDeposit", "📈", "Fixed Deposit"},
            {"Cards",        "💳", "Cards"},
            {"FX",           "🌐", "Forex / SWIFT"},
            {"Interest",     "📐", "Interest Mgmt"},
            {"GL",           "📒", "General Ledger"},
            {"AuditLog",     "📜", "Audit Log"},
            {"Users",        "🔒", "User Mgmt"},
            {"Settings",     "⚙️",  "Settings"},
        };

        sidebar.add(Box.createVerticalStrut(10));

        int userLevel = SessionManager.getInstance().getPermissionLevel();

        for (String[] item : navItems) {
            String key = item[0];
            String icon = item[1];
            String label = item[2];

            // Permission-gated items
            if ("AuditLog".equals(key) && userLevel < 4) continue;
            if ("GL".equals(key) && userLevel < 4) continue;
            if ("Interest".equals(key) && userLevel < 4) continue;
            if ("Users".equals(key) && userLevel < 8) continue;

            JButton btn = createNavButton(icon + "  " + label, key);
            sidebar.add(btn);

            if ("Dashboard".equals(key)) {
                setActivePage(btn, key);
            }
        }

        sidebar.add(Box.createVerticalGlue());

        // Version info
        JLabel ver = new JLabel("BankPro v1.0  •  Level " + userLevel,
            SwingConstants.CENTER);
        ver.setFont(Theme.FONT_SMALL);
        ver.setForeground(Theme.TEXT_MUTED);
        ver.setBorder(new EmptyBorder(8, 0, 12, 0));
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(ver);

        return sidebar;
    }

    private JButton createNavButton(String label, String pageKey) {
        JButton btn = new JButton(label);
        btn.setFont(Theme.FONT_BODY);
        btn.setForeground(Theme.TEXT_SECONDARY);
        btn.setBackground(Theme.BG_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(12, 20, 12, 20));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn != activeNavBtn) btn.setBackground(Theme.HOVER_BG);
            }
            public void mouseExited(MouseEvent e) {
                if (btn != activeNavBtn) btn.setBackground(Theme.BG_SIDEBAR);
            }
        });

        btn.addActionListener(e -> setActivePage(btn, pageKey));
        return btn;
    }

    private void setActivePage(JButton btn, String pageKey) {
        if (activeNavBtn != null) {
            activeNavBtn.setBackground(Theme.BG_SIDEBAR);
            activeNavBtn.setForeground(Theme.TEXT_SECONDARY);
        }
        activeNavBtn = btn;
        btn.setBackground(new Color(30, 58, 95));
        btn.setForeground(Theme.ACCENT_BLUE);
        cardLayout.show(contentPanel, pageKey);
    }

    private JPanel buildContent() {
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.BG_DARK);

        contentPanel.add(new DashboardPanel(), "Dashboard");
        contentPanel.add(new PartyPanel(), "Parties");
        contentPanel.add(new AccountPanel(), "Accounts");
        contentPanel.add(new DepositPanel(), "Deposit");
        contentPanel.add(new WithdrawalPanel(), "Withdrawal");
        contentPanel.add(new TransferPanel(), "Transfer");
        contentPanel.add(new LoanPanel(), "Loans");
        contentPanel.add(new FixedDepositPanel(), "FixedDeposit");
        contentPanel.add(new CardPanel(), "Cards");
        contentPanel.add(new ForexPanel(), "FX");
        contentPanel.add(new InterestPanel(), "Interest");
        contentPanel.add(new GeneralLedgerPanel(), "GL");
        contentPanel.add(new AuditLogPanel(), "AuditLog");
        contentPanel.add(new UserManagementPanel(), "Users");
        contentPanel.add(new SettingsPanel(), "Settings");

        return contentPanel;
    }

    private void startClock() {
        Timer t = new Timer(1000, e -> {
            clockLabel.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss")));
        });
        t.start();
        t.getActionListeners()[0].actionPerformed(null);
    }
}
