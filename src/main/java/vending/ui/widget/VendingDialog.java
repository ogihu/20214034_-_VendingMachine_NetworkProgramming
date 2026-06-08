package vending.ui.widget;

import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Ellipse2D;

public final class VendingDialog {

    private VendingDialog() {
    }

    public static void showResult(Component parent, String title, String message, boolean success) {
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        JPanel shell = new JPanel(new BorderLayout(18, 16));
        shell.setBackground(Color.WHITE);
        shell.setBorder(BorderFactory.createCompoundBorder(
                UiKit.selectedBorder(),
                BorderFactory.createEmptyBorder(26, 30, 24, 30)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(KioskFonts.title());
        titleLabel.setForeground(success ? KioskColors.BLUE_DARK : KioskColors.RED);

        JLabel msgLabel = new JLabel("<html><body style='width: 330px'>" + message + "</body></html>");
        msgLabel.setFont(KioskFonts.body());
        msgLabel.setForeground(KioskColors.TEXT);

        JPanel text = new JPanel(new BorderLayout(0, 8));
        text.setOpaque(false);
        text.add(titleLabel, BorderLayout.NORTH);
        text.add(msgLabel, BorderLayout.CENTER);

        JPanel icon = new ResultIcon(success);
        icon.setPreferredSize(new Dimension(62, 62));
        icon.setOpaque(false);

        JButton ok = UiKit.primaryButton("확인");
        ok.setPreferredSize(new Dimension(130, 42));
        ok.addActionListener(e -> dialog.dispose());

        JPanel buttonWrap = new JPanel(new BorderLayout());
        buttonWrap.setOpaque(false);
        buttonWrap.add(ok, BorderLayout.EAST);

        shell.add(icon, BorderLayout.WEST);
        shell.add(text, BorderLayout.CENTER);
        shell.add(buttonWrap, BorderLayout.SOUTH);

        dialog.setContentPane(shell);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static class ResultIcon extends JPanel {
        private final boolean success;

        ResultIcon(boolean success) {
            this.success = success;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = success ? KioskColors.BLUE : KioskColors.RED;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 35));
            g2.fill(new Ellipse2D.Float(2, 2, getWidth() - 4, getHeight() - 4));
            g2.setColor(color);
            g2.setStroke(new java.awt.BasicStroke(4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            if (success) {
                g2.drawLine(18, 32, 28, 42);
                g2.drawLine(28, 42, 46, 22);
            } else {
                g2.drawLine(20, 20, 42, 42);
                g2.drawLine(42, 20, 20, 42);
            }
            g2.dispose();
        }
    }
}
