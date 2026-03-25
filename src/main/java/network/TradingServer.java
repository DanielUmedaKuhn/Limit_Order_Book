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
                System.out.println("[SERVER] Recebido: " + inputLine);
                out.println("Ordem recebida com sucesso.");
            }
        }
        catch(IOException e){
            System.out.println("[SERVER] Cliente desconectado.");
        }
    }
}
