package vending.sales.search;

import vending.sales.model.SaleEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 매출 검색/정렬 유틸.
 */
public class SalesSorter {

    public List<SaleEntry> sortByDateDesc(List<SaleEntry> source) {
        List<SaleEntry> copy = new ArrayList<>(source);
        copy.sort(Comparator.comparing(SaleEntry::getDate).reversed());
        return copy;
    }

    public List<SaleEntry> sortByAmountDesc(List<SaleEntry> source) {
        List<SaleEntry> copy = new ArrayList<>(source);
        copy.sort(Comparator.comparingInt(SaleEntry::getAmount).reversed());
        return copy;
    }

    public List<SaleEntry> sortByQuantityDesc(List<SaleEntry> source) {
        List<SaleEntry> copy = new ArrayList<>(source);
        copy.sort(Comparator.comparingInt(SaleEntry::getQuantity).reversed());
        return copy;
    }
}
