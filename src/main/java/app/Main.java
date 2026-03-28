package app;
import engine.MatchingEngine;
import database.DatabaseConfig;
import network.TradingServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("Inicializando Trading Server...\n");
        DatabaseConfig.initializeDatabase();  //Infraestrutura de dados (SQLite)
        MatchingEngine engine = new MatchingEngine();
        engine.rebuildBookFromDatabase();  //Carrega o que estava no banco para a RAM

        int porta = 8080;
        TradingServer server = new TradingServer(porta, engine);
        System.out.println("[INFO] Servidor aguardando conexões na porta " + porta + "...");

        Thread metricsThread = new Thread(new metrics.MetricsReporter());
        metricsThread.setDaemon(true);
        metricsThread.start();

        server.start();  //Programa ficará rodando até ser parado manualmente
    }
}