package tn.esprit.boussole.service;

import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.utils.MyBdConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChargeService implements crud<Charge> {

    private Connection cnx;

    public ChargeService() {
        cnx = MyBdConnexion.getinstance().getCnx();
    }

    @Override
    public void insertone(Charge charge) throws SQLException {
        String req = "INSERT INTO `charge` (`titre`, `montant`, `date_charge`, `type`, `preuve_image`, `status_validation`, `franchise_id`) VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, charge.getTitre());
        ps.setDouble(2, charge.getMontant());
        ps.setDate(3, Date.valueOf(charge.getDateCharge()));
        ps.setString(4, charge.getType().name());
        ps.setString(5, charge.getPreuveImage());
        ps.setString(6, charge.getStatusValidation().name());
        ps.setInt(7, charge.getFranchiseId());

        ps.executeUpdate();
        System.out.println("Charge ajoutée avec succès !");
    }

    @Override
    public void updateone(Charge charge) throws SQLException {
        String req = "UPDATE `charge` SET `titre`=?, `montant`=?, `type`=?, `status_validation`=?, `franchise_id`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, charge.getTitre());
        ps.setDouble(2, charge.getMontant());
        ps.setString(3, charge.getType().name());
        ps.setString(4, charge.getStatusValidation().name());
        ps.setInt(5, charge.getFranchiseId());
        ps.setInt(6, charge.getId());

        ps.executeUpdate();
        System.out.println("Charge mise à jour !");
    }

    @Override
    public void deleteone(Charge charge) throws SQLException {
        String req = "DELETE FROM `charge` WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, charge.getId());
        ps.executeUpdate();
        System.out.println("Charge supprimée !");
    }

    @Override
    public List<Charge> selectAll(Charge charge) throws SQLException {
        List<Charge> charges = new ArrayList<>();
        // Jointure pour récupérer le nom de la franchise
        String req = "SELECT c.*, f.nom as franchise_nom FROM `charge` c JOIN `franchises` f ON c.franchise_id = f.id";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            charge = new Charge();
            charge.setId(rs.getInt("id"));
            charge.setTitre(rs.getString("titre"));
            charge.setMontant(rs.getDouble("montant"));
            charge.setDateCharge(rs.getDate("date_charge").toLocalDate());
            charge.setType(Charge.TypeCharge.valueOf(rs.getString("type")));
            charge.setStatusValidation(Charge.StatusValidation.valueOf(rs.getString("status_validation")));
            charge.setPreuveImage(rs.getString("preuve_image"));
            charge.setFranchiseId(rs.getInt("franchise_id"));
            charge.setFranchiseName(rs.getString("franchise_nom")); // Récupération du nom

            charges.add(charge);
        }
        return charges;
    }

    @Override
    public List<Charge> selectAll() {
        return List.of();
    }

    // Nouvelle méthode pour trouver l'ID par le nom
    public int getFranchiseIdByName(String name) throws SQLException {
        String req = "SELECT id FROM franchises WHERE nom = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        // Retourne -1 ou lève une exception si non trouvé
        return -1; 
    }
    
    // Nouvelle méthode pour trouver le nom par l'ID
    public String getFranchiseNameById(int id) throws SQLException {
        String req = "SELECT nom FROM franchises WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("nom");
        }
        return "Inconnu";
    }
}
