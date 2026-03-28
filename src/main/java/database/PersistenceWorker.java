package database;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PersistenceWorker implements Runnable{
    private final BlockingQueue<PersistenceTask> queue = new LinkedBlockingQueue<>();
    private final OrderDAO orderDAO = new OrderDAO();
    private final TradeDAO tradeDAO = new TradeDAO();

    public void enqueue(PersistenceTask task){
        queue.offer(task);  //Adiciona na fila sem bloquear o motor
    }

    @Override
    public void run(){
        System.out.println("[DB-WORKER] Thread de persistência iniciada.");
        while(true){
            try{
                PersistenceTask task = queue.take();  //Espera até ter algo na fila
                switch(task.type){
                    case SAVE_ORDER -> orderDAO.save(task.order);
                    case UPDATE_ORDER -> orderDAO.update(task.order);
                    case SAVE_TRADE -> tradeDAO.save(task.trade);
                }
            }
            catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
