/*package tn.esprit.boussole.service;

import models.Pret;
import models.StatutPret;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PretDAO {

    // CREATE : Déjà correct, rien à changer.
    public void insert(Pret pret) throws SQLException {
        String sql = "INSERT INTO pret (montant_demande, duree_mois, taux, statut, motif) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDouble(1, pret.getMontantDemande());
            stmt.setInt(2, pret.getDureeMois());
            stmt.setFloat(3, pret.getTaux());
            stmt.setString(4, pret.getStatut().name());
            stmt.setString(5, pret.getMotif());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) pret.setId(rs.getInt(1));
            }
        }
    }

    // READ : Déjà correct.
    public List<Pret> getAll() throws SQLException {
        List<Pret> list = new ArrayList<>();
        String sql = "SELECT * FROM pret";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToPret(rs));
            }
        }
        return list;
    }

    // UPDATE : Déjà correct.
    public void update(Pret pret) throws SQLException {
        String sql = "UPDATE pret SET montant_demande=?, duree_mois=?, taux=?, statut=?, motif=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, pret.getMontantDemande());
            stmt.setInt(2, pret.getDureeMois());
            stmt.setFloat(3, pret.getTaux());
            stmt.setString(4, pret.getStatut().name());
            stmt.setString(5, pret.getMotif());
            stmt.setInt(6, pret.getId());

            stmt.executeUpdate();
        }
    }

    // DELETE : Déjà correct.
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM pret WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * MODIFICATION MAJEURE : Recherche sécurisée par PreparedStatement
     */
    /*public List<Pret> search(Double montantMin, Double montantMax, StatutPret statut, String motif) throws SQLException {
        List<Pret> list = new ArrayList<>();

        // Construction de la requête dynamique
        String sql = "SELECT * FROM pret WHERE 1=1";
        if (montantMin != null) sql += " AND montant_demande >= ?";
        if (montantMax != null) sql += " AND montant_demande <= ?";
        if (statut != null) sql += " AND statut = ?";
        if (motif != null && !motif.isEmpty()) sql += " AND motif LIKE ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int idx = 1;
            if (montantMin != null) stmt.setDouble(idx++, montantMin);
            if (montantMax != null) stmt.setDouble(idx++, montantMax);
            if (statut != null) stmt.setString(idx++, statut.name());
            if (motif != null && !motif.isEmpty()) stmt.setString(idx++, "%" + motif + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPret(rs));
                }
            }
        }
        return list;
    }
*/
    /**
     * Petite astuce : Méthode utilitaire pour éviter la répétition du code de mapping
     */
   /* private Pret mapResultSetToPret(ResultSet rs) throws SQLException {
        Pret p = new Pret();
        p.setId(rs.getInt("id"));
        p.setMontantDemande(rs.getDouble("montant_demande"));
        p.setDureeMois(rs.getInt("duree_mois"));
        p.setTaux(rs.getFloat("taux"));
        p.setStatut(StatutPret.valueOf(rs.getString("statut")));
        p.setMotif(rs.getString("motif"));
        return p;
    }
}*/