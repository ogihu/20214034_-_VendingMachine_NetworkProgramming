package vending.kiosk;

import vending.admin.AdminPasswordStore;
import vending.admin.CollectService;
import vending.admin.InventoryFileStore;
import vending.coin.ChangeStack;
import vending.coin.CoinInventory;
import vending.coin.CoinSlot;
import vending.config.DataPaths;
import vending.drink.DrinkCatalog;
import vending.drink.DrinkInfo;
import vending.drink.DrinkStockList;
import vending.event.NetworkEventQueue;
import vending.network.SocketClient;
import vending.payment.ChangeCalculator;
import vending.payment.InsertedMoney;
import vending.payment.PaymentValidator;
import vending.protocol.VendingMessage;
import vending.sales.SalesService;
import vending.thread.AdminWorkerThread;
import vending.thread.NetworkSendThread;

import java.io.IOException;

/**
 * 키오스크 핵심 로직.
 */
public class KioskService {

    public interface Listener {
        void onStateChanged();

        void onMessage(String message);

        void onAdminModeChanged(boolean adminMode);
    }

    private final String clientId;
    private final DrinkCatalog catalog;
    private final CoinInventory coinInventory;
    private final InsertedMoney insertedMoney;
    private final AdminPasswordStore passwordStore;
    private final InventoryFileStore inventoryStore;
    private final CollectService collectService;
    private final SalesService salesService;
    private final NetworkEventQueue eventQueue;

    private Listener listener;
    private boolean adminMode;
    private int sessionSales;

