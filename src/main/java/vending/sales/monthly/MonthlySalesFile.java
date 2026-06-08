package vending.sales.monthly;

import vending.config.DataPaths;
import vending.sales.model.SaleEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 월별매출기능
public class MonthlySalesFile {

    public Path filePath(String monthKey) {
        return DataPaths.SALES_MONTHLY.resolve(monthKey + ".sales");
    }

    public void upsert(SaleEntry entry) throws IOException {
        Map<String, int[]> map = loadMap(entry.getMonthKey());
        int[] cur = map.getOrDefault(entry.getDrinkName(), new int[]{0, 0});
        cur[0] += entry.getQuantity();
        cur[1] += entry.getAmount();
        map.put(entry.getDrinkName(), cur);
        saveMap(entry.getMonthKey(), map);
    }

    public List<String[]> loadRows(String monthKey) throws IOException {
        Map<String, int[]> map = loadMap(monthKey);
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, int[]> e : map.entrySet()) {
            rows.add(new String[]{monthKey, e.getKey(), String.valueOf(e.getValue()[0]), String.valueOf(e.getValue()[1])});
        }
        return rows;
    }

    private Map<String, int[]> loadMap(String monthKey) throws IOException {
        Map<String, int[]> map = new HashMap<>();
        Path path = filePath(monthKey);
        if (!Files.exists(path)) {
            return map;
        }
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] p = line.split("\\|");
            map.put(p[0], new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2])});
        }
        return map;
    }

    private void saveMap(String monthKey, Map<String, int[]> map) throws IOException {
        Path path = filePath(monthKey);
        Files.createDirectories(path.getParent());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[]> e : map.entrySet()) {
            sb.append(e.getKey()).append('|')
                    .append(e.getValue()[0]).append('|')
                    .append(e.getValue()[1]).append(System.lineSeparator());
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }
}
