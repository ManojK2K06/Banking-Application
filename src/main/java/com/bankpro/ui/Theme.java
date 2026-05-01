package com.bankpro.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class Theme {
    // Color palette - Dark Professional Banking Theme
    public static final Color BG_DARK       = new Color(15, 23, 42);       // Deep navy
    public static final Color BG_CARD       = new Color(30, 41, 59);       // Card background
    public static final Color BG_SIDEBAR    = new Color(17, 24, 39);       // Sidebar
    public static final Color BG_INPUT      = new Color(51, 65, 85);       // Input background
    public static final Color ACCENT_GOLD   = new Color(245, 158, 11);     // Gold accent
    public static final Color ACCENT_BLUE   = new Color(59, 130, 246);     // Electric blue
    public static final Color ACCENT_GREEN  = new Color(16, 185, 129);     // Emerald
    public static final Color ACCENT_RED    = new Color(239, 68, 68);      // Error red
    public static final Color ACCENT_ORANGE = new Color(249, 115, 22);     // Warning
    public static final Color TEXT_PRIMARY  = new Color(241, 245, 249);    // White-ish
    public static final Color TEXT_SECONDARY= new Color(148, 163, 184);    // Muted text
    public static final Color TEXT_MUTED    = new Color(100, 116, 139);    // Very muted
    public static final Color BORDER_COLOR  = new Color(51, 65, 85);       // Border
    public static final Color TABLE_ALT     = new Color(30, 41, 59);       // Table alt row
    public static final Color HEADER_BG     = new Color(30, 41, 59);       // Table header
    public static final Color HOVER_BG      = new Color(55, 65, 81);       // Hover state

    // Fonts
    public static final Font FONT_TITLE     = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE  = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_HEADING   = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY      = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL     = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO      = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_AMOUNT    = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_LABEL     = new Font("Segoe UI", Font.PLAIN, 12);

    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT_BLUE);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_HEADING);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setOpaque(true);
        addHoverEffect(btn, ACCENT_BLUE, new Color(37, 99, 235));
        return btn;
    }

    public static JButton successButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT_GREEN);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_HEADING);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setOpaque(true);
        addHoverEffect(btn, ACCENT_GREEN, new Color(5, 150, 105));
        return btn;
    }

    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT_RED);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_HEADING);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setOpaque(true);
        addHoverEffect(btn, ACCENT_RED, new Color(185, 28, 28));
        return btn;
    }

    public static JButton warningButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT_GOLD);
        btn.setForeground(new Color(15, 23, 42));
        btn.setFont(FONT_HEADING);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setOpaque(true);
        addHoverEffect(btn, ACCENT_GOLD, new Color(217, 119, 6));
        return btn;
    }

    public static JButton ghostButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_INPUT);
        btn.setForeground(TEXT_SECONDARY);
        btn.setFont(FONT_BODY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setOpaque(true);
        addHoverEffect(btn, BG_INPUT, HOVER_BG);
        return btn;
    }

    private static void addHoverEffect(JButton btn, Color normal, Color hover) {
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(normal); }
        });
    }

    public static JTextField styledField() {
        JTextField f = new JTextField();
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return f;
    }

    public static JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField();
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return f;
    }

    public static JTextArea styledTextArea() {
        JTextArea ta = new JTextArea();
        ta.setBackground(BG_INPUT);
        ta.setForeground(TEXT_PRIMARY);
        ta.setCaretColor(TEXT_PRIMARY);
        ta.setFont(FONT_BODY);
        ta.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    public static <T> JComboBox<T> styledCombo() {
        JComboBox<T> cb = new JComboBox<>();
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? ACCENT_BLUE : BG_INPUT);
                setForeground(isSelected ? Color.WHITE : TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        return cb;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_SECONDARY);
        l.setFont(FONT_LABEL);
        return l;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_PRIMARY);
        l.setFont(FONT_SUBTITLE);
        return l;
    }

    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_PRIMARY);
        l.setFont(FONT_TITLE);
        return l;
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        return p;
    }

    public static JScrollPane styledScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(BG_DARK);
        sp.getViewport().setBackground(BG_DARK);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        sp.getVerticalScrollBar().setBackground(BG_CARD);
        sp.getHorizontalScrollBar().setBackground(BG_CARD);
        return sp;
    }

    public static JTable styledTable(String[] cols, Object[][] data) {
        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(table);
        return table;
    }

    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(59, 130, 246, 80));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_BG);
        header.setForeground(TEXT_SECONDARY);
        header.setFont(FONT_SMALL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setReorderingAllowed(false);

        // Alternating rows renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? new Color(59, 130, 246, 80) :
                    (row % 2 == 0 ? BG_CARD : BG_DARK));
                setForeground(sel ? TEXT_PRIMARY : TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setFont(FONT_BODY);
                return this;
            }
        });
    }

    public static JPanel statCard(String title, String value, String subtitle, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 3, 0, 0, accentColor),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(FONT_SMALL);
        lTitle.setForeground(TEXT_MUTED);

        JLabel lValue = new JLabel(value);
        lValue.setFont(FONT_AMOUNT);
        lValue.setForeground(accentColor);

        JLabel lSub = new JLabel(subtitle);
        lSub.setFont(FONT_SMALL);
        lSub.setForeground(TEXT_MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lTitle, BorderLayout.WEST);

        card.add(top, BorderLayout.NORTH);
        card.add(lValue, BorderLayout.CENTER);
        card.add(lSub, BorderLayout.SOUTH);
        return card;
    }

    public static void applyGlobalDefaults() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background", ACCENT_BLUE);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("ScrollBar.thumb", BG_INPUT);
        UIManager.put("ScrollBar.track", BG_DARK);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean showConfirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, "Confirm",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
