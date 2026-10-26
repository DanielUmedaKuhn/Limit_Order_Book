package engine;
import enums.OrderType;
import model.Order;
import model.Trade;
import enums.Side;
import database.OrderDAO;
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

    //hashSet pois não precisa de ordenação, apenas de busca rápida
    private final Set<Long> peggedBids = new HashSet<>(); 
    private final Set<Long> peggedAsks = new HashSet<>(); 

    public MatchingEngine(){
        for (int i = 0; i < ringBuffer.length; i++) {
            ringBuffer[i] = new Order(); 
        }
        
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

    public void enqueue(long id, long price, int initialQuantity, int currentQuantity, Side side, OrderType type){
        long currentProducer = producer.getAndIncrement();
       
        while(true){
            long currentConsumer = consumer.get();

            if(currentProducer - currentConsumer < ringBuffer.length){
                int index = (int)(currentProducer & 1023);
                Order realOrder = ringBuffer[index];
                realOrder.id = id;
                realOrder.price = price;
                realOrder.setInitialQuantity(initialQuantity);
                realOrder.setQuantity(currentQuantity);
                realOrder.side = side;
                realOrder.type = type;
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

    private void updatePeggedOrders(Set<Long> peggedIds, long newPrice, Side side){
        Iterator<Long> it = peggedIds.iterator();
        while(it.hasNext()){  //itera sobre os IDs das ordens pegged enquanto houverem IDs
            long id = it.next();
            Order pOrder = book.getOrder(id);
            //se a ordem foi cancelada ou totalmente executada, remove do rastreador
            if(pOrder == null || pOrder.getQuantity() <= 0){
                it.remove();
                continue;
            }

            if (pOrder.price != newPrice){  //se o preço mudou, atualiza a ordem no livro
                var sideMap = (side == Side.BUY) ? book.bids : book.asks;  //pega o mapa de ordens do lado correto
                PriceLevelQueue queue = sideMap.get(pOrder.price);  //pega a fila de ordens no preço antigo
                if (queue != null){
                    queue.remove(pOrder);  //remove a ordem da fila
                    if(queue.isEmpty()){
                        sideMap.remove(pOrder.price);  //remove o nível de preço se a fila estiver vazia
                    }
                }

                pOrder.price = newPrice;  //atualiza o preço da ordem
                book.addOrder(pOrder);    //reinsere a ordem no livro

                //envia a atualização assíncrona de preço para o banco de dados 
                dbWorker.enqueueOrder((byte) 1, pOrder.id, pOrder.price, pOrder.getInitialQuantity(), pOrder.getQuantity(), side == Side.BUY, false);
            }   
        }
    }

    public List<Trade> submitOrder(Order incoming) {
        long startTime = System.nanoTime();
    
        metrics.MetricsRegistry.totalOrders.increment();
        
        //captura o melhor preçodo topo do livro para ordens pegged
        long oldBestBid = book.bids.isEmpty() ? 0 : book.bids.firstKey();
        long oldBestAsk = book.asks.isEmpty() ? 0 : book.asks.firstKey();

        if (incoming.type == OrderType.PEGGED){
            if (incoming.side == Side.BUY){
                incoming.price = oldBestBid;  //pega a melhor bid atual
                peggedBids.add(incoming.id);
            } else {
                incoming.price = oldBestAsk;  //pega a melhor ask atual
                peggedAsks.add(incoming.id);
            }
        }

        //incoming é uma referência, envia a ordem para o banco de dados antes de tentar match nela (byte 0 = insert)
        dbWorker.enqueueOrder((byte) 0, incoming.id, incoming.price, incoming.getInitialQuantity(), incoming.getQuantity(), incoming.side == Side.BUY, incoming.type == OrderType.MARKET);  

        List<Trade> trades = new ArrayList<>();  //lista de trades que serão retornados para o servidor e enviados ao cliente
        if (incoming.side == Side.BUY) {
            match(incoming, book.asks, trades);
        } else {
            match(incoming, book.bids, trades);
        }

        //envia a ordem para o banco de dados depois do match
        dbWorker.enqueueOrder((byte) 1, incoming.id, incoming.price, incoming.getInitialQuantity(), incoming.getQuantity(), incoming.side == Side.BUY, incoming.type == OrderType.MARKET);

        //ordens limit e pegged não executadas totalmente também vão para o OrderBook
        if (incoming.getQuantity() > 0 && (incoming.type == OrderType.LIMIT || incoming.type == OrderType.PEGGED)) {
            book.addOrder(incoming);
        }

        long newBestBid = book.bids.isEmpty() ? 0 : book.bids.firstKey();
        long newBestAsk = book.asks.isEmpty() ? 0 : book.asks.firstKey();

        if(newBestBid != oldBestBid && newBestBid != 0){
            updatePeggedOrders(peggedBids, newBestBid, Side.BUY);
        }
        if(newBestAsk != oldBestAsk && newBestAsk != 0){
            updatePeggedOrders(peggedAsks, newBestAsk, Side.SELL);
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

        //atualiza a ordem no livro (byte 1 = update) e no banco de dados
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

            PriceLevelQueue ordersAtLevel = oppositeSide.get(bestOppositePrice);   //pega a fila de ordens no melhor preço da side oposta

            while(!ordersAtLevel.isEmpty() && incoming.getQuantity() > 0){
                Order restingOrder = ordersAtLevel.peekFirst();   //pega a primeira ordem da fila

                if(restingOrder.getQuantity() <= 0){    //se a ordem estiver vazia, remove do livro e do mapa de IDs
                    ordersAtLevel.removeFirst();
                    book.removeOrderFromId(restingOrder.id);
                    continue;
                }

                int matchQuantity = Math.min(incoming.getQuantity(), restingOrder.getQuantity());   //quantidade a ser negociada é o mínimo entre as duas ordens

                Trade trade = createTrade(incoming, restingOrder, matchQuantity, bestOppositePrice);    //cria o objeto trade com comprador, vendedor, quantidade e preço
                trades.add(trade);

                System.out.println("Trade, price: " + trade.price / 100.0 + ", qty: " + trade.quantity);  //divide preço por 100 para mostrar o preço real, já que foi armazenado como inteiro
                
                dbWorker.enqueueTrade((byte) 2, trade.buyerId, trade.sellerId, trade.price, trade.quantity);  //grava o trade no banco de dados (byte 2 = trade)

                incoming.reduceQuantity(matchQuantity);     //reduz a quantidade da ordem que agrediu o mercado
                restingOrder.reduceQuantity(matchQuantity); //reduz a quantidade da ordem que já estava no livro  

                //atualiza a ordem no livro (byte 1 = update) e no banco de dados
                dbWorker.enqueueOrder((byte) 1, restingOrder.id, restingOrder.price, restingOrder.getInitialQuantity(), restingOrder.getQuantity(), restingOrder.side == Side.BUY, restingOrder.type == OrderType.MARKET);
                if(restingOrder.getQuantity() == 0){
                    ordersAtLevel.removeFirst();
                    book.removeOrderFromId(restingOrder.id);  //se a order acabou, é removida do mapa de IDs
                }
            }
            if(ordersAtLevel.isEmpty()){
                oppositeSide.remove(bestOppositePrice);  //se a fila de ordens estiver vazia, remove do mapa de ordens
            }
        }
    }

    private Trade createTrade(Order incoming, Order resting, int quantity, long price){
        //define comprador/vendedor, independente de quem agrediu o mercado
        long buyerId = (incoming.side == Side.BUY) ? incoming.id : resting.id;
        long sellerId = (incoming.side == Side.SELL) ? incoming.id : resting.id;

        return new Trade(buyerId, sellerId, quantity, price);

    }

    public void printOrderBook(){
        this.book.printOrderBook();
    }

    public boolean modifyOrder(long oldOrderId, long newOrderId, long newPrice, int newQuantity){
        Order originalOrder = book.getOrder(oldOrderId);   //resgata a ordem original pelo ID
        if (originalOrder == null){
            return false;   //ordem não encontrada ou já executada
        }

        enums.Side originalSide = originalOrder.getSide();
        enums.OrderType originalType = originalOrder.getType();

        boolean canceled = cancelOrder(oldOrderId);   //cancela a ordem original
        if(!canceled){
            return false;
        }

        enqueue(newOrderId, newPrice, newQuantity, newQuantity, originalSide, originalType);   //cria uma nova ordem com um novo ID, preço e quantidade modificados
        return true;
    }
}