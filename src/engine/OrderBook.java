package engine;
import model.Order;
import java.util.*;

public class OrderBook {
    //Bids: preço maior tem prioridade (reverse order)
    public final TreeMap<Long, LinkedList<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    //Asks: preço menor tem prioridade (natural order)
    public final TreeMap<Long, LinkedList<Order>> asks = new TreeMap<>();

    public void addOrder(Order order){
        var sideMap = (order.side == enums.Side.BUY) ? bids : asks;
        sideMap.computeIfAbsent(order.price, k -> new LinkedList<>()).addLast(order);

    }
}
