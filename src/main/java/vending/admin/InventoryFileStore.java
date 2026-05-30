package vending.admin;

import vending.coin.CoinInventory;
import vending.coin.CoinSlot;
import vending.drink.DrinkCatalog;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

import vending.config.DataPaths;

/**
 * 재고/화폐/품절 기록 파일 저장.
 */
public class InventoryFileStore {

    private final Path drinkPath = DataPaths.INVENTORY.resolve("drinks.dat");
    private final Path coinPath = DataPaths.INVENTORY.resolve("coins.dat");
    private final Path soldOutLog = DataPaths.INVENTORY.resolve("soldout.log");

    public void saveDrinks(DrinkCatalog catalog) throws IOException {
        Files.createDirectories(drinkPath.getParent());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(drinkPath))) {
            out.writeObject(catalog);
        }
    }

    public DrinkCatalog loadDrinks() throws IOException, ClassNotFoundException {
        if (!Files.exists(drinkPath)) {
            return new DrinkCatalog();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(drinkPath))) {
            return (DrinkCatalog) in.readObject();
        }
    }

    public void saveCoins(CoinInventory inventory) throws IOException {
        Files.createDirectories(coinPath.getParent());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(coinPath))) {
            out.writeObject(inventory);
        }
    }

    public CoinInventory loadCoins() throws IOException, ClassNotFoundException {
        if (!Files.exists(coinPath)) {
            return new CoinInventory();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(coinPath))) {
            return (CoinInventory) in.readObject();
        }
    }

    public void logSoldOut(String drinkName) throws IOException {
        Files.createDirectories(soldOutLog.getParent());
        String line = LocalDate.now() + "|" + drinkName + System.lineSeparator();
        Files.write(soldOutLog, line.getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }
}
