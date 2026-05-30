package vending.payment;

import vending.coin.ChangeStack;
import vending.coin.CoinInventory;
import vending.coin.CoinSlot;

/**
 * 거스름돈 계산 및 실제 반환 처리.
 */
public class ChangeCalculator {

    private static final int[] UNITS = {1000, 500, 100, 50, 10};

    /**
     * @return null이면 반환 가능, 문자열이면 거스름 불가 사유
     */
    public static String canMakeChange(CoinInventory inventory, int amount) {
        if (amount <= 0) {
            return null;
        }
        CoinInventory copy = copyInventory(inventory);
        if (!deduct(copy, amount)) {
            return "거스름돈 없음";
        }
        return null;
    }

    /**
     * 거스름 반환. 성공 시 ChangeStack에 반환 단위를 담는다.
     */
    public static boolean dispense(CoinInventory inventory, int amount, ChangeStack out) {
        if (amount <= 0) {
            return true;
        }

        int remaining = amount;
        for (int unit : UNITS) {
            CoinSlot slot = inventory.findSlot(unit);
            if (slot == null) {
                continue;
            }
            while (remaining >= unit && slot.getCount() > 0) {
                slot.take(1);
                out.push(unit);
                remaining -= unit;
            }
        }

        return remaining == 0;
    }

    private static boolean deduct(CoinInventory inventory, int amount) {
        int remaining = amount;
        for (int unit : UNITS) {
            CoinSlot slot = inventory.findSlot(unit);
            if (slot == null) {
                continue;
            }
            int need = remaining / unit;
            int use = Math.min(need, slot.getCount());
            slot.take(use);
            remaining -= use * unit;
        }
        return remaining == 0;
    }

    private static CoinInventory copyInventory(CoinInventory src) {
        CoinInventory copy = new CoinInventory();
        for (int i = 0; i < src.getSlots().size(); i++) {
            copy.getSlots().get(i).setCount(src.getSlots().get(i).getCount());
        }
        return copy;
    }
}
