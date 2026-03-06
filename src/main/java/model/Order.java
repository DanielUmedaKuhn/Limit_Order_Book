package model;
import enums.*;

public class Order {
    public final long id;
    public final long price;
    public final int initialQuantity;
    public int quantity;
    public final Side side;
    public final OrderType type;

    public Order(long id, long price, int quantity, Side side,  OrderType type){
        this.id = id;
        this.price = price;
        this.initialQuantity = quantity;
        this.quantity = quantity;
        this.side = side;
        this.type = type;
    }
}
