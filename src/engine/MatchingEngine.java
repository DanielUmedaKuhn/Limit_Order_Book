package engine;
import model.Order;
import model.Trade;
import enums.Side;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MatchingEngine {
    private final OrderBook book = new OrderBook();
    private final ReentrantLock lock = new ReentrantLock();  //Controla concorrência

    public List<Trade> submitOrder(Order incoming) {
        //início da região crítica
        lock.lock();
        try {
            List<Trade> trades = new ArrayList<>();
            if (incoming.side == Side.BUY) {
                match(incoming, book.asks, trades);
            } else {
                match(incoming, book.bids, trades);
            }

            if (incoming.quantity > 0) {
                book.addOrder(incoming);
            }
            return trades;
        }
        finally {
            //garante que o lock é liberado
            lock.unlock();
        }
    }

    public boolean cancelOrder(long orderId) {  //não precisa percorrer o livro todo, cancela direto pelo ID
        lock.lock();
        try {
            Order order = book.getOrder(orderId);
            if (order == null) {
                return false;
            }

            var sideMap = (order.side == Side.BUY) ? book.bids : book.asks;
            LinkedList<Order> ordersAtPrice = sideMap.get(order.price);

            if (ordersAtPrice != null) {
                ordersAtPrice.remove(order);  //remove da fila FIFO
                if (ordersAtPrice.isEmpty()) {
                    sideMap.remove(order.price);
                }
            }

            book.removeOrderFromId(orderId);  //libera memória ao remover do mapa de IDs
            return true;
        }
        finally{
                lock.unlock();
        }
    }
    private void match(Order incoming, TreeMap<Long, LinkedList<Order>> oppositeSide, List<Trade> trades){
        //enquanto houver ordens do lado oposto e a ordem atual ainda tiver quantidade
        while(!oppositeSide.isEmpty() && incoming.quantity > 0){
            //melhor preço disponível no lado oposto (prioridade de preço)
            long bestOppositePrice = oppositeSide.firstKey();
            boolean canMatch = (incoming.side == Side.BUY) ?
                    incoming.price >= bestOppositePrice : //ex: compra por 10 o que custa 9
                    incoming.price <= bestOppositePrice;  //ex: vende por 10 o que vale 11

            if(!canMatch){
                break; //se o melhor preço não serve, nenhum servirá
            }

            LinkedList<Order> ordersAtLevel = oppositeSide.get(bestOppositePrice);

            while(!ordersAtLevel.isEmpty() && incoming.quantity > 0){
                Order restingOrder = ordersAtLevel.peekFirst();
                int matchQuantity = Math.min(incoming.quantity, restingOrder.quantity);

                trades.add(createTrade(incoming, restingOrder, matchQuantity, bestOppositePrice));

                incoming.quantity -= matchQuantity;
                restingOrder.quantity -= matchQuantity;

                if(restingOrder.quantity == 0){
                    ordersAtLevel.removeFirst();
                    book.removeOrderFromId(restingOrder.id);  //se a order acabou, é removida do mapa de IDs
                }
            }
            if(ordersAtLevel.isEmpty()){
                oppositeSide.remove(bestOppositePrice);

            }
        }
    }
    private Trade createTrade(Order incoming, Order resting, int quantity, long price){
        //define comprador/vendedor, independente de quem agrediu o mercado
        long buyerId = (incoming.side == Side.BUY) ? incoming.id : resting.id;
        long sellerId = (incoming.side == Side.SELL) ? incoming.id : resting.id;

        return new Trade(buyerId, sellerId, quantity, price);

    }
}