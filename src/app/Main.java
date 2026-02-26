package app;
import engine.MatchingEngine;
import model.Order;
import model.Trade;
import enums.Side;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine();
        List<Trade> resultados = new ArrayList<>();

        System.out.println("Simulando.\n");

        //2 vendedores entram no livro
        engine.submitOrder(new Order(1, 100, 10, Side.SELL));
        engine.submitOrder(new Order(2, 100, 10, Side.SELL));
        engine.submitOrder(new Order(3, 100, 10, Side.SELL));

        System.out.println("Comprador ID 4 quer comprar 15 unidades.\nComprador ID 5 quer comprar 10 unidades. \n");

        //comprador compra de ambos
        Order orderCompra1 = new Order(4, 100, 15, Side.BUY);
        resultados.addAll(engine.submitOrder(orderCompra1));

        //outra compra
        Order orderCompra2 = new Order(5, 100, 5, Side.BUY);
        resultados.addAll(engine.submitOrder(orderCompra2));

        System.out.println("Pedido processado. Resumo de execuções: ");
        for (Trade t : resultados) {
            System.out.println(t);
        }
    }
}