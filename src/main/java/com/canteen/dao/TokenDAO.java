package com.canteen.dao;

import com.canteen.db.DBConnection;
import com.canteen.model.Token;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class TokenDAO {

    private TokenDAO() {}

    /** All WAITING tokens, oldest first (this is the FIFO order). */
    public static List<Token> loadWaiting() throws SQLException {
        List<Token> list = new ArrayList<>();
        String sql = "SELECT t.token_id, t.order_id, t.token_number, t.status, u.name "
                + "FROM tokens t JOIN orders o ON o.order_id = t.order_id "
                + "JOIN users u ON u.user_id = o.user_id "
                + "WHERE t.status = 'WAITING' ORDER BY t.token_id";
        try (Connection c = DBConnection.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Token(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5)));
            }
        }
        return list;
    }

    public static void markServed(Token t) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("UPDATE tokens SET status='SERVED' WHERE token_id=?");
                 PreparedStatement p2 = c.prepareStatement("UPDATE orders SET status='COMPLETED' WHERE order_id=?")) {
                p1.setInt(1, t.getTokenId());
                p1.executeUpdate();
                p2.setInt(1, t.getOrderId());
                p2.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
