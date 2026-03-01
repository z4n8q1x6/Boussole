package tn.esprit.boussole.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBdConnexion {
    // Identifiants de la base de données
    private static final String USER     = "root"; // Ou "boussole_user" selon votre config
    private static final String PASSWORD = "";     // Ou "2121"
    private static final String URL      = "jdbc:mysql://localhost:3306/boussole";

    private static MyBdConnexion instance;
    private Connection cnx;

    /**
     * Singleton : Constructeur privé pour empêcher l'instanciation multiple
     */
    private MyBdConnexion() {
        connect();
    }

    /**
     * Établit la connexion initiale
     */
    private void connect() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ MyBdConnexion: CONNECTION OK");
        } catch (SQLException e) {
            System.err.println("❌ MyBdConnexion: " + e.getMessage());
        }
    }

    /**
     * Retourne l'instance unique de la classe (Design Pattern Singleton)
     */
    public static MyBdConnexion getinstance() {
        if (instance == null) {
            instance = new MyBdConnexion();
        }
        return instance;
    }

    /**
     * Retourne l'objet Connection.
     * Vérifie si la connexion est fermée ou expirée avant de la fournir.
     */
    public Connection getCnx() {
        try {
            // Vérifie si la connexion est nulle, fermée ou n'est plus valide (timeout de 2s)
            if (cnx == null || cnx.isClosed() || !cnx.isValid(2)) {
                System.out.println("🔄 MyBdConnexion: reconnexion...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("❌ MyBdConnexion.getCnx: " + e.getMessage());
            connect();
        }
        return cnx;
    }
}