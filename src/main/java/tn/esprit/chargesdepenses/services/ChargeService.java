package tn.esprit.chargesdepenses.services;

import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.models.enums.StatusValidation;
import tn.esprit.chargesdepenses.models.enums.TypeCharge;
import tn.esprit.chargesdepenses.utils.MyBDConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChargeService implements CRUD<Charge> {

    private Connection cnx;

    public ChargeService() {
        cnx = MyBDConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Charge charge) throws SQLException {
        // Ajout des colonnes obligatoires selon ton SQL
        String req = "INSERT INTO `charge` (`titre`, `montant`, `date_charge`, `type`, `preuve_image`, `status_validation`, `franchise_id`) VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, charge.getTitre());
        ps.setDouble(2, charge.getMontant());
        ps.setDate(3, Date.valueOf(charge.getDateCharge())); // Conversion LocalDate -> sql.Date
        ps.setString(4, charge.getType().name());
        ps.setString(5, charge.getPreuveImage());
        ps.setString(6, charge.getStatusValidation().name());
        ps.setInt(7, charge.getFranchiseId());

        ps.executeUpdate();
        System.out.println("Charge ajoutée avec succès !");
    }

    @Override
    public void updateOne(Charge charge) throws SQLException {
        String req = "UPDATE `charge` SET `titre`=?, `montant`=?, `type`=?, `status_validation`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, charge.getTitre());
        ps.setDouble(2, charge.getMontant());
        ps.setString(3, charge.getType().name());
        ps.setString(4, charge.getStatusValidation().name());
        ps.setInt(5, charge.getId());

        ps.executeUpdate();
        System.out.println("Charge mise à jour !");
    }

    @Override
    public void deleteOne(Charge charge) throws SQLException {
        String req = "DELETE FROM `charge` WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, charge.getId());
        ps.executeUpdate();
        System.out.println("Charge supprimée !");
    }

    @Override
    public List<Charge> selectAll() throws SQLException {
        List<Charge> charges = new ArrayList<>();
        String req = "SELECT * FROM `charge`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Charge charge = new Charge();
            charge.setId(rs.getInt("id"));
            charge.setTitre(rs.getString("titre"));
            charge.setMontant(rs.getDouble("montant"));
            charge.setDateCharge(rs.getDate("date_charge").toLocalDate()); // sql.Date -> LocalDate

            // Conversion String (DB) vers Enum (Java)
            charge.setType(TypeCharge.valueOf(rs.getString("type")));
            charge.setStatusValidation(StatusValidation.valueOf(rs.getString("status_validation")));

            charge.setPreuveImage(rs.getString("preuve_image"));
            charge.setFranchiseId(rs.getInt("franchise_id"));

            charges.add(charge);
        }
        return charges;
    }
}

