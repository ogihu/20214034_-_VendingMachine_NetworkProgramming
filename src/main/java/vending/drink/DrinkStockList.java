package vending.drink;

// 재고리스트기능
public class DrinkStockList implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private StockNode head;
    private int size;

    public DrinkStockList(int initialCount) {
        for (int i = 0; i < initialCount; i++) {
            addOne();
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isSoldOut() {
        return size <= 0;
    }

    public void addOne() {
        // 재고 추가
        StockNode node = new StockNode();
        if (head == null) {
            head = node;
        } else {
            StockNode cur = head;
            while (cur.next != null) {
                cur = cur.next;
            }
            cur.next = node;
        }
        size++;
    }

    public void addCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("보충 수량은 0 이상이어야 합니다.");
        }
        for (int i = 0; i < count; i++) {
            addOne();
        }
    }

    public void setCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
        while (size < count) {
            addOne();
        }
        while (size > count) {
            sellOne();
        }
    }

    public boolean sellOne() {
        if (head == null) {
            return false;
        }

        // 재고 배출
        head = head.next;
        size--;
        return true;
    }
}
