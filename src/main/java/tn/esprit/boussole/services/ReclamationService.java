package tn.esprit.boussole.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.boussole.models.Reclamation;
import tn.esprit.boussole.models.StatutReclamation;
import tn.esprit.boussole.utils.Database;

public class ReclamationService {
  private Connection connection;

  public ReclamationService() {
    this.connection = Database.getInstance().getConnection();
  }

  public boolean add(Reclamation reclamation) {
    String sql = "INSERT INTO reclamations (sujet, description, franchise_id) VALUES (?, ?, ?)";
    try {
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setString(1, reclamation.getSujet());
      preparedStatement.setString(2, reclamation.getDescription());
      preparedStatement.setInt(3, reclamation.getFranchiseId());
      preparedStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to add reclamation: " + e.getMessage());
    }
    return false;
  }

  public boolean delete(int id) {
    try {
      String sql = "DELETE FROM reclamations WHERE id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, id);
      preparedStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to delete reclamation: " + e.getMessage());
    }
    return false;
  }

  public boolean updateStatus(int id, String status) {
    try {
      String sql = "UPDATE reclamations SET statut = ? WHERE id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setString(1, status);
      preparedStatement.setInt(2, id);
      preparedStatement.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to update reclamation status: " + e.getMessage());
    }
    return false;
  }

  public ObservableList<Reclamation> getByFranchise(int franchiseId) {
    ObservableList<Reclamation> list = FXCollections.observableArrayList();
    try {
      String sql = "SELECT * FROM reclamations WHERE franchise_id = ?";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setInt(1, franchiseId);
      ResultSet rs = preparedStatement.executeQuery();
      while (rs.next()) {
        Reclamation reclamation = new Reclamation();
        reclamation.setId(rs.getInt("id"));
        reclamation.setSujet(rs.getString("sujet"));
        reclamation.setDescription(rs.getString("description"));
        reclamation.setStatut(StatutReclamation.valueOf(rs.getString("statut").toUpperCase()));
        reclamation.setDateCreation(rs.getDate("date_creation"));
        reclamation.setFranchiseId(franchiseId);
        list.add(reclamation);
      }
    } catch (SQLException e) {
      System.err.println("Failed to display reclamations: " + e.getMessage());
    }
    return list;
  }

  public ObservableList<Reclamation> getAll() {
    ObservableList<Reclamation> list = FXCollections.observableArrayList();
    try {
      String sql = "SELECT * FROM reclamations";
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      ResultSet rs = preparedStatement.executeQuery();
      while (rs.next()) {
        Reclamation reclamation = new Reclamation();
        reclamation.setId(rs.getInt("id"));
        reclamation.setSujet(rs.getString("sujet"));
        reclamation.setDescription(rs.getString("description"));
        reclamation.setStatut(StatutReclamation.valueOf(rs.getString("statut").toUpperCase()));
        reclamation.setDateCreation(rs.getDate("date_creation"));
        reclamation.setFranchiseId(rs.getInt("franchise_id"));
        list.add(reclamation);
      }
    } catch (SQLException e) {
      System.err.println("Failed to display reclamations: " + e.getMessage());
    }
    return list;
  }
}
