package tn.esprit.boussole.service;

import tn.esprit.boussole.models.transaction;
import tn.esprit.boussole.models.transaction.Type;
import tn.esprit.boussole.utils.MyBdConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTransaction implements crud<transaction> {

    /** Toujours récupérer une connexion fraîche (singleton MyBdConnexion) */
    private Connection getConn() throws SQLException {
        Connection c = MyBdConnexion.getinstance().getCnx();
        if (c == null || c.isClosed()) {
            // Forcer une nouvelle instance
            MyBdConnexion.getinstance();
            c = MyBdConnexion.getinstance().getCnx();
        }
        return c;
    }

    public ServiceTransaction() {
        // Teste la connexion au démarrage
        try {
            Connection c = getConn();
            System.out.println("✅ ServiceTransaction: connexion OK - " + c);
        } catch (SQLException e) {
            System.err.println("❌ ServiceTransaction: connexion ÉCHOUÉE - " + e.getMessage());
        }
    }

    @Override
    public void insertone(transaction t) throws SQLException {
        Connection conn = getConn();
        String sql = "INSERT INTO transaction (date, montant, type, description, franchise_id) VALUES (?, ?, ?, ?, ?)";
        System.out.println("🔵 SQL INSERT: franchiseId=" + t.getFranchiseId() + " montant=" + t.getMontant());
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, t.getDate() != null
                    ? new java.sql.Date(t.getDate().getTime())
                    : new java.sql.Date(System.currentTimeMillis()));
            ps.setDouble(2, t.getMontant());
            ps.setString(3, t.getType() != null ? t.getType().name() : "RECETTE");
            ps.setString(4, t.getDescription());
            ps.setInt(5, t.getFranchiseId());

            int rows = ps.executeUpdate();
            System.out.println("✅ insertone: " + rows + " ligne(s) insérée(s)");

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public void updateone(transaction t) throws SQLException {
        Connection conn = getConn();
        String sql = "UPDATE transaction SET date=?, montant=?, type=?, description=?, franchise_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, t.getDate() != null ? new java.sql.Date(t.getDate().getTime()) : null);
            ps.setDouble(2, t.getMontant());
            ps.setString(3, t.getType() != null ? t.getType().name() : null);
            ps.setString(4, t.getDescription());
            ps.setInt(5, t.getFranchiseId());
            ps.setInt(6, t.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteone(transaction t) throws SQLException {
        Connection conn = getConn();
        String sql = "DELETE FROM transaction WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public List<transaction> selectAll(transaction transaction) throws SQLException {
        return selectAll();
    }

    @Override
    public List<transaction> selectAll() {
        List<transaction> list = new ArrayList<>();
        try {
            Connection conn = getConn();
            String sql = "SELECT * FROM transaction ORDER BY date DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRowToTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ selectAll: " + e.getMessage());
        }
        return list;
    }

    public List<transaction> getAllByFranchise(int franchiseId) {
        List<transaction> list = new ArrayList<>();
        try {
            Connection conn = getConn();
            String sql = "SELECT * FROM transaction WHERE franchise_id = ? ORDER BY date DESC";
            System.out.println("🔍 getAllByFranchise franchiseId=" + franchiseId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, franchiseId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRowToTransaction(rs));
                }
            }
            System.out.println("📋 Transactions trouvées : " + list.size());
        } catch (SQLException e) {
            System.err.println("❌ getAllByFranchise: " + e.getMessage());
        }
        return list;
    }

    public double calculerSolde(int franchiseId) {
        double solde = 0.0;
        try {
            Connection conn = getConn();
            String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN type='RECETTE' THEN montant ELSE 0 END), 0) " +
                "- COALESCE(SUM(CASE WHEN type='DEPENSE' THEN montant ELSE 0 END), 0) AS solde " +
                "FROM transaction WHERE franchise_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, franchiseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) solde = rs.getDouble("solde");
                }
            }
            System.out.println("💰 Solde franchiseId=" + franchiseId + " = " + solde);
        } catch (SQLException e) {
            System.err.println("❌ calculerSolde: " + e.getMessage());
        }
        return solde;
    }

    public double getTotalRevenus() {
        try {
            Connection conn = getConn();
            String sql = "SELECT COALESCE(SUM(montant),0) as total FROM transaction WHERE type='RECETTE'";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ getTotalRevenus: " + e.getMessage());
        }
        return 0.0;
    }

    public double getTotalDepenses() {
        try {
            Connection conn = getConn();
            String sql = "SELECT COALESCE(SUM(montant),0) as total FROM transaction WHERE type='DEPENSE'";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ getTotalDepenses: " + e.getMessage());
        }
        return 0.0;
    }

    public double getSoldeTotalReseau() {
        return getTotalRevenus() - getTotalDepenses();
    }

    public List<tn.esprit.boussole.models.FranchiseData> getDonneesFinancieresGlobales() {
        List<tn.esprit.boussole.models.FranchiseData> dataList = new ArrayList<>();
        try {
            Connection conn = getConn();
            String sql = "SELECT franchise_id, " +
                "SUM(CASE WHEN type='RECETTE' THEN montant ELSE 0 END) as total_recettes, " +
                "SUM(CASE WHEN type='DEPENSE' THEN montant ELSE 0 END) as total_depenses " +
                "FROM transaction GROUP BY franchise_id";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("franchise_id");
                    dataList.add(new tn.esprit.boussole.models.FranchiseData(
                        id, "Franchise " + id,
                        rs.getDouble("total_recettes"),
                        rs.getDouble("total_depenses")));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ getDonneesFinancieresGlobales: " + e.getMessage());
        }
        return dataList;
    }

    public java.util.Map<String, Double> getRepartitionCharges() {
        java.util.Map<String, Double> result = new java.util.HashMap<>();
        try {
            Connection conn = getConn();
            String sql = "SELECT description, SUM(montant) as total FROM transaction WHERE type='DEPENSE' GROUP BY description";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cat = rs.getString("description");
                    if (cat != null && cat.contains(" ")) cat = cat.split(" ")[0];
                    result.put(cat, result.getOrDefault(cat, 0.0) + rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ getRepartitionCharges: " + e.getMessage());
        }
        return result;
    }

    public java.util.Map<Integer, Double> getDepensesParMois(int annee) {
        java.util.Map<Integer, Double> result = new java.util.HashMap<>();
        try {
            Connection conn = getConn();
            String sql = "SELECT MONTH(date) as mois, SUM(montant) as total FROM transaction " +
                "WHERE type='DEPENSE' AND YEAR(date)=? GROUP BY MONTH(date)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, annee);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.put(rs.getInt("mois"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ getDepensesParMois: " + e.getMessage());
        }
        return result;
    }

    private transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Date date = rs.getDate("date");
        double montant = rs.getDouble("montant");
        String typeStr = rs.getString("type");
        Type type = null;
        if (typeStr != null && !typeStr.isEmpty()) {
            try { type = Type.valueOf(typeStr); }
            catch (IllegalArgumentException e) { System.out.println("Type inconnu: " + typeStr); }
        }
        String description = rs.getString("description");
        int franchiseId = rs.getInt("franchise_id");

        return new transaction(id, date, montant, type, description, franchiseId);
    }
}