    public KioskService(String clientId) {
        this.clientId = clientId;
        DataPaths.ensureDirectories();

        inventoryStore = new InventoryFileStore();
        passwordStore = new AdminPasswordStore();
        collectService = new CollectService();
        eventQueue = new NetworkEventQueue();

        DrinkCatalog loadedCatalog;
        CoinInventory loadedCoins;
        try {
            loadedCatalog = inventoryStore.loadDrinks();
            loadedCoins = inventoryStore.loadCoins();
        } catch (Exception e) {
            loadedCatalog = new DrinkCatalog();
            loadedCoins = new CoinInventory();
        }

        catalog = loadedCatalog;
        coinInventory = loadedCoins;
        insertedMoney = new InsertedMoney();
        salesService = new SalesService(clientId, eventQueue);

        try {
            salesService.loadFromDisk();
        } catch (IOException e) {
            notifyMessage("매출 파일 로드 실패: " + e.getMessage());
        }

        NetworkSendThread sendThread = new NetworkSendThread(eventQueue, clientId);
        sendThread.setDaemon(true);
        sendThread.start();

        AdminWorkerThread adminThread = new AdminWorkerThread(this);
        adminThread.setDaemon(true);
        adminThread.start();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isAdminMode() {
        return adminMode;
    }

    public String getClientId() {
        return clientId;
    }

    public DrinkCatalog getCatalog() {
        return catalog;
    }

    public CoinInventory getCoinInventory() {
        return coinInventory;
    }

    public SalesService getSalesService() {
        return salesService;
    }

    public int getSessionSales() {
        return sessionSales;
    }

    public int insertedTotal() {
        return insertedMoney.isReleased() ? 0 : insertedMoney.total();
    }

    public boolean canBuy(int drinkIndex) {
        if (adminMode) {
            return false;
        }
        DrinkInfo drink = catalog.getDrink(drinkIndex);
        return !catalog.getStock(drinkIndex).isSoldOut() && insertedTotal() >= drink.getPrice();
    }

    public boolean isSoldOut(int drinkIndex) {
        return catalog.getStock(drinkIndex).isSoldOut();
    }

    public boolean isAffordable(int drinkIndex) {
        if (isSoldOut(drinkIndex)) {
            return false;
        }
        return insertedTotal() >= catalog.getDrink(drinkIndex).getPrice();
    }

    public String insertMoney(int unit) {
        if (adminMode) {
            return "관리자 모드에서는 판매할 수 없습니다.";
        }
        if (insertedMoney.isReleased()) {
            insertedMoney.reset();
        }

        String err = PaymentValidator.validateInsert(insertedMoney, unit);
        if (err != null) {
            return err;
        }

        insertedMoney.add(unit);
        CoinSlot slot = coinInventory.findSlot(unit);
        if (slot != null) {
            slot.add(1);
        }

        fireChanged();
        return null;
    }

    public String returnInsertedMoney() {
        if (adminMode) {
            return "관리자 모드에서는 판매할 수 없습니다.";
        }
        int amount = insertedTotal();
        if (amount <= 0) {
            return "반환할 금액이 없습니다.";
        }

        String lack = ChangeCalculator.canMakeChange(coinInventory, amount);
        if (lack != null) {
            return lack;
        }

        ChangeStack stack = new ChangeStack(16);
        if (!ChangeCalculator.dispense(coinInventory, amount, stack)) {
            return "거스름돈 없음";
        }

        insertedMoney.release();
        fireChanged();
        return "반환 완료: " + amount + "원";
    }

    public String buyDrink(int drinkIndex) {
        if (adminMode) {
            return "관리자 모드에서는 판매할 수 없습니다.";
        }

        DrinkInfo drink = catalog.getDrink(drinkIndex);
        DrinkStockList stock = catalog.getStock(drinkIndex);

        if (stock.isSoldOut()) {
            return "품절입니다.";
        }

        int price = drink.getPrice();
        if (insertedTotal() < price) {
            return "금액이 부족합니다.";
        }

        int change = insertedTotal() - price;
        String lack = ChangeCalculator.canMakeChange(coinInventory, change);
        if (lack != null) {
            return lack;
        }

        if (!stock.sellOne()) {
            return "품절입니다.";
        }

        int remaining = stock.size();
        eventQueue.enqueue(VendingMessage.stock(clientId, drink.getName(), remaining));
        if (remaining <= 2 && remaining > 0) {
            eventQueue.enqueue(VendingMessage.stockAlert(clientId, drink.getName(), remaining));
        }
        if (stock.isSoldOut()) {
            try {
                inventoryStore.logSoldOut(drink.getName());
            } catch (IOException e) {
                notifyMessage("품절 기록 저장 실패");
            }
            eventQueue.enqueue(VendingMessage.stockAlert(clientId, drink.getName(), 0));
        }

        ChangeStack stack = new ChangeStack(16);
        if (!ChangeCalculator.dispense(coinInventory, change, stack)) {
            stock.addOne();
            return "거스름돈 없음";
        }

        try {
            salesService.recordSale(drink.getName(), price);
            sessionSales += price;
            saveInventory();
        } catch (Exception e) {
            stock.addOne();
            return "매출 저장 실패: " + e.getMessage();
        }

        insertedMoney.release();
        fireChanged();
        return "판매 완료: " + drink.getName();
    }

    public boolean loginAdmin(String password) {
        if (passwordStore.verify(password)) {
            adminMode = true;
            fireAdminChanged();
            fireChanged();
            return true;
        }
        return false;
    }

    public void logoutAdmin() {
        adminMode = false;
        fireAdminChanged();
        fireChanged();
    }

    public String changePassword(String newPw) {
        try {
            return passwordStore.changePassword(newPw);
        } catch (IOException e) {
            return "비밀번호 저장 실패";
        }
    }

    public void restockDrink(int index, int count) {
        catalog.getStock(index).addCount(count);
        DrinkInfo drink = catalog.getDrink(index);
        eventQueue.enqueue(VendingMessage.restock(clientId, drink.getName(), count, catalog.getStock(index).size()));
        saveInventoryQuiet();
        fireChanged();
    }

    public void addCoinStock(int unit, int count) {
        CoinSlot slot = coinInventory.findSlot(unit);
        if (slot != null) {
            slot.add(count);
            saveInventoryQuiet();
            fireChanged();
        }
    }

    public String collectMoney(int amount) {
        String err = collectService.collect(coinInventory, amount);
        if (err == null) {
            eventQueue.enqueue(VendingMessage.collect(clientId, amount));
            saveInventoryQuiet();
            fireChanged();
        }
        return err;
    }

    public void updateDrinkName(int index, String name) {
        String oldName = catalog.getDrink(index).getName();
        catalog.getDrink(index).setName(name);
        eventQueue.enqueue(VendingMessage.drinkUpdate(clientId, oldName, name, catalog.getDrink(index).getPrice()));
        saveInventoryQuiet();
        fireChanged();
    }

    public void updateDrinkPrice(int index, int price) {
        DrinkInfo drink = catalog.getDrink(index);
        drink.setPrice(price);
        eventQueue.enqueue(VendingMessage.drinkUpdate(clientId, drink.getName(), drink.getName(), price));
        saveInventoryQuiet();
        fireChanged();
    }

    public void saveInventory() throws IOException {
        inventoryStore.saveDrinks(catalog);
        inventoryStore.saveCoins(coinInventory);
    }

    public void periodicAdminSave() {
        saveInventoryQuiet();
    }

    private void saveInventoryQuiet() {
        try {
            saveInventory();
        } catch (IOException e) {
            notifyMessage("재고 저장 실패");
        }
    }

    public boolean isServerOk() {
        String backupHost = System.getProperty("backup.host", System.getProperty("server.host", "127.0.0.1"));
        int backupPort = Integer.parseInt(System.getProperty("backup.port", "9092"));
        return new SocketClient(backupHost, backupPort).ping()
                || new SocketClient().ping();
    }

    public String getServerStatusText() {
        return isServerOk() ? "정상" : "대기";
    }

    public boolean isChangeOk() {
        return ChangeCalculator.canMakeChange(coinInventory, 100) == null;
    }

    public String getChangeStatusText() {
        return isChangeOk() ? "정상" : "부족";
    }

    private void fireChanged() {
        if (listener != null) {
            listener.onStateChanged();
        }
    }

    private void fireAdminChanged() {
        if (listener != null) {
            listener.onAdminModeChanged(adminMode);
        }
    }

    private void notifyMessage(String msg) {
        if (listener != null) {
            listener.onMessage(msg);
        }
    }
}
