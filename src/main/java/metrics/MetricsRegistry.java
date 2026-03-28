package metrics;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

public class MetricsRegistry {
    public static final LongAdder totalOrders = new LongAdder();
    public static final LongAdder totalTrades = new LongAdder();

    private static final int BUFFER_SIZE = 10000;
    private static final ConcurrentLinkedQueue<Long> latencyBuffer = new ConcurrentLinkedQueue<>();

    public static void recordLatency(long micros){
        latencyBuffer.offer(micros);
        if(latencyBuffer.size() > BUFFER_SIZE){
            latencyBuffer.poll();
        }
    }

    public static java.util.List<Long> getSnapshot(){
        return new java.util.ArrayList<>(latencyBuffer);
    }
}
