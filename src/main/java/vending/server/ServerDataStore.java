package vending.server;

import vending.config.DataPaths;
import vending.protocol.VendingMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버가 보관하는 매출·재고·알림 데이터.
 */
public class ServerDataStore {

    private final Path salesLog = DataPaths.ROOT.resolve("server").resolve("sales.log");
    private final Path stockLog = DataPaths.ROOT.resolve("server").resolve("stock.log");
    private final Path alertLog = DataPaths.ROOT.resolve("server").resolve("alerts.log");
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> stockByClient = new ConcurrentHashMap<>();

    public ServerDataStore() {
        try {
            Files.createDirectories(salesLog.getParent());
        } catch (IOException e) {
            throw new RuntimeException("서버 데이터 폴더 생성 실패", e);
        }
    }

    public synchronized void apply(VendingMessage message) throws IOException {
        switch (message.type) {
            case SALE:
                appendLine(salesLog, message.toJson());
                break;
            case STOCK:
            case RESTOCK:
                updateStock(message.clientId, message.drink, message.remaining);
                appendLine(stockLog, message.toJson());
                break;
            case STOCK_ALERT:
                appendLine(alertLog, message.toJson());
                updateStock(message.clientId, message.drink, message.remaining);
                break;
            case DRINK_UPDATE:
            case COLLECT:
                appendLine(stockLog, message.toJson());
                break;
            case SYNC:
                appendLine(salesLog, message.payload == null ? message.toJson() : message.payload);
                break;
            default:
                break;
        }
    }

    private void updateStock(String clientId, String drink, int remaining) {
        stockByClient.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>()).put(drink, remaining);
    }

    public int getStock(String clientId, String drink) {
        return stockByClient.getOrDefault(clientId, new ConcurrentHashMap<>()).getOrDefault(drink, -1);
    }

    public List<String> recentAlerts() throws IOException {
        if (!Files.exists(alertLog)) {
            return List.of();
        }
        return Files.readAllLines(alertLog, StandardCharsets.UTF_8);
    }

    private void appendLine(Path path, String line) throws IOException {
        Files.write(path, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
