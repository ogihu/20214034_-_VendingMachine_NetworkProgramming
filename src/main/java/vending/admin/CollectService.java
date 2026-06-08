package vending.admin;

import vending.coin.CoinInventory;
import vending.coin.CoinSlot;

// 수금기능
public class CollectService {

    private static final int[] MIN_KEEP = {5, 5, 5, 5, 5};
    private static final int[] UNITS = {1000, 500, 100, 50, 10};

    public String collect(CoinInventory inventory, int requestAmount) {
        if (requestAmount <= 0) {
            return "수금 금액을 입력하세요.";
        }

        int maxCollect = maxCollectable(inventory);
        if (requestAmount > maxCollect) {
            return "반환용 최소 화폐(각 5개)를 남겨야 합니다.";
        }

        int remain = requestAmount;
        int[] planned = new int[UNITS.length];
        for (int i = 0; i < UNITS.length; i++) {
            CoinSlot slot = inventory.findSlot(UNITS[i]);
            int available = slot.getCount() - MIN_KEEP[i];
            if (available <= 0) {
                continue;
            }
            int need = remain / UNITS[i];
            int take = Math.min(need, available);
            planned[i] = take;
            remain -= take * UNITS[i];
        }

        if (remain > 0) {
            return "요청 금액만큼 수금할 수 없습니다.";
        }

        for (int i = 0; i < UNITS.length; i++) {
            if (planned[i] <= 0) {
                continue;
            }
            if (!inventory.findSlot(UNITS[i]).take(planned[i])) {
                rollback(inventory, planned, i);
                return "수금 처리 중 오류가 발생했습니다.";
            }
        }
        return null;
    }

    private void rollback(CoinInventory inventory, int[] planned, int failedIndex) {
        for (int i = 0; i < failedIndex; i++) {
            if (planned[i] > 0) {
                inventory.findSlot(UNITS[i]).add(planned[i]);
            }
        }
    }

    public int maxCollectable(CoinInventory inventory) {
        int total = inventory.totalBalance();
        int minBalance = 0;
        for (int i = 0; i < UNITS.length; i++) {
            minBalance += MIN_KEEP[i] * UNITS[i];
        }
        return Math.max(0, total - minBalance);
    }
}
