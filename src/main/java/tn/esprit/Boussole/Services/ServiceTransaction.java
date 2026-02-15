package tn.esprit.Boussole.Services;

import tn.esprit.Boussole.Models.transaction;
import tn.esprit.Boussole.Models.transaction.Type;
import tn.esprit.Boussole.Utilis.MyBDConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTransaction implements CRUD<transaction> {

    private final Connection cnx;

    public ServiceTransaction() {
        this.cnx = MyBDConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(transaction t) {
        String sql = "INSERT INTO transaction (date, montant, type, description, franchise_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (t.getDate() != null) {
                ps.setDate(1, new java.sql.Date(t.getDate().getTime()));
            } else {
                ps.setNull(1, Types.DATE);
            }
            ps.setDouble(2, t.getMontant());
            if (t.getType() != null) {
                ps.setString(3, t.getType().name());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, t.getDescription());
            ps.setInt(5, t.getFranchiseId());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    t.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void updateOne(transaction t) {
        String sql = "UPDATE transaction SET date = ?, montant = ?, type = ?, description = ?, franchise_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (t.getDate() != null) {
                ps.setDate(1, new java.sql.Date(t.getDate().getTime()));
            } else {
                ps.setNull(1, Types.DATE);
            }
            ps.setDouble(2, t.getMontant());
            if (t.getType() != null) {
                ps.setString(3, t.getType().name());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, t.getDescription());
            ps.setInt(5, t.getFranchiseId());
            ps.setInt(6, t.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void deleteOne(transaction t) {
        String sql = "DELETE FROM transaction WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, t.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<transaction> selectAll() {
        List<transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transaction";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                transaction t = mapRowToTransaction(rs);
                list.add(t);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<transaction> getAllByFranchise(int franchiseId) {
        List<transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transaction WHERE franchise_id = ? ORDER BY date DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, franchiseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transaction t = mapRowToTransaction(rs);
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public double calculerSolde(int franchiseId) {
        double solde = 0.0;
        String sqlRecettes = "SELECT SUM(montant) as total FROM transaction WHERE franchise_id = ? AND type = 'RECETTE'";
        String sqlDepenses = "SELECT SUM(montant) as total FROM transaction WHERE franchise_id = ? AND type = 'DEPENSE'";
        try (PreparedStatement psRec = cnx.prepareStatement(sqlRecettes);
             PreparedStatement psDep = cnx.prepareStatement(sqlDepenses)) {

            psRec.setInt(1, franchiseId);
            psDep.setInt(1, franchiseId);

            double totalRecettes = 0.0;
            try (ResultSet rs = psRec.executeQuery()) {
                if (rs.next()) {
                    totalRecettes = rs.getDouble("total");
                }
            }

            double totalDepenses = 0.0;
            try (ResultSet rs = psDep.executeQuery()) {
                if (rs.next()) {
                    totalDepenses = rs.getDouble("total");
                }
            }

            solde = totalRecettes - totalDepenses;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return solde;
    }

    public double getTotalRevenus() {
        String sql = "SELECT COALESCE(SUM(montant), 0.0) as total FROM transaction WHERE type = 'RECETTE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0.0;
    }

    public double getTotalDepenses() {
        String sql = "SELECT COALESCE(SUM(montant), 0.0) as total FROM transaction WHERE type = 'DEPENSE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0.0;
    }

    public double getSoldeTotalReseau() {
        double revenus = getTotalRevenus();
        double depenses = getTotalDepenses();
        return revenus - depenses;
    }

    private transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Date date = rs.getDate("date");
        double montant = rs.getDouble("montant");
        String typeStr = rs.getString("type");
        Type type = null;
        if (typeStr != null && !typeStr.isEmpty()) {
            try {
                type = Type.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                // Unknown type in DB, leave null or handle default
                System.out.println("Unknown transaction type: " + typeStr);
            }
        }
        String description = rs.getString("description");
        int franchiseId = rs.getInt("franchise_id");

        return new transaction(id, date, montant, type, description, franchiseId);
    }
}
