package vending.network;

import vending.protocol.MessageType;
import vending.protocol.VendingMessage;
import vending.util.AppLog;

public final class ClientBootstrap {

    private ClientBootstrap() {
    }

    public static void resolveActiveServer() {
        String backupHost = System.getProperty("backup.host", "127.0.0.1");
        int backupPort;
        try {
            backupPort = Integer.parseInt(System.getProperty("backup.port", "9092"));
        } catch (NumberFormatException e) {
            AppLog.error("BOOTSTRAP", "backup.port 형식 오류", e);
            return;
        }

        try {
            VendingMessage query = VendingMessage.heartbeat("bootstrap");
            VendingMessage response = new SocketClient(backupHost, backupPort).sendAndRead(query);
            if (response != null && response.type == MessageType.ACTIVE_SERVER
                    && response.host != null && !response.host.isBlank()) {
                System.setProperty("server.host", response.host.trim());
                System.setProperty("server.port", String.valueOf(response.amount));
            }
        } catch (Exception e) {
            AppLog.warn("BOOTSTRAP", "활성 서버 조회 실패, 설정 파일의 server.host/port 사용: " + e.getMessage());
        }
    }
}
