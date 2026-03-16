package database;
import model.Trade;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TradeDAO {
    public void save(Trade trade){
        String sql = """
                INSERT INTO trades (buyer_order_id, seller_order_id, price, quantity)
                VALUES(?, ?, ?, ?)
                """;

        try(Connection conn = DatabaseConfig.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setLong(1, trade.buyerId);
            pstmt.setLong(2, trade.sellerId);
            pstmt.setLong(3, trade.price);
            pstmt.setInt(4, trade.quantity);

            pstmt.executeUpdate();
        }
        catch(SQLException e){
            System.err.println("[DAO] Erro ao registrar trade: " + e.getMessage());
        }
    }
}
