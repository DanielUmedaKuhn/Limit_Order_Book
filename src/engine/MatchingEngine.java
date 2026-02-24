package engine;
import model.Order;
import enums.Side;
import java.util.TreeMap;
import java.util.LinkedList;

public class MatchingEngine {
    private final OrderBook book = new OrderBook();

    public void submitOrder(Order incoming){
        if(incoming.side == Side.BUY){
            match(incoming, book.asks, book.bids);
        }
        else{
            match(incoming, book.bids, book.asks);
        }
    }

    private void match(Order incoming, TreeMap<Long, LinkedList<Order>> oppositeSide, TreeMap<Long, LinkedList<Order>> ownBook){
        //enquanto houver ordens do lado oposto e a ordem atual ainda tiver quantidade
        while(!oppositeSide.isEmpty() && incoming.quantity > 0){
            //melhor preço disponível no lado oposto (prioridade de preço)
            long bestOppositePrice = oppositeSide.firstKey();
            //verifica se o preço é compatível para negócio
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
                executeTrade(incoming, restingOrder, matchQuantity, bestOppositePrice);

                incoming.quantity -= matchQuantity;
                restingOrder.quantity -= matchQuantity;

                if(restingOrder.quantity == 0){
                    ordersAtLevel.removeFirst();
                }
            }
            if(ordersAtLevel.isEmpty()){
                oppositeSide.remove(bestOppositePrice);

            }
        }
        if(incoming.quantity > 0){
            book.addOrder(incoming);
        }
    }
    private void executeTrade(Order incoming, Order resting, int quantity, long price){
        long buyerId;
        long sellerId;

        if(incoming.side == Side.BUY){
            buyerId = incoming.id;
            sellerId = resting.id;
        }
        else {
            buyerId = resting.id;
            sellerId = incoming.id;
        }
        System.out.println("[TRADE] Quantidade: " + quantity +
                           " | Preço: " + price +
                           " | Comprador (ID): " + buyerId +
                           " | Vendedor (ID): " + sellerId);
    }
}