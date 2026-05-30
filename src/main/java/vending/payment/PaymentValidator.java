package vending.payment;

/**
 * 화폐 투입 규칙 검사.
 */
public final class PaymentValidator {

    public static final int MAX_BILL = 5000;
    public static final int MAX_TOTAL = 7000;
    private static final int[] ALLOWED = {10, 50, 100, 500, 1000};

    private PaymentValidator() {
    }

    public static boolean isAllowedUnit(int unit) {
        for (int v : ALLOWED) {
            if (v == unit) {
                return true;
            }
        }
        return false;
    }

    public static String validateInsert(InsertedMoney current, int newUnit) {
        if (!isAllowedUnit(newUnit)) {
            return "허용되지 않는 화폐 단위입니다.";
        }

        int nextBill = current.billTotal();
        if (newUnit >= 500) {
            nextBill += newUnit;
        }
        if (nextBill > MAX_BILL) {
            return "지폐 투입 상한은 5000원입니다.";
        }

        int nextTotal = current.total() + newUnit;
        if (nextTotal > MAX_TOTAL) {
            return "총 투입 상한은 7000원입니다.";
        }

        return null;
    }
}
