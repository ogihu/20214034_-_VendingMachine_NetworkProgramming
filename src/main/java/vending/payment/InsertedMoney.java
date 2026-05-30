package vending.payment;

/**
 * 이번 거래에 투입된 화폐를 동적 배열로 관리한다.
 * 반환 또는 판매 종료 시 release()로 해제한다.
 */
public class InsertedMoney {

    private int[] units;
    private int size;

    public InsertedMoney() {
        units = new int[4];
        size = 0;
    }

    public void add(int unit) {
        ensureCapacity(size + 1);
        units[size++] = unit;
    }

    public int total() {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += units[i];
        }
        return sum;
    }

    public int billTotal() {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            if (units[i] >= 500) {
                sum += units[i];
            }
        }
        return sum;
    }

    public int size() {
        return size;
    }

    public int get(int index) {
        return units[index];
    }

    /** 동적 할당 해제 */
    public void release() {
        units = null;
        size = 0;
    }

    public boolean isReleased() {
        return units == null;
    }

    /** 새 거래 시작 */
    public void reset() {
        if (units == null) {
            units = new int[4];
        }
        size = 0;
    }

    private void ensureCapacity(int needed) {
        if (units == null) {
            units = new int[4];
        }
        if (needed <= units.length) {
            return;
        }
        int[] bigger = new int[units.length * 2];
        System.arraycopy(units, 0, bigger, 0, size);
        units = bigger;
    }
}
