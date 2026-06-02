package vending.server;

import vending.protocol.MessageType;
import vending.protocol.VendingMessage;
import vending.util.AppLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP 서버. Client/Peer/Backup 요청 처리.
 */
public class VendingServer implements Runnable {

    private final ServerRole role;
    private final int port;
    private final ServerDataStore store;
    private final PeerSyncManager peerSync;
    private final BackupHealthMonitor backupMonitor;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final AtomicBoolean failoverLogged = new AtomicBoolean(false);

    public VendingServer(ServerRole role, int port, ServerDataStore store,
                         PeerSyncManager peerSync, BackupHealthMonitor backupMonitor) {
        this.role = role;
        this.port = port;
        this.store = store;
        this.peerSync = peerSync;
        this.backupMonitor = backupMonitor;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] " + role + " listening on " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handle(socket));
            }
        } catch (IOException e) {
            AppLog.error("SERVER", "종료", e);
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return;
            }

            VendingMessage message = VendingMessage.fromJson(line.trim());
            VendingMessage response = process(message);

            if (response != null) {
                OutputStream out = s.getOutputStream();
                out.write((response.toJson() + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (Exception e) {
            AppLog.error("SERVER", "처리 오류", e);
        }
    }

    private VendingMessage process(VendingMessage message) throws IOException {
        switch (message.type) {
            case HEARTBEAT:
                if (role == ServerRole.BACKUP && backupMonitor != null
                        && "bootstrap".equals(message.serverId)) {
                    return backupMonitor.activeServerMessage();
                }
                return VendingMessage.heartbeatAck(role.name());

            case ACTIVE_SERVER:
                if (backupMonitor != null) {
                    return backupMonitor.activeServerMessage();
                }
                return VendingMessage.activeServer(role.name(), "127.0.0.1", port);

            case QUERY_ALERTS:
                return VendingMessage.payloadResponse(MessageType.ALERT_LIST, store.buildAlertsPayload());

            case QUERY_SALES:
                return VendingMessage.payloadResponse(MessageType.SALES_SUMMARY, store.buildSalesSummaryPayload());

            case SYNC:
                if (message.payload != null && !message.payload.isBlank()) {
                    String innerJson = VendingMessage.decodeB64(message.payload);
                    if (!innerJson.isBlank()) {
                        store.apply(VendingMessage.fromJson(innerJson));
                    }
                }
                return null;

            default:
                store.apply(message);

                if (role == ServerRole.SERVER1 || role == ServerRole.SERVER2) {
                    if (peerSync != null) {
                        peerSync.forward(message);
                    }
                }

                if (role == ServerRole.BACKUP && backupMonitor != null && backupMonitor.isFailoverMode()) {
                    if (failoverLogged.compareAndSet(false, true)) {
                        System.out.println("[BACKUP] Failover 활성 - Server1/2 대체 처리 중");
                    }
                }

                if (role == ServerRole.CLOUD) {
                    System.out.println("[CLOUD] 백업 수신: " + message.type + " / " + message.clientId);
                }

                if (message.type == MessageType.STOCK_ALERT) {
                    System.out.println("[ALERT] " + message.clientId + " / "
                            + message.drink + " 재고 " + message.remaining);
                }
                return null;
        }
    }
}
