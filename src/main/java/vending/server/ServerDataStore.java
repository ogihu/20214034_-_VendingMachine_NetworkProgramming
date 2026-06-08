package vending.server;

import vending.config.DataPaths;
import vending.protocol.MessageType;
import vending.protocol.VendingMessage;
import vending.util.AppLog;
import vending.util.VendingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// 서버데이터기능
public class ServerDataStore {

    private final Path salesLog = DataPaths.ROOT.resolve("server").resolve("sales.log");
    private final Path stockLog = DataPaths.ROOT.resolve("server").resolve("stock.log");
    private final Path alertLog = DataPaths.ROOT.resolve("server").resolve("alerts.log");
    private final Path summaryLog = DataPaths.ROOT.resolve("server").resolve("summary.log");
    private final Path snapshotFile = DataPaths.ROOT.resolve("server").resolve("state.snapshot");

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> stockByClient =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> totalSalesByClient = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> dailyTotalByClient =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> monthlyTotalByClient =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> dailyByDrink =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> monthlyByDrink =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> drinkNames = new ConcurrentHashMap<>();
    private final List<String> recentAlerts = Collections.synchronizedList(new ArrayList<>());
    private final List<String> syncHistory = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    private final List<String> processedOrder = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> remoteCommands =
            new ConcurrentHashMap<>();
    private volatile boolean importingSnapshot;

    public ServerDataStore() {
        try {
            Files.createDirectories(salesLog.getParent());
            loadSnapshotFromDisk();
        } catch (IOException e) {
            throw new RuntimeException("서버 데이터 폴더 생성 실패", e);
        }
    }

    public synchronized void apply(VendingMessage message) throws VendingException {
        // 메시지 적용
        try {
            applyInternal(message);
        } catch (VendingException e) {
            throw e;
        } catch (IOException e) {
            throw new VendingException("서버 데이터 처리 실패", e);
        }
    }

    private void applyInternal(VendingMessage message) throws IOException, VendingException {
        // 중복 방지
        if (isTracked(message) && isProcessed(message)) {
            return;
        }

        switch (message.type) {
            case SALE:
                appendIfLive(salesLog, message.toJson());
                addSale(message);
                finishTracked(message);
                break;
            case STOCK:
            case RESTOCK:
                updateStock(message.clientId, message.drink, message.remaining);
                appendIfLive(stockLog, message.toJson());
                finishTracked(message);
                break;
            case STOCK_ALERT:
                registerAlert(message);
                appendIfLive(alertLog, message.toJson());
                updateStock(message.clientId, message.drink, message.remaining);
                finishTracked(message);
                break;
            case DRINK_UPDATE:
                drinkNames.put(message.clientId + "|" + message.drink, message.newName);
                appendIfLive(stockLog, message.toJson());
                finishTracked(message);
                break;
            case COLLECT:
                appendIfLive(stockLog, message.toJson());
                finishTracked(message);
                break;
            case REMOTE_DRINK_SET:
                enqueueRemoteCommand(message.clientId,
                        VendingMessage.remoteDrinkUpdate(
                                message.quantity, message.drink, message.newName, message.price).toJson());
                finishTracked(message);
                break;
            case SYNC:
                if (message.payload != null && !message.payload.isBlank()) {
                    applyInternal(VendingMessage.fromJson(VendingMessage.decodeB64(message.payload)));
                }
                break;
            case SNAPSHOT:
                if (message.payload != null && !message.payload.isBlank()) {
                    importSnapshot(VendingMessage.decodeB64(message.payload));
                }
                break;
            default:
                break;
        }
    }

    private boolean isTracked(VendingMessage message) {
        switch (message.type) {
            case SALE:
            case STOCK:
            case RESTOCK:
            case STOCK_ALERT:
            case DRINK_UPDATE:
            case COLLECT:
            case REMOTE_DRINK_SET:
                return true;
            default:
                return false;
        }
    }

    private boolean isProcessed(VendingMessage message) {
        return message.messageId != null && processedMessageIds.contains(message.messageId);
    }

    private void finishTracked(VendingMessage message) throws IOException {
        rememberProcessed(message);
        recordSync(message);
    }

