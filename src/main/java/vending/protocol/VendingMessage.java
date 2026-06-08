package vending.protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class VendingMessage {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MessageType type;
    public String clientId;
    public String drink;
    public int amount;
    public int quantity;
    public int remaining;
    public int added;
    public String newName;
    public int price;
    public String date;
    public String serverId;
    public String host;
    public String payload;
    public String timestamp;
    public String messageId;

    public VendingMessage() {
        this.timestamp = LocalDateTime.now().format(TS);
        this.messageId = UUID.randomUUID().toString();
    }

    public static VendingMessage sale(String clientId, String drink, int amount, String date) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.SALE;
        m.clientId = clientId;
        m.drink = drink;
        m.amount = amount;
        m.quantity = 1;
        m.date = date;
        return m;
    }

    public static VendingMessage stock(String clientId, String drink, int remaining) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.STOCK;
        m.clientId = clientId;
        m.drink = drink;
        m.remaining = remaining;
        return m;
    }

    public static VendingMessage stockAlert(String clientId, String drink, int remaining) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.STOCK_ALERT;
        m.clientId = clientId;
        m.drink = drink;
        m.remaining = remaining;
        return m;
    }

    public static VendingMessage restock(String clientId, String drink, int added, int remaining) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.RESTOCK;
        m.clientId = clientId;
        m.drink = drink;
        m.added = added;
        m.remaining = remaining;
        return m;
    }

    public static VendingMessage drinkUpdate(String clientId, String drink, String newName, int price) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.DRINK_UPDATE;
        m.clientId = clientId;
        m.drink = drink;
        m.newName = newName;
        m.price = price;
        return m;
    }

    public static VendingMessage collect(String clientId, int amount) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.COLLECT;
        m.clientId = clientId;
        m.amount = amount;
        return m;
    }

    public static VendingMessage sync(String serverId, String payload) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.SYNC;
        m.serverId = serverId;
        m.payload = encodeB64(payload);
        return m;
    }

    public static VendingMessage heartbeat(String serverId) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.HEARTBEAT;
        m.serverId = serverId;
        return m;
    }

    public static VendingMessage heartbeatAck(String serverId) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.HEARTBEAT_ACK;
        m.serverId = serverId;
        return m;
    }

    public static VendingMessage activeServer(String serverId, String host, int port) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.ACTIVE_SERVER;
        m.serverId = serverId;
        m.host = host;
        m.amount = port;
        return m;
    }

    public static VendingMessage queryAlerts() {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.QUERY_ALERTS;
        return m;
    }

    public static VendingMessage querySales() {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.QUERY_SALES;
        return m;
    }

    public static VendingMessage payloadResponse(MessageType type, String payload) {
        VendingMessage m = new VendingMessage();
        m.type = type;
        m.payload = payload;
        return m;
    }

    public static VendingMessage error(String message) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.ERROR;
        m.payload = message;
        return m;
    }

    public static VendingMessage remoteDrinkSet(String targetClientId, int index, String oldName,
                                                String newName, int price) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.REMOTE_DRINK_SET;
        m.clientId = targetClientId;
        m.drink = oldName;
        m.newName = newName;
        m.price = price;
        m.quantity = index;
        return m;
    }

    public static VendingMessage remoteDrinkUpdate(int index, String oldName, String newName, int price) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.REMOTE_DRINK_UPDATE;
        m.drink = oldName;
        m.newName = newName;
        m.price = price;
        m.quantity = index;
        return m;
    }

    public static VendingMessage queryRemote(String clientId) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.QUERY_REMOTE;
        m.clientId = clientId;
        return m;
    }

    public static VendingMessage snapshotRequest(String serverId) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.SNAPSHOT_REQUEST;
        m.serverId = serverId;
        return m;
    }

    public static VendingMessage snapshot(String serverId, String snapshotPayload) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.SNAPSHOT;
        m.serverId = serverId;
        m.payload = encodeB64(snapshotPayload);
        return m;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"type\":\"").append(type).append("\"");
        append(sb, "clientId", clientId);
        append(sb, "drink", drink);
        append(sb, "newName", newName);
        append(sb, "date", date);
        append(sb, "serverId", serverId);
        append(sb, "host", host);
        append(sb, "payload", payload);
        append(sb, "timestamp", timestamp);
        append(sb, "messageId", messageId);
        if (amount != 0) {
            sb.append(",\"amount\":").append(amount);
        }
        if (quantity != 0) {
            sb.append(",\"quantity\":").append(quantity);
        }
        if (remaining != 0 || type == MessageType.STOCK || type == MessageType.STOCK_ALERT) {
            sb.append(",\"remaining\":").append(remaining);
        }
        if (added != 0) {
            sb.append(",\"added\":").append(added);
        }
        if (price != 0) {
            sb.append(",\"price\":").append(price);
        }
        sb.append("}");
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(",\"").append(key).append("\":\"").append(escape(value)).append("\"");
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String decodeB64(String b64) {
        if (b64 == null || b64.isBlank()) {
            return "";
        }
        return new String(java.util.Base64.getDecoder().decode(b64), java.nio.charset.StandardCharsets.UTF_8);
    }

    public static String encodeB64(String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        return java.util.Base64.getEncoder().encodeToString(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static VendingMessage fromJson(String json) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.valueOf(extractString(json, "type"));
        m.clientId = extractString(json, "clientId");
        m.drink = extractString(json, "drink");
        m.newName = extractString(json, "newName");
        m.date = extractString(json, "date");
        m.serverId = extractString(json, "serverId");
        m.host = extractString(json, "host");
        if (m.host.isEmpty()) {
            m.host = extractString(json, "clientId");
        }
        m.payload = extractString(json, "payload");
        m.timestamp = extractString(json, "timestamp");
        m.amount = extractInt(json, "amount");
        m.quantity = extractInt(json, "quantity");
        m.remaining = extractInt(json, "remaining");
        m.added = extractInt(json, "added");
        m.price = extractInt(json, "price");
        m.messageId = extractString(json, "messageId");
        if (m.messageId == null || m.messageId.isBlank()) {
            m.messageId = legacyMessageId(m);
        }
        return m;
    }

    private static String legacyMessageId(VendingMessage m) {
        return String.join("|",
                safe(m.type == null ? "" : m.type.name()),
                safe(m.clientId),
                safe(m.drink),
                safe(m.newName),
                safe(m.date),
                safe(m.timestamp),
                String.valueOf(m.amount),
                String.valueOf(m.quantity),
                String.valueOf(m.remaining),
                String.valueOf(m.added),
                String.valueOf(m.price));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String extractString(String json, String key) {
        String token = "\"" + key + "\":\"";
        int start = json.indexOf(token);
        if (start < 0) {
            return "";
        }
        start += token.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(c);
                        break;
                    default:
                        sb.append(c);
                        break;
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int extractInt(String json, String key) {
        String token = "\"" + key + "\":";
        int start = json.indexOf(token);
        if (start < 0) {
            return 0;
        }
        start += token.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (end == start) {
            return 0;
        }
        return Integer.parseInt(json.substring(start, end));
    }
}
