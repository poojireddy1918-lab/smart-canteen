package com.canteen.ui;

import com.canteen.MainApp;
import com.canteen.dao.FoodDAO;
import com.canteen.dao.OrderDAO;
import com.canteen.model.FoodItem;
import com.canteen.model.OrderSummary;
import com.canteen.model.Token;
import com.canteen.service.TokenQueue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;

public class AdminView {

    private final ListView<Token> queueList = new ListView<>();
    private final Label nowServing = new Label("Now serving: -");
    private final Label queueSize = new Label();
    private final TableView<FoodItem> menuTable = new TableView<>();

    public Parent build() {
        TabPane tabs = new TabPane(buildQueueTab(), buildMenuTab(), buildOrdersTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Label hello = new Label("Admin Dashboard  -  " + MainApp.currentUser.getName());
        hello.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = new Button("Logout");
        logout.setOnAction(e -> MainApp.showLogin());

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setTop(new HBox(10, hello, spacer, logout));
        BorderPane.setMargin(tabs, new Insets(10, 0, 0, 0));
        root.setCenter(tabs);

        refreshQueue();
        return root;
    }

    // ---------------- Queue tab ----------------
    private Tab buildQueueTab() {
        nowServing.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a5e20;");

        Button serveNext = new Button("Serve Next Token");
        serveNext.setStyle("-fx-font-size: 14px;");
        serveNext.setOnAction(e -> {
            try {
                Token t = TokenQueue.get().serveNext();
                if (t == null) {
                    Ui.info("The queue is empty.");
                } else {
                    nowServing.setText("Now serving: Token #" + t.getTokenNumber() + " (" + t.getCustomerName() + ")");
                }
                refreshQueue();
            } catch (SQLException ex) {
                Ui.error("Database error: " + ex.getMessage());
            }
        });

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> {
            try {
                TokenQueue.get().reload();
            } catch (SQLException ex) {
                Ui.error("Database error: " + ex.getMessage());
            }
            refreshQueue();
        });

        HBox buttons = new HBox(10, serveNext, refresh);
        buttons.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(12, nowServing, queueSize, queueList, buttons);
        box.setPadding(new Insets(14));
        VBox.setVgrow(queueList, Priority.ALWAYS);
        return new Tab("Token Queue", box);
    }

    private void refreshQueue() {
        queueList.setItems(FXCollections.observableArrayList(TokenQueue.get().snapshot()));
        queueSize.setText("Waiting tokens (first in line at top): " + TokenQueue.get().size());
    }

    // ---------------- Menu tab ----------------
    private Tab buildMenuTab() {
        menuTable.getColumns().add(Ui.<FoodItem, Integer>col("ID", "foodId", 60));
        menuTable.getColumns().add(Ui.<FoodItem, String>col("Item", "name", 250));
        menuTable.getColumns().add(Ui.<FoodItem, Double>col("Price (Rs.)", "price", 110));
        loadMenu();

        TextField name = new TextField();
        name.setPromptText("Item name");
        TextField price = new TextField();
        price.setPromptText("Price");
        price.setPrefWidth(90);

        Button add = new Button("Add Item");
        add.setOnAction(e -> {
            Double p = parsePrice(price.getText());
            if (name.getText().isBlank() || p == null) {
                Ui.error("Enter a name and a valid price.");
                return;
            }
            try {
                FoodDAO.add(name.getText().trim(), p);
                name.clear();
                price.clear();
                loadMenu();
            } catch (SQLException ex) {
                Ui.error("Database error: " + ex.getMessage());
            }
        });

        Button update = new Button("Update Price of Selected");
        update.setOnAction(e -> {
            FoodItem sel = menuTable.getSelectionModel().getSelectedItem();
            Double p = parsePrice(price.getText());
            if (sel == null || p == null) {
                Ui.error("Select an item and enter the new price.");
                return;
            }
            try {
                FoodDAO.updatePrice(sel.getFoodId(), p);
                price.clear();
                loadMenu();
            } catch (SQLException ex) {
                Ui.error("Database error: " + ex.getMessage());
            }
        });

        Button remove = new Button("Remove Selected");
        remove.setOnAction(e -> {
            FoodItem sel = menuTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                Ui.error("Select an item first.");
                return;
            }
            try {
                FoodDAO.remove(sel.getFoodId());
                loadMenu();
            } catch (SQLException ex) {
                Ui.error("Database error: " + ex.getMessage());
            }
        });

