package vending.event;

import vending.protocol.VendingMessage;
import vending.util.AppLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

// 보류저장기능
public class PendingEventStore {

    private final Path path;

    public PendingEventStore(Path path) {
        this.path = path;
    }

    public synchronized void append(VendingMessage message) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, message.toJson() + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized List<VendingMessage> drainAll() {
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            List<VendingMessage> loaded = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                loaded.add(VendingMessage.fromJson(line.trim()));
            }
            Files.deleteIfExists(path);
            return loaded;
        } catch (IOException e) {
            AppLog.warn("EVENT", "보류 이벤트 로드 실패: " + path + " / " + e.getMessage());
            return List.of();
        }
    }

    public synchronized int count() {
        if (!Files.exists(path)) {
            return 0;
        }
        try {
            return (int) Files.lines(path).count();
        } catch (IOException e) {
            AppLog.warn("EVENT", "보류 이벤트 개수 조회 실패: " + path + " / " + e.getMessage());
            return 0;
        }
    }
}
