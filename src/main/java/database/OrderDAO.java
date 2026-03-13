package database;
import model.Order;
import enums.Side;
import enums.OrderType;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class OrderDAO {
    //salva nova order no banco de dados
    public void save(Order order) {
        String sql = """
                INSERT INTO orders (id, price, initial_quantity, quantity, side, type, status)
                VALUES (?, ?, ?, ?, ?, ?, 'OPEN')
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, order.id);
            pstmt.setLong(2, order.price);
            pstmt.setInt(3, order.initialQuantity);
            pstmt.setInt(4, order.quantity);
            pstmt.setString(5, order.side.toString());
            pstmt.setString(6, order.type.toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] Erro ao salvar order: " + e.getMessage());
        }
    }

    //atualiza a quantidade restante e o status de uma order
    public void update(Order order) {
        String status = (order.quantity == 0) ? "FILLED" : "PARTIAL";
        String sql = "UPDATE orders SET quantity = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, order.quantity);
            pstmt.setString(2, status);
            pstmt.setLong(3, order.id);

            pstmt.executeUpdate();
        }
        catch(SQLException e){
            System.err.println("[DAO] Erro ao atualizar order: " + e.getMessage());
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
            System.err.println("[DAO] Erro ao buscar orders abertas: " + e.getMessage());
            e.printStackTrace();
        }

        return openOrders;




    }
}