package vending.thread;

import vending.config.DataPaths;
import vending.event.NetworkEventQueue;
import vending.event.PendingEventStore;
import vending.network.ActiveServerResolver;
import vending.network.CloudRelay;
import vending.network.SocketClient;
import vending.protocol.VendingMessage;
import vending.server.sync.SyncRetryQueue;
import vending.server.sync.SyncRetryQueue.PendingItem;
import vending.util.AppLog;
import vending.util.VendingException;

import java.io.IOException;
import java.util.List;

// 네트워크전송기능
public class NetworkSendThread extends Thread {

    private final NetworkEventQueue queue;
    private final CloudRelay cloudRelay;
    private final SyncRetryQueue clientRetryQueue = new SyncRetryQueue();
    private final PendingEventStore pendingStore = new PendingEventStore(DataPaths.PENDING_EVENTS);
    private volatile boolean running = true;

    public NetworkSendThread(NetworkEventQueue queue, String clientId) {
        super("network-send-" + clientId);
        this.queue = queue;
        this.cloudRelay = new CloudRelay();
        restorePendingFromDisk();
        ActiveServerResolver.onServerChanged(() ->
                clientRetryQueue.retargetAll(ActiveServerResolver.host(), ActiveServerResolver.port()));
    }

    @Override
    public void run() {
        while (running) {
            try {
                processClientRetries();
                VendingMessage message = queue.dequeue();
                if (message == null) {
                    Thread.sleep(200);
                    continue;
                }
                try {
                    new SocketClient(ActiveServerResolver.host(), ActiveServerResolver.port()).send(message);
                } catch (VendingException e) {
                    AppLog.warn("NET", "전송 실패, 재시도 큐 등록: " + e.getMessage());
                    clientRetryQueue.enqueue(
                            ActiveServerResolver.host(), ActiveServerResolver.port(), message, false);
                }
                try {
                    cloudRelay.forward(message);
                } catch (Exception e) {
                    AppLog.warn("NET", "Cloud 전송 실패: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void restorePendingFromDisk() {
        List<VendingMessage> restored = pendingStore.drainAll();
        for (VendingMessage message : restored) {
            clientRetryQueue.enqueue(ActiveServerResolver.host(), ActiveServerResolver.port(), message, false);
        }
        if (!restored.isEmpty()) {
            AppLog.info("NET", "보류 이벤트 " + restored.size() + "건 복구");
        }
    }

    private void processClientRetries() {
        reloadPendingIfIdle();
        for (PendingItem item : clientRetryQueue.drainDue()) {
            String host = ActiveServerResolver.host();
            int port = ActiveServerResolver.port();
            try {
                new SocketClient(host, port).send(item.message);
                clientRetryQueue.remove(item);
            } catch (VendingException e) {
                if (!clientRetryQueue.requeue(item)) {
                    persistPending(item.message);
                    clientRetryQueue.remove(item);
                }
            }
        }
    }

    private void reloadPendingIfIdle() {
        if (clientRetryQueue.size() > 0 || pendingStore.count() == 0) {
            return;
        }
        restorePendingFromDisk();
    }

    private void persistPending(VendingMessage message) {
        try {
            pendingStore.append(message);
            AppLog.warn("NET", "전송 보류 이벤트 디스크 저장: " + message.type);
        } catch (IOException e) {
            AppLog.error("NET", "보류 이벤트 저장 실패", e);
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
