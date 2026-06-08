package vending.server;

import vending.network.SocketClient;
import vending.protocol.MessageType;
import vending.protocol.VendingMessage;
import vending.util.AppLog;

// 백업헬스체크기능
public class BackupHealthMonitor extends Thread {

    private final String server1Host;
    private final int server1Port;
    private final String server2Host;
    private final int server2Port;

    private volatile boolean server1Alive;
    private volatile boolean server2Alive;
    private boolean prevServer1Alive = true;
    private boolean prevServer2Alive = true;
    private volatile String activeServerId = "Server1";
    private volatile String activeHost;
    private volatile int activePort;

    public BackupHealthMonitor(String server1Host, int server1Port, String server2Host, int server2Port) {
        super("backup-health");
        this.server1Host = server1Host;
        this.server1Port = server1Port;
        this.server2Host = server2Host;
        this.server2Port = server2Port;
        this.activeHost = server1Host;
        this.activePort = server1Port;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            boolean s1 = ping(server1Host, server1Port);
            boolean s2 = ping(server2Host, server2Port);
            logStateChange("Server1", server1Host, server1Port, prevServer1Alive, s1);
            logStateChange("Server2", server2Host, server2Port, prevServer2Alive, s2);
            prevServer1Alive = s1;
            prevServer2Alive = s2;
            server1Alive = s1;
            server2Alive = s2;

            if (server1Alive) {
                activeServerId = "Server1";
                activeHost = server1Host;
                activePort = server1Port;
            } else if (server2Alive) {
                activeServerId = "Server2";
                activeHost = server2Host;
                activePort = server2Port;
            } else {
                activeServerId = "Backup";
                activeHost = System.getProperty("backup.public.host", "127.0.0.1");
                activePort = parseBackupPort();
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean ping(String host, int port) {
        try {
            VendingMessage ack = new SocketClient(host, port).sendAndRead(VendingMessage.heartbeat("Backup"));
            return ack != null && ack.type == MessageType.HEARTBEAT_ACK;
        } catch (Exception e) {
            return false;
        }
    }

    private void logStateChange(String label, String host, int port, boolean wasAlive, boolean alive) {
        if (wasAlive == alive) {
            return;
        }
        if (alive) {
            AppLog.info("BACKUP", label + " 복구: " + host + ":" + port);
        } else {
            AppLog.warn("BACKUP", label + " 응답 없음: " + host + ":" + port);
        }
    }

    private int parseBackupPort() {
        try {
            return Integer.parseInt(System.getProperty("backup.port", "9092"));
        } catch (NumberFormatException e) {
            AppLog.warn("BACKUP", "backup.port 형식 오류, 9092 사용: " + e.getMessage());
            return 9092;
        }
    }

    public VendingMessage activeServerMessage() {
        return VendingMessage.activeServer(activeServerId, activeHost, activePort);
    }

    public boolean isServer1Alive() {
        return server1Alive;
    }

    public boolean isServer2Alive() {
        return server2Alive;
    }

    // 대체서버기능
    public boolean isFailoverMode() {
        return !server1Alive && !server2Alive;
    }
}
