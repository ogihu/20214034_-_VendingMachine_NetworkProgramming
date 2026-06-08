package vending.ui.admin;

import vending.ui.customer.ProductImageLoader;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Image;

public class DrinkImageRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setText("");

        int maxW = Math.max(40, table.getColumnModel().getColumn(column).getWidth() - 8);
        int maxH = Math.max(40, table.getRowHeight(row) - 8);
        Image image = ProductImageLoader.load(row);
        if (image != null) {
            label.setIcon(new ImageIcon(ProductImageLoader.fit(image, maxW, maxH)));
        } else {
            label.setText("-");
            label.setIcon(null);
        }
        return label;
    }
}
