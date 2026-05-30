package vending.network;

import vending.protocol.VendingMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 서버와 TCP(JSON) 통신.
 */
public class SocketClient {

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

    public void send(VendingMessage message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1500);
            OutputStream out = socket.getOutputStream();
            out.write((message.toJson() + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    public VendingMessage sendAndRead(VendingMessage message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1500);
            OutputStream out = socket.getOutputStream();
            out.write((message.toJson() + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return null;
            }
            return VendingMessage.fromJson(line.trim());
        }
    }

    public boolean ping() {
        try {
            send(VendingMessage.heartbeat("client"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
