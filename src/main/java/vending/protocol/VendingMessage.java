package vending.protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 한 줄로 주고받는 공통 메시지.
 */
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
    public String payload;
    public String timestamp;

    public VendingMessage() {
        this.timestamp = LocalDateTime.now().format(TS);
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
        m.payload = payload;
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
        m.clientId = host;
        m.amount = port;
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
        append(sb, "payload", payload);
        append(sb, "timestamp", timestamp);
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

    public static VendingMessage fromJson(String json) {
        VendingMessage m = new VendingMessage();
        m.type = MessageType.valueOf(extractString(json, "type"));
        m.clientId = extractString(json, "clientId");
        m.drink = extractString(json, "drink");
        m.newName = extractString(json, "newName");
        m.date = extractString(json, "date");
        m.serverId = extractString(json, "serverId");
        m.payload = extractString(json, "payload");
        m.timestamp = extractString(json, "timestamp");
        m.amount = extractInt(json, "amount");
        m.quantity = extractInt(json, "quantity");
        m.remaining = extractInt(json, "remaining");
        m.added = extractInt(json, "added");
        m.price = extractInt(json, "price");
        return m;
    }

    private static String extractString(String json, String key) {
        String token = "\"" + key + "\":\"";
        int start = json.indexOf(token);
        if (start < 0) {
            return "";
        }
        start += token.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            return "";
        }
        return json.substring(start, end);
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
