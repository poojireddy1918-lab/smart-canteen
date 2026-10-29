package com.canteen.dao;

import com.canteen.db.DBConnection;
import com.canteen.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public final class UserDAO {

    private static final String ADMIN_EMAIL = "admin@canteen.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private UserDAO() {}

    /** SHA-256 hex. Fine for a college project; use BCrypt/Argon2 in production. */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return true if registered, false if the email already exists. */
    public static boolean register(String name, String email, String password, String role) throws SQLException {
        String sql = "INSERT INTO users(name, email, password_hash, role) VALUES (?,?,?,?)";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hash(password));
            ps.setString(4, role);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            return false;
        }
    }

    /** @return the user, or null if credentials are wrong. */
    public static User login(String email, String password) throws SQLException {
        String sql = "SELECT user_id, name, email, role FROM users WHERE email=? AND password_hash=?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
                }
            }
        }
        return null;
    }

    /** Creates the default admin account on first run. */
    public static void ensureAdmin() throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE email=?")) {
            ps.setString(1, ADMIN_EMAIL);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }
        register("Canteen Admin", ADMIN_EMAIL, ADMIN_PASSWORD, "ADMIN");
    }
}
