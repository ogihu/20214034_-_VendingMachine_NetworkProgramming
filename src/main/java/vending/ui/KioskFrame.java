package vending.ui;

import vending.kiosk.KioskService;
import vending.ui.admin.AdminLoginPanel;
import vending.ui.admin.AdminPanel;
import vending.ui.customer.CustomerPanel;
import vending.ui.theme.AppIcon;
import vending.ui.theme.KioskColors;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

public class KioskFrame extends JFrame {

    private static final String CARD_CUSTOMER = "customer";
    private static final String CARD_LOGIN = "login";
    private static final String CARD_ADMIN = "admin";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    public KioskFrame(String clientId) {
        super("20214034_정영웅_자판기프로그램 - " + clientId);

        KioskService service = new KioskService(clientId);

        AppIcon.applyToFrame(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 780));
        setLocationRelativeTo(null);
        getContentPane().setBackground(KioskColors.BG);

        AdminLoginPanel loginPanel = new AdminLoginPanel(service, () -> cardLayout.show(cardPanel, CARD_CUSTOMER));
        CustomerPanel customerPanel = new CustomerPanel(service, () -> {
            loginPanel.reset();
            cardLayout.show(cardPanel, CARD_LOGIN);
        });
        AdminPanel adminPanel = new AdminPanel(service, customerPanel, () -> {
            if (service.isAdminMode()) {
                service.logoutAdmin();
            }
            cardLayout.show(cardPanel, CARD_CUSTOMER);
        });

        cardPanel.setBackground(KioskColors.BG);
        cardPanel.setPreferredSize(new Dimension(1040, 780));
        cardPanel.add(customerPanel, CARD_CUSTOMER);
        cardPanel.add(loginPanel, CARD_LOGIN);
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
                if (adminMode) {
                    cardLayout.show(cardPanel, CARD_ADMIN);
                }
            }
        });

        setContentPane(cardPanel);
        setSize(new Dimension(1040, 780));
        setVisible(true);

        cardLayout.show(cardPanel, CARD_CUSTOMER);
        customerPanel.refresh();
        adminPanel.refresh();
    }
}
