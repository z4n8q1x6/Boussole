package tn.esprit.boussole.services;

import tn.esprit.boussole.models.Franchise;
import tn.esprit.boussole.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FranchiseService {

    private Connection cnx;

    public FranchiseService() {
        cnx = Database.getInstance().getConnection();
    }

    // ===================== CRUD DE BASE =====================

    /**
     * Ajouter une nouvelle franchise
     */
    public void insertOne(Franchise franchise) throws SQLException {
        String req = "INSERT INTO franchises (nom, email, telephone, adresse, actif, solde_actuel) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, franchise.getNom());
        ps.setString(2, franchise.getEmail());
        ps.setString(3, franchise.getTelephone());
        ps.setString(4, franchise.getAdresse());
        ps.setBoolean(5, franchise.isActif());
        ps.setDouble(6, franchise.getSoldeActuel());

        ps.executeUpdate();

        // Récupérer l'ID généré
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            franchise.setId(rs.getInt(1));
        }
    }

    /**
     * Mettre à jour une franchise
     */
    public void updateOne(Franchise franchise) throws SQLException {
        String req = "UPDATE franchises SET nom = ?, email = ?, telephone = ?, adresse = ?, actif = ?, solde_actuel = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, franchise.getNom());
        ps.setString(2, franchise.getEmail());
        ps.setString(3, franchise.getTelephone());
        ps.setString(4, franchise.getAdresse());
        ps.setBoolean(5, franchise.isActif());
        ps.setDouble(6, franchise.getSoldeActuel());
        ps.setInt(7, franchise.getId());

        ps.executeUpdate();
    }

    /**
     * Supprimer une franchise
     */
    public void deleteOne(Franchise franchise) throws SQLException {
        String req = "DELETE FROM franchises WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, franchise.getId());
        ps.executeUpdate();
    }

    /**
     * Supprimer une franchise par son ID
     */
    public void deleteById(int id) throws SQLException {
        String req = "DELETE FROM franchises WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    /**
     * Récupérer toutes les franchises
     */
    public List<Franchise> selectAll() throws SQLException {
        List<Franchise> franchises = new ArrayList<>();
        String req = "SELECT * FROM franchises ORDER BY nom";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            franchises.add(mapResultSetToFranchise(rs));
        }
        return franchises;
    }

    /**
     * Récupérer une franchise par son ID
     */
    public Franchise getFranchiseById(int id) throws SQLException {
        String req = "SELECT * FROM franchises WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapResultSetToFranchise(rs);
        }
        return null;
    }

    /**
     * Récupérer une franchise par son email
     */
    public Franchise getFranchiseByEmail(String email) throws SQLException {
        String req = "SELECT * FROM franchises WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapResultSetToFranchise(rs);
        }
        return null;
    }

    /**
     * Récupérer les franchises actives
     */
    public List<Franchise> getFranchisesActives() throws SQLException {
        List<Franchise> franchises = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE actif = 1 ORDER BY nom";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            franchises.add(mapResultSetToFranchise(rs));
        }
        return franchises;
    }

    /**
     * Récupérer les franchises inactives
     */
    public List<Franchise> getFranchisesInactives() throws SQLException {
        List<Franchise> franchises = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE actif = 0 ORDER BY nom";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            franchises.add(mapResultSetToFranchise(rs));
        }
        return franchises;
    }

    // ===================== MÉTHODES DE COMPTAGE =====================

    /**
     * Compter le nombre total de franchises
     */
    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM franchises";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compter le nombre de franchises actives
     */
    public int countActives() throws SQLException {
        String req = "SELECT COUNT(*) FROM franchises WHERE actif = 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compter le nombre de franchises inactives
     */
    public int countInactives() throws SQLException {
        String req = "SELECT COUNT(*) FROM franchises WHERE actif = 0";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    // ===================== MÉTHODES DE RECHERCHE =====================

    /**
     * Rechercher des franchises par nom
     */
    public List<Franchise> searchByNom(String recherche) throws SQLException {
        List<Franchise> franchises = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE nom LIKE ? ORDER BY nom";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, "%" + recherche + "%");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            franchises.add(mapResultSetToFranchise(rs));
        }
        return franchises;
    }

    /**
     * Rechercher des franchises par ville (dans l'adresse)
     */
    public List<Franchise> searchByVille(String ville) throws SQLException {
        List<Franchise> franchises = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE adresse LIKE ? ORDER BY nom";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, "%" + ville + "%");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            franchises.add(mapResultSetToFranchise(rs));
        }
        return franchises;
    }

    // ===================== MÉTHODES DE SOLDE =====================

    /**
     * Mettre à jour le solde d'une franchise
     */
    public void updateSolde(int franchiseId, double montant) throws SQLException {
        String req = "UPDATE franchises SET solde_actuel = solde_actuel + ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setDouble(1, montant);
        ps.setInt(2, franchiseId);
        ps.executeUpdate();
    }

    /**
     * Obtenir le solde total de toutes les franchises
     */
    public double getSoldeTotal() throws SQLException {
        String req = "SELECT SUM(solde_actuel) FROM franchises";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    /**
     * Obtenir le solde moyen des franchises
     */
    public double getSoldeMoyen() throws SQLException {
        String req = "SELECT AVG(solde_actuel) FROM franchises WHERE actif = 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    // ===================== MÉTHODES DE STATISTIQUES =====================

    /**
     * Obtenir le nombre de commandes par franchise
     */
    public List<Object[]> getStatistiquesCommandes() throws SQLException {
        List<Object[]> stats = new ArrayList<>();
        String req = "SELECT f.id, f.nom, COUNT(c.id) as nb_commandes, SUM(c.montant_total) as total_achats " +
                "FROM franchises f " +
                "LEFT JOIN commande c ON f.id = c.franchise_id " +
                "GROUP BY f.id, f.nom " +
                "ORDER BY total_achats DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Object[] row = new Object[4];
            row[0] = rs.getInt("id");
            row[1] = rs.getString("nom");
            row[2] = rs.getInt("nb_commandes");
            row[3] = rs.getDouble("total_achats");
            stats.add(row);
        }
        return stats;
    }

    // ===================== MÉTHODES DE PAGINATION =====================

    /**
     * Récupérer les franchises avec pagination
     */
    public List<Franchise> selectWithPagination(int page, int limit) throws SQLException {
        List<Franchise> franchises = new ArrayList<>();
        String req = "SELECT * FROM franchises ORDER BY nom LIMIT ? OFFSET ?";
        PreparedStatement ps = cnx.prepareStatement(req);

        int offset = (page - 1) * limit;
        ps.setInt(1, limit);
        ps.setInt(2, offset);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            franchises.add(mapResultSetToFranchise(rs));
        }
        return franchises;
    }

    // ===================== MÉTHODE DE MAPPAGE =====================

    /**
     * Convertir un ResultSet en objet Franchise
     */
    private Franchise mapResultSetToFranchise(ResultSet rs) throws SQLException {
        Franchise f = new Franchise();
        f.setId(rs.getInt("id"));
        f.setNom(rs.getString("nom"));
        f.setEmail(rs.getString("email"));
        f.setTelephone(rs.getString("telephone"));
        f.setAdresse(rs.getString("adresse"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            f.setDateCreation(ts.toLocalDateTime());
        }

        f.setActif(rs.getBoolean("actif"));
        f.setSoldeActuel(rs.getDouble("solde_actuel"));
        return f;
    }

    // ===================== POUR LES TESTS =====================

    /**
     * Insérer des franchises de test
     */
    public void insertTestData() throws SQLException {
        Franchise f1 = new Franchise("Franchise Tunis Centre", "tunis@boussole.tn", "71234567", "Tunis Centre");
        Franchise f2 = new Franchise("Franchise Sfax", "sfax@boussole.tn", "74234567", "Sfax Ville");
        Franchise f3 = new Franchise("Franchise Sousse", "sousse@boussole.tn", "73234567", "Sousse Corniche");

        insertOne(f1);
        insertOne(f2);
        insertOne(f3);

        System.out.println("Données de test insérées avec succès !");
    }
}