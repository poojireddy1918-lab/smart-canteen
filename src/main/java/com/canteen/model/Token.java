package com.canteen.model;

public class Token {
    private final int tokenId;
    private final int orderId;
    private final int tokenNumber;
    private final String status;
    private final String customerName;

    public Token(int tokenId, int orderId, int tokenNumber, String status, String customerName) {
        this.tokenId = tokenId;
        this.orderId = orderId;
        this.tokenNumber = tokenNumber;
        this.status = status;
        this.customerName = customerName;
    }

    public int getTokenId() { return tokenId; }
    public int getOrderId() { return orderId; }
    public int getTokenNumber() { return tokenNumber; }
    public String getStatus() { return status; }
    public String getCustomerName() { return customerName; }

    @Override
    public String toString() {
        return "Token #" + tokenNumber + "  -  " + customerName + "  (order " + orderId + ")";
    }
}
