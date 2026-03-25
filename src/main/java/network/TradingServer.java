package network;
import engine.MatchingEngine;
import java.io.*;
import java.net.*;

public class TradingServer {
    private final int port;
    private final MatchingEngine engine;

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
                handleClient(clientSocket);
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
                    //protocolo: SIDE;PRICE;QUANTITY;TYPE
                    String[] parts = inputLine.split(";");
                    enums.Side side = enums.Side.valueOf(parts[0].toUpperCase());
                    long price = Long.parseLong(parts[1]);
                    int qty = Integer.parseInt(parts[2]);
                    enums.OrderType type = enums.OrderType.valueOf(parts[3].toUpperCase());

                    //ID único baseado no tempo para cada order de rede
                    long orderId = System.currentTimeMillis();

                    model.Order newOrder = new model.Order(orderId, price, qty, qty, side, type);
                    var trades = engine.submitOrder(newOrder);
                    out.println("Order " + orderId + "processada. Matches " + trades.size());
                }
                catch(Exception e){
                    out.println("ERRO: Formato Inválido. Use SIDE;PRICE;QTY;TYPE (ex: BUY;150;10;LIMIT)");
                }
            }
        }
        catch(IOException e){
            System.out.println("[SERVER] Conexão encerrado com o cliente.");
        }
    }
}
