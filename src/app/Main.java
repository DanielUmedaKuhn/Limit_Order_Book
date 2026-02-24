package app;
import engine.MatchingEngine;
import model.Order;
import enums.Side;

public class Main {
    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine();
        
        System.out.println("Simulando.\n");

        System.out.println("Teste 1: ordem entra no livro(resting order)");
        engine.submitOrder(new Order(1, 100, 10, Side.SELL));
        System.out.println("Vendedor ID 1 postou 10 unidades a $100.\n");

        System.out.println("Teste 2: match exato");
        engine.submitOrder(new Order(2, 100, 10, Side.BUY));
        //esperado: [TRADE] 10 unidades a $100 entre ID 2 e ID 1
        System.out.println();

        System.out.println("Teste 3: prioridade de preço");
        engine.submitOrder(new Order(3, 101, 10, Side.SELL));
        engine.submitOrder(new Order(4, 102, 10, Side.SELL));
        //comprador quer 15 unidades e paga até $105
        engine.submitOrder(new Order(5, 105, 15, Side.BUY));
        System.out.println();

        System.out.println("Teste 4: prioridade de tempo (FIFO)");
        engine.submitOrder(new Order (6, 110, 10, Side.SELL));
        engine.submitOrder(new Order (7, 110, 10, Side.SELL));
        //esperado: deve executar com o ID 6 (que chegou primeiro)
        engine.submitOrder(new Order ( 8, 110, 10, Side.BUY));
    }
}