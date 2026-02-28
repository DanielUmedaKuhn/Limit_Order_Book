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

        //vendedor entra no livro
        engine.submitOrder(new Order(1, 100, 10, Side.SELL));
        System.out.println("Comprador ID 1 postou 10 unidades a %100.\n");

        System.out.println("Vendedor ID 1 solicitou cancelamento...\n");
        boolean cancelado = engine.cancelOrder(1);
        if (cancelado){
            System.out.println("Order 1 removida com sucesso.\n");
        }
        else{
            System.out.println("Falha ao cancelar, order não encontrada ou já executada.");
        }

        //comprador tenta comprar o que já foi cancelado
        Order compra = new Order(2, 100, 10, Side.BUY);
        List<Trade> result = engine.submitOrder(compra);
        if(result.isEmpty()){
            System.out.println("Comprador ID 2 não encontrou vendedor (funcionou).");
        }

        System.out.println("Pedido processado. Resumo de execuções: ");
        for (Trade t : resultados) {
            System.out.println(t);
        }
    }
}