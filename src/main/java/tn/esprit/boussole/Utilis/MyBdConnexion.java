package tn.esprit.boussole.Utilis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBdConnexion {

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static final String URL = "jdbc:mysql://localhost:3306/boussole";

    private Connection cnx;

    private static MyBdConnexion instance;

    private MyBdConnexion(){

        try {
            cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD );
            System.out.println("CONNECTION SUCCESSFUL");
        } catch (SQLException e) {
            System.err.println( e.getMessage());
        }
    }
    public static MyBdConnexion getinstance(){

        if ( instance == null )
            instance = new MyBdConnexion();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    public boolean hashcode() {
        return false;
    }
}
