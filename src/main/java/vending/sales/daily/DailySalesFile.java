package vending.sales.daily;

import vending.config.DataPaths;
import vending.sales.model.SaleEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 일별매출기능
public class DailySalesFile {

    public Path filePath(LocalDate date) {
        return DataPaths.SALES_DAILY.resolve(date + ".sales");
    }

    public void append(SaleEntry entry) throws IOException {
        Path path = filePath(LocalDate.parse(entry.getDate()));
        Files.createDirectories(path.getParent());
        Files.write(path, (entry.toLine() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    public List<SaleEntry> load(LocalDate date) throws IOException {
        Path path = filePath(date);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        List<SaleEntry> list = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                list.add(SaleEntry.fromLine(line.trim()));
            }
        }
        return list;
    }

    public List<SaleEntry> loadAll() throws IOException {
        List<SaleEntry> all = new ArrayList<>();
        if (!Files.exists(DataPaths.SALES_DAILY)) {
            return all;
        }
        try (var stream = Files.list(DataPaths.SALES_DAILY)) {
            List<Path> files = stream.filter(f -> f.toString().endsWith(".sales")).collect(Collectors.toList());
            for (Path p : files) {
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    if (!line.trim().isEmpty()) {
                        all.add(SaleEntry.fromLine(line.trim()));
                    }
                }
            }
        }
        return all;
    }
}