    private void rememberProcessed(VendingMessage message) {
        if (message.messageId == null || message.messageId.isBlank()) {
            return;
        }
        if (processedMessageIds.add(message.messageId)) {
            processedOrder.add(message.messageId);
            while (processedOrder.size() > 2000) {
                String old = processedOrder.remove(0);
                processedMessageIds.remove(old);
            }
        }
    }

    private void recordSync(VendingMessage message) throws IOException {
        if (importingSnapshot || message.type == MessageType.SYNC || message.type == MessageType.SNAPSHOT) {
            return;
        }
        syncHistory.add(message.toJson());
        while (syncHistory.size() > 800) {
            syncHistory.remove(0);
        }
        persistSnapshot();
    }

    public void enqueueRemoteCommand(String targetClientId, String commandJson) {
        remoteCommands
                .computeIfAbsent(targetClientId, k -> new ConcurrentLinkedQueue<>())
                .offer(commandJson);
    }

    public String drainRemoteCommands(String clientId) {
        ConcurrentLinkedQueue<String> q = remoteCommands.get(clientId);
        if (q == null || q.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = q.poll()) != null) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private void addSale(VendingMessage message) {
        // 매출 집계
        totalSalesByClient.merge(message.clientId, message.amount, Integer::sum);

        String dayKey = message.date == null || message.date.isBlank()
                ? java.time.LocalDate.now().toString()
                : message.date;
        String monthKey = dayKey.length() >= 7 ? dayKey.substring(0, 7) : dayKey;

        dailyTotalByClient
                .computeIfAbsent(dayKey, k -> new ConcurrentHashMap<>())
                .merge(message.clientId, message.amount, Integer::sum);

        monthlyTotalByClient
                .computeIfAbsent(monthKey, k -> new ConcurrentHashMap<>())
                .merge(message.clientId, message.amount, Integer::sum);

        dailyByDrink
                .computeIfAbsent(dayKey, k -> new ConcurrentHashMap<>())
                .merge(message.clientId + "|" + message.drink, message.amount, Integer::sum);

        monthlyByDrink
                .computeIfAbsent(monthKey, k -> new ConcurrentHashMap<>())
                .merge(message.clientId + "|" + message.drink, message.amount, Integer::sum);
    }

    private void registerAlert(VendingMessage message) {
        String line = message.timestamp + " | " + message.clientId + " | "
                + message.drink + " | 재고 " + message.remaining;
        recentAlerts.add(0, line);
        while (recentAlerts.size() > 50) {
            recentAlerts.remove(recentAlerts.size() - 1);
        }
    }

