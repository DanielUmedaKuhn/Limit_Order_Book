package network;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TradingClient {    //classe cliente para acessar o servidor
    public static void main(String[] args){
        try(Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in)){

            System.out.println("Conectado ao Trading Server.");
            System.out.println("Para LIMIT: 'limit SIDE PRICE QTY' (ex: limit buy 100 10)");
            System.out.println("Para MARKET: 'market SIDE QTY' (ex: market buy 10)");
            System.out.println("Para MODIFICAR: 'modify ID NEW_PRICE NEW_QUANTITY' (ex: modify 16954203 105 15)");
            System.out.println("Para CANCELAR: cancel ID (Exemplo: cancel 16954203)");
            System.out.println("Para ver o livro de ordens: book");
            System.out.println("Digite 'sair' para encerrar.");

            while(true){
                System.out.print("> ");
                String command = scanner.nextLine();

                if(command.equalsIgnoreCase("sair")){
                    break;
                }

                out.println(command);
                System.out.println("Response: " + in.readLine());
            }
        }
        catch(IOException e){
            System.err.println("Erro ao conectar: " + e.getMessage());
        }
    }
}