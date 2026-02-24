package model;
import enums.Side;

public class Order {
    public final long id;
    public final long price;
    public final int initialQuantity;
    public int quantity;
    public final Side side;

    public Order(long id, long price, int quantity, Side side){
        this.id = id;
        this.price = price;
        this.initialQuantity = quantity;
        this.quantity = quantity;
        this.side = side;
    }
}
