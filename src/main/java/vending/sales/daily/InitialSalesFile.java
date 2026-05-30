package vending.sales.daily;

import vending.config.DataPaths;
import vending.sales.model.SaleEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 과제 요구: 사전에 저장해 둔 초기 매출 파일과 현재 매출 연동.
 * 경로: data/sales/initial.sales
 */
public class InitialSalesFile {

    private final Path path = DataPaths.ROOT.resolve("sales").resolve("initial.sales");

    public void ensureSample() throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        String sample = ""
                + "2026-01-15|Client1|믹스커피|2|400\n"
                + "2026-01-15|Client1|물|1|450\n"
                + "2026-02-10|Client2|캔커피|3|1500\n";
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
