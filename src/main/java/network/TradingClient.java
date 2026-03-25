package network;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TradingClient {
    public static void main(String[] args){
        try(Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in)){

            System.out.println("Conectado ao Trading Server.");
            System.out.println("Envie orders no formato: SIDE;PRICE;QTY;TYPE");
            System.out.println("Exemplo: BUY;100;5;LIMIT (ou 'sair' para encerrar)");

            while(true){
                System.out.print("> ");
                String command = scanner.nextLine();

                if(command.equalsIgnoreCase("sair")){
                    break;
                }

                out.println(command);
                System.out.println("Resposta: " + in.readLine());
            }
        }
        catch(IOException e){
            System.err.println("Erro ao conectar: " + e.getMessage());
        }
    }
}
