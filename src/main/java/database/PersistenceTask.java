package database;
import model.Order;
import model.Trade;

public class PersistenceTask {
    public enum Type {SAVE_ORDER, UPDATE_ORDER, SAVE_TRADE}
    public final Type type;
    public final Order order;
    public final Trade trade;

    public PersistenceTask(Type type, Order order, Trade trade){
        this.type = type;
        this.order = order;
        this.trade = trade;
    }
}
