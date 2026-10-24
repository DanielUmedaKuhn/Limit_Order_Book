package network;
import engine.MatchingEngine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

public class TradingServer {
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
                        out.println("Order " + orderId + (canceled ? " cancelada com sucesso." : " não encontrada ou já executada."));
                    } else {
                        //protocolo: TYPE SIDE PRICE QUANTITY 
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
                        out.println("Order " + orderId + " recebida e enviada para a fila de processamento.");
                    }
                }
                catch(Exception e){
                    out.println("ERRO: Formato Inválido. Use limit SIDE PRICE QTY (ex: limit buy 100 10), market SIDE QTY (ex: market buy 10) ou cancel ID (ex: cancel 16954203)");
                }
            }
        }
        catch(IOException e){
            System.out.println("[SERVER] Conexão encerrado com o cliente.");
        }
    }
}
