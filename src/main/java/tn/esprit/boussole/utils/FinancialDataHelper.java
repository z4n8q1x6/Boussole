package tn.esprit.boussole.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tn.esprit.boussole.models.Charge;

public class FinancialDataHelper {

  public static class FinancialData {
    public double totalRecettes;
    public double totalChargesExploitation;
    public double totalChargesFinanciere;
    public double totalChargesExceptionnelle;
    public double resultatNet;
    public double soldeActuel;
    public int pendingChargesCount;
    public int rejectedChargesCount;
    public int transactionCount;

    public FinancialData(double totalRecettes, double totalChargesExploitation,
        double totalChargesFinanciere, double totalChargesExceptionnelle, double resultatNet,
        double soldeActuel, int pendingChargesCount, int rejectedChargesCount,
        int transactionCount) {
      this.totalRecettes = totalRecettes;
      this.totalChargesExploitation = totalChargesExploitation;
      this.totalChargesFinanciere = totalChargesFinanciere;
      this.totalChargesExceptionnelle = totalChargesExceptionnelle;
      this.resultatNet = resultatNet;
      this.soldeActuel = soldeActuel;
      this.pendingChargesCount = pendingChargesCount;
      this.rejectedChargesCount = rejectedChargesCount;
      this.transactionCount = transactionCount;
    }
  }

  public static FinancialData getFinancialData(int franchiseId, int month, int year) {
    try {
      Connection cnx = MyBdConnexion.getinstance().getCnx();

      double totalRecettes = getMonthlyTotal(cnx, month, year, franchiseId, "RECETTE");
      double totalChargesExploitation =
          getChargesByType(cnx, franchiseId, month, year, Charge.TypeCharge.CHARGES_EXPLOITATIONS);
      double totalChargesFinanciere =
          getChargesByType(cnx, franchiseId, month, year, Charge.TypeCharge.CHARGES_FINANCIERES);
      double totalChargesExceptionnelle =
          getChargesByType(cnx, franchiseId, month, year, Charge.TypeCharge.CHARGES_EXCEPTIONNELLES);

      double resultatNet = totalRecettes - (totalChargesExploitation + totalChargesFinanciere + totalChargesExceptionnelle);
      double soldeActuel = getAccountBalance(cnx, franchiseId);
      int pendingChargesCount = getChargesByStatus(cnx, franchiseId, Charge.StatusValidation.EN_ATTENTE);
      int rejectedChargesCount = getChargesByStatus(cnx, franchiseId, Charge.StatusValidation.REJETTE);
      int transactionCount = getMonthlyTransactionCount(cnx, month, year, franchiseId);

      return new FinancialData(totalRecettes, totalChargesExploitation, totalChargesFinanciere,
          totalChargesExceptionnelle, resultatNet, soldeActuel, pendingChargesCount,
          rejectedChargesCount, transactionCount);
    } catch (SQLException e) {
      System.err.println("Database error: " + e.getMessage());
      return null;
    }
  }

  private static double getMonthlyTotal(Connection cnx, int month, int year, int franchiseId, String type) throws SQLException {
    String sql = "SELECT COALESCE(SUM(montant), 0.0) AS total FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ? AND type = ? AND franchise_id = ?";
    try (PreparedStatement ps = cnx.prepareStatement(sql)) {
      ps.setInt(1, month);
      ps.setInt(2, year);
      ps.setString(3, type);
      ps.setInt(4, franchiseId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getDouble("total");
      }
    }
    return 0.0;
  }

  private static double getChargesByType(Connection cnx, int franchiseId, int month, int year, Charge.TypeCharge type) throws SQLException {
    String sql = "SELECT COALESCE(SUM(montant), 0.0) AS total FROM charge WHERE franchise_id = ? AND type = ? AND MONTH(date_charge) = ? AND YEAR(date_charge) = ? AND status_validation = 'VALIDE'";
    try (PreparedStatement ps = cnx.prepareStatement(sql)) {
      ps.setInt(1, franchiseId);
      ps.setString(2, type.name());
      ps.setInt(3, month);
      ps.setInt(4, year);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getDouble("total");
      }
    }
    return 0.0;
  }

  private static double getAccountBalance(Connection cnx, int franchiseId) throws SQLException {
    String sql = "SELECT solde_actuel FROM franchises WHERE id = ?";
    try (PreparedStatement ps = cnx.prepareStatement(sql)) {
      ps.setInt(1, franchiseId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getDouble("solde_actuel");
      }
    }
    return 0.0;
  }

  private static int getChargesByStatus(Connection cnx, int franchiseId, Charge.StatusValidation status) throws SQLException {
    String sql = "SELECT COUNT(*) AS count FROM charge WHERE franchise_id = ? AND status_validation = ?";
    try (PreparedStatement ps = cnx.prepareStatement(sql)) {
      ps.setInt(1, franchiseId);
      ps.setString(2, status.name());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt("count");
      }
    }
    return 0;
  }

  private static int getMonthlyTransactionCount(Connection cnx, int month, int year, int franchiseId) throws SQLException {
    String sql = "SELECT COUNT(*) AS count FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ? AND franchise_id = ?";
    try (PreparedStatement ps = cnx.prepareStatement(sql)) {
      ps.setInt(1, month);
      ps.setInt(2, year);
      ps.setInt(3, franchiseId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt("count");
      }
    }
    return 0;
  }
}
