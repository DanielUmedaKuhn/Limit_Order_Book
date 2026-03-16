package engine;
import enums.OrderType;
import model.Order;
import model.Trade;
import enums.Side;
import database.OrderDAO;
import database.TradeDAO;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MatchingEngine {
    private final OrderBook book = new OrderBook();
    private final ReentrantLock lock = new ReentrantLock();  //Controla concorrência
    private final OrderDAO orderDAO = new OrderDAO();
    private final TradeDAO tradeDAO = new TradeDAO();

    public void rebuildBookFromDatabase(){
        lock.lock();
        try{
            System.out.println("[CORE] Carregando orders abertas do banco de dados...");
            List<Order> openOrders = orderDAO.findAllOpen();

            for(Order order : openOrders){
                book.addOrder(order);
            }

            System.out.println("[CORE] Recuperação concluída. " + openOrders.size() + " orders em memória.");
        }
        finally{
            lock.unlock();
        }
    }

    public List<Trade> submitOrder(Order incoming) {
        //início da região crítica
        lock.lock();
        long startTime = System.nanoTime();
        try {
            orderDAO.save(incoming);
            List<Trade> trades = new ArrayList<>();
            if (incoming.side == Side.BUY) {
                match(incoming, book.asks, trades);
            } else {
                match(incoming, book.bids, trades);
            }

            orderDAO.update(incoming);

            //apenas orders limit com saldo vão para o livro, orders market não executadas são canceladas
            if (incoming.quantity > 0 && incoming.type ==  OrderType.LIMIT) {
                book.addOrder(incoming);
            }

            long endTime = System.nanoTime();
            System.out.println("Latência: " + (endTime - startTime) / 1000 + "µs\n");

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
            boolean canMatch = (incoming.type == OrderType.MARKET) || (incoming.side == Side.BUY ?
                                                                       incoming.price >= bestOppositePrice : //ex: compra por 10 o que custa 9
                                                                       incoming.price <= bestOppositePrice);  //ex: vende por 10 o que vale 11

            if(!canMatch){
                break; //se o melhor preço não serve, nenhum servirá
            }

            LinkedList<Order> ordersAtLevel = oppositeSide.get(bestOppositePrice);

            while(!ordersAtLevel.isEmpty() && incoming.quantity > 0){
                Order restingOrder = ordersAtLevel.peekFirst();
                int matchQuantity = Math.min(incoming.quantity, restingOrder.quantity);

                Trade trade = createTrade(incoming, restingOrder, matchQuantity, bestOppositePrice);
                trades.add(trade);
                tradeDAO.save(trade);

                incoming.quantity -= matchQuantity;
                restingOrder.quantity -= matchQuantity;
                orderDAO.update(restingOrder);  //Atualiza saldo da order que estava no livro no banco

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