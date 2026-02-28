package engine;
import model.Order;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderBook {
    //TreeMap para a prioridade de preço
    //Bids: preço maior tem prioridade (reverse order)
    public final TreeMap<Long, LinkedList<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    //Asks: preço menor tem prioridade (natural order)
    public final TreeMap<Long, LinkedList<Order>> asks = new TreeMap<>();
    private final Map<Long, Order> ordersById = new ConcurrentHashMap<>();

    public void addOrder(Order order){
        var sideMap = (order.side == enums.Side.BUY) ? bids : asks;
        sideMap.computeIfAbsent(order.price, k -> new LinkedList<>()).addLast(order);
        ordersById.put(order.id, order);
    }

    public Order getOrder(long id){
        return ordersById.get(id);
    }

    public void removeOrderFromId(long id){
        ordersById.remove(id);
    }
}
