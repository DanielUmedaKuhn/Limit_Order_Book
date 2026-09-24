package engine;
import model.Order;

public class PriceLevelQueue{
    private Order head;
    private Order tail;
    private int size = 0;

    public void addLast(Order order){
        if (tail == null){      //caso seja o primeira order
            head = order;
            tail = order;
            order.prev = null;
            order.next = null;
        } else {                //caso não seja a primeira order
            tail.next = order;
            order.prev = tail;
            order.next = null;
            tail = order;
        }

        size++;
    }

public void remove(Order order){
        if (order.prev != null) {
            order.prev.next = order.next;    //atualiza o next da order anterior
        } else {    
            head = order.next;               //se não há order anterior, o próximo passa a ser a nova head
        }

        if (order.next != null) {             
            order.next.prev = order.prev;    //atualiza o prev da order seguinte
        } else {
            tail = order.prev;               //se não há order seguinte, o anterior passa a ser a nova tail
        }

        //libera a memória da order removida
        order.prev = null;      
        order.next = null;
        size--;
    }

    public Order peekFirst(){
        return head;    //retorna a head em O(1)
    }

    public Order removeFirst(){     //remove e retorna a head removida
        if (head == null) return null;
        Order first = head;
        remove(first);
        return first;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    public int size(){
        return size;
    }
}