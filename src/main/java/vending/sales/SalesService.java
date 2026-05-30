package vending.sales;

import vending.event.NetworkEventQueue;
import vending.protocol.VendingMessage;
import vending.sales.daily.DailySalesFile;
import vending.sales.daily.InitialSalesFile;
import vending.sales.model.SaleEntry;
import vending.sales.monthly.MonthlySalesFile;
import vending.sales.search.SalesSearcher;
import vending.sales.search.SalesSorter;
import vending.sales.tree.SalesTree;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 일별/월별 저장, 초기 매출 연동, Tree, Search.
 */
public class SalesService {

    private final DailySalesFile dailyFile = new DailySalesFile();
    private final MonthlySalesFile monthlyFile = new MonthlySalesFile();
    private final InitialSalesFile initialFile = new InitialSalesFile();
    private final SalesTree salesTree = new SalesTree();
    private final SalesSorter sorter = new SalesSorter();
    private final SalesSearcher searcher = new SalesSearcher();
    private final NetworkEventQueue eventQueue;
    private final String clientId;

    public SalesService(String clientId, NetworkEventQueue eventQueue) {
        this.clientId = clientId;
        this.eventQueue = eventQueue;
    }

    public void loadFromDisk() throws IOException {
        salesTree.addAll(initialFile.load());
        salesTree.addAll(dailyFile.loadAll());
    }

    public void recordSale(String drinkName, int price) throws IOException {
        SaleEntry entry = new SaleEntry(clientId, drinkName, 1, price, LocalDate.now());
        dailyFile.append(entry);
        monthlyFile.upsert(entry);
        salesTree.add(entry);
        eventQueue.enqueue(VendingMessage.sale(clientId, drinkName, price, entry.getDate()));
    }

    public int searchDrinkTotal(String drinkName) throws IOException {
        List<SaleEntry> all = allEntries();
        String[] names = all.stream().map(SaleEntry::getDrinkName).distinct().sorted().toArray(String[]::new);
        int idx = searcher.binarySearchDrinkIndex(names, drinkName);
        if (idx < 0) {
            return 0;
        }
        return searcher.sumAmountByDrink(all, drinkName);
    }

    public List<SaleEntry> filterByDrink(String drinkName) throws IOException {
        List<SaleEntry> result = new ArrayList<>();
        for (SaleEntry entry : allEntries()) {
            if (entry.getDrinkName().equals(drinkName)) {
                result.add(entry);
            }
        }
        return sorter.sortByDateDesc(result);
    }

    public SalesTree getSalesTree() {
        return salesTree;
    }

    public SalesSorter getSorter() {
        return sorter;
    }

    public SalesSearcher getSearcher() {
        return searcher;
    }

    public List<SaleEntry> allEntries() throws IOException {
        List<SaleEntry> all = new ArrayList<>(initialFile.load());
        all.addAll(dailyFile.loadAll());
        return all;
    }

    public List<String[]> monthlyRows(String monthKey) throws IOException {
        return monthlyFile.loadRows(monthKey);
    }

    public String[] sortedDrinkNames() throws IOException {
        return allEntries().stream()
                .map(SaleEntry::getDrinkName)
                .distinct()
                .sorted()
                .toArray(String[]::new);
    }
}
