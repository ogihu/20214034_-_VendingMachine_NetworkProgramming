package vending.server;

import vending.network.SocketClient;
import vending.protocol.VendingMessage;

import java.io.IOException;

/**
 * Server1/2 -> 상대 서버 + Backup 으로 SYNC 전달.
 */
public class PeerSyncManager extends Thread {

    private final String serverId;
    private final String peerHost;
    private final int peerPort;
    private final String backupHost;
    private final int backupPort;
    private volatile boolean running = true;

    public PeerSyncManager(String serverId, String peerHost, int peerPort,
                           String backupHost, int backupPort) {
        super("peer-sync-" + serverId);
        this.serverId = serverId;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.backupHost = backupHost;
        this.backupPort = backupPort;
    }

    public void forward(VendingMessage message) {
        sendSync(peerHost, peerPort, message);
        sendSync(backupHost, backupPort, message);
    }

    private void sendSync(String host, int port, VendingMessage message) {
        if (host == null || host.isBlank()) {
            return;
        }
        try {
            // heartbeat는 그대로 보내고, 나머지는 SYNC로 전달
            if (message.type == vending.protocol.MessageType.HEARTBEAT) {
                new SocketClient(host, port).send(message);
            } else {
                VendingMessage sync = VendingMessage.sync(serverId, message.toJson());
                new SocketClient(host, port).send(sync);
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(3000);
                // 연결 상태 확인용 heartbeat
                sendSync(peerHost, peerPort, VendingMessage.heartbeat(serverId));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
