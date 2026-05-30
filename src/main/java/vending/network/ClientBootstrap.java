package vending.network;

import vending.protocol.MessageType;
import vending.protocol.VendingMessage;

/**
 * 시작 시 Backup에서 활성 Server 주소를 받아온다.
 */
public final class ClientBootstrap {

    private ClientBootstrap() {
    }

    public static void resolveActiveServer() {
        String backupHost = System.getProperty("backup.host", "127.0.0.1");
        int backupPort = Integer.parseInt(System.getProperty("backup.port", "9092"));

        try {
            VendingMessage query = VendingMessage.heartbeat("bootstrap");
            VendingMessage response = new SocketClient(backupHost, backupPort).sendAndRead(query);
            if (response != null && response.type == MessageType.ACTIVE_SERVER) {
                System.setProperty("server.host", response.clientId);
                System.setProperty("server.port", String.valueOf(response.amount));
            }
        } catch (Exception ignored) {
            // Backup 없으면 기본 server.host/port 사용
        }
    }
}
