package vending.sales.tree;

import vending.sales.model.SaleEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * 날짜 키 기준 TreeMap으로 매출을 묶어 둔다.
 */
public class SalesTree {

    private final TreeMap<String, List<SaleEntry>> byDate = new TreeMap<>();

    public void add(SaleEntry entry) {
        byDate.computeIfAbsent(entry.getDate(), k -> new ArrayList<>()).add(entry);
    }

    public void addAll(List<SaleEntry> entries) {
        for (SaleEntry entry : entries) {
            add(entry);
        }
    }

    public TreeMap<String, List<SaleEntry>> getByDate() {
        return byDate;
    }

    public int totalAmountOn(String date) {
        List<SaleEntry> list = byDate.get(date);
        if (list == null) {
            return 0;
        }
        int sum = 0;
        for (SaleEntry e : list) {
            sum += e.getAmount();
        }
        return sum;
    }
}
