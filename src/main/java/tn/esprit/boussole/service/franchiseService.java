package tn.esprit.boussole.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.utils.MyBdConnexion;

public class franchiseService implements crud<franchise> {
    private Connection cnx;

    public franchiseService() {
        cnx = MyBdConnexion.getinstance().getCnx();
    }

    @Override
    public void insertone(franchise franchise) throws SQLException {
        String req =
                "INSERT INTO franchises (nom, email, telephone, adresse, actif, date_creation,"
                        + " solde_actuel) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, franchise.getNom());
            ps.setString(2, franchise.getEmail());
            ps.setString(3, franchise.getTelephone());
            ps.setString(4, franchise.getAdresse());
            ps.setBoolean(5, franchise.getActif());
            ps.setTimestamp(
                    6,
                    franchise.getDateCreation() != null
                            ? Timestamp.valueOf(franchise.getDateCreation())
                            : null);
            ps.setDouble(7, franchise.getSoldeActuel());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    franchise.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateone(franchise franchise) throws SQLException {
        String req =
                "UPDATE franchises SET nom=?, email=?, telephone=?, adresse=?, actif=?, solde_actuel=?"
                        + " WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, franchise.getNom());
            ps.setString(2, franchise.getEmail());
            ps.setString(3, franchise.getTelephone());
            ps.setString(4, franchise.getAdresse());
            ps.setBoolean(5, franchise.getActif());
            ps.setDouble(6, franchise.getSoldeActuel());
            ps.setInt(7, franchise.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteone(franchise franchise) throws SQLException {
        String req = "DELETE FROM franchises WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, franchise.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<franchise> selectAll(franchise ignored) throws SQLException {
        List<franchise> list = new ArrayList<>();
        String req = "SELECT * FROM franchises";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                franchise f = new franchise();
                f.setId(rs.getInt("id"));
                f.setNom(rs.getString("nom"));
                f.setEmail(rs.getString("email"));
                f.setTelephone(rs.getString("telephone"));
                f.setAdresse(rs.getString("adresse"));
                f.setActif(rs.getBoolean("actif"));
                f.setSoldeActuel(rs.getDouble("solde_actuel"));
                Timestamp ts = rs.getTimestamp("date_creation");
                if (ts != null) f.setDateCreation(ts.toLocalDateTime());
                list.add(f);
            }
        }
        return list;
    }

    @Override
    public List<franchise> selectAll() {
        try {
            return selectAll(null);
        } catch (SQLException e) {
            System.err.println("Error fetching franchises: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public franchise getById(int id) throws SQLException {
        String req = "SELECT * FROM franchises WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    franchise f = new franchise();
                    f.setId(rs.getInt("id"));
                    f.setNom(rs.getString("nom"));
                    f.setEmail(rs.getString("email"));
                    f.setTelephone(rs.getString("telephone"));
                    f.setAdresse(rs.getString("adresse"));
                    f.setActif(rs.getBoolean("actif"));
                    f.setSoldeActuel(rs.getDouble("solde_actuel"));
                    Timestamp ts = rs.getTimestamp("date_creation");
                    if (ts != null) f.setDateCreation(ts.toLocalDateTime());
                    return f;
                }
            }
        }
        return null;
    }

    public String getNomById(int id) throws SQLException {
        String req = "SELECT nom FROM franchises WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nom");
                }
            }
        }
        return "Unknown Franchise";
    }
}