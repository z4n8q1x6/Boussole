package com.boussole.app.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
  private static final String URL = "jdbc:mysql://localhost:3306/boussole";
  private static final String USER = "boussole_user";
  private static final String PASSWORD = "2121";
  private Connection connection;
  private static Database instance;

  private Database() {
    try {
      connection = DriverManager.getConnection(URL, USER, PASSWORD);
      System.out.println("Connected to database successfully!");
    } catch (SQLException e) {
      System.err.println("Database connection failed: " + e.getMessage());
    }
  }

  public static Database getInstance() {
    if (instance == null) {
      instance = new Database();
    }
    return instance;
  }

  public Connection getConnection() {
    return connection;
  }

  public void closeConnection() {
    try {
      connection.close();
    } catch (SQLException e) {
      System.err.println("Database closure failed: " + e.getMessage());
    }
  }
}
