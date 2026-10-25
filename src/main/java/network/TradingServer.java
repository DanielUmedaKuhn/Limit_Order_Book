package network;
import engine.MatchingEngine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

public class TradingServer {  //classe servidor para receber conexões de clientes
    private final int port;
    private final MatchingEngine engine;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static final AtomicLong idGenerator = new AtomicLong(System.currentTimeMillis());

    public TradingServer(int port, MatchingEngine engine){
        this.port = port;
        this.engine = engine;
    }

    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("[SERVER] Motor de Negociação rodando na porta "  + port);

            while(true){
                //servidor espera alguém conectar
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Novo cliente conectado: " + clientSocket.getInetAddress());
                threadPool.execute(() -> handleClient(clientSocket));  //joga handleClient para a pool de threads
            }
        }
        catch(IOException e){
            System.out.println("[SERVER] Erro no servidor: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket){
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)){

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                try {
                    String[] parts = inputLine.trim().split("\\s+");
                    if(parts[0].equalsIgnoreCase("CANCEL")){
                        long orderId = Long.parseLong(parts[1]);
                        boolean canceled = engine.cancelOrder(orderId);
                        out.println("Order " + orderId + (canceled ? " cancelled successfully." : " not found or already executed."));
                    } else if (parts[0].equalsIgnoreCase("BOOK")){
                        engine.printOrderBook();
                        out.println("Order Book printed.");
                    } else if (parts[0].equalsIgnoreCase("MODIFY")){
                        //coleta o ID da ordem, o novo preço e a nova quantidade
                        long oldOrderId = Long.parseLong(parts[1]);
                        long newPrice = Long.parseLong(parts[2]);
                        int newQty = Integer.parseInt(parts[3]);

                        long newOrderId = idGenerator.getAndIncrement();   //gera um novo ID para a ordem modificada

                        boolean modified = engine.modifyOrder(oldOrderId, newOrderId, newPrice, newQty);
                        out.println("Order " + oldOrderId + (modified ? (" modified. New ID: " + newOrderId + " " + newQty + " @ " + newPrice) : " not found or already executed."));

                    } else {
                        //coleta os parâmetros da ordem (type, side, price, quantity)
                        enums.OrderType type = enums.OrderType.valueOf(parts[0].toUpperCase());
                        enums.Side side = enums.Side.valueOf(parts[1].toUpperCase());
                        long price;
                        int qty;

                        if (type == enums.OrderType.MARKET){
                            price = 0;
                            qty = Integer.parseInt(parts[2]);
                        } else {
                            price = Long.parseLong(parts[2]);
                            qty = Integer.parseInt(parts[3]);
                        }

                        //ID único baseado no tempo para cada order de rede
                        long orderId = idGenerator.getAndIncrement();

                        engine.enqueue(orderId, price, qty, qty, side, type);
                        
                        if (type == enums.OrderType.LIMIT){
                            out.println("Order created: " + side + " " + qty + " @ " + price + " " + orderId);
                        } else if (type == enums.OrderType.MARKET){  //print é diferente para market orders por não possuirem preço definido
                            out.println("Order created: " + side + " " + qty + " @ MARKET " + orderId);
                        }
                    }
                }
                catch(Exception e){
                    out.println("ERRO: Formato Inválido. Use limit SIDE PRICE QTY (ex: limit buy 100 10), market SIDE QTY (ex: market buy 10), cancel ID (ex: cancel 16954203) ou 'book' para imprimir o livro de ordens.");
                }
            }
        }
        catch(IOException e){
            System.out.println("[SERVER] Conexão encerrado com o cliente.");
        }
    }
}
