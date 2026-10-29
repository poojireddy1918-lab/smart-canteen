package com.canteen.model;

public class OrderSummary {
    private final int orderId;
    private final String customerName;
    private final String items;
    private final double totalAmount;
    private final String status;
    private final String createdAt;

    public OrderSummary(int orderId, String customerName, String items, double totalAmount, String status, String createdAt) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
