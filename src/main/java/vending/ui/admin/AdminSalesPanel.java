package vending.ui.admin;

import vending.kiosk.KioskService;
import vending.sales.model.SaleEntry;
import vending.thread.AdminReportThread;
import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;
import vending.util.VendingException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Vector;

public class AdminSalesPanel extends JPanel {

    private final KioskService service;
    private final DefaultTableModel dailyModel = new DefaultTableModel(
            new String[]{"날짜", "음료", "수량", "금액"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel monthlyModel = new DefaultTableModel(
            new String[]{"월", "음료", "수량", "금액"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel searchResultLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");

    public AdminSalesPanel(KioskService service) {
        this.service = service;
        setOpaque(false);
        setLayout(new BorderLayout());

        statusLabel.setFont(KioskFonts.small());
        statusLabel.setForeground(KioskColors.SUBTEXT);
        searchResultLabel.setFont(KioskFonts.small());
        searchResultLabel.setForeground(KioskColors.SUBTEXT);

        JTable dailyTable = new JTable(dailyModel);
        JTable monthlyTable = new JTable(monthlyModel);
        AdminTableKit.style(dailyTable);
        AdminTableKit.style(monthlyTable);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(KioskFonts.body());
        tabs.add("일별 매출", AdminTableKit.wrap(dailyTable));
        tabs.add("월별 매출", AdminTableKit.wrap(monthlyTable));

        JButton sortDate = UiKit.outlineButton("날짜순");
        JButton sortAmount = UiKit.outlineButton("금액순");
        sortDate.addActionListener(e -> loadDailySorted("date"));
        sortAmount.addActionListener(e -> loadDailySorted("amount"));

        JComboBox<String> drinkBox = new JComboBox<>();
        AdminFormKit.styleField(drinkBox);
        JButton searchBtn = UiKit.primaryButton("음료 검색");
        searchBtn.addActionListener(e -> searchDrink((String) drinkBox.getSelectedItem()));

        try {
            for (String name : service.getSalesService().sortedDrinkNames()) {
                drinkBox.addItem(name);
            }
        } catch (VendingException ex) {
            statusLabel.setText(ex.getMessage());
        }

        JPanel tools = new JPanel();
        tools.setOpaque(false);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        tools.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sortRow.setOpaque(false);
        sortRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sortRow.add(new JLabel("정렬"));
        sortRow.add(sortDate);
        sortRow.add(sortAmount);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.add(new JLabel("음료 검색"));
        searchRow.add(drinkBox);
        searchRow.add(searchBtn);
        searchRow.add(searchResultLabel);

        tools.add(sortRow);
        tools.add(Box.createVerticalStrut(8));
        tools.add(searchRow);
        tools.add(Box.createVerticalStrut(6));
        tools.add(statusLabel);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(tabs, BorderLayout.CENTER);
        body.add(tools, BorderLayout.SOUTH);

        String clientLabel;
        try {
            clientLabel = service.getSalesService().getClientId();
        } catch (Exception e) {
            clientLabel = "Client";
        }
        add(AdminFormKit.page("📈", "일별/월별 매출",
                clientLabel + " 단말의 일별·월별 매출 기록을 조회하고 음료별로 검색할 수 있습니다.", body), BorderLayout.CENTER);

        AdminReportThread thread = new AdminReportThread(service, this::fillDaily, this::fillMonthly);
        thread.start();
    }

    private void searchDrink(String drinkName) {
        if (drinkName == null || drinkName.isBlank()) {
            searchResultLabel.setText("음료를 선택하세요.");
            return;
        }
        try {
            int total = service.getSalesService().searchDrinkTotal(drinkName);
            searchResultLabel.setText(drinkName + " 누적 매출: " + total + "원");
        } catch (VendingException ex) {
            searchResultLabel.setText(ex.getMessage());
        }
    }

    private void loadDailySorted(String mode) {
        try {
            List<SaleEntry> entries;
            if ("amount".equals(mode)) {
                entries = service.getSalesService().getSorter().sortByAmountDesc(
                        service.getSalesService().clientEntries());
            } else {
                entries = service.getSalesService().allEntriesSortedByDate();
            }
            fillDaily(entries);
            statusLabel.setText("일별 매출을 " + ("amount".equals(mode) ? "금액순" : "날짜순") + "으로 정렬했습니다.");
        } catch (VendingException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void fillDaily(List<SaleEntry> entries) {
        dailyModel.setRowCount(0);
        for (SaleEntry e : entries) {
            Vector<Object> row = new Vector<>();
            row.add(e.getDate());
            row.add(e.getDrinkName());
            row.add(e.getQuantity());
            row.add(e.getAmount());
            dailyModel.addRow(row);
        }
    }

    private void fillMonthly(List<String[]> rows) {
        monthlyModel.setRowCount(0);
        for (String[] r : rows) {
            Vector<Object> row = new Vector<>();
            for (String cell : r) {
                row.add(cell);
            }
            monthlyModel.addRow(row);
        }
    }
}
