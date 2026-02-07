package com.boussole.app.services;

import com.boussole.app.models.AlerteIA;
import com.boussole.app.utils.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AlerteIAService {
  private Connection connection;

  public AlerteIAService() {
    this.connection = Database.getInstance().getConnection();
  }

  public boolean add(AlerteIA alerteIA) {
    String sql = "INSERT INTO alerteIAs (type_alerte, message, score_gravite, franchise_id) VALUES (?, ?, ?, ?)";
    try {
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setString(1, alerteIA.getType_alerte());
      preparedStatement.setString(2, alerteIA.getMessage());
      preparedStatement.setFloat(3, alerteIA.getScore_gravite());
      preparedStatement.setFloat(4, alerteIA.getFranchiseId());
      preparedStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to add alerteIA: " + e.getMessage());
    }
    return false;
  }

  public boolean delete(int id) {
    try {
      String sql = "DELETE FROM alerteIAs WHERE id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, id);
      preparedStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to add alerteIA: " + e.getMessage());
    }
    return false;
  }

  public ObservableList<AlerteIA> getByFranchise(int franchiseId) {
    ObservableList<AlerteIA> list = FXCollections.observableArrayList();
    try {
      String sql = "SELECT * FROM alerteIAs WHERE franchise_id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, franchiseId);
      ResultSet rs = preparedStatement.executeQuery();
      while (rs.next()) {
        AlerteIA alerteIA = new AlerteIA();
        alerteIA.setId(rs.getInt("id"));
        alerteIA.setType_alerte(rs.getString("type_alerte"));
        alerteIA.setScore_gravite(rs.getFloat("score_gravite"));
        alerteIA.setDate_detection(rs.getDate("date_detection"));
        alerteIA.setMessage(rs.getString("message"));
        alerteIA.setFranchiseId(franchiseId);
        list.add(alerteIA);
      }
    } catch (SQLException e) {
      System.err.println("Failed to display reclamations: " + e.getMessage());
    }
    return list;
  }

  public ObservableList<AlerteIA> getAll() {
    ObservableList<AlerteIA> list = FXCollections.observableArrayList();
    try {
      String sql = "SELECT * FROM alerteIAs";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      ResultSet rs = preparedStatement.executeQuery();
      while (rs.next()) {
        AlerteIA alerteIA = new AlerteIA();
        alerteIA.setId(rs.getInt("id"));
        alerteIA.setType_alerte(rs.getString("type_alerte"));
        alerteIA.setScore_gravite(rs.getFloat("score_gravite"));
        alerteIA.setDate_detection(rs.getDate("date_detection"));
        alerteIA.setMessage(rs.getString("message"));
        alerteIA.setFranchiseId(rs.getInt("franchise_id"));
        list.add(alerteIA);
      }
    } catch (SQLException e) {
      System.err.println("Failed to display reclamations: " + e.getMessage());
    }
    return list;
  }
}
