package com.canteen.dao;

import com.canteen.db.DBConnection;
import com.canteen.model.FoodItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class FoodDAO {

    private FoodDAO() {}

    public static List<FoodItem> findAll() throws SQLException {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT food_id, name, price FROM food_items WHERE available = TRUE ORDER BY name";
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new FoodItem(rs.getInt(1), rs.getString(2), rs.getDouble(3)));
            }
        }
        return list;
    }

    public static void add(String name, double price) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO food_items(name, price) VALUES (?,?)")) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.executeUpdate();
        }
    }

    public static void updatePrice(int foodId, double price) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE food_items SET price=? WHERE food_id=?")) {
            ps.setDouble(1, price);
            ps.setInt(2, foodId);
            ps.executeUpdate();
        }
    }

    /** Soft delete so past orders keep their food reference. */
    public static void remove(int foodId) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE food_items SET available=FALSE WHERE food_id=?")) {
            ps.setInt(1, foodId);
            ps.executeUpdate();
        }
    }
}
