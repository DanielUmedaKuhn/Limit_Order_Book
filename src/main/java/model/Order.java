package model;
import enums.*;

public class Order {
    public final long id;
    public final long price;
    public final int initialQuantity;
    public int quantity;
    public final Side side;
    public final OrderType type;

    public Order(long id, long price, int initialQuantity, int currentQuantity, Side side, OrderType type){
        this.id = id;
        this.price = price;
        this.initialQuantity = initialQuantity;
        this.quantity = currentQuantity;
        this.side = side;
        this.type = type;
    }
}
