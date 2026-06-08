package vending.sales.daily;

import vending.config.DataPaths;
import vending.sales.model.SaleEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// 초기매출기능
public class InitialSalesFile {

    private final Path path;
    private final String clientId;

    public InitialSalesFile(String clientId) {
        this.clientId = clientId == null || clientId.isBlank() ? "Client1" : clientId;
        this.path = DataPaths.SALES_DAILY.getParent().resolve("initial.sales");
    }

    public void ensureSample() throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        String sample = ""
                + "2026-01-15|" + clientId + "|믹스커피|2|400\n"
                + "2026-01-15|" + clientId + "|생수|1|450\n"
                + "2026-02-10|" + clientId + "|캔커피|3|1500\n";
        Files.writeString(path, sample, StandardCharsets.UTF_8);
    }

    public List<SaleEntry> load() throws IOException {
        ensureSample();
        List<SaleEntry> list = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                list.add(SaleEntry.fromLine(line.trim()));
            }
        }
        return list;
    }
}
