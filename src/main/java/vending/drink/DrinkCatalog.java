package vending.drink;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// 음료카탈로그기능
public class DrinkCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<DrinkInfo> drinks = new ArrayList<>();
    private final List<DrinkStockList> stocks = new ArrayList<>();

    public DrinkCatalog() {
        addDefault("믹스커피", 200);
        addDefault("고급믹스커피", 300);
        addDefault("물", 450);
        addDefault("캔커피", 500);
        addDefault("이온음료", 550);
        addDefault("고급캔커피", 700);
        addDefault("탄산음료", 750);
        addDefault("특화음료", 800);
    }

    private void addDefault(String name, int price) {
        drinks.add(new DrinkInfo(name, price));
        stocks.add(new DrinkStockList(10));
    }

    public int drinkCount() {
        return drinks.size();
    }

    public DrinkInfo getDrink(int index) {
        return drinks.get(index);
    }

    public DrinkStockList getStock(int index) {
        return stocks.get(index);
    }

    public int findIndexByName(String name) {
        for (int i = 0; i < drinks.size(); i++) {
            if (drinks.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public List<DrinkInfo> getDrinks() {
        return drinks;
    }

    public List<DrinkStockList> getStocks() {
        return stocks;
    }
}
