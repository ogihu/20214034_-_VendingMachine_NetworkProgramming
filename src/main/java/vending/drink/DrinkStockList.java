package vending.drink;

/**
 * 음료 재고를 연결 리스트로 관리한다.
 * 판매 시 head를 제거하고, 보충 시 tail에 노드를 추가한다.
 */
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

    /** 재고 1개 추가 (관리자 보충) */
    public void addOne() {
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

    /** 재고 여러 개 추가 */
    public void addCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("보충 수량은 0 이상이어야 합니다.");
        }
        for (int i = 0; i < count; i++) {
            addOne();
        }
    }

    /** 판매 1건 처리 */
    public boolean sellOne() {
        if (head == null) {
            return false;
        }
        head = head.next;
        size--;
        return true;
    }
}
