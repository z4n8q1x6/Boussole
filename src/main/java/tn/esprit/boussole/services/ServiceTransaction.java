package tn.esprit.boussole.services;

import tn.esprit.boussole.models.transaction;
import tn.esprit.boussole.models.transaction.Type;
import tn.esprit.boussole.Utilis.MyBdConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTransaction implements crud<transaction> {

    private final Connection cnx;

    public ServiceTransaction() {
        this.cnx = MyBdConnexion.getinstance().getCnx();
    }

    @Override
    public void insertone(transaction t) {
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
    public void updateone(transaction t) {
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
    public void deleteone(transaction t) {
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

    // Méthode pour récupérer les données agrégées pour l'IA (Clustering)
    // Retourne une map : FranchiseID -> {TotalRecettes, TotalDepenses}
    public List<tn.esprit.boussole.models.FranchiseData> getDonneesFinancieresGlobales() {
        List<tn.esprit.boussole.models.FranchiseData> dataList = new ArrayList<>();
        String sql = "SELECT franchise_id, " +
                     "SUM(CASE WHEN type = 'RECETTE' THEN montant ELSE 0 END) as total_recettes, " +
                     "SUM(CASE WHEN type = 'DEPENSE' THEN montant ELSE 0 END) as total_depenses " +
                     "FROM transaction GROUP BY franchise_id";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("franchise_id");
                double recettes = rs.getDouble("total_recettes");
                double depenses = rs.getDouble("total_depenses");
                String label = "Franchise " + id; // Pourra être remplacé par le vrai nom si dispo

                dataList.add(new tn.esprit.boussole.models.FranchiseData(id, label, recettes, depenses));
            }
        } catch (SQLException e) {
            System.out.println("Erreur getDonneesFinancieresGlobales: " + e.getMessage());
        }
        return dataList;
    }

    // Méthode pour le PieChart : Répartition des dépenses par catégorie
    public java.util.Map<String, Double> getRepartitionCharges() {
        java.util.Map<String, Double> result = new java.util.HashMap<>();
        // Note: La catégorie est stockée dans la description ou un champ spécifique selon votre modèle.
        // Si 'description' sert de catégorie pour les dépenses, ou si on n'a pas de colonne catégorie dans transaction,
        // on va supposer ici que la 'description' contient la catégorie pour les dépenses simples,
        // OU on va se baser sur les budgets.
        // MAIS, le prompt demande "Répartition des Charges".
        // Comme Transaction n'a pas de colonne "categorie" explicite (c'est dans Budget),
        // On va simuler ou extraire depuis description si possible, OU mieux :
        // Pour cet exercice, nous allons assumer que la description contient la catégorie
        // Ou ajouter une logique simple.

        // CORRECTION : Le modèle BudgetPrevisionnel A une catégorie. Le modèle Transaction a une description.
        // Pour faire simple et robuste sans changer le schéma Transaction maintenant :
        // On va grouper par DESCRIPTION pour l'instant (ex: "Loyer Janvier", "Salaire").
        // Idéalement, il faudrait une colonne category_id dans transaction.

        String sql = "SELECT description, SUM(montant) as total FROM transaction WHERE type = 'DEPENSE' GROUP BY description";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String cat = rs.getString("description");
                // Nettoyage simple pour regrouper (ex: "Salaire Janvier" -> "Salaire")
                // Ceci est optionnel pour l'IA mais rend le graph plus propre
                if (cat.contains(" ")) {
                    cat = cat.split(" ")[0];
                }
                double montant = rs.getDouble("total");

                result.put(cat, result.getOrDefault(cat, 0.0) + montant);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getRepartitionCharges : " + e.getMessage());
        }
        return result;
    }

    // Méthode pour obtenir les dépenses totales par mois pour une année donnée (BarChart)
    // Retourne Map<Mois(int), Montant>
    public java.util.Map<Integer, Double> getDepensesParMois(int annee) {
        java.util.Map<Integer, Double> result = new java.util.HashMap<>();
        String sql = "SELECT MONTH(date) as mois, SUM(montant) as total FROM transaction " +
                     "WHERE type = 'DEPENSE' AND YEAR(date) = ? GROUP BY MONTH(date)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, annee);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("mois"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur getDepensesParMois : " + e.getMessage());
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
