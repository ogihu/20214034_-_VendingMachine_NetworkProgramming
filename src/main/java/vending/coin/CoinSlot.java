package vending.coin;

import java.io.Serializable;

/**
 * 동전/지폐 1종의 보관함.
 */
public class CoinSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int value;
    private int count;

    public CoinSlot(int value, int count) {
        this.value = value;
        this.count = count;
    }

    public int getValue() {
        return value;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void add(int n) {
        count += n;
    }

    public boolean take(int n) {
        if (count < n) {
            return false;
        }
        count -= n;
        return true;
    }
}
