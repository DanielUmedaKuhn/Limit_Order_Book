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
        model.Order testOrder = new model.Order(101, 150, 10, 10, enums.Side.BUY, enums.OrderType.LIMIT);
        database.OrderDAO dao = new database.OrderDAO();
        dao.save(testOrder);
        System.out.println("Order de teste enviada ao banco");

        testOrder.quantity = 5;
        dao.update(testOrder);
        System.out.println("order de teste atualizada no banco.");
    }
}