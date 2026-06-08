package vending.ui.customer;

import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class ProductCardPanel extends JPanel {

    public interface OnSelect {
        void onSelect(int index);
    }

    private static final int THUMB_W = 150;
    private static final int THUMB_H = 175;
    private static final int IMAGE_BOTTOM_PAD = 10;

    private final int index;
    private final JLabel nameLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel priceBadge = new JLabel("", SwingConstants.CENTER);
    private final JLabel stockLabel = new JLabel("", SwingConstants.CENTER);
    private final Color thumbColor;
    private Image productImage;
    private boolean soldOut;
    private boolean selected;
    private boolean affordable;

    public ProductCardPanel(int index, String name, int price, Color thumbColor, OnSelect onSelect) {
        this.index = index;
        this.thumbColor = thumbColor;
        reloadImage();
        setLayout(new BorderLayout(0, 6));
        setBackground(KioskColors.CARD);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 2, 4, 2));
        setPreferredSize(new Dimension(168, 280));
        setMinimumSize(new Dimension(160, 270));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        JPanel thumb = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawProduct(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        thumb.setLayout(new BorderLayout());
        thumb.setOpaque(false);
        thumb.setPreferredSize(new Dimension(THUMB_W, THUMB_H));
        thumb.setMinimumSize(new Dimension(THUMB_W, THUMB_H));

        priceBadge.setFont(KioskFonts.section());
        priceBadge.setForeground(KioskColors.BLUE_DARK);
        priceBadge.setOpaque(false);
        priceBadge.setText(price + "원");

        nameLabel.setText(name);
        nameLabel.setFont(KioskFonts.bodyBold());
        nameLabel.setForeground(KioskColors.TEXT);

        stockLabel.setFont(KioskFonts.small());
        stockLabel.setForeground(KioskColors.SUBTEXT);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        text.add(nameLabel);
        text.add(stockLabel);
        text.add(priceBadge);

        add(thumb, BorderLayout.CENTER);
        add(text, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!soldOut && isEnabled()) {
                    onSelect.onSelect(index);
                }
            }
        });
    }

    public void update(String name, int price, int stock, boolean soldOut, boolean selected, boolean affordable) {
        this.soldOut = soldOut;
        this.selected = selected;
        this.affordable = affordable;
        nameLabel.setText(name);
        priceBadge.setText(price + "원");
        reloadImage();

        if (soldOut) {
            stockLabel.setText("품절");
            stockLabel.setForeground(KioskColors.RED);
            nameLabel.setForeground(KioskColors.SUBTEXT);
            priceBadge.setForeground(KioskColors.SUBTEXT);
            setBackground(KioskColors.CARD);
            setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new Color(235, 238, 244)),
                    javax.swing.BorderFactory.createEmptyBorder(3, 1, 3, 1)));
            setCursor(java.awt.Cursor.getDefaultCursor());
            setEnabled(false);
        } else {
            stockLabel.setText("재고 " + stock + "개");
            stockLabel.setForeground(KioskColors.SUBTEXT);
            nameLabel.setForeground(KioskColors.TEXT);
            priceBadge.setForeground(KioskColors.BLUE_DARK);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            if (selected) {
                setBackground(new Color(245, 248, 255));
                setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        UiKit.selectedBorder(),
                        javax.swing.BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            } else {
                setBackground(KioskColors.CARD);
                setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 2, 4, 2));
            }
            setEnabled(true);
        }
        repaint();
    }

    private void drawProduct(Graphics2D g2, int width, int height) {
        if (productImage != null) {
            drawImage(g2, width, height);
        } else if (index == 2 || index == 4 || index == 7) {
            drawBottle(g2, width, height);
        } else if (index == 0 || index == 1) {
            drawCup(g2, width, height);
        } else {
            drawCan(g2, width, height);
        }

        if (affordable && !soldOut) {
            g2.setColor(new Color(35, 160, 92));
            g2.fill(new Ellipse2D.Float(10, 10, 14, 14));
        }

        if (soldOut) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(6, 6, width - 12, height - 12, 12, 12));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            int badgeW = 64;
            int badgeH = 26;
            int bx = (width - badgeW) / 2;
            int by = height / 2 - badgeH / 2 + 8;
            g2.setColor(new Color(224, 49, 49, 230));
            g2.fill(new RoundRectangle2D.Float(bx, by, badgeW, badgeH, 14, 14));
            g2.setColor(Color.WHITE);
            g2.setFont(KioskFonts.small().deriveFont(Font.BOLD));
            String text = "품절";
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, bx + (badgeW - tw) / 2, by + 18);
        }
    }

    private void reloadImage() {
        productImage = ProductImageLoader.load(index);
    }

    private void drawImage(Graphics2D g2, int width, int height) {
        int margin = 4;
        int areaW = width - margin * 2;
        int areaH = height - margin * 2;
        Image fitted = ProductImageLoader.fit(productImage, areaW, areaH);
        if (fitted == null) {
            return;
        }
        int drawW = fitted.getWidth(null);
        int drawH = fitted.getHeight(null);
        if (drawW <= 0 || drawH <= 0) {
            return;
        }
        int x = (width - drawW) / 2;
        int y = height - margin - IMAGE_BOTTOM_PAD - drawH;

        if (soldOut) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }
        g2.drawImage(fitted, x, y, drawW, drawH, null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawCan(Graphics2D g2, int width, int height) {
        int canW = 54;
        int canH = 84;
        int x = (width - canW) / 2;
        int y = 18;
        g2.setColor(thumbColor.darker());
        g2.fillOval(x, y, canW, 12);
        g2.setColor(thumbColor);
        g2.fillRoundRect(x, y + 6, canW, canH, 16, 16);
        g2.setColor(thumbColor.brighter());
        g2.fillRoundRect(x + 8, y + 18, canW - 16, 28, 10, 10);
        g2.setColor(Color.WHITE);
        drawCenterText(g2, labelText(), width, y + 60);
    }

    private void drawBottle(Graphics2D g2, int width, int height) {
        int x = width / 2 - 25;
        int y = 14;
        g2.setColor(thumbColor.darker());
        g2.fillRoundRect(x + 14, y, 22, 18, 8, 8);
        g2.setColor(thumbColor);
        g2.fillRoundRect(x + 4, y + 15, 44, 86, 16, 16);
        g2.setColor(new Color(255, 255, 255, 95));
        g2.fillRoundRect(x + 12, y + 25, 28, 34, 10, 10);
        g2.setColor(Color.WHITE);
        drawCenterText(g2, labelText(), width, y + 70);
    }

    private void drawCup(Graphics2D g2, int width, int height) {
        int x = width / 2 - 35;
        int y = 24;
        g2.setColor(new Color(245, 238, 224));
        g2.fillRoundRect(x, y, 70, 72, 12, 12);
        g2.setColor(thumbColor);
        g2.fillRoundRect(x + 5, y + 18, 60, 45, 10, 10);
        g2.setColor(new Color(120, 80, 45));
        g2.drawArc(x + 12, y - 10, 40, 20, 180, 180);
        g2.setColor(Color.WHITE);
        drawCenterText(g2, labelText(), width, y + 50);
    }

    private String labelText() {
        switch (index) {
            case 0: return "MIX";
            case 1: return "PREM";
            case 2: return "WATER";
            case 3: return "CAN";
            case 4: return "ION";
            case 5: return "COFFEE";
            case 6: return "SODA";
            default: return "DRINK";
        }
    }

    private void drawCenterText(Graphics2D g2, String text, int width, int y) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        int textW = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (width - textW) / 2, y);
    }
}
