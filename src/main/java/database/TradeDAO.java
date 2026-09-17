package database;
import model.Trade;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
public class TradeDAO {
    public void saveAll(List<Trade> trades){
        String sql = """
                INSERT INTO trades (buyer_order_id, seller_order_id, price, quantity)
                VALUES(?, ?, ?, ?)
                """;

        try(Connection conn = DatabaseConfig.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            for(Trade trade : trades){
                pstmt.setLong(1, trade.buyerId);
                pstmt.setLong(2, trade.sellerId);
                pstmt.setLong(3, trade.price);
                pstmt.setInt(4, trade.quantity);

                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
        }
        catch(SQLException e){
            System.err.println("[DAO] Erro ao registrar trade: " + e.getMessage());
        }
    }
}
