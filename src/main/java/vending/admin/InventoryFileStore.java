package vending.admin;

import vending.coin.CoinInventory;
import vending.config.DataPaths;
import vending.drink.DrinkCatalog;
import vending.util.VendingException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

// 재고파일기능
public class InventoryFileStore {

    private final Path drinkPath = DataPaths.INVENTORY.resolve("drinks.dat");
    private final Path coinPath = DataPaths.INVENTORY.resolve("coins.dat");
    private final Path soldOutLog = DataPaths.INVENTORY.resolve("soldout.log");

    public void saveDrinks(DrinkCatalog catalog) throws VendingException {
        try {
            Files.createDirectories(drinkPath.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(drinkPath))) {
                out.writeObject(catalog);
            }
        } catch (IOException e) {
            throw new VendingException("음료 재고 저장 실패", e);
        }
    }

    public DrinkCatalog loadDrinks() throws VendingException {
        if (!Files.exists(drinkPath)) {
            return new DrinkCatalog();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(drinkPath))) {
            return (DrinkCatalog) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new VendingException("음료 재고 로드 실패", e);
        }
    }

    public void saveCoins(CoinInventory inventory) throws VendingException {
        try {
            Files.createDirectories(coinPath.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(coinPath))) {
                out.writeObject(inventory);
            }
        } catch (IOException e) {
            throw new VendingException("화폐 재고 저장 실패", e);
        }
    }

    public CoinInventory loadCoins() throws VendingException {
        if (!Files.exists(coinPath)) {
            return new CoinInventory();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(coinPath))) {
            return (CoinInventory) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new VendingException("화폐 재고 로드 실패", e);
        }
    }

    public void logSoldOut(String drinkName) throws VendingException {
        try {
            Files.createDirectories(soldOutLog.getParent());
            String line = LocalDate.now() + "|" + drinkName + System.lineSeparator();
            Files.write(soldOutLog, line.getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new VendingException("품절 기록 저장 실패", e);
        }
    }
}
