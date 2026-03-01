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

    // ===================== CRUD DE BASE =====================

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
        return List.of();
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
        return "Unknown Franchise"; // Default return if ID does not exist
    }

    // ===================== MÉTHODES DE FILTRAGE (FROM PROJECT 1) =====================

    /**
     * Récupérer les franchises actives (actif = true)
     */
    public List<franchise> getFranchisesActives() throws SQLException {
        List<franchise> list = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE actif = 1 ORDER BY nom";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSetToFranchise(rs));
            }
        }
        return list;
    }

    /**
     * Récupérer les franchises inactives (actif = false)
     */
    public List<franchise> getFranchisesInactives() throws SQLException {
        List<franchise> list = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE actif = 0 ORDER BY nom";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapResultSetToFranchise(rs));
            }
        }
        return list;
    }

    // ===================== MÉTHODES DE RECHERCHE (FROM PROJECT 1) =====================

    /**
     * Rechercher des franchises par nom (recherche partielle)
     */
    public List<franchise> searchByNom(String recherche) throws SQLException {
        List<franchise> list = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE nom LIKE ? ORDER BY nom";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, "%" + recherche + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFranchise(rs));
                }
            }
        }
        return list;
    }

    /**
     * Rechercher des franchises par ville (dans l'adresse)
     */
    public List<franchise> searchByVille(String ville) throws SQLException {
        List<franchise> list = new ArrayList<>();
        String req = "SELECT * FROM franchises WHERE adresse LIKE ? ORDER BY nom";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, "%" + ville + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFranchise(rs));
                }
            }
        }
        return list;
    }

    // ===================== MÉTHODES DE COMPTAGE (FROM PROJECT 1) =====================

    /**
     * Compter le nombre total de franchises
     */
    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM franchises";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de franchises actives
     */
    public int countActives() throws SQLException {
        String req = "SELECT COUNT(*) FROM franchises WHERE actif = 1";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de franchises inactives
     */
    public int countInactives() throws SQLException {
        String req = "SELECT COUNT(*) FROM franchises WHERE actif = 0";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // ===================== MÉTHODES DE SOLDE (FROM PROJECT 1) =====================

    /**
     * Mettre à jour le solde d'une franchise
     */
    public void updateSolde(int franchiseId, double montant) throws SQLException {
        String req = "UPDATE franchises SET solde_actuel = solde_actuel + ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setDouble(1, montant);
            ps.setInt(2, franchiseId);
            ps.executeUpdate();
        }
    }

    /**
     * Obtenir le solde total de toutes les franchises
     */
    public double getSoldeTotal() throws SQLException {
        String req = "SELECT SUM(solde_actuel) FROM franchises";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }

    /**
     * Obtenir le solde moyen des franchises actives
     */
    public double getSoldeMoyen() throws SQLException {
        String req = "SELECT AVG(solde_actuel) FROM franchises WHERE actif = 1";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }

    // ===================== STATISTIQUES DE COMMANDES (FROM PROJECT 1) =====================

    /**
     * Obtenir le nombre de commandes par franchise
     * Retourne une liste d'objets [id, nom, nb_commandes, total_achats]
     */
    public List<Object[]> getStatistiquesCommandes() throws SQLException {
        List<Object[]> stats = new ArrayList<>();
        String req = "SELECT f.id, f.nom, COUNT(c.id) as nb_commandes, SUM(c.montant_total) as total_achats " +
                "FROM franchises f " +
                "LEFT JOIN commande c ON f.id = c.franchise_id " +
                "GROUP BY f.id, f.nom " +
                "ORDER BY total_achats DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Object[] row = new Object[4];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("nom");
                row[2] = rs.getInt("nb_commandes");
                row[3] = rs.getDouble("total_achats");
                stats.add(row);
            }
        }
        return stats;
    }

    // ===================== MÉTHODES DE PAGINATION (FROM PROJECT 1) =====================

    /**
     * Récupérer les franchises avec pagination
     */
    public List<franchise> selectWithPagination(int page, int limit) throws SQLException {
        List<franchise> list = new ArrayList<>();
        String req = "SELECT * FROM franchises ORDER BY nom LIMIT ? OFFSET ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            int offset = (page - 1) * limit;
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToFranchise(rs));
                }
            }
        }
        return list;
    }

    // ===================== MÉTHODE DE MAPPAGE UTILITAIRE =====================

    /**
     * Convertir un ResultSet en objet franchise
     */
    private franchise mapResultSetToFranchise(ResultSet rs) throws SQLException {
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

    // ===================== POUR LES TESTS (FROM PROJECT 1) =====================

    /**
     * Insérer des franchises de test
     */
    public void insertTestData() throws SQLException {
        franchise f1 = new franchise();
        f1.setNom("Franchise Tunis Centre");
        f1.setEmail("tunis@boussole.tn");
        f1.setTelephone("71234567");
        f1.setAdresse("Tunis Centre");
        f1.setActif(true);
        f1.setDateCreation(java.time.LocalDateTime.now());
        f1.setSoldeActuel(0.0);

        franchise f2 = new franchise();
        f2.setNom("Franchise Sfax");
        f2.setEmail("sfax@boussole.tn");
        f2.setTelephone("74234567");
        f2.setAdresse("Sfax Ville");
        f2.setActif(true);
        f2.setDateCreation(java.time.LocalDateTime.now());
        f2.setSoldeActuel(0.0);

        franchise f3 = new franchise();
        f3.setNom("Franchise Sousse");
        f3.setEmail("sousse@boussole.tn");
        f3.setTelephone("73234567");
        f3.setAdresse("Sousse Corniche");
        f3.setActif(true);
        f3.setDateCreation(java.time.LocalDateTime.now());
        f3.setSoldeActuel(0.0);

        insertone(f1);
        insertone(f2);
        insertone(f3);

        System.out.println("✅ Données de test insérées dans franchises");
    }
}