package vending.ui.customer;

import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 상품 1개 카드.
 */
public class ProductCardPanel extends JPanel {

    public interface OnSelect {
        void onSelect(int index);
    }

    private final int index;
    private final JLabel nameLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel priceBadge = new JLabel("", SwingConstants.CENTER);
    private final JLabel stockLabel = new JLabel("", SwingConstants.CENTER);
    private final Color thumbColor;
    private boolean soldOut;

    public ProductCardPanel(int index, String name, int price, Color thumbColor, OnSelect onSelect) {
        this.index = index;
        this.thumbColor = thumbColor;
        setLayout(new BorderLayout(0, 6));
        setBackground(KioskColors.CARD);
        setBorder(UiKit.cardBorder());
        setPreferredSize(new Dimension(150, 170));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        JPanel thumb = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        thumb.setLayout(new BorderLayout());
        thumb.setOpaque(false);
        thumb.setPreferredSize(new Dimension(120, 72));

        priceBadge.setFont(KioskFonts.small());
        priceBadge.setForeground(Color.WHITE);
        priceBadge.setOpaque(true);
        priceBadge.setBackground(KioskColors.BLUE);
        priceBadge.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 8, 2, 8));
        priceBadge.setText(price + "원");

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(priceBadge);
        thumb.add(badgeWrap, BorderLayout.NORTH);

        nameLabel.setText(name);
        nameLabel.setFont(KioskFonts.bodyBold());
        nameLabel.setForeground(KioskColors.TEXT);

        stockLabel.setFont(KioskFonts.small());
        stockLabel.setForeground(KioskColors.SUBTEXT);

        add(thumb, BorderLayout.CENTER);
        add(nameLabel, BorderLayout.SOUTH);
        add(stockLabel, BorderLayout.PAGE_END);

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
        nameLabel.setText(name);
        priceBadge.setText(price + "원");

        if (soldOut) {
            stockLabel.setText("품절");
            stockLabel.setForeground(KioskColors.RED);
            setBorder(UiKit.cardBorder());
            setBackground(KioskColors.RED_BG);
            setEnabled(false);
        } else {
            stockLabel.setText("재고 " + stock + "개");
            stockLabel.setForeground(KioskColors.SUBTEXT);
            if (selected) {
                setBackground(KioskColors.SELECTED);
                setBorder(UiKit.selectedBorder());
            } else if (affordable) {
                setBackground(new Color(235, 250, 240));
                setBorder(UiKit.cardBorder());
            } else {
                setBackground(KioskColors.CARD);
                setBorder(UiKit.cardBorder());
            }
            setEnabled(true);
        }
    }
}
