package vending.network;

import vending.util.AppLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// 페일오버기능
public final class ActiveServerResolver {

    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ActiveServerResolver() {
    }

    public static void onServerChanged(Runnable listener) {
        listeners.add(listener);
    }

    public static void refresh() {
        String prevHost = host();
        int prevPort = port();
        ClientBootstrap.resolveActiveServer();
        String nextHost = host();
        int nextPort = port();
        if (!nextHost.equals(prevHost) || nextPort != prevPort) {
            AppLog.info("SERVER", "활성 서버 전환: " + prevHost + ":" + prevPort
                    + " -> " + nextHost + ":" + nextPort);
            for (Runnable listener : listeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    AppLog.warn("SERVER", "서버 전환 리스너 오류: " + e.getMessage());
                }
            }
        }
    }

    public static String host() {
        return System.getProperty("server.host", "127.0.0.1");
    }

    public static int port() {
        try {
            return Integer.parseInt(System.getProperty("server.port", "9090"));
        } catch (NumberFormatException e) {
            AppLog.warn("SERVER", "server.port 형식 오류, 9090 사용: " + e.getMessage());
            return 9090;
        }
    }
}
