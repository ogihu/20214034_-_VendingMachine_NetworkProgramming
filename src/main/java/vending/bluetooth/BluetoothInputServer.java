package vending.bluetooth;

import vending.kiosk.KioskService;
import vending.util.AppLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// 블루투스입력기능
public class BluetoothInputServer extends Thread {

    private static final int CLIENT_IDLE_MS = 300_000;

    private final KioskService service;
    private final int port;
    private volatile boolean running = true;

    public BluetoothInputServer(KioskService service, int port) {
        super("bluetooth-input");
        this.service = service;
        this.port = port;
    }

    @Override
    public void run() {
        BluetoothCommandHandler handler = new BluetoothCommandHandler(service);
        AppLog.info("BT", "입력 서버 시작 port=" + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            while (running && !Thread.currentThread().isInterrupted()) {
                try (Socket client = serverSocket.accept()) {
                    client.setSoTimeout(CLIENT_IDLE_MS);
                    AppLog.info("BT", "클라이언트 연결: " + client.getRemoteSocketAddress());
                    handleClient(client, handler);
                } catch (IOException e) {
                    if (running) {
                        AppLog.warn("BT", "클라이언트 처리 실패: " + e.getMessage());
                    }
                }
            }
        } catch (BindException e) {
            AppLog.error("BT", "포트 " + port + " 바인드 실패 (이미 사용 중일 수 있음)", e);
        } catch (IOException e) {
            AppLog.error("BT", "입력 서버 종료", e);
        }
    }

    private void handleClient(Socket client, BluetoothCommandHandler handler) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        OutputStream out = client.getOutputStream();

        String line;
        while ((line = reader.readLine()) != null) {
            String response = handler.handle(line);
            out.write((response + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            AppLog.info("BT", "명령=" + line.trim() + " 응답=" + response);
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }
}
