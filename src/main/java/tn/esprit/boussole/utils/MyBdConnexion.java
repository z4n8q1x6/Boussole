package tn.esprit.boussole.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBdConnexion {
<<<<<<< HEAD
    private static final String USER     = "root";
    private static final String PASSWORD = "";
    private static final String URL      = "jdbc:mysql://localhost:3306/boussole";

    private static MyBdConnexion instance;
    private Connection cnx;

    private MyBdConnexion() {
        connect();
    }

    private void connect() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ MyBdConnexion: CONNECTION OK");
        } catch (SQLException e) {
            System.err.println("❌ MyBdConnexion: " + e.getMessage());
        }
    }

    public static MyBdConnexion getinstance() {
        if (instance == null) instance = new MyBdConnexion();
        return instance;
    }

    public Connection getCnx() {
        try {
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
=======
  private static final String USER = "boussole_user";
  private static final String PASSWORD = "2121";
  private static final String URL = "jdbc:mysql://localhost:3306/boussole";
  private static MyBdConnexion instance;

  private Connection cnx;

  // singleton pour creation d'une seul instance : constructeur privé + cree variable ayant mm type
  // de la class+retourner linstance avec une methode getinstance()
  private MyBdConnexion() {
    try {
      cnx = DriverManager.getConnection(URL, USER, PASSWORD);
      System.out.println("CONNECTION OK!");
    } catch (SQLException e) {
      System.err.println(e.getMessage());
    }
  }

  public static MyBdConnexion getinstance() {
    if (instance == null) instance = new MyBdConnexion();
    return instance;
  }

  public Connection getCnx() {
    return cnx;
  }
>>>>>>> 2118e9cc01de212c47c7cbfda8004c4fa0bea0f9
}
