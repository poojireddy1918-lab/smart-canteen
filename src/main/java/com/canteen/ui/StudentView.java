package com.canteen.ui;

import com.canteen.MainApp;
import com.canteen.dao.FoodDAO;
import com.canteen.dao.OrderDAO;
import com.canteen.model.CartItem;
import com.canteen.model.FoodItem;
import com.canteen.model.OrderSummary;
import com.canteen.model.Token;
import com.canteen.service.TokenQueue;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.ArrayList;

public class StudentView {

    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private final TableView<CartItem> cartTable = new TableView<>();
    private final Label totalLabel = new Label("Total: Rs. 0.00");
    private final Label tokenLabel = new Label("No active token");
    private Token myToken;
    private Timeline autoRefresh;

    public Parent build() {
        // ----- Menu (left) -----
        TableView<FoodItem> menuTable = new TableView<>();
        menuTable.getColumns().add(Ui.<FoodItem, String>col("Item", "name", 220));
        menuTable.getColumns().add(Ui.<FoodItem, Double>col("Price (Rs.)", "price", 100));
        try {
            menuTable.setItems(FXCollections.observableArrayList(FoodDAO.findAll()));
        } catch (SQLException e) {
            Ui.error("Could not load menu: " + e.getMessage());
        }

        Spinner<Integer> qty = new Spinner<>(1, 20, 1);
        qty.setPrefWidth(80);
        Button addBtn = new Button("Add to Cart");
        addBtn.setOnAction(e -> {
            FoodItem f = menuTable.getSelectionModel().getSelectedItem();
            if (f == null) {
                Ui.error("Select a food item first.");
                return;
            }
            addToCart(f, qty.getValue());
        });

        HBox addRow = new HBox(10, new Label("Qty:"), qty, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);
        VBox left = new VBox(10, sectionTitle("Food Menu"), menuTable, addRow);
        VBox.setVgrow(menuTable, Priority.ALWAYS);

        // ----- Cart (right) -----
        cartTable.getColumns().add(Ui.<CartItem, String>col("Item", "name", 150));
        cartTable.getColumns().add(Ui.<CartItem, Integer>col("Qty", "quantity", 50));
        cartTable.getColumns().add(Ui.<CartItem, Double>col("Subtotal", "subtotal", 90));
        cartTable.setItems(cart);

        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            CartItem sel = cartTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                cart.remove(sel);
                updateTotal();
            }
        });
        Button placeBtn = new Button("Place Order & Get Token");
        placeBtn.setDefaultButton(true);
        placeBtn.setOnAction(e -> placeOrder());

        totalLabel.setStyle("-fx-font-weight: bold;");
        VBox right = new VBox(10, sectionTitle("Your Cart"), cartTable, totalLabel,
                new HBox(10, removeBtn, placeBtn));
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        HBox center = new HBox(20, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        center.setPadding(new Insets(10, 0, 10, 0));

        // ----- Top bar: user + logout -----
        Label hello = new Label("Hello, " + MainApp.currentUser.getName());
        hello.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = new Button("Logout");
        logout.setOnAction(e -> {
            if (autoRefresh != null) autoRefresh.stop();
            MainApp.showLogin();
        });
        HBox top = new HBox(10, hello, spacer, logout);

        // ----- Bottom: token status -----
        tokenLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #1a5e20; -fx-font-weight: bold;");
        Button refresh = new Button("Refresh Status");
        refresh.setOnAction(e -> refreshToken());
        Button viewOrders = new Button("View Orders");
        viewOrders.setOnAction(e -> showOrderHistory());
        HBox bottom = new HBox(15, tokenLabel, refresh, viewOrders);
        bottom.setAlignment(Pos.CENTER_LEFT);

        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshToken()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);
        return root;
    }

    private Label sectionTitle(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        return l;
    }

    private void addToCart(FoodItem f, int quantity) {
        for (CartItem ci : cart) {
            if (ci.getFoodId() == f.getFoodId()) {
                ci.addQuantity(quantity);
                cartTable.refresh();
                updateTotal();
                return;
            }
        }
        cart.add(new CartItem(f, quantity));
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem ci : cart) total += ci.getSubtotal();
        totalLabel.setText(String.format("Total: Rs. %.2f", total));
    }

    private void placeOrder() {
        if (cart.isEmpty()) {
            Ui.error("Your cart is empty.");
            return;
        }
        try {
            Token t = OrderDAO.placeOrder(MainApp.currentUser, new ArrayList<>(cart));
            TokenQueue.get().enqueue(t);
            myToken = t;
            cart.clear();
            updateTotal();
            refreshToken();
            Ui.info("Order placed!\n\nYour token number is " + t.getTokenNumber()
                    + ".\nPeople ahead of you: " + (TokenQueue.get().positionOf(t.getTokenId()) - 1));
        } catch (SQLException e) {
            Ui.error("Could not place order: " + e.getMessage());
        }
    }

    private void refreshToken() {
        if (myToken == null) return;
        try {
            TokenQueue.get().reload();
        } catch (SQLException e) {
            tokenLabel.setText("Could not refresh status (database error)");
            return;
        }
        int pos = TokenQueue.get().positionOf(myToken.getTokenId());
        if (pos == -1) {
            tokenLabel.setText("Token #" + myToken.getTokenNumber() + " - READY! Please collect your order.");
        } else {
            tokenLabel.setText("Token #" + myToken.getTokenNumber() + " - position " + pos
                    + " in queue (" + (pos - 1) + " ahead of you)");
        }
    }

    private void showOrderHistory() {
        TableView<OrderSummary> ordersTable = new TableView<>();
        ordersTable.getColumns().add(Ui.<OrderSummary, Integer>col("Order ID", "orderId", 90));
        ordersTable.getColumns().add(Ui.<OrderSummary, String>col("Items", "items", 260));
        ordersTable.getColumns().add(Ui.<OrderSummary, Double>col("Total (Rs.)", "totalAmount", 100));
        ordersTable.getColumns().add(Ui.<OrderSummary, String>col("Status", "status", 100));
        ordersTable.getColumns().add(Ui.<OrderSummary, String>col("Date", "createdAt", 170));

        ComboBox<String> filter = new ComboBox<>(FXCollections.observableArrayList("ALL", "PLACED", "COMPLETED", "DELIVERED", "CANCELLED"));
        filter.setValue("ALL");
        filter.setOnAction(e -> loadStudentOrders(ordersTable, filter.getValue()));

        Button cancel = new Button("Cancel Selected");
        cancel.setOnAction(e -> {
            OrderSummary selected = ordersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Select an order first.");
                return;
            }
            if (!"PLACED".equalsIgnoreCase(selected.getStatus())) {
                Ui.error("Only placed orders can be cancelled.");
                return;
            }
            try {
                OrderDAO.cancelOrder(selected.getOrderId());
                loadStudentOrders(ordersTable, filter.getValue());
                Ui.info("Order #" + selected.getOrderId() + " has been cancelled.");
            } catch (SQLException ex) {
                Ui.error("Could not cancel order: " + ex.getMessage());
            }
        });

        loadStudentOrders(ordersTable, filter.getValue());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("My Orders");
        dialog.setHeaderText("Your order history");
        VBox content = new VBox(10, new HBox(10, new Label("Filter:"), filter, cancel), ordersTable);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void loadStudentOrders(TableView<OrderSummary> ordersTable, String statusFilter) {
        try {
            ordersTable.setItems(FXCollections.observableArrayList(OrderDAO.findByUser(MainApp.currentUser.getUserId(), statusFilter)));
        } catch (SQLException e) {
            Ui.error("Could not load your orders: " + e.getMessage());
        }
    }
}
