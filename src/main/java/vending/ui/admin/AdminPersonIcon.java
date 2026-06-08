package vending.ui.admin;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

public class AdminPersonIcon extends JPanel {

    public AdminPersonIcon() {
        setOpaque(false);
        setPreferredSize(new Dimension(56, 56));
        setMaximumSize(new Dimension(56, 56));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2.2f));

        int cx = getWidth() / 2;
        g2.fill(new Ellipse2D.Float(cx - 11, 10, 22, 22));
        g2.fillRoundRect(cx - 18, 34, 36, 18, 14, 14);
        g2.dispose();
    }
}
