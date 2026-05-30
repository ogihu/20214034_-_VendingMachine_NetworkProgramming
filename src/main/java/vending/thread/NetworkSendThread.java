package vending.thread;

import vending.event.NetworkEventQueue;
import vending.network.SocketClient;
import vending.protocol.VendingMessage;

/**
 * 네트워크 이벤트 큐를 읽어 서버로 보내는 스레드.
 */
public class NetworkSendThread extends Thread {

    private final NetworkEventQueue queue;
    private volatile boolean running = true;

    public NetworkSendThread(NetworkEventQueue queue, String clientId) {
        super("network-send-" + clientId);
        this.queue = queue;
    }

    @Override
    public void run() {
        SocketClient client = new SocketClient();
        while (running) {
            try {
                VendingMessage message = queue.dequeue();
                if (message != null) {
                    client.send(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
                // 서버 미연결 시에도 로컬 판매는 계속
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
