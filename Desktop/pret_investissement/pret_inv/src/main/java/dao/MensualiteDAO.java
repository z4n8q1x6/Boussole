package dao;

import entity.Mensualite;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensualiteDAO {

    // CREATE
    public void insert(Mensualite m, int pretId) throws SQLException {
        String sql = "INSERT INTO mensualite (date_echeance, montant, est_paye, pret_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDate(1, new java.sql.Date(m.getDateEcheance().getTime()));
            stmt.setDouble(2, m.getMontant());
            stmt.setBoolean(3, m.isEstPaye());
            stmt.setInt(4, pretId);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) m.setId(rs.getInt(1));
            }
        }
    }

    // READ
    public List<Mensualite> getByPret(int pretId) throws SQLException {
        List<Mensualite> list = new ArrayList<>();
        String sql = "SELECT * FROM mensualite WHERE pret_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, pretId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMensualite(rs));
                }
            }
        }
        return list;
    }

    // UPDATE
    public void update(Mensualite m) throws SQLException {
        String sql = "UPDATE mensualite SET date_echeance=?, montant=?, est_paye=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, new java.sql.Date(m.getDateEcheance().getTime()));
            stmt.setDouble(2, m.getMontant());
            stmt.setBoolean(3, m.isEstPaye());
            stmt.setInt(4, m.getId());

            stmt.executeUpdate();
        }
    }

    // DELETE (Unique)
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM mensualite WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * AJOUT INDISPENSABLE : Suppression en cascade manuelle
     * Pour supprimer toutes les mensualités avant de supprimer le prêt parent.
     */
    public void deleteByPretId(int pretId) throws SQLException {
        String sql = "DELETE FROM mensualite WHERE pret_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pretId);
            stmt.executeUpdate();
        }
    }

    /**
     * Méthode utilitaire de mapping
     */
    private Mensualite mapResultSetToMensualite(ResultSet rs) throws SQLException {
        Mensualite m = new Mensualite();
        m.setId(rs.getInt("id"));
        m.setDateEcheance(rs.getDate("date_echeance"));
        m.setMontant(rs.getDouble("montant"));
        m.setEstPaye(rs.getBoolean("est_paye"));
        // Note: pret_id n'est pas forcément nécessaire dans l'objet entity
        // s'il n'est utilisé que pour la requête SQL
        return m;
    }
}