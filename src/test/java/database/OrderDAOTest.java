package database;
import enums.OrderType;
import enums.Side;
import model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class OrderDAOTest {
    private OrderDAO dao;

    @BeforeEach
    void setup(){
        //inicializa o banco (cria tabelas se ainda não existirem)
        DatabaseConfig.initializeDatabase();
        dao = new OrderDAO();
    }

    @Test
    void testSaveAndFindAllOpen(){
        long testId = System.currentTimeMillis(); //prepara order com ID único
        Order order = new Order(testId, 150, 10, 10, Side.BUY, OrderType.LIMIT);
        dao.save(order);

        List<Order> openOrders = dao.findAllOpen();  //busca todas as orders abertas no banco
        boolean found = openOrders.stream().anyMatch(o -> o.id == testId);
        assertTrue(found, "A order salva deveria estar presente na lista de orders abertas.");
    }

    @Test
    void testUpdateOrderQuantityAndStatus(){
        long testId = System.currentTimeMillis();
        Order order = new Order(testId, 200, 20, 10, Side.SELL, OrderType.LIMIT);
        dao.save(order);

        order.quantity = 5;  //altera quantidade para 5
        dao.update(order);

        List<Order> openOrders = dao.findAllOpen();
        Order updatedOrder = openOrders.stream()
                .filter(o -> o.id == testId)
                .findFirst()
                .orElse(null);

        assertNotNull(updatedOrder);
        assertEquals(5, updatedOrder.quantity, "A quantitdade no banco deveria ser 5 após o update.");
    }
}
