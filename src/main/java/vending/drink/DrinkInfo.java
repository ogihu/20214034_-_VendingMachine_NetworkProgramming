package vending.drink;

import java.io.Serializable;

/**
 * 음료 1종에 대한 기본 정보.
 */
public class DrinkInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int price;

    public DrinkInfo(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
