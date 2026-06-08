package vending;

import vending.config.ClientConfig;
import vending.network.ActiveServerResolver;
import vending.ui.KioskFrame;
import vending.ui.boot.BootSplashFrame;
import vending.util.AppLog;
import vending.util.ConsoleEncoding;

// 클라이언트실행기능
public class ClientMain {

    public static void main(String[] args) {
        ConsoleEncoding.configureUtf8();
        if (System.getProperty("vending.base") == null) {
            System.setProperty("vending.base", java.nio.file.Paths.get("").toAbsolutePath().normalize().toString());
        }
        String configPath = System.getProperty("client.config", "config/client1.properties");
        ClientConfig.load(configPath);
        String clientId = System.getProperty("client.id", "Client1");

        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                AppLog.warn("UI", "시스템 LookAndFeel 적용 실패: " + e.getMessage());
            }

            BootSplashFrame boot = new BootSplashFrame(clientId, () -> {
                ActiveServerResolver.refresh();
                new KioskFrame(clientId);
            });
            boot.setVisible(true);
        });
    }
}
