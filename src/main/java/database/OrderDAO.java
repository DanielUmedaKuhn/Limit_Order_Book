package database;
import model.Order;
import enums.Side;
import enums.OrderType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class OrderDAO {
    private Connection conn;
    private PreparedStatement saveStmt;
    private PreparedStatement updateStmt;

    public OrderDAO(){
        try{
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);   //desabilita o commit automático, para que as transações sejam feitas manualmente

            saveStmt = conn.prepareStatement("""
                    INSERT INTO orders (id, price, initial_quantity, quantity, side, type, status)
                    VALUES (?, ?, ?, ?, ?, ?, 'OPEN')
                    """);

            updateStmt = conn.prepareStatement("""
                    UPDATE orders
                    SET quantity = ?, status = ?
                    WHERE id = ?
                    """);
        } catch (SQLException e){
            System.err.println("DAO - Erro ao inicializar PreparedStatements: " + e.getMessage());
        }
    }

    //adiciona os dados à fila para gravação
    public void addSaveBatch(long id, long price, int initialQty, int qty, boolean side, boolean type) throws SQLException{
        saveStmt.setLong(1, id);
        saveStmt.setLong(2, price);
        saveStmt.setInt(3, initialQty);
        saveStmt.setInt(4, qty);
        saveStmt.setString(5, side ? "BUY" : "SELL");
        saveStmt.setString(6, type ? "MARKET" : "LIMIT");
        
        saveStmt.addBatch();
    }

    //adiciona os dados à fila para atualização
    public void addUpdateBatch(long id, int qty) throws SQLException{
        updateStmt.setInt(1, qty);
        updateStmt.setString(2, qty == 0 ? "FILLED" : "PARTIAL");
        updateStmt.setLong(3, id);
        updateStmt.addBatch();
    }

    //executa no banco
    public void executeBatches(){
        try{
            saveStmt.executeBatch();
            updateStmt.executeBatch();
            conn.commit();
        } catch (SQLException e){
            System.err.println("DAO - Erro ao executar batches: " + e.getMessage());
        }
    }

    public List<Order> findAllOpen(){
        List<Order> openOrders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status != 'FILLED'";

        try(Connection conn = DatabaseConfig.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){

            while(rs.next()){
                Order order = new Order(
                    rs.getLong("id"),
                    rs.getLong("price"),
                    rs.getInt("initial_quantity"),
                    rs.getInt("quantity"),  //quantidade restante no banco
                    Side.valueOf(rs.getString("side")),
                    OrderType.valueOf(rs.getString("type"))
                );
                openOrders.add(order);
            }
        }
        catch (SQLException e){
            System.err.println("DAO - Erro ao buscar orders abertas: " + e.getMessage());
            e.printStackTrace();
        }

        return openOrders;
    }

    //último id de order já salva no banco de dados
    public long getLastOrderId(){
        String sql = "SELECT MAX(id) FROM orders";
        try(Connection conn = DatabaseConfig.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    return rs.getLong(1);
                }
        } catch (SQLException e){
            System.err.println("DAO - Erro ao buscar último id: " + e.getMessage());
        }
        return 0;   //retorna 0 se nenhuma order for encontrada
    }
}

