package database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig{
    private static final String URL = "jdbc:sqlite:trading_system.db";  //é criado o arquivo
    public static Connection getConnection() throws SQLException{  //ponte para conexão
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase(){
        String sqlOrders = """
                Create TabLE IF NOT EXISTS orders(
                    id BIGINT PRIMARY KEY,
                    price BIGINT NOT NULL,
                    initial_quantity INT NOT NULL,
                    quantity INT NOT NULL,
                    side TEXT NOT NULL,
                    type TEXT NOT NULL,
                    status TEXT DEFAULT 'OPEN'
                );
                """;

        String sqlTrades = """
                CREATE TABLE IF NOT EXISTS trades(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    buyer_order_id BIGINT,
                    seller_order_id BIGINT,
                    price BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (buyer_order_id) REFERENCES orders(id),
                    FOREIGN KEY (seller_order_id) REFERENCES orders(id)
                );
                """;

        try(Connection conn = getConnection();
            Statement stmt = conn.createStatement()){

            stmt.execute(sqlOrders);
            stmt.execute(sqlTrades);

            System.out.println("[DATABASE] Tabelas orders e trades verificadas/criadas com sucesso");
        }
        catch(SQLException e){
            System.err.println("[DATABASE] Erro ao inicializar o banco: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
