package vending.config;

import vending.util.AppLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * config/clientX.properties 로 Client/Server 접속 정보 로드.
 */
public final class ClientConfig {

    private ClientConfig() {
    }

    public static void load(String path) {
        Properties props = new Properties();
        Path file = Paths.get(path);

        try (InputStream in = openStream(file)) {
            if (in == null) {
                return;
            }
            props.load(in);
            apply(props);
        } catch (Exception e) {
            AppLog.error("CONFIG", "로드 실패: " + path, e);
        }
    }

    private static InputStream openStream(Path file) throws IOException {
        if (Files.exists(file)) {
            return Files.newInputStream(file);
        }
        String resource = "/config/" + file.getFileName();
        InputStream in = ClientConfig.class.getResourceAsStream(resource);
        if (in != null) {
            return in;
        }
        return null;
    }

    private static void apply(Properties props) {
        setIfPresent("client.id", props.getProperty("client.id"));
        setIfPresent("server.host", props.getProperty("server.host"));
        setIfPresent("server.port", props.getProperty("server.port"));
        setIfPresent("backup.host", props.getProperty("backup.host"));
        setIfPresent("backup.port", props.getProperty("backup.port"));
        setIfPresent("cloud.host", props.getProperty("cloud.host"));
        setIfPresent("cloud.port", props.getProperty("cloud.port"));
        setIfPresent("bluetooth.enabled", props.getProperty("bluetooth.enabled"));
        setIfPresent("bluetooth.port", props.getProperty("bluetooth.port"));
    }

    private static void setIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            System.setProperty(key, value.trim());
        }
    }
}
