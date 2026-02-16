package util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    // URL de la base MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/banque_interne?useSSL=false&serverTimezone=UTC";

    // Identifiants MySQL
    private static final String USER = "root";
    private static final String PASSWORD = "azizroot2025";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException("Erreur connexion BDD", e);
        }
    }
}
