package vending.coin;

/**
 * 거스름돈 반환 시 사용할 스택.
 * 큰 단위부터 반환하기 위해 금액을 스택에 쌓아 둔다.
 */
public class ChangeStack {

    private int[] values;
    private int top;

    public ChangeStack(int capacity) {
        values = new int[capacity];
        top = -1;
    }

    public void push(int value) {
        if (top >= values.length - 1) {
            int[] bigger = new int[values.length + 8];
            System.arraycopy(values, 0, bigger, 0, values.length);
            values = bigger;
        }
        values[++top] = value;
    }

    public int pop() {
        if (isEmpty()) {
            throw new IllegalStateException("스택이 비어 있습니다.");
        }
        return values[top--];
    }

    public boolean isEmpty() {
        return top < 0;
    }

    public int size() {
        return top + 1;
    }

    /** pop 없이 전체 반환 목록 복사 */
    public int[] toArray() {
        int[] copy = new int[size()];
        System.arraycopy(values, 0, copy, 0, copy.length);
        return copy;
    }
}
