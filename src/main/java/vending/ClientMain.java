package vending;

import vending.ui.KioskFrame;
import vending.ui.boot.BootSplashFrame;
import vending.config.ClientConfig;
import vending.network.ClientBootstrap;

/**
 * 키오스크(자판기) 클라이언트 실행 진입점.
 */
public class ClientMain {

    public static void main(String[] args) {
        String configPath = System.getProperty("client.config", "config/client1.properties");
        ClientConfig.load(configPath);
        String clientId = System.getProperty("client.id", "Client1");

        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            BootSplashFrame boot = new BootSplashFrame(clientId, () -> {
                ClientBootstrap.resolveActiveServer();
                new KioskFrame(clientId);
            });
            boot.setVisible(true);
        });
    }
}
