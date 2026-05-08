package tn.esprit.boussole.service;

import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.utils.MyBdConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChargeService implements crud<Charge> {

    private Connection cnx;

    /** Connexion fraîche à chaque appel pour éviter les connexions obsolètes */
    private Connection getConn() {
        return MyBdConnexion.getinstance().getCnx();
    }

    public ChargeService() {
        cnx = MyBdConnexion.getinstance().getCnx();
    }

    @Override
    public void insertone(Charge charge) throws SQLException {
        String req = "INSERT INTO `charge` (`titre`, `montant`, `date_charge`, `type`, `preuve_image`, `status_validation`, `franchise_id`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = getConn().prepareStatement(req);
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
        PreparedStatement ps = getConn().prepareStatement(req);

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
        PreparedStatement ps = getConn().prepareStatement(req);
        ps.setInt(1, charge.getId());
        ps.executeUpdate();
        System.out.println("Charge supprimée !");
    }

    /**
     * Récupère toutes les charges avec les détails de la franchise associée.
     */
    private Charge.TypeCharge parseType(String typeStr) {
        if (typeStr == null) return Charge.TypeCharge.CHARGES_EXPLOITATIONS;
        String t = typeStr.toUpperCase();
        if (t.contains("EXPLOITATION")) return Charge.TypeCharge.CHARGES_EXPLOITATIONS;
        if (t.contains("FINANCIER")) return Charge.TypeCharge.CHARGES_FINANCIERES;
        if (t.contains("EXCEPTIONNEL")) return Charge.TypeCharge.CHARGES_EXCEPTIONNELLES;
        try { return Charge.TypeCharge.valueOf(t); } 
        catch (IllegalArgumentException e) { return Charge.TypeCharge.CHARGES_EXPLOITATIONS; }
    }

    private Charge.StatusValidation parseStatus(String statusStr) {
        if (statusStr == null) return Charge.StatusValidation.EN_ATTENTE;
        String s = statusStr.toUpperCase().replace("É", "E").replace("È", "E");
        if (s.contains("VALIDE") || s.contains("VALIDÉ")) return Charge.StatusValidation.VALIDE;
        if (s.contains("ATTENTE")) return Charge.StatusValidation.EN_ATTENTE;
        if (s.contains("REJET")) return Charge.StatusValidation.REJETTE;
        try { return Charge.StatusValidation.valueOf(s); } 
        catch (IllegalArgumentException e) { return Charge.StatusValidation.EN_ATTENTE; }
    }

    @Override
    public List<Charge> selectAll(Charge c) throws SQLException {
        List<Charge> charges = new ArrayList<>();
        String req = "SELECT c.*, f.nom as franchise_nom FROM `charge` c JOIN `franchises` f ON c.franchise_id = f.id";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Charge charge = new Charge();
                charge.setId(rs.getInt("id"));
                charge.setTitre(rs.getString("titre"));
                charge.setMontant(rs.getDouble("montant"));
                charge.setDateCharge(rs.getDate("date_charge").toLocalDate());
                charge.setType(parseType(rs.getString("type")));
                charge.setStatusValidation(parseStatus(rs.getString("status_validation")));
                charge.setPreuveImage(rs.getString("preuve_image"));
                charge.setFranchiseId(rs.getInt("franchise_id"));
                charge.setFranchiseName(rs.getString("franchise_nom"));

                charges.add(charge);
            }
        }
        return charges;
    }

    @Override
    public List<Charge> selectAll() {
        try {
            return selectAll(null);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    // --- Méthodes utilitaires pour l'interface UI ---

    public int getFranchiseIdByName(String name) throws SQLException {
        String req = "SELECT id FROM franchises WHERE nom = ?";
        PreparedStatement ps = getConn().prepareStatement(req);
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("id") : -1;
    }

    public String getFranchiseNameById(int id) throws SQLException {
        String req = "SELECT nom FROM franchises WHERE id = ?";
        PreparedStatement ps = getConn().prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getString("nom") : "Inconnu";
    }

    /**
     * Récupère toutes les charges d'une franchise donnée, triées par date décroissante.
     */
    public List<Charge> getChargesByFranchise(int franchiseId) {
        List<Charge> charges = new ArrayList<>();
        String req = "SELECT * FROM `charge` WHERE franchise_id = ? ORDER BY date_charge DESC";
        System.out.println("🔍 getChargesByFranchise franchiseId=" + franchiseId);
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setInt(1, franchiseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Charge charge = new Charge();
                    charge.setId(rs.getInt("id"));
                    charge.setTitre(rs.getString("titre"));
                    charge.setMontant(rs.getDouble("montant"));
                    charge.setDateCharge(rs.getDate("date_charge").toLocalDate());
                    charge.setType(parseType(rs.getString("type")));
                    charge.setStatusValidation(parseStatus(rs.getString("status_validation")));
                    charge.setPreuveImage(rs.getString("preuve_image"));
                    charge.setFranchiseId(rs.getInt("franchise_id"));
                    charges.add(charge);
                }
            }
            System.out.println("📋 Charges trouvées pour franchise " + franchiseId + " : " + charges.size());
        } catch (SQLException e) {
            System.err.println("❌ getChargesByFranchise: " + e.getMessage());
        }
        return charges;
    }

    /**
     * Calcule le total des charges d'une franchise.
     */
    public double getTotalChargesByFranchise(int franchiseId) {
        String req = "SELECT COALESCE(SUM(montant),0) as total FROM `charge` WHERE franchise_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setInt(1, franchiseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ getTotalChargesByFranchise: " + e.getMessage());
        }
        return 0.0;
    }
}