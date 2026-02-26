package app;
import engine.MatchingEngine;
import model.Order;
import model.Trade;
import enums.Side;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine();

        System.out.println("Simulando.\n");

        //2 vendedores entram no livro
        engine.submitOrder(new Order(1, 100, 5, Side.SELL));
        engine.submitOrder(new Order(2, 100, 10, Side.SELL));

        System.out.println("Comprador ID 3 quer comprar 15 unidades.\n");

        //comprador compra de ambos
        Order orderCompra = new Order(3, 100, 15, Side.BUY);
        List<Trade> resultados = engine.submitOrder(orderCompra);

        System.out.println("Pedido processado. Resumo de execuções: ");
        for (Trade t : resultados) {
            System.out.println(t);
        }
    }
}