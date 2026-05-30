package vending.server;

import vending.network.SocketClient;
import vending.protocol.VendingMessage;

/**
 * Cloud 노드: Server1로 데이터 백업 전달.
 */
public class CloudForwarder extends Thread {

    private final String targetHost;
    private final int targetPort;

    public CloudForwarder(String targetHost, int targetPort) {
        super("cloud-forwarder");
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(15000);
                VendingMessage ping = VendingMessage.heartbeat("Cloud");
                new SocketClient(targetHost, targetPort).send(ping);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
            }
        }
    }
}
