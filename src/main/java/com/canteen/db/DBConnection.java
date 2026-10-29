package com.canteen.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place for opening JDBC connections.
 * Override defaults with environment variables:
 *   CANTEEN_DB_URL, CANTEEN_DB_USER, CANTEEN_DB_PASS
 */
public final class DBConnection {

    private static final String URL = env("CANTEEN_DB_URL",
            "jdbc:mysql://localhost:3306/smart_canteen?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = env("CANTEEN_DB_USER", "root");
    private static final String PASS = env("CANTEEN_DB_PASS", "");

    private DBConnection() {}

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void ensureSchemaCompatibility() throws SQLException {
        try (Connection c = get(); java.sql.Statement st = c.createStatement()) {
            st.execute("ALTER TABLE orders MODIFY COLUMN status ENUM('PLACED','COMPLETED','CANCELLED','DELIVERED') NOT NULL DEFAULT 'PLACED'");
            st.execute("ALTER TABLE tokens MODIFY COLUMN status ENUM('WAITING','SERVED','CANCELLED','DELIVERED') NOT NULL DEFAULT 'WAITING'");
        }
    }

    /** Step 7: run this class to test the JDBC connection. */
    public static void main(String[] args) {
        try (Connection c = get()) {
            System.out.println("Connected to database: " + c.getCatalog());
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }
}
