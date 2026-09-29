package database;
import java.util.concurrent.atomic.*;

public class PersistenceWorker implements Runnable{
    private final OrderDAO orderDAO = new OrderDAO();
    private final TradeDAO tradeDAO = new TradeDAO();

    //arrays da order
    private final long[] dbIds = new long[1024];
    private final long[] dbPrices = new long[1024];
    private final int[] dbInitialQuantity = new int[1024];
    private final int[] dbQuantity = new int[1024];
    private final boolean[] dbSide = new boolean[1024];
    private final boolean[] dbType = new boolean[1024];
    
    //arrays da trade
    private final long[] tradeBuyerIds = new long[1024];
    private final long[] tradeSellerIds = new long[1024];

    private final byte[] dbTaskType = new byte[1024];
    private final AtomicIntegerArray ready = new AtomicIntegerArray(1024); 

    private final AtomicLong producer = new AtomicLong();
    private final AtomicLong consumer = new AtomicLong();

    public void enqueueOrder(byte taskType, long id, long price, int initialQuantity, int quantity, boolean side, boolean type){
        long currentProducer = producer.getAndIncrement();

        while(true){
            long currentConsumer = consumer.get();

            if(currentProducer - currentConsumer < 1024){
                int index = (int)(currentProducer & 1023);
                dbTaskType[index] = taskType;       //SAVE_ORDER ou UPDATE_ORDER

                dbIds[index] = id;
                dbPrices[index] = price;
                dbInitialQuantity[index] = initialQuantity;
                dbQuantity[index] = quantity;
                dbSide[index] = side;
                dbType[index] = type;

                ready.set(index, 1);
                break;
            } else {
                Thread.yield();
            }
        }
    }
    
    public void enqueueTrade(byte taskType, long buyerId, long sellerId, long price, int quantity){
        long currentProducer = producer.getAndIncrement();

        while(true){
            long currentConsumer = consumer.get();

            if(currentProducer - currentConsumer < 1024){
                int index = (int)(currentProducer & 1023);
                dbTaskType[index] = taskType;       //SAVE_ORDER ou UPDATE_ORDER

                tradeBuyerIds[index] = buyerId;
                tradeSellerIds[index] = sellerId;
                dbPrices[index] = price;
                dbQuantity[index] = quantity;

                ready.set(index, 1);
                break;
            } else {
                Thread.yield();
            }
        }
    }

    @Override
    public void run(){
        System.out.println("DB-WORKER - Thread de persistência iniciada.");
        while(true){
            long currentProducer = producer.get();
            long currentConsumer = consumer.get();
            if(currentConsumer < currentProducer){
                int batchCount = 0;

                while(currentConsumer < currentProducer){
                    int index = (int)(currentConsumer & (1023));

                    while(ready.get(index) == 0){
                        Thread.yield();
                    } 

                    byte task = dbTaskType[index];

                    try{
                        if (task == 0){
                            orderDAO.addSaveBatch(dbIds[index], dbPrices[index], dbInitialQuantity[index], dbQuantity[index], dbSide[index], dbType[index]);
                        } else if (task == 1){
                            orderDAO.addUpdateBatch(dbIds[index], dbQuantity[index]);
                        } else if (task == 2){
                            tradeDAO.addSaveBatch(tradeBuyerIds[index], tradeSellerIds[index], dbPrices[index], dbQuantity[index]);
                        }
                    } catch (Exception e){
                        System.err.println("Erro ao processar batch: " + e.getMessage());
                    }
                    ready.set(index,0);
                    
                    currentConsumer++;
                    batchCount++;
                }

                consumer.set(currentConsumer);

                if(batchCount > 0){
                        orderDAO.executeBatches();
                        tradeDAO.executeBatches();
                }
            } else {
                Thread.yield();
            }
        }   
    }
}
