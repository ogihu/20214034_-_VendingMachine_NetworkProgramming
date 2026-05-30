package vending.server;

/**
 * Server1 / Server2 / Backup / Cloud 실행 진입점.
 *
 * 예)
 * java -Dserver.role=SERVER1 -Dserver.port=9090 -Dpeer.host=127.0.0.1 -Dpeer.port=9091 vending.server.ServerMain
 */
public class ServerMain {

    public static void main(String[] args) {
        ServerRole role = ServerRole.valueOf(System.getProperty("server.role", "SERVER1"));
        int port = Integer.parseInt(System.getProperty("server.port", defaultPort(role)));

        ServerDataStore store = new ServerDataStore();
        PeerSyncManager peerSync = null;
        BackupHealthMonitor backupMonitor = null;

        if (role == ServerRole.SERVER1 || role == ServerRole.SERVER2) {
            String peerHost = System.getProperty("peer.host", "127.0.0.1");
            int peerPort = Integer.parseInt(System.getProperty("peer.port",
                    role == ServerRole.SERVER1 ? "9091" : "9090"));
            peerSync = new PeerSyncManager(role.name(), peerHost, peerPort);
            peerSync.start();
        }

        if (role == ServerRole.BACKUP) {
            String s1Host = System.getProperty("server1.host", "127.0.0.1");
            int s1Port = Integer.parseInt(System.getProperty("server1.port", "9090"));
            String s2Host = System.getProperty("server2.host", "127.0.0.1");
            int s2Port = Integer.parseInt(System.getProperty("server2.port", "9091"));
            backupMonitor = new BackupHealthMonitor(s1Host, s1Port, s2Host, s2Port);
            backupMonitor.start();
        }

        if (role == ServerRole.CLOUD) {
            CloudForwarder forwarder = new CloudForwarder(
                    System.getProperty("server1.host", "127.0.0.1"),
                    Integer.parseInt(System.getProperty("server1.port", "9090")));
            forwarder.start();
        }

        VendingServer server = new VendingServer(role, port, store, peerSync, backupMonitor);
        Thread t = new Thread(server, role.name());
        t.start();
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
