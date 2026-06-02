package vending.thread;

import vending.event.NetworkEventQueue;
import vending.network.CloudRelay;
import vending.network.SocketClient;
import vending.protocol.VendingMessage;
import vending.util.AppLog;

/**
 * 네트워크 이벤트 큐를 읽어 Server + Cloud(Railway)로 전송.
 */
public class NetworkSendThread extends Thread {

    private final NetworkEventQueue queue;
    private final CloudRelay cloudRelay;
    private volatile boolean running = true;

    public NetworkSendThread(NetworkEventQueue queue, String clientId) {
        super("network-send-" + clientId);
        this.queue = queue;
        this.cloudRelay = new CloudRelay();
    }

    @Override
    public void run() {
        SocketClient client = new SocketClient();
        while (running) {
            try {
                VendingMessage message = queue.dequeue();
                if (message == null) {
                    continue;
                }
                try {
                    client.send(message);
                } catch (Exception e) {
                    AppLog.warn("NET", "Server 전송 실패: " + e.getMessage());
                }
                cloudRelay.forward(message);
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
