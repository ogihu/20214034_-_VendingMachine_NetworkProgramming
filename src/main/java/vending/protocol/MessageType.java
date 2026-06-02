package vending.protocol;

/**
 * 클라이언트-서버 간 메시지 종류.
 */
public enum MessageType {
    SALE,
    STOCK,
    STOCK_ALERT,
    RESTOCK,
    DRINK_UPDATE,
    COLLECT,
    SYNC,
    HEARTBEAT,
    HEARTBEAT_ACK,
    ACTIVE_SERVER,
    QUERY_ALERTS,
    QUERY_SALES,
    ALERT_LIST,
    SALES_SUMMARY
}
