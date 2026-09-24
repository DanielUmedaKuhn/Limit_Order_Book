package model;
import enums.*;

public class Order {
    public final long id;
    public final long price;
    private final int initialQuantity;
    private int quantity;
    public final Side side;
    public final OrderType type;
    public Order prev;
    public Order next;

    public Order(long id, long price, int initialQuantity, int currentQuantity, Side side, OrderType type){
        this.id = id;
        this.price = price;
        this.initialQuantity = initialQuantity;
        this.quantity = currentQuantity;
        this.side = side;
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }

    public void reduceQuantity(int tradedQuantity) {
        if (tradedQuantity > 0 && tradedQuantity <= this.quantity) {
            this.quantity -= tradedQuantity;
        } else {
            throw new IllegalArgumentException("Quantidade negociada inválida ou superior ao saldo.");
        }
    }
}
