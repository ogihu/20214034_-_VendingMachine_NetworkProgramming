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
import vending.util.VendingException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 매출서비스기능
public class SalesService {

    private final DailySalesFile dailyFile = new DailySalesFile();
    private final MonthlySalesFile monthlyFile = new MonthlySalesFile();
    private final InitialSalesFile initialFile;
    private final SalesTree salesTree = new SalesTree();
    private final SalesSorter sorter = new SalesSorter();
    private final SalesSearcher searcher = new SalesSearcher();
    private final NetworkEventQueue eventQueue;
    private final String clientId;

    public SalesService(String clientId, NetworkEventQueue eventQueue) {
        this.clientId = clientId;
        this.eventQueue = eventQueue;
        this.initialFile = new InitialSalesFile(clientId);
    }

    public void loadFromDisk() throws VendingException {
        try {
            initialFile.ensureSample();
            salesTree.addAll(initialFile.load());
            salesTree.addAll(dailyFile.loadAll());
        } catch (IOException e) {
            throw new VendingException("매출 데이터 로드 실패", e);
        }
    }

    public void recordSale(String drinkName, int price) throws VendingException {
        try {
            SaleEntry entry = new SaleEntry(clientId, drinkName, 1, price, LocalDate.now());
            dailyFile.append(entry);
            monthlyFile.upsert(entry);
            salesTree.add(entry);
            eventQueue.enqueue(VendingMessage.sale(clientId, drinkName, price, entry.getDate()));
        } catch (IOException e) {
            throw new VendingException("매출 기록 실패", e);
        }
    }

    public int totalSalesAmount() throws VendingException {
        return sumEntries(entriesForThisClient());
    }

    public int todaySalesAmount() throws VendingException {
        String today = LocalDate.now().toString();
        int sum = 0;
        for (SaleEntry e : entriesForThisClient()) {
            if (today.equals(e.getDate())) {
                sum += e.getAmount();
            }
        }
        return sum;
    }

    public int todaySalesByDrink(String drinkName) throws VendingException {
        String today = LocalDate.now().toString();
        int sum = 0;
        for (SaleEntry e : entriesForThisClient()) {
            if (today.equals(e.getDate()) && drinkName.equals(e.getDrinkName())) {
                sum += e.getAmount();
            }
        }
        return sum;
    }

    private List<SaleEntry> entriesForThisClient() throws VendingException {
        List<SaleEntry> mine = new ArrayList<>();
        for (SaleEntry e : allEntries()) {
            if (clientId.equals(e.getClientId())) {
                mine.add(e);
            }
        }
        return mine;
    }

    private int sumEntries(List<SaleEntry> entries) {
        int sum = 0;
        for (SaleEntry e : entries) {
            sum += e.getAmount();
        }
        return sum;
    }

    public int searchDrinkTotal(String drinkName) throws VendingException {
        List<SaleEntry> mine = entriesForThisClient();
        String[] names = mine.stream().map(SaleEntry::getDrinkName).distinct().sorted().toArray(String[]::new);
        int idx = searcher.binarySearchDrinkIndex(names, drinkName);
        if (idx < 0) {
            return 0;
        }
        return searcher.sumAmountByDrink(mine, drinkName);
    }

    public List<SaleEntry> filterByDrink(String drinkName) throws VendingException {
        List<SaleEntry> result = new ArrayList<>();
        for (SaleEntry entry : entriesForThisClient()) {
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

    public List<SaleEntry> allEntries() throws VendingException {
        try {
            List<SaleEntry> all = new ArrayList<>(initialFile.load());
            all.addAll(dailyFile.loadAll());
            return all;
        } catch (IOException e) {
            throw new VendingException("매출 목록 로드 실패", e);
        }
    }

    public List<SaleEntry> allEntriesSortedByDate() throws VendingException {
        return sorter.sortByDateDesc(entriesForThisClient());
    }

    public List<SaleEntry> clientEntries() throws VendingException {
        return entriesForThisClient();
    }

    public String getClientId() {
        return clientId;
    }

    public List<String[]> monthlyRows(String monthKey) throws VendingException {
        try {
            return monthlyFile.loadRows(monthKey);
        } catch (IOException e) {
            throw new VendingException("월별 매출 로드 실패", e);
        }
    }

    public String[] sortedDrinkNames() throws VendingException {
        return entriesForThisClient().stream()
                .map(SaleEntry::getDrinkName)
                .distinct()
                .sorted()
                .toArray(String[]::new);
    }
}
