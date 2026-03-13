package test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import engine.MatchingEngine;
import model.*;
import enums.*;
import java.util.List;

public class MatchingEngineTest {
    private MatchingEngine engine;

    @BeforeEach
    void setup(){
        engine = new MatchingEngine();
    }

    @Test
    void testMarketOrderClearBook(){
        //popula o livro com vendedores limit
        engine.submitOrder(new Order(1, 100, 10, 10, Side.SELL, OrderType.LIMIT));
        engine.submitOrder(new Order(2, 110, 10, 10, Side.SELL, OrderType.LIMIT));

        //order market deve ignorar preço e buscar liquidez
        Order marketBuy = new Order(3, 0, 15, 15, Side.BUY, OrderType.MARKET);
        List<Trade> trades = engine.submitOrder(marketBuy);

        assertEquals(2, trades.size(), "Deveria ter executado contra dois vendedores.");
        assertEquals(0, marketBuy.quantity, "A order MARKET deveria ter sido totalmente preenchida.");
    }
    @Test
    void testOrderIntegrity(){
        Order sellOrder = new Order(1, 100, 10, 10, Side.SELL, OrderType.LIMIT);
        engine.submitOrder(sellOrder);

        Order buyOrder = new Order(2, 100, 5, 5, Side.BUY, OrderType.LIMIT);
        List<Trade> trades = engine.submitOrder(buyOrder);

        Trade trade = trades.get(0);
        assertEquals(5, trade.quantity);
        assertEquals(5, sellOrder.quantity, "(Quantity) da ordem no livro deve ser reduzida");
        assertEquals(10, sellOrder.initialQuantity, "A quantidade original (InitialQuantity) deve ser imutável");
    }
}
