package vending.network;

import vending.protocol.VendingMessage;
import vending.util.AppLog;

// 클라우드전송기능
public class CloudRelay {

    private final String cloudHost;
    private final int cloudPort;
    private final boolean enabled;

    public CloudRelay() {
        cloudHost = System.getProperty("cloud.host", "").trim();
        String portStr = System.getProperty("cloud.port", "9093");
        cloudPort = portStr.isBlank() ? 9093 : Integer.parseInt(portStr);
        enabled = !cloudHost.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void forward(VendingMessage message) {
        if (!enabled) {
            return;
        }
        try {
            new SocketClient(cloudHost, cloudPort).send(message);
            AppLog.info("CLOUD", "백업 전송: " + message.type + " / " + message.clientId);
        } catch (vending.util.VendingException e) {
            AppLog.warn("CLOUD", "백업 실패: " + e.getMessage());
        }
    }
}
