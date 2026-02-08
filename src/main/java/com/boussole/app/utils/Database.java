package com.boussole.app.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
  private static final String URL = Config.get("DB_URL");
  private static final String USER = Config.get("DB_USER");
  private static final String PASSWORD = Config.get("DB_PASSWORD");
  private Connection connection;
  private static final Database instance = new Database();

  private Database() {
    try {
      connection = DriverManager.getConnection(URL, USER, PASSWORD);
      System.out.println("Connected to database successfully!");
    } catch (SQLException e) {
      System.err.println("Database connection failed: " + e.getMessage());
    }
  }

  public static Database getInstance() {
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
