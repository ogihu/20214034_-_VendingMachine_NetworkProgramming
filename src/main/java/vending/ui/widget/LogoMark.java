package vending.ui.widget;

import vending.ui.theme.KioskColors;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public class LogoMark extends JPanel {

    public LogoMark() {
        setOpaque(false);
        setPreferredSize(new Dimension(28, 28));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(KioskColors.NAVY);
        g2.fill(new RoundRectangle2D.Float(2, 4, 22, 18, 6, 6));
        g2.setColor(KioskColors.BLUE);
        g2.fill(new RoundRectangle2D.Float(6, 8, 14, 10, 4, 4));
        g2.dispose();
    }
}
