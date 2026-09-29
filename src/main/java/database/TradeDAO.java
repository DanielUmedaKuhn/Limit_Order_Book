package database;
import model.Trade;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
public class TradeDAO {
    private Connection conn;
    private PreparedStatement saveStmt;

    public TradeDAO(){
        try{
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);   //desabilita o commit automático, para que as transações sejam feitas manualmente
            
            saveStmt = conn.prepareStatement("""
                    INSERT INTO trades(buyer_order_id, seller_order_id, price, quantity)
                    VALUES(?, ?, ?, ?)
                    """);
        } catch (SQLException e){
            System.err.println("DAO - Erro ao inicializar PreparedStatements: " + e.getMessage());
        }
        
    }

    public void addSaveBatch(long buyerId, long sellerId, long price, int quantity) throws SQLException{
        saveStmt.setLong(1, buyerId);
        saveStmt.setLong(2, sellerId);
        saveStmt.setLong(3, price);
        saveStmt.setInt(4, quantity);
        
        saveStmt.addBatch();
    }

    public void executeBatches(){
        try{
            saveStmt.executeBatch();
            conn.commit();
        } catch (SQLException e){
            System.err.println("DAO - Erro ao salvar trades: " + e.getMessage());
        }
    }
}
