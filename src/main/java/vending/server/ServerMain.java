package vending.server;

import vending.util.AppLog;
import vending.util.ConsoleEncoding;

// 서버실행기능
public class ServerMain {

    public static void main(String[] args) {
        ConsoleEncoding.configureUtf8();
        ServerRole role = ServerRole.valueOf(
                System.getenv().getOrDefault("SERVER_ROLE",
                        System.getProperty("server.role", "SERVER1")));

        int port = resolvePort(role);
        AppLog.info("SERVER", role + " 시작 port=" + port);

        ServerDataStore store = new ServerDataStore();
        PeerSyncManager peerSync = null;
        BackupHealthMonitor backupMonitor = null;

        String backupHost = envOrProp("BACKUP_SYNC_HOST", "backup.sync.host", "");
        int backupPort = Integer.parseInt(envOrProp("BACKUP_SYNC_PORT", "backup.sync.port", "9092"));

        if (role == ServerRole.SERVER1 || role == ServerRole.SERVER2) {
            String peerHost = envOrProp("PEER_HOST", "peer.host", "127.0.0.1");
            int peerPort = Integer.parseInt(envOrProp("PEER_PORT", "peer.port",
                    role == ServerRole.SERVER1 ? "9091" : "9090"));
            peerSync = new PeerSyncManager(role.name(), peerHost, peerPort, backupHost, backupPort, store);
            peerSync.start();

            ServerSummaryThread summary = new ServerSummaryThread(store, role.name());
            summary.setDaemon(true);
            summary.start();
        }

        if (role == ServerRole.BACKUP) {
            String s1Host = envOrProp("SERVER1_HOST", "server1.host", "127.0.0.1");
            int s1Port = Integer.parseInt(envOrProp("SERVER1_PORT", "server1.port", "9090"));
            String s2Host = envOrProp("SERVER2_HOST", "server2.host", "127.0.0.1");
            int s2Port = Integer.parseInt(envOrProp("SERVER2_PORT", "server2.port", "9091"));
            backupMonitor = new BackupHealthMonitor(s1Host, s1Port, s2Host, s2Port);
            backupMonitor.start();
        }

        if (role == ServerRole.CLOUD) {
            CloudForwarder forwarder = new CloudForwarder(
                    envOrProp("SERVER1_HOST", "server1.host", "127.0.0.1"),
                    Integer.parseInt(envOrProp("SERVER1_PORT", "server1.port", "9090")),
                    store);
            forwarder.start();
        }

        VendingServer server = new VendingServer(role, port, store, peerSync, backupMonitor);
        Thread t = new Thread(server, role.name());
        t.start();
    }

    // 포트설정
    private static int resolvePort(ServerRole role) {
        String railwayPort = System.getenv("PORT");
        if (railwayPort != null && !railwayPort.isBlank()) {
            return Integer.parseInt(railwayPort.trim());
        }
        return Integer.parseInt(System.getProperty("server.port", defaultPort(role)));
    }

    private static String envOrProp(String envKey, String propKey, String fallback) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return System.getProperty(propKey, fallback);
    }

    private static String defaultPort(ServerRole role) {
        switch (role) {
            case SERVER2:
                return "9091";
            case BACKUP:
                return "9092";
            case CLOUD:
                return "9093";
            default:
                return "9090";
        }
    }
}
