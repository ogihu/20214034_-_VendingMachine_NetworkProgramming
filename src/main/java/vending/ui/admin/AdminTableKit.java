package vending.ui.admin;

import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

public final class AdminTableKit {

    private AdminTableKit() {
    }

    public static void style(JTable table) {
        table.setFont(KioskFonts.body());
        table.setRowHeight(40);
        table.setGridColor(new Color(240, 243, 248));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(237, 242, 255));
        table.setSelectionForeground(KioskColors.TEXT);
        table.setBackground(KioskColors.CARD);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(KioskFonts.bodyBold());
        header.setBackground(new Color(248, 250, 253));
        header.setForeground(KioskColors.NAVY);
        header.setPreferredSize(new Dimension(10, 36));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
    }

    public static JScrollPane wrap(JTable table) {
        style(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(KioskColors.BORDER));
        scroll.getViewport().setBackground(KioskColors.CARD);
        return scroll;
    }

    public static JPanel panel(String title, JTable table) {
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 8));
        panel.setBackground(KioskColors.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel label = new JLabel(title);
        label.setFont(KioskFonts.bodyBold());
        label.setForeground(KioskColors.NAVY);
        panel.add(label, java.awt.BorderLayout.NORTH);
        panel.add(wrap(table), java.awt.BorderLayout.CENTER);
        return panel;
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBackground(new Color(248, 250, 253));
            label.setForeground(KioskColors.NAVY);
            label.setFont(KioskFonts.bodyBold());
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, KioskColors.BORDER));
            return label;
        }
    }
}
