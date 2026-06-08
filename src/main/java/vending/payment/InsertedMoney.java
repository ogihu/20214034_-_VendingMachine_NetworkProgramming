package vending.payment;

// 투입화폐기능
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
            if (units[i] == 1000) {
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

    public int[] toArray() {
        int[] copy = new int[size];
        if (size > 0) {
            System.arraycopy(units, 0, copy, 0, size);
        }
        return copy;
    }

    public void release() {
        units = null;
        size = 0;
    }

    public boolean isReleased() {
        return units == null;
    }

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

        // 배열 확장
        int[] bigger = new int[units.length * 2];
        System.arraycopy(units, 0, bigger, 0, size);
        units = bigger;
    }
}
