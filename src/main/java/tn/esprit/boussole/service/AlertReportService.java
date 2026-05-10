package tn.esprit.boussole.service;

import java.sql.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.boussole.models.AlertReport;
import tn.esprit.boussole.utils.MyBdConnexion;

public class AlertReportService {
  private Connection connection;

  public AlertReportService() {
    this.connection = MyBdConnexion.getinstance().getCnx();
  }

  public boolean add(AlertReport report) {
    String sql = "INSERT INTO alert_report (url, generated_at, alert_count) VALUES (?, ?, ?)";
    try {
      PreparedStatement ps = connection.prepareStatement(sql);
      ps.setString(1, report.getUrl());
      ps.setTimestamp(2, Timestamp.valueOf(report.getGeneratedAt()));
      ps.setInt(3, report.getAlertCount());
      ps.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to add alert report: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public ObservableList<AlertReport> getAll() {
    ObservableList<AlertReport> list = FXCollections.observableArrayList();
    String sql =
        "SELECT id, url, generated_at, alert_count FROM alert_report ORDER BY generated_at DESC";
    try {
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        AlertReport report = new AlertReport();
        report.setId(rs.getLong("id"));
        report.setUrl(rs.getString("url"));
        Timestamp timestamp = rs.getTimestamp("generated_at");
        report.setGeneratedAt(timestamp.toLocalDateTime());
        report.setAlertCount(rs.getInt("alert_count"));
        list.add(report);
      }
    } catch (SQLException e) {
      System.err.println("Failed to fetch alert reports: " + e.getMessage());
      e.printStackTrace();
    }
    return list;
  }

  public boolean delete(Long id) {
    String sql = "DELETE FROM alert_report WHERE id = ?";
    try {
      PreparedStatement ps = connection.prepareStatement(sql);
      ps.setLong(1, id);
      ps.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to delete alert report: " + e.getMessage());
      return false;
    }
  }

  public boolean deleteAll() {
    String sql = "DELETE FROM alert_report";
    try {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate(sql);
      return true;
    } catch (SQLException e) {
      System.err.println("Failed to delete all reports: " + e.getMessage());
      return false;
    }
  }
}
