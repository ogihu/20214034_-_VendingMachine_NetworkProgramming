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
import vending.bluetooth.BluetoothInputServer;
import vending.network.SocketClient;
import vending.util.AppLog;
import vending.util.VendingException;
import vending.payment.ChangeCalculator;
import vending.payment.InsertedMoney;
import vending.payment.PaymentValidator;
import vending.protocol.MessageType;
import vending.protocol.VendingMessage;
import vending.sales.SalesService;
import vending.thread.AdminWorkerThread;
import vending.thread.AlertPollThread;
import vending.thread.NetworkSendThread;

// 키오스크핵심기능
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
    private volatile String serverAlerts = "";
    private volatile String serverSalesSummary = "";

    public KioskService(String clientId) {
        this.clientId = clientId;
        try {
            DataPaths.ensureDirectories();
        } catch (VendingException e) {
            AppLog.error("KIOSK", e.getMessage(), e);
        }

        inventoryStore = new InventoryFileStore();
        passwordStore = new AdminPasswordStore();
        collectService = new CollectService();
        eventQueue = new NetworkEventQueue();

        DrinkCatalog loadedCatalog;
        CoinInventory loadedCoins;
        try {
            loadedCatalog = inventoryStore.loadDrinks();
            loadedCoins = inventoryStore.loadCoins();
        } catch (VendingException e) {
            AppLog.error("KIOSK", e.getMessage(), e);
            notifyMessage(e.getMessage());
            loadedCatalog = new DrinkCatalog();
            loadedCoins = new CoinInventory();
        }

        catalog = loadedCatalog;
        coinInventory = loadedCoins;
        ensureMinimumCoins();
        normalizeDrinkNames();
        insertedMoney = new InsertedMoney();
        salesService = new SalesService(clientId, eventQueue);

        try {
            salesService.loadFromDisk();
        } catch (VendingException e) {
            AppLog.error("KIOSK", "매출 파일 로드 실패", e);
            notifyMessage(e.getMessage());
        }

        NetworkSendThread sendThread = new NetworkSendThread(eventQueue, clientId);
        sendThread.setDaemon(true);
        sendThread.start();

        AdminWorkerThread adminThread = new AdminWorkerThread(this);
        adminThread.setDaemon(true);
        adminThread.start();

        AlertPollThread alertPollThread = new AlertPollThread(this);
        alertPollThread.setDaemon(true);
        alertPollThread.start();

        startBluetoothIfEnabled();
    }

    private void ensureMinimumCoins() {
        int[][] minimums = {{1000, 20}, {500, 30}, {100, 40}, {50, 40}, {10, 50}};
        for (int[] min : minimums) {
            CoinSlot slot = coinInventory.findSlot(min[0]);
            if (slot != null && slot.getCount() < min[1]) {
                slot.setCount(min[1]);
            }
        }
    }

    private void normalizeDrinkNames() {
        renameDefault(1, "프리미엄커피", "고급믹스커피");
        renameDefault(2, "생수", "물");
        renameDefault(5, "캔커피(고급)", "고급캔커피");
        saveInventoryQuiet();
    }

    private void renameDefault(int index, String oldName, String newName) {
        if (index < catalog.drinkCount() && oldName.equals(catalog.getDrink(index).getName())) {
            catalog.getDrink(index).setName(newName);
        }
    }

    private void startBluetoothIfEnabled() {
        boolean enabled = "true".equalsIgnoreCase(System.getProperty("bluetooth.enabled", ""))
                || "End_Dev".equalsIgnoreCase(clientId);
        if (!enabled) {
            return;
        }
        int btPort;
        try {
            btPort = Integer.parseInt(System.getProperty("bluetooth.port", "9200"));
        } catch (NumberFormatException e) {
            AppLog.error("BT", "bluetooth.port 형식 오류", e);
            notifyMessage("Bluetooth 포트 설정 오류");
            return;
        }
        try {
            BluetoothInputServer btServer = new BluetoothInputServer(this, btPort);
            btServer.setDaemon(true);
            btServer.start();
            AppLog.info("BT", "Bluetooth 입력 대기 (port " + btPort + ", client=" + clientId + ")");
        } catch (Exception e) {
            AppLog.error("BT", "Bluetooth 서버 시작 실패 (port " + btPort + ")", e);
            notifyMessage("Bluetooth 입력 서버 시작 실패 (port " + btPort + ")");
        }
    }

    public String listDrinksForBluetooth() {
        StringBuilder sb = new StringBuilder("OK:LIST:");
        for (int i = 0; i < catalog.drinkCount(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            var drink = catalog.getDrink(i);
            int stock = catalog.getStock(i).size();
            sb.append(i).append('=').append(drink.getName())
                    .append('(').append(drink.getPrice()).append("원,").append(stock).append("개)");
        }
        return sb.toString();
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

    public int getTotalSalesAmount() {
        try {
            return salesService.totalSalesAmount();
        } catch (VendingException e) {
            return sessionSales;
        }
    }

    public int getTodaySalesAmount() {
        try {
            return salesService.todaySalesAmount();
        } catch (VendingException e) {
            return 0;
        }
    }

    public int insertedTotal() {
        return insertedMoney.isReleased() ? 0 : insertedMoney.total();
    }

    public String insertedSummary() {
        if (insertedMoney.isReleased() || insertedMoney.size() == 0) {
            return "투입 내역 없음";
        }
        return formatUnits(insertedMoney.toArray());
    }

    public boolean canBuy(int drinkIndex) {
        if (adminMode) {
            return false;
        }
        DrinkInfo drink = catalog.getDrink(drinkIndex);
        if (catalog.getStock(drinkIndex).isSoldOut() || insertedTotal() < drink.getPrice()) {
            return false;
        }
        return canReturnChange(drinkIndex);
    }

    public boolean canAfford(int drinkIndex) {
        if (adminMode || catalog.getStock(drinkIndex).isSoldOut()) {
            return false;
        }
        return insertedTotal() >= catalog.getDrink(drinkIndex).getPrice();
    }

    public boolean canReturnChange(int drinkIndex) {
        if (drinkIndex < 0 || drinkIndex >= catalog.drinkCount()) {
            return false;
        }
        int change = changeAmountFor(drinkIndex);
        return ChangeCalculator.canMakeChange(coinInventory, change) == null;
    }

    public int changeAmountFor(int drinkIndex) {
        if (drinkIndex < 0 || drinkIndex >= catalog.drinkCount()) {
            return 0;
        }
        int change = insertedTotal() - catalog.getDrink(drinkIndex).getPrice();
        return Math.max(change, 0);
    }

    public String changePreviewFor(int drinkIndex) {
        return ChangeCalculator.preview(coinInventory, changeAmountFor(drinkIndex));
    }

    public boolean isSoldOut(int drinkIndex) {
        return catalog.getStock(drinkIndex).isSoldOut();
    }

    public boolean isAffordable(int drinkIndex) {
        if (isSoldOut(drinkIndex)) {
            return false;
        }
        return canAfford(drinkIndex);
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

        int[] units = insertedMoney.toArray();
        if (!canReturnSameUnits(units)) {
            return "반환 화폐 부족";
        }

        for (int unit : units) {
            CoinSlot slot = coinInventory.findSlot(unit);
            slot.take(1);
        }
        insertedMoney.release();
        fireChanged();
        return "반환 완료: " + amount + "원 " + formatUnits(units);
    }

    private boolean canReturnSameUnits(int[] units) {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (int unit : units) {
            counts.merge(unit, 1, Integer::sum);
        }
        for (java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) {
            CoinSlot slot = coinInventory.findSlot(e.getKey());
            if (slot == null || slot.getCount() < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    private String formatUnits(int[] units) {
        java.util.Map<Integer, Integer> counts = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        for (int unit : units) {
            counts.merge(unit, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder("(");
        int i = 0;
        for (java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append("원 ").append(e.getValue()).append("개");
        }
        return sb.append(")").toString();
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
            } catch (VendingException e) {
                notifyMessage(e.getMessage());
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
        } catch (VendingException e) {
            stock.addOne();
            ChangeCalculator.refund(coinInventory, stack);
            return e.getMessage();
        }

        insertedMoney.release();
        fireChanged();
        if (change <= 0) {
            return "판매 완료: " + drink.getName() + " / 거스름돈 0원";
        }
        return "판매 완료: " + drink.getName() + " / 거스름돈 " + change + "원 (" + ChangeCalculator.format(stack) + ")";
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
        } catch (VendingException e) {
            return e.getMessage();
        }
    }

    public void restockDrink(int index, int count) {
        catalog.getStock(index).addCount(count);
        DrinkInfo drink = catalog.getDrink(index);
        eventQueue.enqueue(VendingMessage.restock(clientId, drink.getName(), count, catalog.getStock(index).size()));
        saveInventoryQuiet();
        fireChanged();
    }

    public void updateDrinkStock(int index, int count) {
        catalog.getStock(index).setCount(count);
        DrinkInfo drink = catalog.getDrink(index);
        eventQueue.enqueue(VendingMessage.stock(clientId, drink.getName(), count));
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

    public void updateCoinStock(int unit, int count) {
        CoinSlot slot = coinInventory.findSlot(unit);
        if (slot != null && count >= 0) {
            slot.setCount(count);
            saveInventoryQuiet();
            fireChanged();
        }
    }

    public int maxCollectableAmount() {
        return collectService.maxCollectable(coinInventory);
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

    public void saveInventory() throws VendingException {
        inventoryStore.saveDrinks(catalog);
        inventoryStore.saveCoins(coinInventory);
    }

    public void periodicAdminSave() {
        saveInventoryQuiet();
    }

    private void saveInventoryQuiet() {
        try {
            saveInventory();
        } catch (VendingException e) {
            notifyMessage(e.getMessage());
        }
    }

    public boolean isServerOk() {
        return new SocketClient().ping();
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

    public String getServerAlerts() {
        return serverAlerts;
    }

    public String getServerSalesSummary() {
        return serverSalesSummary;
    }

    public void pollServerInfo() {
        try {
            SocketClient client = new SocketClient();

            VendingMessage alerts = client.sendAndRead(VendingMessage.queryAlerts());
            if (alerts != null && alerts.type == MessageType.ALERT_LIST) {
                serverAlerts = alerts.payload == null ? "" : alerts.payload;
            }

            VendingMessage sales = client.sendAndRead(VendingMessage.querySales());
            if (sales != null && sales.type == MessageType.SALES_SUMMARY) {
                serverSalesSummary = sales.payload == null ? "" : sales.payload;
            }

            VendingMessage remote = client.sendAndRead(VendingMessage.queryRemote(clientId));
            if (remote != null && remote.type == MessageType.REMOTE_COMMAND_LIST
                    && remote.payload != null && !remote.payload.isBlank()) {
                applyRemoteCommands(remote.payload);
            }

            fireChanged();
        } catch (VendingException e) {
            AppLog.warn("KIOSK", "서버 조회 실패: " + e.getMessage());
        }
    }

    public String requestRemoteDrinkChange(String targetClientId, int index, String newName, int price) {
        if (!adminMode) {
            return "관리자 모드에서만 가능합니다.";
        }
        if (index < 0 || index >= catalog.drinkCount()) {
            return "음료 인덱스가 올바르지 않습니다.";
        }
        String oldName = catalog.getDrink(index).getName();
        try {
            VendingMessage response = new SocketClient().sendAndRead(
                    VendingMessage.remoteDrinkSet(targetClientId, index, oldName, newName, price));
            if (response != null && response.type == MessageType.ERROR) {
                return response.payload;
            }
            return "서버에 원격 변경 요청 완료: " + targetClientId;
        } catch (VendingException e) {
            return e.getMessage();
        }
    }

    private void applyRemoteCommands(String payload) {
        String[] lines = payload.split("\n");
        boolean changed = false;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            try {
                VendingMessage cmd = VendingMessage.fromJson(line.trim());
                if (cmd.type != MessageType.REMOTE_DRINK_UPDATE) {
                    continue;
                }
                int index = cmd.quantity;
                if (index >= 0 && index < catalog.drinkCount()) {
                    catalog.getDrink(index).setName(cmd.newName);
                    catalog.getDrink(index).setPrice(cmd.price);
                    changed = true;
                    notifyMessage("서버 원격 변경: " + cmd.newName + " (" + cmd.price + "원)");
                } else {
                    int byName = catalog.findIndexByName(cmd.drink);
                    if (byName >= 0) {
                        catalog.getDrink(byName).setName(cmd.newName);
                        catalog.getDrink(byName).setPrice(cmd.price);
                        changed = true;
                        notifyMessage("서버 원격 변경: " + cmd.newName);
                    }
                }
            } catch (Exception e) {
                AppLog.warn("KIOSK", "원격 명령 처리 실패: " + e.getMessage());
            }
        }
        if (changed) {
            saveInventoryQuiet();
            fireChanged();
        }
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
