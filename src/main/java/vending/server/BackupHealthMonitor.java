package vending.server;

import vending.network.SocketClient;
import vending.protocol.MessageType;
import vending.protocol.VendingMessage;

/**
 * Backup 서버: 10초마다 Server1/2 헬스체크 후 활성 서버 결정.
 */
public class BackupHealthMonitor extends Thread {

    private final String server1Host;
    private final int server1Port;
    private final String server2Host;
    private final int server2Port;

    private volatile boolean server1Alive;
    private volatile boolean server2Alive;
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
            server1Alive = ping(server1Host, server1Port);
            server2Alive = ping(server2Host, server2Port);

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
                activeHost = "127.0.0.1";
                activePort = Integer.parseInt(System.getProperty("backup.port", "9092"));
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

    public VendingMessage activeServerMessage() {
        return VendingMessage.activeServer(activeServerId, activeHost, activePort);
    }

    public boolean isServer1Alive() {
        return server1Alive;
    }

    public boolean isServer2Alive() {
        return server2Alive;
    }
}
