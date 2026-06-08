package vending.sales.model;

import java.io.Serializable;
import java.time.LocalDate;

// 매출엔트리
public class SaleEntry implements Serializable, Comparable<SaleEntry> {

    private static final long serialVersionUID = 1L;

    private final String date;
    private final String drinkName;
    private final int quantity;
    private final int amount;
    private final String clientId;

    public SaleEntry(String clientId, String drinkName, int quantity, int amount, LocalDate date) {
        this.clientId = clientId;
        this.drinkName = drinkName;
        this.quantity = quantity;
        this.amount = amount;
        this.date = date.toString();
    }

    public SaleEntry(String clientId, String drinkName, int quantity, int amount, String date) {
        this.clientId = clientId;
        this.drinkName = drinkName;
        this.quantity = quantity;
        this.amount = amount;
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public String getMonthKey() {
        return date.substring(0, 7);
    }

    public String getDrinkName() {
        return drinkName;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getAmount() {
        return amount;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public int compareTo(SaleEntry other) {
        return other.date.compareTo(this.date);
    }

    public String toLine() {
        return date + "|" + clientId + "|" + drinkName + "|" + quantity + "|" + amount;
    }

    public static SaleEntry fromLine(String line) {
        String[] p = line.split("\\|");
        if (p.length != 5) {
            throw new IllegalArgumentException("잘못된 매출 라인: " + line);
        }
        return new SaleEntry(p[1], p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4]), p[0]);
    }
}
