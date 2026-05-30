package vending.ui.admin;

import vending.kiosk.KioskService;
import vending.sales.model.SaleEntry;
import vending.thread.AdminReportThread;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.util.List;

/**
 * 일별/월별 매출 + 음료 Search.
 */
public class SalesReportDialog extends JDialog {

    private final DefaultTableModel dailyModel = new DefaultTableModel(
            new String[]{"날짜", "클라이언트", "음료", "수량", "금액"}, 0);
    private final DefaultTableModel monthlyModel = new DefaultTableModel(
            new String[]{"월", "음료", "수량", "금액"}, 0);
    private final JLabel searchResultLabel = new JLabel(" ");

    public SalesReportDialog(Component parent, KioskService service) {
        super((Frame) null, "매출 보고서", true);
        setSize(720, 520);
        setLocationRelativeTo(parent);

        JTable dailyTable = new JTable(dailyModel);
        JTable monthlyTable = new JTable(monthlyModel);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("일별", new JScrollPane(dailyTable));
        tabs.add("월별", new JScrollPane(monthlyTable));

        JButton sortDate = new JButton("날짜순");
        JButton sortAmount = new JButton("금액순");
        sortDate.addActionListener(e -> loadDailySorted(service, "date"));
        sortAmount.addActionListener(e -> loadDailySorted(service, "amount"));

        JComboBox<String> drinkBox = new JComboBox<>();
        JButton searchBtn = new JButton("음료 검색");
        searchBtn.addActionListener(e -> searchDrink(service, (String) drinkBox.getSelectedItem()));

        try {
            for (String name : service.getSalesService().sortedDrinkNames()) {
                drinkBox.addItem(name);
            }
        } catch (Exception ex) {
            searchResultLabel.setText("음료 목록 로드 실패");
        }

        JPanel bottom = new JPanel();
        bottom.add(sortDate);
        bottom.add(sortAmount);
        bottom.add(new JLabel("음료"));
        bottom.add(drinkBox);
        bottom.add(searchBtn);
        bottom.add(searchResultLabel);

        add(tabs, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        AdminReportThread thread = new AdminReportThread(service, this::fillDaily, this::fillMonthly);
        thread.start();
    }

    private void searchDrink(KioskService service, String drinkName) {
        if (drinkName == null || drinkName.isBlank()) {
            searchResultLabel.setText("음료를 선택하세요.");
            return;
        }
        try {
            int total = service.getSalesService().searchDrinkTotal(drinkName);
            List<SaleEntry> rows = service.getSalesService().filterByDrink(drinkName);
            fillDaily(rows);
            searchResultLabel.setText(drinkName + " 누적 매출: " + total + "원");
        } catch (Exception ex) {
            searchResultLabel.setText("검색 실패: " + ex.getMessage());
        }
    }

    private void fillDaily(List<SaleEntry> entries) {
        dailyModel.setRowCount(0);
        for (SaleEntry e : entries) {
            dailyModel.addRow(new Object[]{
                    e.getDate(), e.getClientId(), e.getDrinkName(), e.getQuantity(), e.getAmount()
            });
        }
    }

    private void fillMonthly(List<String[]> rows) {
        monthlyModel.setRowCount(0);
        for (String[] r : rows) {
            monthlyModel.addRow(r);
        }
    }

    private void loadDailySorted(KioskService service, String mode) {
        try {
            List<SaleEntry> list = service.getSalesService().allEntries();
            if ("amount".equals(mode)) {
                list = service.getSalesService().getSorter().sortByAmountDesc(list);
            } else {
                list = service.getSalesService().getSorter().sortByDateDesc(list);
            }
            fillDaily(list);
        } catch (Exception ex) {
            searchResultLabel.setText("정렬 실패");
        }
    }
}
