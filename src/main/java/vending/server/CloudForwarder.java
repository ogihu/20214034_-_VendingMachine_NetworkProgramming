package vending.server;

import vending.network.SocketClient;
import vending.protocol.VendingMessage;

// 클라우드연동기능
public class CloudForwarder extends Thread {

    private final String targetHost;
    private final int targetPort;
    private final ServerDataStore store;

    public CloudForwarder(String targetHost, int targetPort, ServerDataStore store) {
        super("cloud-forwarder");
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.store = store;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(15000);
                VendingMessage ping = VendingMessage.heartbeat("Cloud");
                new SocketClient(targetHost, targetPort).send(ping);
                store.writeSummarySnapshot();
                System.out.println("[CLOUD] Server1 연결 확인 및 summary 저장");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[CLOUD] 연결 실패: " + e.getMessage());
            }
        }
    }
}
