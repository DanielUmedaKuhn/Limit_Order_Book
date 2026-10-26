package engine;
import model.Order;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderBook {
    //TreeMap para a ordenamento automático por preço
    //Bids: preço maior tem prioridade (reverse order)
    public final TreeMap<Long, PriceLevelQueue> bids = new TreeMap<>(Collections.reverseOrder());
    //Asks: preço menor tem prioridade (natural order)
    public final TreeMap<Long, PriceLevelQueue> asks = new TreeMap<>();
    private final Map<Long, Order> ordersById = new ConcurrentHashMap<>();

    public void addOrder(Order order){
        var sideMap = (order.side == enums.Side.BUY) ? bids : asks;
        sideMap.computeIfAbsent(order.price, k -> new PriceLevelQueue()).addLast(order);
        ordersById.put(order.id, order);
    }

    public Order getOrder(long id){
        return ordersById.get(id);
    }

    public void removeOrderFromId(long id){
        ordersById.remove(id);
    }

    //imprime book atual no terminal do servidor (main)
    public void printOrderBook(){
        System.out.println("===================== Order Book =====================");
        System.out.println("* ----- Asks (Vendas) -----\n");
        asks.descendingMap().forEach((price, orderList) -> {  //inverte a ordem com "descendingMap" para mostrar o menor Asking Price no topo do book
            for(Order order : orderList.getOrders()){
                System.out.printf("Preço: %.2f | Qtd: %d | ID: %d | Tipo: %s%n", 
                                  price / 100.0, order.getQuantity(), order.id, order.getType());            
            }
        });

        System.out.println("----------------------------------------------");
        System.out.println("              Spread: " + (asks.isEmpty() || bids.isEmpty() ? "N/A" : (asks.firstKey() - bids.firstKey()) / 100.0));
        System.out.println("----------------------------------------------");
        bids.forEach((price, orderList) -> {  //ordem natural para mostrar o maior Bidding Price no topo do book
            for(Order order : orderList.getOrders()){
                System.out.printf("Preço: %.2f | Qtd: %d | ID: %d | Tipo: %s%n", 
                                  price / 100.0, order.getQuantity(), order.id, order.type);        
            }
        });
        System.out.println("\n* ----- Bids (Compras) -----");


        System.out.println("=======================================================\n");
    }


}

