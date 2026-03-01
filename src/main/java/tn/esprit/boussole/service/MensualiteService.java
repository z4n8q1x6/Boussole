package tn.esprit.boussole.service;

import tn.esprit.boussole.models.Mensualite;
import tn.esprit.boussole.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensualiteService implements crud<Mensualite> {

    private Connection cnx;

    public MensualiteService() {
        this.cnx = DBConnection.getConnection();
    }

    /**
     * CORRECTION : Nom de méthode harmonisé pour correspondre à l'appel dans PretService
     */
    public List<Mensualite> getMensualitesByPret(int pretId) {
        List<Mensualite> list = new ArrayList<>();
        String sql = "SELECT * FROM mensualite WHERE pret_id = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, pretId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToMensualite(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getMensualitesByPret: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void insertone(Mensualite m) throws SQLException {
        String sql = "INSERT INTO mensualite (date_echeance, montant, est_paye) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (m.getDateEcheance() != null) {
                stmt.setDate(1, new java.sql.Date(m.getDateEcheance().getTime()));
            } else {
                stmt.setNull(1, Types.DATE);
            }
            stmt.setDouble(2, m.getMontant());
            stmt.setBoolean(3, m.isEstPaye());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) m.setId(rs.getInt(1));
            }
        }
    }

    public void insertWithPretId(Mensualite m, int pretId) throws SQLException {
        String sql = "INSERT INTO mensualite (date_echeance, montant, est_paye, pret_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (m.getDateEcheance() != null) {
                stmt.setDate(1, new java.sql.Date(m.getDateEcheance().getTime()));
            } else {
                stmt.setNull(1, Types.DATE);
            }
            stmt.setDouble(2, m.getMontant());
            stmt.setBoolean(3, m.isEstPaye());
            stmt.setInt(4, pretId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) m.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public void updateone(Mensualite m) throws SQLException {
        String sql = "UPDATE mensualite SET date_echeance=?, montant=?, est_paye=? WHERE id=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            if (m.getDateEcheance() != null) {
                stmt.setDate(1, new java.sql.Date(m.getDateEcheance().getTime()));
            } else {
                stmt.setNull(1, Types.DATE);
            }
            stmt.setDouble(2, m.getMontant());
            stmt.setBoolean(3, m.isEstPaye());
            stmt.setInt(4, m.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteone(Mensualite m) throws SQLException {
        String sql = "DELETE FROM mensualite WHERE id=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, m.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Mensualite> selectAll(Mensualite t) throws SQLException {
        return selectAll();
    }

    @Override
    public List<Mensualite> selectAll() {
        List<Mensualite> list = new ArrayList<>();
        String sql = "SELECT * FROM mensualite";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToMensualite(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du selectAll: " + e.getMessage());
        }
        return list;
    }

    public void deleteByPretId(int pretId) throws SQLException {
        String sql = "DELETE FROM mensualite WHERE pret_id=?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setInt(1, pretId);
            stmt.executeUpdate();
        }
    }

    private Mensualite mapResultSetToMensualite(ResultSet rs) throws SQLException {
        Mensualite m = new Mensualite();
        m.setId(rs.getInt("id"));
        m.setDateEcheance(rs.getDate("date_echeance"));
        m.setMontant(rs.getDouble("montant"));
        m.setEstPaye(rs.getBoolean("est_paye"));
        return m;
    }
}