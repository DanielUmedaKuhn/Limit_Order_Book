package app;
import engine.MatchingEngine;
import model.Order;
import model.Trade;
import enums.Side;
import enums.OrderType;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine();
        List<Trade> resultados = new ArrayList<>();

        System.out.println("Simulando.\n");

        //vendedor entra no livro
        engine.submitOrder(new Order(1, 100, 10, Side.SELL, OrderType.LIMIT));
        engine.submitOrder(new Order(2, 100, 10, Side.SELL, OrderType.LIMIT));
        engine.submitOrder(new Order(3, 100, 10, Side.SELL, OrderType.LIMIT));

        System.out.println("Cancelando vendedor ID 3...");
        engine.cancelOrder(3);

        System.out.println("Comprador ID 4 envia MARKET ORDER (15 unidades)...");
        Order marketBuy = new Order(4, 0, 15, Side.BUY, OrderType.MARKET);
        resultados.addAll(engine.submitOrder(marketBuy));

        //comprador tenta comprar o que já foi cancelado
        System.out.println("Comprador ID 5 envia LIMIT ORDER (90$) - Aguardando...");
        engine.submitOrder(new Order(5, 90, 5, Side.BUY, OrderType.LIMIT));

        System.out.println("Pedidos processados. Resumo de execuções: ");
        for (Trade t : resultados) {
            System.out.println(t);
        }
    }
}