package vending.ui.admin;

import vending.coin.CoinInventory;
import vending.coin.CoinSlot;
import vending.drink.DrinkCatalog;
import vending.drink.DrinkInfo;
import vending.kiosk.KioskService;
import vending.ui.customer.CustomerPanel;
import vending.ui.theme.KioskColors;
import vending.ui.theme.KioskFonts;
import vending.ui.theme.UiKit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * 관리자 전용 화면. 접속 중에는 고객 화면이 잠긴다.
 */
public class AdminPanel extends JPanel {

    private final KioskService service;
    private final CustomerPanel customerPanel;

    private final JPasswordField passwordField = new JPasswordField(10);
    private final JButton loginBtn = new JButton("관리자 접속");
    private final JLabel statusLabel = new JLabel("비밀번호 입력");

    private final JPanel managePanel = new JPanel();
    private DefaultTableModel drinkModel;
    private DefaultTableModel coinModel;
    private final JTextField collectField = new JTextField(8);
    private final JTextField newPwField = new JTextField(10);
    private final JLabel salesLabel = new JLabel("누적 매출: 0원");

    public AdminPanel(KioskService service, CustomerPanel customerPanel) {
        this.service = service;
        this.customerPanel = customerPanel;

        setPreferredSize(new Dimension(300, 600));
        setBackground(KioskColors.CARD);
        setBorder(UiKit.cardBorder());
        setLayout(new BorderLayout(6, 6));

        JPanel loginPanel = new JPanel();
        loginPanel.add(new JLabel("비밀번호"));
        loginPanel.add(passwordField);
        loginPanel.add(loginBtn);

        statusLabel.setFont(KioskFonts.small());
        loginBtn.setFont(KioskFonts.body());
        loginBtn.addActionListener(e -> onLoginToggle());

        buildManagePanel();

        add(loginPanel, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.CENTER);
        add(managePanel, BorderLayout.SOUTH);

        managePanel.setVisible(false);
    }

    private void buildManagePanel() {
        managePanel.setLayout(new BorderLayout(4, 4));

        drinkModel = new DefaultTableModel(new String[]{"음료", "재고", "가격"}, 0);
        JTable drinkTable = new JTable(drinkModel);
        drinkTable.getModel().addTableModelListener(e -> onDrinkTableEdit(drinkTable));

        coinModel = new DefaultTableModel(new String[]{"화폐", "수량"}, 0);
        JTable coinTable = new JTable(coinModel);

        JButton restockBtn = new JButton("선택 음료 +10");
        restockBtn.addActionListener(e -> {
            int row = drinkTable.getSelectedRow();
            if (row >= 0) {
                service.restockDrink(row, 10);
                refresh();
            }
        });

        JButton addCoinBtn = new JButton("100원 +10");
        addCoinBtn.addActionListener(e -> {
            service.addCoinStock(100, 10);
            refresh();
        });

        JButton collectBtn = new JButton("수금");
        collectBtn.addActionListener(e -> {
            try {
                int amount = Integer.parseInt(collectField.getText().trim());
                String err = service.collectMoney(amount);
                if (err != null) {
                    JOptionPane.showMessageDialog(this, err);
                } else {
                    JOptionPane.showMessageDialog(this, amount + "원 수금 완료");
                    collectField.setText("");
                    refresh();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "숫자를 입력하세요.");
            }
        });

        JButton pwBtn = new JButton("비밀번호 변경");
        pwBtn.addActionListener(e -> {
            String err = service.changePassword(newPwField.getText().trim());
            if (err != null) {
                JOptionPane.showMessageDialog(this, err);
            } else {
                JOptionPane.showMessageDialog(this, "비밀번호가 변경되었습니다.");
                newPwField.setText("");
            }
        });

        JButton reportBtn = new JButton("일별/월별 매출");
        reportBtn.addActionListener(e -> new SalesReportDialog(this, service).setVisible(true));

        JPanel btnPanel = new JPanel(new GridLayout(3, 2, 4, 4));
        btnPanel.add(restockBtn);
        btnPanel.add(addCoinBtn);
        btnPanel.add(collectBtn);
        btnPanel.add(pwBtn);
        btnPanel.add(reportBtn);

        JPanel collectPanel = new JPanel();
        collectPanel.add(new JLabel("수금액"));
        collectPanel.add(collectField);

        JPanel pwPanel = new JPanel();
        pwPanel.add(new JLabel("새 비밀번호"));
        pwPanel.add(newPwField);

        JPanel center = new JPanel(new GridLayout(2, 1));
        center.add(new JScrollPane(drinkTable));
        center.add(new JScrollPane(coinTable));

        managePanel.add(salesLabel, BorderLayout.NORTH);
        managePanel.add(center, BorderLayout.CENTER);
        managePanel.add(collectPanel, BorderLayout.WEST);
        managePanel.add(pwPanel, BorderLayout.EAST);
        managePanel.add(btnPanel, BorderLayout.SOUTH);
    }

    private void onLoginToggle() {
        if (!service.isAdminMode()) {
            String pw = new String(passwordField.getPassword());
            if (service.loginAdmin(pw)) {
                managePanel.setVisible(true);
                statusLabel.setText("관리자 모드");
                loginBtn.setText("접속 해제");
                passwordField.setText("");
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "비밀번호가 틀렸습니다.");
            }
        } else {
            service.logoutAdmin();
            managePanel.setVisible(false);
            statusLabel.setText("비밀번호 입력");
            loginBtn.setText("관리자 접속");
            refresh();
        }
    }

    private void onDrinkTableEdit(JTable table) {
        if (!service.isAdminMode()) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        try {
            String name = (String) drinkModel.getValueAt(row, 0);
            int stock = Integer.parseInt(String.valueOf(drinkModel.getValueAt(row, 1)));
            int price = Integer.parseInt(String.valueOf(drinkModel.getValueAt(row, 2)));

            service.updateDrinkName(row, name);
            service.updateDrinkPrice(row, price);

            int current = service.getCatalog().getStock(row).size();
            int diff = stock - current;
            if (diff > 0) {
                service.restockDrink(row, diff);
            }
        } catch (Exception ignored) {
        }
    }

    public void refresh() {
        DrinkCatalog catalog = service.getCatalog();
        drinkModel.setRowCount(0);
        for (int i = 0; i < catalog.drinkCount(); i++) {
            DrinkInfo d = catalog.getDrink(i);
            drinkModel.addRow(new Object[]{
                    d.getName(),
                    catalog.getStock(i).size(),
                    d.getPrice()
            });
        }

        CoinInventory coins = service.getCoinInventory();
        coinModel.setRowCount(0);
        for (CoinSlot slot : coins.getSlots()) {
            coinModel.addRow(new Object[]{slot.getValue(), slot.getCount()});
        }

        salesLabel.setText("누적 매출: " + service.getSessionSales() + "원 | "
                + "자판기 잔고: " + coins.totalBalance() + "원");
    }
}
