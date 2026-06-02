package vending.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 파일 + 콘솔 로그. 예외 기록용.
 */
public final class AppLog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_PATH = Path.of("data", "app.log");

    private AppLog() {
    }

    public static void info(String tag, String message) {
        write("INFO", tag, message);
    }

    public static void warn(String tag, String message) {
        write("WARN", tag, message);
    }

    public static void error(String tag, String message, Throwable t) {
        write("ERROR", tag, message + (t == null ? "" : " / " + t.getMessage()));
        if (t != null) {
            t.printStackTrace();
        }
    }

    private static void write(String level, String tag, String message) {
        String line = FMT.format(LocalDateTime.now()) + " [" + level + "][" + tag + "] " + message;
        System.out.println(line);
        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.writeString(LOG_PATH, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
