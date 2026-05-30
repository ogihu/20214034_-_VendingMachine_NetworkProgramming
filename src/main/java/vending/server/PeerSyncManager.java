package vending.server;

import vending.network.SocketClient;
import vending.protocol.VendingMessage;

import java.io.IOException;

/**
 * Server1 <-> Server2 실시간 동기화.
 */
public class PeerSyncManager extends Thread {

    private final String serverId;
    private final String peerHost;
    private final int peerPort;
    private volatile boolean running = true;

    public PeerSyncManager(String serverId, String peerHost, int peerPort) {
        super("peer-sync-" + serverId);
        this.serverId = serverId;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
    }

    public void forward(VendingMessage message) {
        if (peerHost == null || peerHost.isBlank()) {
            return;
        }
        try {
            VendingMessage sync = VendingMessage.sync(serverId, message.toJson());
            new SocketClient(peerHost, peerPort).send(sync);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(3000);
                VendingMessage ping = VendingMessage.heartbeat(serverId);
                new SocketClient(peerHost, peerPort).send(ping);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
