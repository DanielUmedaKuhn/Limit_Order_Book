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
    //salva novas orders no banco de dados
    public void saveAll(List<Order> orders) {
        String sql = """
                INSERT INTO orders (id, price, initial_quantity, quantity, side, type, status)
                VALUES (?, ?, ?, ?, ?, ?, 'OPEN')
                """;
        
        try (Connection conn = DatabaseConfig.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
                
            for(Order order : orders){

                pstmt.setLong(1, order.id);
                pstmt.setLong(2, order.price);
                pstmt.setInt(3, order.getInitialQuantity());
                pstmt.setInt(4, order.getQuantity());
                pstmt.setString(5, order.side.toString());
                pstmt.setString(6, order.type.toString());

                pstmt.addBatch();
            };

            pstmt.executeBatch();

        } catch (SQLException e) {
            System.err.println("DAO - Erro ao salvar order: " + e.getMessage());
        }
    }
    

    //atualiza a quantidade restante e o status das orders
    public void updateAll(List<Order> orders) {
        String sql = """
                UPDATE orders 
                SET quantity = ?, status = ? 
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConfig.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for(Order order : orders){
                String status = (order.getQuantity() == 0) ? "FILLED" : "PARTIAL";
                pstmt.setInt(1, order.getQuantity());
                pstmt.setString(2, status);
                pstmt.setLong(3, order.id);

                pstmt.addBatch();
            }

            pstmt.executeBatch();

        } catch(SQLException e){
            System.err.println("DAO - Erro ao atualizar order: " + e.getMessage());
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
}