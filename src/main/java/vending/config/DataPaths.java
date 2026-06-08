package vending.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// 데이터경로기능
public final class DataPaths {

    public static final Path PROJECT_ROOT = detectProjectRoot();
    public static final Path ROOT = PROJECT_ROOT.resolve("data");
    private static final Path ACTIVE = activeDataRoot();

    public static final Path SALES_DAILY = ACTIVE.resolve("sales").resolve("daily");
    public static final Path SALES_MONTHLY = ACTIVE.resolve("sales").resolve("monthly");
    public static final Path INVENTORY = ACTIVE.resolve("inventory");
    public static final Path NETWORK = ACTIVE.resolve("network");
    public static final Path PENDING_EVENTS = NETWORK.resolve("pending_events.jsonl");
    // 공통비밀번호경로
    public static final Path ADMIN = ROOT.resolve("admin");
    public static final Path SERVER_DIR = ROOT.resolve("server");
    public static final Path PRODUCT_IMAGES = ROOT.resolve("images").resolve("products");
    public static final Path PROJECT_IMAGES = PROJECT_ROOT.resolve("image");

    private DataPaths() {
    }

    private static Path activeDataRoot() {
        if (isServerProcess()) {
            return ROOT;
        }
        String clientId = System.getProperty("client.id");
        if (clientId != null && !clientId.isBlank()) {
            return ROOT.resolve("clients").resolve(clientId);
        }
        return ROOT;
    }

    private static boolean isServerProcess() {
        String role = System.getenv("SERVER_ROLE");
        if (role != null && !role.isBlank()) {
            return true;
        }
        role = System.getProperty("server.role");
        return role != null && !role.isBlank();
    }

    private static Path detectProjectRoot() {
        String configured = System.getProperty("vending.base");
        if (configured != null && !configured.isBlank()) {
            Path base = Paths.get(configured).toAbsolutePath().normalize();
            if (Files.isDirectory(base)) {
                return base;
            }
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (hasProjectMarkers(cwd)) {
            return cwd;
        }

        Path desktop = cwd.resolve("20214034_-_VendingMachine_NetworkProgramming");
        if (hasProjectMarkers(desktop)) {
            return desktop;
        }

        Path parent = cwd.getParent();
        if (parent != null && hasProjectMarkers(parent)) {
            return parent;
        }
        return cwd;
    }

    private static boolean hasProjectMarkers(Path base) {
        return Files.isDirectory(base.resolve("image"))
                || Files.isDirectory(base.resolve("src"))
                || Files.isDirectory(base.resolve("data"));
    }

    public static void ensureDirectories() throws vending.util.VendingException {
        try {
            Files.createDirectories(SALES_DAILY);
            Files.createDirectories(SALES_MONTHLY);
            Files.createDirectories(INVENTORY);
            Files.createDirectories(NETWORK);
            Files.createDirectories(ADMIN);
            Files.createDirectories(SERVER_DIR);
            Files.createDirectories(PRODUCT_IMAGES);
        } catch (Exception e) {
            throw new vending.util.VendingException("데이터 폴더 생성 실패", e);
        }
    }
}
