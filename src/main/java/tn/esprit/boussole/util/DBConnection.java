package tn.esprit.boussole.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Utilisation de la base boussole sur le port standard 3306
    private static final String URL = "jdbc:mysql://localhost:3306/boussole2?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "2121";

    public static Connection getConnection() {
        Connection cn = null;
        try {
            // Tentative de connexion
            cn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion réussie à boussole_db !");
        } catch (SQLException e) {
            // Gestion de l'erreur si le mot de passe ou l'URL est incorrect
            System.out.println("Erreur de connexion : " + e.getMessage());
            // Optionnel : on peut aussi jeter une exception pour stopper le programme
            // throw new RuntimeException("Impossible de se connecter à la base", e);
        }
        return cn;
    }
}