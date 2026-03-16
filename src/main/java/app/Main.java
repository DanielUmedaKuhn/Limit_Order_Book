package app;
import engine.MatchingEngine;
import model.Order;
import model.Trade;
import enums.Side;
import enums.OrderType;
import database.DatabaseConfig;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Simulando.\n");
        DatabaseConfig.initializeDatabase();
        MatchingEngine engine = new MatchingEngine();
        engine.rebuildBookFromDatabase();  //Carrega o que estava no banco para a RAM

        long id1 = System.currentTimeMillis();
        //engine.submitOrder(new Order(id1, 100, 10, 10, Side.SELL,OrderType.LIMIT));

        long id2 = System.currentTimeMillis();
        //engine.submitOrder(new Order(id2, 110, 5, 5, Side.SELL, OrderType.LIMIT));

        long id3 = System.currentTimeMillis();
        engine.submitOrder(new Order(id3, 100, 10, 10, Side.BUY, OrderType.LIMIT));
        System.out.print("Orders enviadas. Verifique o banco de dados e feche o programa.");
    }
}