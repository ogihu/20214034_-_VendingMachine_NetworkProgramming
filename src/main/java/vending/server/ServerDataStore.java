package vending.server;

import vending.config.DataPaths;
import vending.protocol.VendingMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버 매출·재고·알림 집계 및 파일 저장.
 */
public class ServerDataStore {

    private final Path salesLog = DataPaths.ROOT.resolve("server").resolve("sales.log");
    private final Path stockLog = DataPaths.ROOT.resolve("server").resolve("stock.log");
    private final Path alertLog = DataPaths.ROOT.resolve("server").resolve("alerts.log");
    private final Path summaryLog = DataPaths.ROOT.resolve("server").resolve("summary.log");

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> stockByClient =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> totalSalesByClient = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> dailyByDrink =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> monthlyByDrink =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> drinkNames = new ConcurrentHashMap<>();
    private final List<String> recentAlerts = Collections.synchronizedList(new ArrayList<>());

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
                addSale(message);
                break;
            case STOCK:
            case RESTOCK:
                updateStock(message.clientId, message.drink, message.remaining);
                appendLine(stockLog, message.toJson());
                break;
            case STOCK_ALERT:
                registerAlert(message);
                appendLine(alertLog, message.toJson());
                updateStock(message.clientId, message.drink, message.remaining);
                break;
            case DRINK_UPDATE:
                drinkNames.put(message.clientId + "|" + message.drink, message.newName);
                appendLine(stockLog, message.toJson());
                break;
            case COLLECT:
                appendLine(stockLog, message.toJson());
                break;
            case SYNC:
                if (message.payload != null && !message.payload.isBlank()) {
                    apply(VendingMessage.fromJson(message.payload));
                }
                break;
            default:
                break;
        }
    }

    private void addSale(VendingMessage message) {
        totalSalesByClient.merge(message.clientId, message.amount, Integer::sum);

        String dayKey = message.date == null || message.date.isBlank()
                ? java.time.LocalDate.now().toString()
                : message.date;
        String monthKey = dayKey.length() >= 7 ? dayKey.substring(0, 7) : dayKey;

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

    public int getStock(String clientId, String drink) {
        return stockByClient.getOrDefault(clientId, new ConcurrentHashMap<>()).getOrDefault(drink, -1);
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
        StringBuilder sb = new StringBuilder();
        sb.append("[클라이언트별 누적]\n");
        for (Map.Entry<String, Integer> e : totalSalesByClient.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("원\n");
        }
        sb.append("\n[월별 음료 매출]\n");
        for (Map.Entry<String, ConcurrentHashMap<String, Integer>> month : monthlyByDrink.entrySet()) {
            sb.append("== ").append(month.getKey()).append(" ==\n");
            for (Map.Entry<String, Integer> row : month.getValue().entrySet()) {
                sb.append("  ").append(row.getKey()).append(": ").append(row.getValue()).append("원\n");
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

    public void writeSummarySnapshot() throws IOException {
        appendLine(summaryLog, "---- " + java.time.LocalDateTime.now() + " ----");
        appendLine(summaryLog, buildSalesSummaryPayload().replace("\n", " | "));
    }

    private void appendLine(Path path, String line) throws IOException {
        Files.write(path, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
