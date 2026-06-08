package vending.server.sync;

import vending.protocol.VendingMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SyncRetryQueue {

    public static final int MAX_ATTEMPTS = 12;
    private static final long BASE_DELAY_MS = 400;

    public static final class PendingItem {
        public final String host;
        public final int port;
        public final VendingMessage message;
        public final boolean asSync;
        public int attempts;
        public long nextAttemptAt;

        PendingItem(String host, int port, VendingMessage message, boolean asSync) {
            this.host = host;
            this.port = port;
            this.message = message;
            this.asSync = asSync;
            this.attempts = 0;
            this.nextAttemptAt = System.currentTimeMillis();
        }

        PendingItem(PendingItem source, String host, int port) {
            this.host = host;
            this.port = port;
            this.message = source.message;
            this.asSync = source.asSync;
            this.attempts = source.attempts;
            this.nextAttemptAt = source.nextAttemptAt;
        }

        void scheduleRetry() {
            attempts++;
            long delay = BASE_DELAY_MS * (1L << Math.min(attempts, 4));
            nextAttemptAt = System.currentTimeMillis() + delay;
        }
    }

    private final ConcurrentLinkedQueue<PendingItem> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(String host, int port, VendingMessage message, boolean asSync) {
        if (host == null || host.isBlank()) {
            return;
        }
        queue.offer(new PendingItem(host, port, message, asSync));
    }

    public List<PendingItem> drainDue() {
        long now = System.currentTimeMillis();
        List<PendingItem> due = new ArrayList<>();
        for (PendingItem item : queue) {
            if (item.nextAttemptAt <= now) {
                due.add(item);
            }
        }
        return due;
    }

    public void remove(PendingItem item) {
        queue.remove(item);
    }

    public boolean requeue(PendingItem item) {
        queue.remove(item);
        if (item.attempts < MAX_ATTEMPTS) {
            item.scheduleRetry();
            queue.offer(item);
            return true;
        }
        return false;
    }

    public void retargetAll(String host, int port) {
        if (host == null || host.isBlank()) {
            return;
        }
        List<PendingItem> items = new ArrayList<>(queue);
        queue.clear();
        for (PendingItem item : items) {
            queue.offer(new PendingItem(item, host, port));
        }
    }

    public int size() {
        return queue.size();
    }
}