    private void updateStock(String clientId, String drink, int remaining) {
        stockByClient.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>()).put(drink, remaining);
    }

    public String exportSnapshot() {
        // 스냅샷 생성
        StringBuilder sb = new StringBuilder();
        synchronized (syncHistory) {
            for (String line : syncHistory) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    public synchronized void importSnapshot(String payload) throws VendingException {
        // 스냅샷 적용
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            importingSnapshot = true;
            String[] lines = payload.split("\n");
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                VendingMessage m = VendingMessage.fromJson(line.trim());
                if (m.type != MessageType.SYNC && m.type != MessageType.SNAPSHOT) {
                    applyInternal(m);
                    rememberSnapshotLine(line.trim());
                }
            }
            persistSnapshot();
        } catch (IOException e) {
            throw new VendingException("스냅샷 적용 실패", e);
        } finally {
            importingSnapshot = false;
        }
    }

    private void persistSnapshot() throws IOException {
        String snap = exportSnapshot();
        if (!snap.isBlank()) {
            Files.writeString(snapshotFile, snap, StandardCharsets.UTF_8);
        }
    }

    private void rememberSnapshotLine(String line) {
        synchronized (syncHistory) {
            if (!syncHistory.contains(line)) {
                syncHistory.add(line);
                while (syncHistory.size() > 800) {
                    syncHistory.remove(0);
                }
            }
        }
    }

    private void loadSnapshotFromDisk() {
        if (!Files.exists(snapshotFile)) {
            return;
        }
        try {
            String content = Files.readString(snapshotFile, StandardCharsets.UTF_8);
            if (!content.isBlank()) {
                importSnapshot(content);
            }
        } catch (Exception e) {
            AppLog.warn("SERVER", "스냅샷 파일 로드 실패: " + e.getMessage());
        }
    }

    public String buildAlertsPayload() {
        StringBuilder sb = new StringBuilder();
        synchronized (recentAlerts) {
            for (int i = 0; i < Math.min(10, recentAlerts.size()); i++) {
                sb.append(recentAlerts.get(i)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    public String buildSalesSummaryPayload() {
        // 집계 출력
        StringBuilder sb = new StringBuilder();
        sb.append("[전체 누적 매출]\n");
        sb.append(totalAmount(totalSalesByClient)).append("원\n");

        sb.append("[클라이언트별 누적 매출]\n");
        for (Map.Entry<String, Integer> e : totalSalesByClient.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("원\n");
        }
        sb.append("\n[일별 자판기 총매출]\n");
        for (Map.Entry<String, ConcurrentHashMap<String, Integer>> day : dailyTotalByClient.entrySet()) {
            sb.append("== ").append(day.getKey())
                    .append(" / 전체 ").append(totalAmount(day.getValue())).append("원 ==\n");
            for (Map.Entry<String, Integer> row : day.getValue().entrySet()) {
                sb.append("  ").append(row.getKey()).append(": ").append(row.getValue()).append("원\n");
            }
        }
        sb.append("\n[월별 자판기 총매출]\n");
        for (Map.Entry<String, ConcurrentHashMap<String, Integer>> month : monthlyTotalByClient.entrySet()) {
            sb.append("== ").append(month.getKey())
                    .append(" / 전체 ").append(totalAmount(month.getValue())).append("원 ==\n");
            for (Map.Entry<String, Integer> row : month.getValue().entrySet()) {
                sb.append("  ").append(row.getKey()).append(": ").append(row.getValue()).append("원\n");
            }
        }
        sb.append("\n[일별 음료별 매출]\n");
        for (Map.Entry<String, ConcurrentHashMap<String, Integer>> day : dailyByDrink.entrySet()) {
            sb.append("== ").append(day.getKey()).append(" ==\n");
            for (Map.Entry<String, Integer> row : day.getValue().entrySet()) {
                sb.append("  ").append(row.getKey()).append(": ").append(row.getValue()).append("원\n");
            }
        }
        sb.append("\n[월별 음료별 매출]\n");
        for (Map.Entry<String, ConcurrentHashMap<String, Integer>> month : monthlyByDrink.entrySet()) {
            sb.append("== ").append(month.getKey()).append(" ==\n");
            for (Map.Entry<String, Integer> row : month.getValue().entrySet()) {
                sb.append("  ").append(row.getKey()).append(": ").append(row.getValue()).append("원\n");
            }
        }
        if (!drinkNames.isEmpty()) {
            sb.append("\n[음료 이름 변경]\n");
            for (Map.Entry<String, String> e : drinkNames.entrySet()) {
                sb.append("  ").append(e.getKey()).append(" -> ").append(e.getValue()).append("\n");
            }
        }
        sb.append("\n[실시간 재고]\n");
        for (Map.Entry<String, ConcurrentHashMap<String, Integer>> client : stockByClient.entrySet()) {
            sb.append(client.getKey()).append("\n");
            for (Map.Entry<String, Integer> drink : client.getValue().entrySet()) {
                sb.append("  ").append(drink.getKey()).append(": ").append(drink.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private int totalAmount(Map<String, Integer> rows) {
        int sum = 0;
        for (Integer amount : rows.values()) {
            if (amount != null) {
                sum += amount;
            }
        }
        return sum;
    }

    public void writeSummarySnapshot() throws VendingException {
        try {
            appendLine(summaryLog, "---- " + java.time.LocalDateTime.now() + " ----");
            appendLine(summaryLog, buildSalesSummaryPayload().replace("\n", " | "));
        } catch (IOException e) {
            throw new VendingException("요약 저장 실패", e);
        }
    }

    private void appendLine(Path path, String line) throws IOException {
        Files.write(path, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void appendIfLive(Path path, String line) throws IOException {
        if (!importingSnapshot) {
            appendLine(path, line);
        }
    }
}
