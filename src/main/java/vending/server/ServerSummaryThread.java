package vending.server;

import vending.protocol.VendingMessage;

import java.io.IOException;

/**
 * Server1/2 에서 주기적으로 매출·재고 요약 출력.
 */
public class ServerSummaryThread extends Thread {

    private final ServerDataStore store;
    private final String serverId;

    public ServerSummaryThread(ServerDataStore store, String serverId) {
        super("server-summary-" + serverId);
        this.store = store;
        this.serverId = serverId;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(30000);
                System.out.println("\n===== [" + serverId + "] 집계 =====");
                System.out.println(store.buildSalesSummaryPayload());
                store.writeSummarySnapshot();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("[SUMMARY] 저장 실패: " + e.getMessage());
            }
        }
    }
}
