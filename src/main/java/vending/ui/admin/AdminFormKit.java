package vending.ui.admin;

import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

public final class AdminFormKit {

    private AdminFormKit() {
    }

    public static JPanel page(String emoji, String title, String guide, JPanel body) {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);

        JPanel hero = new JPanel(new BorderLayout(12, 0));
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JLabel icon = new JLabel(emoji, SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        icon.setPreferredSize(new Dimension(48, 48));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(KioskFonts.section());
        titleLabel.setForeground(KioskColors.NAVY);

        JLabel guideLabel = new JLabel("<html>" + guide + "</html>");
        guideLabel.setFont(KioskFonts.small());
        guideLabel.setForeground(KioskColors.SUBTEXT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(guideLabel);

        hero.add(icon, BorderLayout.WEST);
        hero.add(text, BorderLayout.CENTER);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(KioskColors.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)));
        card.add(body, BorderLayout.CENTER);

        page.add(hero, BorderLayout.NORTH);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    public static JPanel formBody(Component... rows) {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setMaximumSize(new Dimension(480, 800));
        for (Component row : rows) {
            body.add(row);
            body.add(Box.createVerticalStrut(14));
        }
        return body;
    }

    public static JPanel splitBody(JPanel form, JPanel side) {
        JPanel split = new JPanel(new BorderLayout(28, 0));
        split.setOpaque(false);
        split.add(form, BorderLayout.WEST);
        split.add(side, BorderLayout.CENTER);
        return split;
    }

    public static JPanel sidePanel(String title, Component content) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(true);
        panel.setPreferredSize(new Dimension(280, 10));
        panel.setBackground(new Color(248, 250, 253));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JLabel t = new JLabel(title);
        t.setFont(KioskFonts.bodyBold());
        t.setForeground(KioskColors.NAVY);
        panel.add(t, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public static JTextArea noteArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFont(KioskFonts.small());
        area.setForeground(KioskColors.SUBTEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return area;
    }

    public static JPanel fieldRow(String labelText, JComponent field, String hint) {
        JPanel row = new JPanel(new BorderLayout(0, 6));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(480, hint == null ? 72 : 92));

        JLabel label = new JLabel(labelText);
        label.setFont(KioskFonts.bodyBold());
        label.setForeground(KioskColors.NAVY);
        styleField(field);
        row.add(label, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        if (hint != null && !hint.isBlank()) {
            JLabel sub = new JLabel(hint);
            sub.setFont(KioskFonts.small());
            sub.setForeground(KioskColors.SUBTEXT);
            row.add(sub, BorderLayout.SOUTH);
        }
        return row;
    }

    public static JPanel infoBox(String title, JLabel valueLabel) {
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setBackground(new Color(248, 250, 253));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        box.setMaximumSize(new Dimension(480, 90));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(KioskFonts.small());
        t.setForeground(KioskColors.SUBTEXT);
        valueLabel.setFont(KioskFonts.section());
        valueLabel.setForeground(KioskColors.NAVY);
        box.add(t, BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);
        return box;
    }

    public static JPanel actionRow(JButton button, JLabel status) {
        JPanel row = new JPanel(new BorderLayout(0, 10));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(480, 90));
        button.setPreferredSize(new Dimension(200, 44));
        button.setMaximumSize(new Dimension(220, 44));
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(button);
        status.setFont(KioskFonts.small());
        row.add(btnWrap, BorderLayout.NORTH);
        row.add(status, BorderLayout.SOUTH);
        return row;
    }

    public static JScrollPane summaryArea(JTextArea area) {
        area.setEditable(false);
        area.setFont(KioskFonts.body());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(18);
        area.setColumns(52);
        area.setBackground(new Color(248, 250, 253));
        area.setForeground(KioskColors.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(KioskColors.BORDER));
        scroll.setPreferredSize(new Dimension(10, 360));
        return scroll;
    }

    public static JPanel previewPanel(JLabel preview, int w, int h) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(w + 24, h + 24));
        panel.setMaximumSize(new Dimension(w + 24, h + 24));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(new Color(248, 250, 253));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setVerticalAlignment(SwingConstants.CENTER);
        preview.setPreferredSize(new Dimension(w, h));
        panel.add(preview, BorderLayout.CENTER);
        return panel;
    }

    public static void styleField(JComponent field) {
        field.setFont(KioskFonts.body());
        if (field instanceof JTextField) {
            JTextField tf = (JTextField) field;
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(KioskColors.BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            tf.setPreferredSize(new Dimension(360, 42));
            tf.setMaximumSize(new Dimension(420, 42));
        } else if (field instanceof JPasswordField) {
            JPasswordField pf = (JPasswordField) field;
            pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(KioskColors.BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            pf.setPreferredSize(new Dimension(360, 42));
            pf.setMaximumSize(new Dimension(420, 42));
        } else if (field instanceof JComboBox) {
            JComboBox<?> box = (JComboBox<?>) field;
            box.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(KioskColors.BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            box.setPreferredSize(new Dimension(360, 42));
            box.setMaximumSize(new Dimension(420, 42));
        }
    }

    public static JPanel statusBadge(String text, boolean ok) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badge.setOpaque(true);
        badge.setBackground(new Color(255, 255, 255, ok ? 18 : 12));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 35)),
                BorderFactory.createEmptyBorder(10, 8, 2, 8)));

        JLabel dot = new JLabel("●");
        dot.setFont(KioskFonts.small());
        dot.setForeground(ok ? KioskColors.GREEN : new Color(230, 120, 30));

        JLabel label = new JLabel(text);
        label.setFont(KioskFonts.small());
        label.setForeground(new Color(190, 205, 225));

        badge.add(dot);
        badge.add(label);
        return badge;
    }
}
