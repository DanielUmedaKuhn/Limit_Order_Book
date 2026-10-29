package model;

public class Trade {
    public final long buyerId;
    public final long sellerId;
    public final int quantity;
    public final long price;

    public Trade(long buyerId, long sellerId, int quantity, long price){
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.quantity = quantity;
        this.price = price;
    }
}
