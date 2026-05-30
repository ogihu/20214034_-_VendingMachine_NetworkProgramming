package vending.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 데이터 파일이 저장되는 경로를 한곳에서 관리한다.
 */
public final class DataPaths {

    public static final Path ROOT = Paths.get("data");
    public static final Path SALES_DAILY = ROOT.resolve("sales").resolve("daily");
    public static final Path SALES_MONTHLY = ROOT.resolve("sales").resolve("monthly");
    public static final Path INVENTORY = ROOT.resolve("inventory");
    public static final Path ADMIN = ROOT.resolve("admin");

    private DataPaths() {
    }

    public static void ensureDirectories() {
        try {
            Files.createDirectories(SALES_DAILY);
            Files.createDirectories(SALES_MONTHLY);
            Files.createDirectories(INVENTORY);
            Files.createDirectories(ADMIN);
        } catch (Exception e) {
            throw new RuntimeException("데이터 폴더 생성 실패", e);
        }
    }
}
