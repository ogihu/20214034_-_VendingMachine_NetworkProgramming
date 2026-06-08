package vending.server;

import vending.config.DataPaths;
import vending.event.PendingEventStore;
import vending.network.SocketClient;
import vending.protocol.MessageType;
import vending.protocol.VendingMessage;
import vending.server.sync.SyncRetryQueue;
import vending.server.sync.SyncRetryQueue.PendingItem;
import vending.util.AppLog;
import vending.util.VendingException;

import java.io.IOException;
import java.util.List;

public class PeerSyncManager extends Thread {

    private final String serverId;
    private final String peerHost;
    private final int peerPort;
    private final String backupHost;
    private final int backupPort;
    private final ServerDataStore store;
    private final SyncRetryQueue retryQueue = new SyncRetryQueue();
    private final PendingEventStore pendingStore;
    private volatile boolean running = true;
    private volatile boolean peerWasDown;

    public PeerSyncManager(String serverId, String peerHost, int peerPort,
                           String backupHost, int backupPort, ServerDataStore store) {
        super("peer-sync-" + serverId);
        this.serverId = serverId;
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.backupHost = backupHost;
        this.backupPort = backupPort;
        this.store = store;
        this.pendingStore = new PendingEventStore(
                DataPaths.SERVER_DIR.resolve("pending_sync_" + serverId + ".jsonl"));
        restorePendingFromDisk();
    }

    public void forward(VendingMessage message) {
        // 동기화 등록
        retryQueue.enqueue(peerHost, peerPort, message, true);
        if (backupHost != null && !backupHost.isBlank()) {
            retryQueue.enqueue(backupHost, backupPort, message, true);
        }
    }

    @Override
    public void run() {
        int tick = 0;
        while (running) {
            processRetries();
            if (++tick % 6 == 0) {
                retryQueue.enqueue(peerHost, peerPort, VendingMessage.heartbeat(serverId), false);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processRetries() {
        reloadPendingIfIdle();
        for (PendingItem item : retryQueue.drainDue()) {
            boolean peerTarget = peerHost.equals(item.host) && peerPort == item.port;
            try {
                if (sendItem(item)) {
                    retryQueue.remove(item);
                    if (peerTarget && peerWasDown) {
                        peerWasDown = false;
                        pullSnapshotFromPeer();
                    }
                } else {
                    handleSendFailure(item, peerTarget);
                }
            } catch (VendingException e) {
                AppLog.warn("SYNC", item.host + ":" + item.port + " - " + e.getMessage());
                handleSendFailure(item, peerTarget);
            }
        }
    }

    private void handleSendFailure(PendingItem item, boolean peerTarget) {
        if (peerTarget) {
            peerWasDown = true;
        }
        if (!retryQueue.requeue(item)) {
            persistPending(item);
            retryQueue.remove(item);
        }
    }

    private void restorePendingFromDisk() {
        List<VendingMessage> restored = pendingStore.drainAll();
        for (VendingMessage message : restored) {
            if (message.type == MessageType.HEARTBEAT) {
                continue;
            }
            retryQueue.enqueue(peerHost, peerPort, message, true);
            if (backupHost != null && !backupHost.isBlank()) {
                retryQueue.enqueue(backupHost, backupPort, message, true);
            }
        }
        if (!restored.isEmpty()) {
            AppLog.info("SYNC", serverId + " 보류 동기화 " + restored.size() + "건 복구");
        }
    }

    private void reloadPendingIfIdle() {
        if (retryQueue.size() > 0 || pendingStore.count() == 0) {
            return;
        }
        restorePendingFromDisk();
    }

    private void persistPending(PendingItem item) {
        if (item.message.type == MessageType.HEARTBEAT) {
            return;
        }
        try {
            pendingStore.append(item.message);
            AppLog.warn("SYNC", item.host + ":" + item.port + " 동기화 보류 저장: " + item.message.type);
        } catch (IOException e) {
            AppLog.error("SYNC", "동기화 보류 저장 실패", e);
        }
    }

    private boolean sendItem(PendingItem item) throws VendingException {
        SocketClient client = new SocketClient(item.host, item.port);
        if (item.message.type == MessageType.HEARTBEAT) {
            client.send(item.message);
            return true;
        }
        if (item.asSync) {
            VendingMessage sync = VendingMessage.sync(serverId, item.message.toJson());
            client.send(sync);
        } else {
            client.send(item.message);
        }
        return true;
    }

    private void pullSnapshotFromPeer() {
        // 스냅샷 복구
        try {
            VendingMessage response = new SocketClient(peerHost, peerPort)
                    .sendAndRead(VendingMessage.snapshotRequest(serverId));
            if (response != null && response.type == MessageType.SNAPSHOT) {
                store.apply(response);
                AppLog.info("SYNC", "피어 스냅샷 복구 완료");
            }
        } catch (VendingException e) {
            AppLog.warn("SYNC", "스냅샷 복구 실패: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
