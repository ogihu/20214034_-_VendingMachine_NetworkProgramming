package vending.server;

import vending.protocol.VendingMessage;

import vending.util.AppLog;
import vending.util.VendingException;

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
                AppLog.info("SUMMARY", "===== [" + serverId + "] 집계 =====");
                for (String line : store.buildSalesSummaryPayload().split("\n")) {
                    if (!line.isBlank()) {
                        AppLog.info("SUMMARY", line);
                    }
                }
                store.writeSummarySnapshot();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (VendingException e) {
                AppLog.warn("SUMMARY", e.getMessage());
            }
        }
    }
}
