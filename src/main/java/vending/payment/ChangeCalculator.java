package vending.payment;

import vending.coin.ChangeStack;
import vending.coin.CoinInventory;
import vending.coin.CoinSlot;

// 거스름계산기능
public class ChangeCalculator {

    private static final int[] UNITS = {1000, 500, 100, 50, 10};

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

    public static String preview(CoinInventory inventory, int amount) {
        if (amount <= 0) {
            return "거스름돈 없음";
        }

        CoinInventory copy = copyInventory(inventory);
        ChangeStack stack = new ChangeStack(16);
        if (!dispense(copy, amount, stack)) {
            return "거스름돈 부족";
        }
        return format(stack);
    }

    public static boolean dispense(CoinInventory inventory, int amount, ChangeStack out) {
        if (amount <= 0) {
            return true;
        }

        CoinInventory copy = copyInventory(inventory);
        int[] plan = buildChangePlan(copy, amount, 0);
        if (plan == null) {
            return false;
        }

        for (int unit : UNITS) {
            CoinSlot slot = inventory.findSlot(unit);
            if (slot == null) {
                continue;
            }
            int count = plan[indexOf(unit)];
            for (int i = 0; i < count; i++) {
                slot.take(1);
                out.push(unit);
            }
        }
        return true;
    }

    public static void refund(CoinInventory inventory, ChangeStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        for (int unit : stack.toArray()) {
            CoinSlot slot = inventory.findSlot(unit);
            if (slot != null) {
                slot.add(1);
            }
        }
    }

    public static String format(ChangeStack stack) {
        java.util.Map<Integer, Integer> counts = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        for (int unit : stack.toArray()) {
            counts.merge(unit, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append("원 ").append(e.getValue()).append("개");
        }
        return sb.length() == 0 ? "거스름돈 없음" : sb.toString();
    }

    private static boolean deduct(CoinInventory inventory, int amount) {
        return buildChangePlan(inventory, amount, 0) != null;
    }

    private static int[] buildChangePlan(CoinInventory inventory, int amount, int unitIndex) {
        if (amount == 0) {
            return new int[UNITS.length];
        }
        if (unitIndex >= UNITS.length) {
            return null;
        }

        int unit = UNITS[unitIndex];
        CoinSlot slot = inventory.findSlot(unit);
        int maxUse = slot == null ? 0 : Math.min(slot.getCount(), amount / unit);

        for (int use = maxUse; use >= 0; use--) {
            CoinInventory copy = copyInventory(inventory);
            CoinSlot copySlot = copy.findSlot(unit);
            if (use > 0) {
                copySlot.take(use);
            }
            int[] rest = buildChangePlan(copy, amount - use * unit, unitIndex + 1);
            if (rest != null) {
                rest[unitIndex] = use;
                return rest;
            }
        }
        return null;
    }

    private static int indexOf(int unit) {
        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i] == unit) {
                return i;
            }
        }
        return -1;
    }

    private static CoinInventory copyInventory(CoinInventory src) {
        CoinInventory copy = new CoinInventory();
        for (int i = 0; i < src.getSlots().size(); i++) {
            copy.getSlots().get(i).setCount(src.getSlots().get(i).getCount());
        }
        return copy;
    }
}
