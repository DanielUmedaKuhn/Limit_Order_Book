package metrics;
import java.util.Collections;
import java.util.List;

public class MetricsReporter implements Runnable{
    private long lastOrdersCount = 0;
    private static final int REPORT_INTERVAL_SEC = 10;

    @Override
    public void run(){
        while(true){
            try{
                Thread.sleep(REPORT_INTERVAL_SEC * 1000);  //reporta a cada 10 segundos

                long currentOrders  = MetricsRegistry.totalOrders.sum();
                long totalInInterval = currentOrders - lastOrdersCount;
                double ops = (double) totalInInterval / REPORT_INTERVAL_SEC;
                lastOrdersCount = currentOrders;

                List<Long> latencies = MetricsRegistry.getSnapshot();

                System.out.println("-----[TELEMETRIA DO SISTEMA]-----");
                System.out.printf("Vazão atual: %.2f orders/segundo (OPS)\n", ops);
                System.out.println("Orders totais: " + MetricsRegistry.totalOrders.sum());

                if(!latencies.isEmpty()) {
                    Collections.sort(latencies);
                    long p50 = latencies.get((int) (latencies.size() * 0.50));
                    long p95 = latencies.get((int) (latencies.size() * 0.95));
                    long p99 = latencies.get((int) (latencies.size() * 0.50));
                    long avg = latencies.stream().mapToLong(Long::longValue).sum() / latencies.size();

                    System.out.println("Negócios (Trades): " + MetricsRegistry.totalTrades.sum());
                    System.out.println("Latência Média: " + avg + "µs");
                    System.out.println("Percentis: p50: " + p50 + "µs | p95: " + p95 + "µs | p99: " + p99 + " µs");
                    System.out.println("---------------------------------");
                }
            }
            catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
