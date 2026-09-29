package engine;
import enums.OrderType;
import model.Order;
import model.Trade;
import enums.Side;
import database.OrderDAO;
import database.PersistenceTask;
import database.PersistenceWorker;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MatchingEngine {
    private final OrderBook book = new OrderBook();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PersistenceWorker dbWorker = new PersistenceWorker();
    
    private final Order[] ringBuffer = new Order[1024];
    private final AtomicLong producer = new AtomicLong();
    private final AtomicLong consumer = new AtomicLong();

    public MatchingEngine(){
        Thread t = new Thread(dbWorker);
        t.setName("DB-Persistence_Thread");
        t.setDaemon(true);
        t.start();

        Runnable coreRunnable = this::run;
        Thread t2 = new Thread(coreRunnable);
        t2.setName("Core_Thread");
        t2.setDaemon(true);
        t2.start();
    }

    public void rebuildBookFromDatabase(){
        System.out.println("CORE - Carregando orders abertas do banco de dados.");
        List<Order> openOrders = orderDAO.findAllOpen();

        for(Order order : openOrders){
            book.addOrder(order);
        }

        System.out.println("CORE - Recuperação concluída. " + openOrders.size() + " orders em memória.");
    }

    public void enqueue(Order order){
        long currentProducer = producer.getAndIncrement();
       
        while(true){
            long currentConsumer = consumer.get();

            if(currentProducer - currentConsumer < ringBuffer.length){
                ringBuffer[(int)(currentProducer & 1023)] = order;
                break;
            } else {
                Thread.yield();  //espera a fila ter espaço
            }
        }
    }

    public void run(){
        while (true){
            long currentProducer = producer.get();
            long currentConsumer = consumer.get();

            if(currentConsumer < currentProducer){
                int index = (int)(currentConsumer & 1023);
                Order processingOrder = ringBuffer[index];
                if(processingOrder != null){
                    consumer.incrementAndGet();
                    submitOrder(processingOrder);
                } else { 
                    Thread.yield();
                }
            } else {
                Thread.yield();  //espera a fila ter elementos
            }
            
        }
    }

    public List<Trade> submitOrder(Order incoming) {
        long startTime = System.nanoTime();
    
        metrics.MetricsRegistry.totalOrders.increment();
        dbWorker.enqueueOrder((byte) 0, incoming.id, incoming.price, incoming.getInitialQuantity(), incoming.getQuantity(), incoming.side == Side.BUY, incoming.type == OrderType.MARKET);  //incoming é uma referência

        List<Trade> trades = new ArrayList<>();
        if (incoming.side == Side.BUY) {
            match(incoming, book.asks, trades);
        } else {
            match(incoming, book.bids, trades);
        }

        dbWorker.enqueueOrder((byte) 1, incoming.id, incoming.price, incoming.getInitialQuantity(), incoming.getQuantity(), incoming.side == Side.BUY, incoming.type == OrderType.MARKET);

        //apenas orders limit com saldo vão para o livro, orders market não executadas são canceladas
        if (incoming.getQuantity() > 0 && incoming.type ==  OrderType.LIMIT) {
            book.addOrder(incoming);
        }

        long endTime = System.nanoTime();
        System.out.println("Latência: " + (endTime - startTime) / 1000 + "µs\n");
        long latencyMicros = (endTime - startTime) / 1000;

        metrics.MetricsRegistry.recordLatency(latencyMicros);
        metrics.MetricsRegistry.totalTrades.add(trades.size());

        return trades;
    }

    public boolean cancelOrder(long orderId) {  //não precisa percorrer o livro todo, cancela direto pelo ID
        Order order = book.getOrder(orderId);
        if (order == null) {
            return false;
        }

        var sideMap = (order.side == Side.BUY) ? book.bids : book.asks;
        PriceLevelQueue ordersAtPrice = sideMap.get(order.price);

        if (ordersAtPrice != null) {
            ordersAtPrice.remove(order);  //remove da fila FIFO
            if (ordersAtPrice.isEmpty()) {
                sideMap.remove(order.price);
            }
        }

        book.removeOrderFromId(orderId);  //libera memória ao remover do mapa de IDs
        order.setQuantity(0);
        dbWorker.enqueueOrder((byte) 1, order.id, order.price, order.getInitialQuantity(), order.getQuantity(), order.side == Side.BUY, order.type == OrderType.MARKET);
        return true;
    }

    private void match(Order incoming, TreeMap<Long, PriceLevelQueue> oppositeSide, List<Trade> trades){
        //enquanto houver ordens do lado oposto e a ordem atual ainda tiver quantidade
        while(!oppositeSide.isEmpty() && incoming.getQuantity() > 0){
            //melhor preço disponível no lado oposto (prioridade de preço)
            long bestOppositePrice = oppositeSide.firstKey();
            boolean canMatch = (incoming.type == OrderType.MARKET) || (incoming.side == Side.BUY ?
                                                                       incoming.price >= bestOppositePrice : //ex: compra por 10 o que custa 9
                                                                       incoming.price <= bestOppositePrice);  //ex: vende por 10 o que vale 11

            if(!canMatch){
                break; //se o melhor preço não serve, nenhum servirá
            }

            PriceLevelQueue ordersAtLevel = oppositeSide.get(bestOppositePrice);

            while(!ordersAtLevel.isEmpty() && incoming.getQuantity() > 0){
                Order restingOrder = ordersAtLevel.peekFirst();

                if(restingOrder.getQuantity() <= 0){
                    ordersAtLevel.removeFirst();
                    book.removeOrderFromId(restingOrder.id);
                    continue;
                }

                int matchQuantity = Math.min(incoming.getQuantity(), restingOrder.getQuantity());

                Trade trade = createTrade(incoming, restingOrder, matchQuantity, bestOppositePrice);
                trades.add(trade);

                dbWorker.enqueueTrade((byte) 2, trade.buyerId, trade.sellerId, trade.price, trade.quantity);

                incoming.reduceQuantity(matchQuantity);;
                restingOrder.reduceQuantity(matchQuantity);;

                dbWorker.enqueueOrder((byte) 1, restingOrder.id, restingOrder.price, restingOrder.getInitialQuantity(), restingOrder.getQuantity(), restingOrder.side == Side.BUY, restingOrder.type == OrderType.MARKET);
                if(restingOrder.getQuantity() == 0){
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