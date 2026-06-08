package vending.coin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// 화폐재고기능
public class CoinInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<CoinSlot> slots = new ArrayList<>();

    public CoinInventory() {
        slots.add(new CoinSlot(1000, 20));
        slots.add(new CoinSlot(500, 30));
        slots.add(new CoinSlot(100, 40));
        slots.add(new CoinSlot(50, 40));
        slots.add(new CoinSlot(10, 50));
    }

    public List<CoinSlot> getSlots() {
        return slots;
    }

    public CoinSlot findSlot(int value) {
        for (CoinSlot slot : slots) {
            if (slot.getValue() == value) {
                return slot;
            }
        }
        return null;
    }

    public int totalBalance() {
        int sum = 0;
        for (CoinSlot slot : slots) {
            sum += slot.getValue() * slot.getCount();
        }
        return sum;
    }
}
