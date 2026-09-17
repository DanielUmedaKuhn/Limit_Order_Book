package database;
import java.util.ArrayList;
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
        System.out.println("DB-WORKER - Thread de persistência iniciada.");
        while(true){
            try{
                ArrayList<PersistenceTask> tasks = new ArrayList<>();
                PersistenceTask task = queue.take();  //Espera até ter algo na fila
                tasks.add(task);       //adiciona-se a 1a task separadamente para thread pausar se mercado estiver parado
                queue.drainTo(tasks);  //Pega o restante das tasks na fila

                ArrayList<PersistenceTask> saveOrderTasks = new ArrayList<>();
                ArrayList<PersistenceTask> updateOrderTasks = new ArrayList<>();
                ArrayList<PersistenceTask> saveTradeTasks = new ArrayList<>();

                for(PersistenceTask t : tasks){
                    switch(t.type){
                        case SAVE_ORDER:
                            saveOrderTasks.add(t);
                            break;
                        case UPDATE_ORDER:
                            updateOrderTasks.add(t);
                            break;
                        case SAVE_TRADE:
                            saveTradeTasks.add(t);
                            break;
                    }
                }

                orderDAO.saveAll(saveOrderTasks.stream().map(t -> t.order).toList());
                orderDAO.updateAll(updateOrderTasks.stream().map(t -> t.order).toList());
                tradeDAO.saveAll(saveTradeTasks.stream().map(t -> t.trade).toList());

            }
            catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
