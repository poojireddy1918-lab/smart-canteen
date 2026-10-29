package com.canteen.dao;

import com.canteen.db.DBConnection;
import com.canteen.model.CartItem;
import com.canteen.model.OrderSummary;
import com.canteen.model.Token;
import com.canteen.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class OrderDAO {

    private OrderDAO() {}

    /**
     * Saves the order, its items and a new token in one transaction.
     * Token numbers restart from 1 every day.
     */
    public static Token placeOrder(User user, List<CartItem> cart) throws SQLException {
        double total = 0;
        for (CartItem ci : cart) total += ci.getSubtotal();

        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO orders(user_id, total_amount) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, user.getUserId());
                    ps.setDouble(2, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO order_items(order_id, food_id, quantity) VALUES (?,?,?)")) {
                    for (CartItem ci : cart) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, ci.getFoodId());
                        ps.setInt(3, ci.getQuantity());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                int tokenNumber;
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT COALESCE(MAX(token_number),0)+1 FROM tokens WHERE token_date = CURDATE() FOR UPDATE")) {
                    rs.next();
                    tokenNumber = rs.getInt(1);
                }

                int tokenId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO tokens(order_id, token_number) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, tokenNumber);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        tokenId = keys.getInt(1);
                    }
                }

                c.commit();
                return new Token(tokenId, orderId, tokenNumber, "WAITING", user.getName());
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static List<OrderSummary> findByUser(int userId) throws SQLException {
        return findByUser(userId, "ALL");
    }

    public static List<OrderSummary> findByUser(int userId, String statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT o.order_id, " +
                "o.total_amount, o.status, o.created_at, " +
                "COALESCE(GROUP_CONCAT(CONCAT(f.name, ' x', oi.quantity) ORDER BY f.name SEPARATOR ', '), 'No items') AS items " +
                "FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.order_id " +
                "LEFT JOIN food_items f ON f.food_id = oi.food_id " +
                "WHERE o.user_id = ? ");

        if (!"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append("AND o.status = ? ");
        }
        sql.append("GROUP BY o.order_id, o.total_amount, o.status, o.created_at ORDER BY o.created_at DESC");

        List<OrderSummary> orders = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, userId);
            if (!"ALL".equalsIgnoreCase(statusFilter)) {
                ps.setString(idx++, statusFilter.toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(new OrderSummary(
                            rs.getInt("order_id"),
                            null,
                            rs.getString("items"),
                            rs.getDouble("total_amount"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at").toString()
                    ));
                }
            }
        }
        return orders;
    }

    public static List<OrderSummary> findAll() throws SQLException {
        return findAll("ALL");
    }

    public static List<OrderSummary> findAll(String statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT o.order_id, u.name AS customer_name, " +
                "o.total_amount, o.status, o.created_at, " +
                "COALESCE(GROUP_CONCAT(CONCAT(f.name, ' x', oi.quantity) ORDER BY f.name SEPARATOR ', '), 'No items') AS items " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON oi.order_id = o.order_id " +
                "LEFT JOIN food_items f ON f.food_id = oi.food_id " +
                "LEFT JOIN users u ON u.user_id = o.user_id WHERE 1=1 ");

        if (!"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append("AND o.status = ? ");
        }
        sql.append("GROUP BY o.order_id, u.name, o.total_amount, o.status, o.created_at ORDER BY o.created_at DESC");

        List<OrderSummary> orders = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!"ALL".equalsIgnoreCase(statusFilter)) {
                ps.setString(idx++, statusFilter.toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(new OrderSummary(
                            rs.getInt("order_id"),
                            rs.getString("customer_name"),
                            rs.getString("items"),
                            rs.getDouble("total_amount"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at").toString()
                    ));
                }
            }
        }
        return orders;
    }

    public static void updateStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus.toUpperCase());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    public static void cancelOrder(int orderId) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?");
                 PreparedStatement p2 = c.prepareStatement("UPDATE tokens SET status = 'CANCELLED' WHERE order_id = ? AND status = 'WAITING'")) {
                p1.setInt(1, orderId);
                p2.setInt(1, orderId);
                p1.executeUpdate();
                p2.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void markOrderCompleted(int orderId) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("UPDATE orders SET status = 'COMPLETED' WHERE order_id = ?");
                 PreparedStatement p2 = c.prepareStatement("UPDATE tokens SET status = 'SERVED' WHERE order_id = ? AND status = 'WAITING'")) {
                p1.setInt(1, orderId);
                p2.setInt(1, orderId);
                p1.executeUpdate();
                p2.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void markOrderDelivered(int orderId) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("UPDATE orders SET status = 'DELIVERED' WHERE order_id = ?");
                 PreparedStatement p2 = c.prepareStatement("UPDATE tokens SET status = 'DELIVERED' WHERE order_id = ?")) {
                p1.setInt(1, orderId);
                p2.setInt(1, orderId);
                p1.executeUpdate();
                p2.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
