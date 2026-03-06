package test;
import engine.MatchingEngine;
import model.Order;
import enums.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceStressTest {
    public static void main(String[] args) throws InterruptedException {
        MatchingEngine engine = new MatchingEngine();
        int numThreads = 4;
        int ordersPerThread = 2500;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        AtomicLong totalLatency = new AtomicLong(0);
        System.out.println("Iniciando teste de estre com 10.000 orders...");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for(int j = 0; j < ordersPerThread; j++){
                    long id = (threadId * ordersPerThread) + j;
                    Order order = new Order(id, 100, 1, Side.BUY, OrderType.LIMIT);

                    long start = System.nanoTime();
                    engine.submitOrder(order);
                    totalLatency.addAndGet(System.nanoTime() - start);
                }
                latch.countDown();
            });
        }
        latch.await();
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Resultados:");
        System.out.println("Tempo total: " + duration + "ms");
        System.out.println("Latência média por ordem: " + (totalLatency.get() / 10000.0) / 1000.0 + "µs");
        executor.shutdown();
    }
}