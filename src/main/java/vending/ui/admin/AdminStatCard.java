package vending.ui.admin;

import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class AdminStatCard extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();

    public AdminStatCard(String title, Color accent) {
        setBackground(KioskColors.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KioskColors.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        setPreferredSize(new Dimension(180, 88));
        setLayout(new BorderLayout(0, 6));

        titleLabel.setText(title);
        titleLabel.setFont(KioskFonts.small());
        titleLabel.setForeground(KioskColors.SUBTEXT);

        valueLabel.setFont(KioskFonts.section());
        valueLabel.setForeground(accent);

        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
