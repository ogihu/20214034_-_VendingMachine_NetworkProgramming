package vending.network;

import vending.protocol.VendingMessage;
import vending.util.VendingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// 소켓클라이언트기능
public class SocketClient {

    private static final int MAX_SEND_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 300;

    private final String host;
    private final int port;

    public SocketClient() {
        this.host = System.getProperty("server.host", "127.0.0.1");
        this.port = Integer.parseInt(System.getProperty("server.port", "9090"));
    }

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void send(VendingMessage message) throws VendingException {
        VendingException last = null;
        for (int attempt = 1; attempt <= MAX_SEND_ATTEMPTS; attempt++) {
            try {
                sendOnce(message);
                return;
            } catch (IOException e) {
                last = new VendingException("서버 전송 실패 (" + attempt + "/" + MAX_SEND_ATTEMPTS + ")", e);
                sleepBeforeRetry(attempt);
            }
        }
        throw last;
    }

    public VendingMessage sendAndRead(VendingMessage message) throws VendingException {
        VendingException last = null;
        for (int attempt = 1; attempt <= MAX_SEND_ATTEMPTS; attempt++) {
            try {
                return sendAndReadOnce(message);
            } catch (IOException e) {
                last = new VendingException("서버 응답 실패 (" + attempt + "/" + MAX_SEND_ATTEMPTS + ")", e);
                sleepBeforeRetry(attempt);
            }
        }
        throw last;
    }

    public boolean ping() {
        try {
            send(VendingMessage.heartbeat("client"));
            return true;
        } catch (VendingException e) {
            return false;
        }
    }

    private void sendOnce(VendingMessage message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            OutputStream out = socket.getOutputStream();
            out.write((message.toJson() + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private VendingMessage sendAndReadOnce(VendingMessage message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            OutputStream out = socket.getOutputStream();
            out.write((message.toJson() + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return null;
            }
            VendingMessage response = VendingMessage.fromJson(line.trim());
            if (response.type == vending.protocol.MessageType.ERROR) {
                throw new IOException(response.payload == null ? "서버 오류" : response.payload);
            }
            return response;
        }
    }

    private void sleepBeforeRetry(int attempt) throws VendingException {
        if (attempt >= MAX_SEND_ATTEMPTS) {
            return;
        }
        try {
            Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VendingException("전송 중단", e);
        }
    }
}
