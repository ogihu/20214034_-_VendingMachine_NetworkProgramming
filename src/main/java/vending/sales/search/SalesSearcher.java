package vending.sales.search;

import vending.sales.model.SaleEntry;

import java.util.List;

// 검색기능
public class SalesSearcher {

    public int binarySearchDrinkIndex(String[] sortedNames, String target) {
        int left = 0;
        int right = sortedNames.length - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            int cmp = sortedNames[mid].compareTo(target);
            if (cmp == 0) {
                return mid;
            }
            if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return -1;
    }

    public int sumAmountByDrink(List<SaleEntry> entries, String drinkName) {
        int sum = 0;
        for (SaleEntry entry : entries) {
            if (entry.getDrinkName().equals(drinkName)) {
                sum += entry.getAmount();
            }
        }
        return sum;
    }
}
