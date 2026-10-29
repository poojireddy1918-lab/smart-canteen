package com.canteen.model;

public class CartItem {
    private final int foodId;
    private final String name;
    private final double price;
    private int quantity;

    public CartItem(FoodItem food, int quantity) {
        this.foodId = food.getFoodId();
        this.name = food.getName();
        this.price = food.getPrice();
        this.quantity = quantity;
    }

    public int getFoodId() { return foodId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void addQuantity(int extra) { this.quantity += extra; }
    public double getSubtotal() { return price * quantity; }
}
