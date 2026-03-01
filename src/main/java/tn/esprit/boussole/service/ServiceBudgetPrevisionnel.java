package tn.esprit.boussole.service;

import tn.esprit.boussole.models.budget_previsionnel;
import tn.esprit.boussole.models.budget_previsionnel.TypeBudget;
import tn.esprit.boussole.utils.MyBdConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceBudgetPrevisionnel {

    private final Connection cnx;

    public ServiceBudgetPrevisionnel() {
        this.cnx = MyBdConnexion.getinstance().getCnx();
    }

    // add: insert or update montantCible if budget already exists for mois/annee/categorie/franchise
    public void add(budget_previsionnel b) {
        try {
            budget_previsionnel existing = getBudgetActuel(b.getFranchiseId(), b.getMois(), b.getAnnee(), b.getCategorie());
            if (existing != null) {
                // update montantCible of existing
                existing.setMontantCible(b.getMontantCible());
                updateone(existing);
            } else {
                insertone(b);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertone(budget_previsionnel b) {
        String sql = "INSERT INTO budget_previsionnel (mois, annee, montant_cible, type_budget, categorie, franchise_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getMois());
            ps.setInt(2, b.getAnnee());
            ps.setDouble(3, b.getMontantCible());
            ps.setString(4, b.getType_budget() != null ? b.getType_budget().name() : null);
            ps.setString(5, b.getCategorie());
            ps.setInt(6, b.getFranchiseId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    b.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void updateone(budget_previsionnel b) {
        String sql = "UPDATE budget_previsionnel SET mois = ?, annee = ?, montant_cible = ?, type_budget = ?, categorie = ?, franchise_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, b.getMois());
            ps.setInt(2, b.getAnnee());
            ps.setDouble(3, b.getMontantCible());
            ps.setString(4, b.getType_budget() != null ? b.getType_budget().name() : null);
            ps.setString(5, b.getCategorie());
            ps.setInt(6, b.getFranchiseId());
            ps.setInt(7, b.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteone(budget_previsionnel b) {
        String sql = "DELETE FROM budget_previsionnel WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, b.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<budget_previsionnel> getTousBudgets() {
        List<budget_previsionnel> list = new ArrayList<>();
        String sql = "SELECT * FROM budget_previsionnel";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToBudget(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<budget_previsionnel> getAllByFranchise(int franchiseId) {
        List<budget_previsionnel> list = new ArrayList<>();
        String sql = "SELECT * FROM budget_previsionnel WHERE franchise_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, franchiseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBudget(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public budget_previsionnel getBudgetActuel(int franchiseId, int mois, int annee, String categorie) {
        String sql = "SELECT * FROM budget_previsionnel WHERE franchise_id = ? AND mois = ? AND annee = ? AND categorie = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, franchiseId);
            ps.setInt(2, mois);
            ps.setInt(3, annee);
            ps.setString(4, categorie);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBudget(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private budget_previsionnel mapRowToBudget(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int mois = rs.getInt("mois");
        int annee = rs.getInt("annee");
        double montant = rs.getDouble("montant_cible");
        String typeStr = rs.getString("type_budget");
        TypeBudget type = null;
        if (typeStr != null && !typeStr.isEmpty()) {
            try {
                type = TypeBudget.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Unknown TypeBudget: " + typeStr);
            }
        }
        String categorie = rs.getString("categorie");
        int franchiseId = rs.getInt("franchise_id");

        return new budget_previsionnel(id, mois, annee, montant, type, categorie, franchiseId);
    }
}
