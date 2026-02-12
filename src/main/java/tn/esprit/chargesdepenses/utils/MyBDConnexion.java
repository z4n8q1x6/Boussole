package tn.esprit.chargesdepenses.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBDConnexion {

    private static final String USER = "root";
    private static final String PASSWORD = ""; // Convention : majuscules pour les constantes
    private static final String URL = "jdbc:mysql://localhost:3306/boussole";

    private Connection cnx;
    private static MyBDConnexion instance;

    // CORRIGÉ : Le constructeur DOIT être privé pour empêcher l'instanciation directe
    private MyBDConnexion() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base de données réussie !");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }

    // Méthode pour obtenir l'instance unique
    public static MyBDConnexion getInstance() {
        if (instance == null) {
            instance = new MyBDConnexion();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    // Note : Le setter pour cnx n'est généralement pas nécessaire dans un Singleton
    // car la connexion est gérée en interne.
}
