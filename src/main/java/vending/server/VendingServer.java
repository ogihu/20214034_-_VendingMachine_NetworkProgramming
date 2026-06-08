package vending.server;

import vending.protocol.MessageType;
import vending.protocol.VendingMessage;
import vending.util.AppLog;
import vending.util.VendingException;

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

// 소켓서버기능
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
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return;
            }

            VendingMessage response = process(VendingMessage.fromJson(line.trim()));
            if (response != null) {
                writeResponse(socket, response);
            }
        } catch (VendingException e) {
            AppLog.error("SERVER", e.getMessage(), e);
            writeError(socket, e.getMessage());
        } catch (Exception e) {
            AppLog.error("SERVER", "처리 오류", e);
            writeError(socket, "서버 처리 오류");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                AppLog.warn("SERVER", "소켓 종료 실패: " + e.getMessage());
            }
        }
    }

    private void writeResponse(Socket socket, VendingMessage message) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write((message.toJson() + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void writeError(Socket socket, String message) {
        try {
            writeResponse(socket, VendingMessage.error(message));
        } catch (IOException e) {
            AppLog.warn("SERVER", "오류 응답 전송 실패: " + e.getMessage());
        }
    }

    private VendingMessage process(VendingMessage message) throws VendingException {
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

            case QUERY_REMOTE:
                return VendingMessage.payloadResponse(
                        MessageType.REMOTE_COMMAND_LIST,
                        store.drainRemoteCommands(message.clientId));

            case SNAPSHOT_REQUEST:
                return VendingMessage.snapshot(role.name(), store.exportSnapshot());

            case SYNC:
                if (message.payload != null && !message.payload.isBlank()) {
                    String innerJson = VendingMessage.decodeB64(message.payload);
                    if (!innerJson.isBlank()) {
                        store.apply(VendingMessage.fromJson(innerJson));
                    }
                }
                return null;

            case SNAPSHOT:
                store.apply(message);
                return null;

            case REMOTE_DRINK_SET:
                store.apply(message);
                forwardIfServer(message);
                return VendingMessage.payloadResponse(MessageType.REMOTE_COMMAND_LIST, "OK");

            default:
                store.apply(message);
                forwardIfServer(message);
                logServerSideEvent(message);
                return null;
        }
    }

    private void logServerSideEvent(VendingMessage message) {
        if (role == ServerRole.BACKUP && backupMonitor != null && backupMonitor.isFailoverMode()) {
            if (failoverLogged.compareAndSet(false, true)) {
                System.out.println("[BACKUP] Failover 활성");
            }
        }
        if (role == ServerRole.CLOUD) {
            System.out.println("[CLOUD] " + message.type + " / " + message.clientId);
        }
        if (message.type == MessageType.STOCK_ALERT) {
            System.out.println("[ALERT] " + message.clientId + " / "
                    + message.drink + " 재고 " + message.remaining);
        }
    }

    private void forwardIfServer(VendingMessage message) {
        if ((role == ServerRole.SERVER1 || role == ServerRole.SERVER2) && peerSync != null) {
            peerSync.forward(message);
        }
    }
}
