package engine;
import enums.OrderType;
import enums.Side;

//classe auta como um Data Transfer Object, permite que o 1º ring buffer dê um wrap-around sem que haja perda de informação
public class OrderCommand {
    public long id;
    public long price;
    public int initialQuantity;
    public int quantity;
    public Side side;
    public OrderType type;

    //construtor vazio para pré-alocação
    public OrderCommand(){
    }
}
