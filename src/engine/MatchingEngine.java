package engine;
import model.Order;
import model.Trade;
import enums.Side;
import java.util.*;

public class MatchingEngine {
    private final OrderBook book = new OrderBook();

    public List<Trade> submitOrder(Order incoming){
        List<Trade> trades = new ArrayList<>();
        if(incoming.side == Side.BUY){
            match(incoming, book.asks, trades);
        }
        else{
            match(incoming, book.bids, trades);
        }

        if(incoming.quantity > 0){
            book.addOrder(incoming);
        }
        return trades;
    }

    private void match(Order incoming, TreeMap<Long, LinkedList<Order>> oppositeSide, List<Trade> trades){
        //enquanto houver ordens do lado oposto e a ordem atual ainda tiver quantidade
        while(!oppositeSide.isEmpty() && incoming.quantity > 0){
            //melhor preço disponível no lado oposto (prioridade de preço)
            long bestOppositePrice = oppositeSide.firstKey();
            //verifica se o preço é compatível para negócio (com operador ternário)
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
                }
            }
            if(ordersAtLevel.isEmpty()){
                oppositeSide.remove(bestOppositePrice);

            }
        }
    }
    private Trade createTrade(Order incoming, Order resting, int quantity, long price){
        long buyerId = (incoming.side == Side.BUY) ? incoming.id : resting.id;
        long sellerId = (incoming.side == Side.SELL) ? incoming.id : resting.id;

        return new Trade(buyerId, sellerId, quantity, price);

    }
}