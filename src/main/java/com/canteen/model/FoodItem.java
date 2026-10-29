package com.canteen.model;

public class FoodItem {
    private final int foodId;
    private final String name;
    private final double price;

    public FoodItem(int foodId, String name, double price) {
        this.foodId = foodId;
        this.name = name;
        this.price = price;
    }

    public int getFoodId() { return foodId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
}
