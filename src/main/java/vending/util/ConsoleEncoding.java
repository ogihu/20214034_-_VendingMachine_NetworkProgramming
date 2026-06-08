package vending.util;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// 콘솔인코딩기능
public final class ConsoleEncoding {

    private ConsoleEncoding() {
    }

    public static void configureUtf8() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return;
        }
        try {
            new ProcessBuilder("cmd.exe", "/c", "chcp", "65001")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
            Charset utf8 = StandardCharsets.UTF_8;
            System.setOut(new PrintStream(System.out, true, utf8));
            System.setErr(new PrintStream(System.err, true, utf8));
        } catch (Exception e) {
            AppLog.warn("ENCODING", "콘솔 UTF-8 설정 실패: " + e.getMessage());
        }
    }
}