        HBox form = new HBox(10, name, price, add, update, remove);
        form.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(12, menuTable, form);
        box.setPadding(new Insets(14));
        VBox.setVgrow(menuTable, Priority.ALWAYS);
        return new Tab("Manage Menu", box);
    }

    // ---------------- Orders tab ----------------
    private Tab buildOrdersTab() {
        TableView<OrderSummary> ordersTable = new TableView<>();
        ordersTable.getColumns().add(Ui.<OrderSummary, Integer>col("Order ID", "orderId", 90));
        ordersTable.getColumns().add(Ui.<OrderSummary, String>col("Customer", "customerName", 170));
        ordersTable.getColumns().add(Ui.<OrderSummary, String>col("Items", "items", 260));
        ordersTable.getColumns().add(Ui.<OrderSummary, Double>col("Total (Rs.)", "totalAmount", 100));
        ordersTable.getColumns().add(Ui.<OrderSummary, String>col("Status", "status", 100));
        ordersTable.getColumns().add(Ui.<OrderSummary, String>col("Date", "createdAt", 170));

        ComboBox<String> filter = new ComboBox<>(FXCollections.observableArrayList("ALL", "PLACED", "COMPLETED", "DELIVERED", "CANCELLED"));
        filter.setValue("ALL");
        filter.setOnAction(e -> loadOrders(ordersTable, filter.getValue()));

        Button markComplete = new Button("Mark Completed");
        markComplete.setOnAction(e -> {
            OrderSummary selected = ordersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Select an order first.");
                return;
            }
            try {
                OrderDAO.markOrderCompleted(selected.getOrderId());
                loadOrders(ordersTable, filter.getValue());
                Ui.info("Order #" + selected.getOrderId() + " marked as completed.");
            } catch (SQLException ex) {
                Ui.error("Could not update order status: " + ex.getMessage());
            }
        });

        Button markDelivered = new Button("Mark Delivered");
        markDelivered.setOnAction(e -> {
            OrderSummary selected = ordersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Select an order first.");
                return;
            }
            try {
                OrderDAO.markOrderDelivered(selected.getOrderId());
                loadOrders(ordersTable, filter.getValue());
                Ui.info("Order #" + selected.getOrderId() + " marked as delivered.");
            } catch (SQLException ex) {
                Ui.error("Could not update order status: " + ex.getMessage());
            }
        });

        Button cancel = new Button("Cancel Order");
        cancel.setOnAction(e -> {
            OrderSummary selected = ordersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Ui.error("Select an order first.");
                return;
            }
            try {
                OrderDAO.cancelOrder(selected.getOrderId());
                loadOrders(ordersTable, filter.getValue());
                Ui.info("Order #" + selected.getOrderId() + " was cancelled.");
            } catch (SQLException ex) {
                Ui.error("Could not cancel order: " + ex.getMessage());
            }
        });

        Button refresh = new Button("Refresh Orders");
        refresh.setOnAction(e -> loadOrders(ordersTable, filter.getValue()));

        HBox controls = new HBox(10, new Label("Filter:"), filter, markComplete, markDelivered, cancel, refresh);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(12, controls, ordersTable);
        box.setPadding(new Insets(14));
        VBox.setVgrow(ordersTable, Priority.ALWAYS);
        loadOrders(ordersTable, filter.getValue());
        return new Tab("View Orders", box);
    }

    private void loadMenu() {
        try {
            menuTable.setItems(FXCollections.observableArrayList(FoodDAO.findAll()));
        } catch (SQLException e) {
            Ui.error("Could not load menu: " + e.getMessage());
        }
    }

    private void loadOrders(TableView<OrderSummary> ordersTable, String statusFilter) {
        try {
            ordersTable.setItems(FXCollections.observableArrayList(OrderDAO.findAll(statusFilter)));
        } catch (SQLException e) {
            Ui.error("Could not load orders: " + e.getMessage());
        }
    }

    private Double parsePrice(String s) {
        try {
            double d = Double.parseDouble(s.trim());
            return d > 0 ? d : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
