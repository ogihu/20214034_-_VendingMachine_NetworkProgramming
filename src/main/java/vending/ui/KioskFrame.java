package vending.ui;

import vending.kiosk.KioskService;
import vending.ui.admin.AdminPanel;
import vending.ui.customer.CustomerPanel;
import vending.ui.theme.KioskColors;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * 키오스크 메인 프레임. 관리자 모드 시 고객 화면을 숨긴다.
 */
public class KioskFrame extends JFrame {

    private static final String CARD_CUSTOMER = "customer";
    private static final String CARD_ADMIN = "admin";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    public KioskFrame(String clientId) {
        super("Smart Vending - " + clientId);

        KioskService service = new KioskService(clientId);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 760));
        setLocationRelativeTo(null);
        getContentPane().setBackground(KioskColors.BG);

        CustomerPanel customerPanel = new CustomerPanel(service);
        AdminPanel adminPanel = new AdminPanel(service, customerPanel);

        cardPanel.setBackground(KioskColors.BG);
        cardPanel.add(customerPanel, CARD_CUSTOMER);
        cardPanel.add(adminPanel, CARD_ADMIN);

        service.setListener(new KioskService.Listener() {
            @Override
            public void onStateChanged() {
                customerPanel.refresh();
                adminPanel.refresh();
                customerPanel.setEnabled(!service.isAdminMode());
            }

            @Override
            public void onMessage(String message) {
                customerPanel.showHint(message);
            }

            @Override
            public void onAdminModeChanged(boolean adminMode) {
                cardLayout.show(cardPanel, adminMode ? CARD_ADMIN : CARD_CUSTOMER);
            }
        });

        setContentPane(cardPanel);
        pack();
        setVisible(true);

        cardLayout.show(cardPanel, CARD_CUSTOMER);
        customerPanel.refresh();
        adminPanel.refresh();
    }
}
