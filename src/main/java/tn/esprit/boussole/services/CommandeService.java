package tn.esprit.boussole.services;

import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.utils.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeService implements CRUD<Commande> {

    private Connection cnx;

    public CommandeService() {
        cnx = Database.getInstance().getConnection();
    }

    // ===================== CRUD DE BASE =====================

    @Override
    public void insertOne(Commande commande) throws SQLException {
        String req = "INSERT INTO `commande`(`date_creation`, `montant_total`, `statut`, `franchise_id`) " +
                "VALUES ('" + commande.getDate_creation() + "','" + commande.getMontant_total() + "','" +
                commande.getStatut() + "','" + commande.getFranchise_id() + "')";

        Statement st = cnx.createStatement();
        st.executeUpdate(req);
    }

    /**
     * Insère une commande et récupère l'ID généré automatiquement
     * @param commande La commande à insérer
     * @throws SQLException Erreur SQL
     */
    public void insertOnePS(Commande commande) throws SQLException {
        String req = "INSERT INTO `commande`(`date_creation`, `montant_total`, `statut`, `franchise_id`) " +
                "VALUES (?,?,?,?)";

        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        ps.setTimestamp(1, Timestamp.valueOf(commande.getDate_creation()));
        ps.setDouble(2, commande.getMontant_total());
        ps.setString(3, commande.getStatut());
        ps.setInt(4, commande.getFranchise_id());

        int affectedRows = ps.executeUpdate();
        System.out.println("✅ CommandeService.insertOnePS: " + affectedRows + " ligne(s) insérée(s)");

        // Récupérer l'ID généré
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int generatedId = rs.getInt(1);
            commande.setId(generatedId);
            System.out.println("✅ ID généré pour la commande: " + generatedId);
        } else {
            System.out.println("⚠️ Aucun ID généré récupéré");
        }
    }

    @Override
    public void updateOne(Commande commande) throws SQLException {
        String req = "UPDATE commande SET date_creation = ?, montant_total = ?, statut = ?, franchise_id = ? " +
                "WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setTimestamp(1, Timestamp.valueOf(commande.getDate_creation()));
        ps.setDouble(2, commande.getMontant_total());
        ps.setString(3, commande.getStatut());
        ps.setInt(4, commande.getFranchise_id());
        ps.setInt(5, commande.getId());

        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ CommandeService.updateOne: " + rowsAffected + " ligne(s) affectée(s) pour l'ID " + commande.getId());
    }

    @Override
    public void deleteOne(Commande commande) throws SQLException {
        String req = "DELETE FROM commande WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commande.getId());
        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ CommandeService.deleteOne: " + rowsAffected + " ligne(s) supprimée(s)");
    }

    public void deleteById(int id) throws SQLException {
        String req = "DELETE FROM commande WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ CommandeService.deleteById: " + rowsAffected + " ligne(s) supprimée(s) pour l'ID " + id);
    }

    @Override
    public List<Commande> selectAll() throws SQLException {
        List<Commande> commandeList = new ArrayList<>();
        String req = "SELECT * FROM commande ORDER BY date_creation DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            commandeList.add(mapResultSetToCommande(rs));
        }

        System.out.println("✅ CommandeService.selectAll: " + commandeList.size() + " commande(s) trouvée(s)");
        return commandeList;
    }

    // ===================== RECHERCHE PAR ID =====================

    /**
     * Récupérer une commande par son ID
     */
    public Commande selectById(int id) throws SQLException {
        String req = "SELECT * FROM commande WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapResultSetToCommande(rs);
        }
        return null;
    }

    // ===================== RECHERCHE PAR FRANCHISE =====================

    /**
     * Récupérer les commandes d'une franchise spécifique
     */
    public List<Commande> selectByFranchiseId(int franchiseId) throws SQLException {
        List<Commande> commandeList = new ArrayList<>();
        String req = "SELECT * FROM commande WHERE franchise_id = ? ORDER BY date_creation DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, franchiseId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            commandeList.add(mapResultSetToCommande(rs));
        }

        return commandeList;
    }

    /**
     * Récupérer les commandes d'une franchise avec pagination
     */
    public List<Commande> selectByFranchiseIdWithPagination(int franchiseId, int page, int limit) throws SQLException {
        List<Commande> commandeList = new ArrayList<>();
        String req = "SELECT * FROM commande WHERE franchise_id = ? ORDER BY date_creation DESC LIMIT ? OFFSET ?";
        PreparedStatement ps = cnx.prepareStatement(req);

        int offset = (page - 1) * limit;
        ps.setInt(1, franchiseId);
        ps.setInt(2, limit);
        ps.setInt(3, offset);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            commandeList.add(mapResultSetToCommande(rs));
        }

        return commandeList;
    }

    // ===================== RECHERCHE PAR STATUT =====================

    /**
     * Récupérer les commandes par statut
     */
    public List<Commande> selectByStatut(String statut) throws SQLException {
        List<Commande> commandeList = new ArrayList<>();
        String req = "SELECT * FROM commande WHERE statut = ? ORDER BY date_creation DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, statut);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            commandeList.add(mapResultSetToCommande(rs));
        }

        return commandeList;
    }

    /**
     * Récupérer les commandes en attente
     */
    public List<Commande> selectEnAttente() throws SQLException {
        return selectByStatut("EN_ATTENTE");
    }

    /**
     * Récupérer les commandes validées
     */
    public List<Commande> selectValidees() throws SQLException {
        return selectByStatut("VALIDEE");
    }

    /**
     * Récupérer les commandes refusées
     */
    public List<Commande> selectRefusees() throws SQLException {
        return selectByStatut("REFUSEE");
    }

    // ===================== RECHERCHE PAR PÉRIODE =====================

    /**
     * Récupérer les commandes entre deux dates
     */
    public List<Commande> selectBetweenDates(LocalDateTime debut, LocalDateTime fin) throws SQLException {
        List<Commande> commandeList = new ArrayList<>();
        String req = "SELECT * FROM commande WHERE date_creation BETWEEN ? AND ? ORDER BY date_creation DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setTimestamp(1, Timestamp.valueOf(debut));
        ps.setTimestamp(2, Timestamp.valueOf(fin));
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            commandeList.add(mapResultSetToCommande(rs));
        }

        return commandeList;
    }

    /**
     * Récupérer les commandes du jour
     */
    public List<Commande> selectDuJour() throws SQLException {
        LocalDateTime debut = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime fin = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return selectBetweenDates(debut, fin);
    }

    /**
     * Récupérer les commandes de la semaine
     */
    public List<Commande> selectDeLaSemaine() throws SQLException {
        LocalDateTime debut = LocalDateTime.now().minusDays(7);
        LocalDateTime fin = LocalDateTime.now();
        return selectBetweenDates(debut, fin);
    }

    /**
     * Récupérer les commandes du mois
     */
    public List<Commande> selectDuMois() throws SQLException {
        LocalDateTime debut = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime fin = LocalDateTime.now();
        return selectBetweenDates(debut, fin);
    }

    // ===================== STATISTIQUES =====================

    /**
     * Compter le nombre total de commandes
     */
    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM commande";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compter les commandes par statut
     */
    public int countByStatut(String statut) throws SQLException {
        String req = "SELECT COUNT(*) FROM commande WHERE statut = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, statut);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compter les commandes en attente
     */
    public int countEnAttente() throws SQLException {
        return countByStatut("EN_ATTENTE");
    }

    /**
     * Compter les commandes validées
     */
    public int countValidees() throws SQLException {
        return countByStatut("VALIDEE");
    }

    /**
     * Compter les commandes refusées
     */
    public int countRefusees() throws SQLException {
        return countByStatut("REFUSEE");
    }

    /**
     * Compter les commandes d'une franchise
     */
    public int countByFranchiseId(int franchiseId) throws SQLException {
        String req = "SELECT COUNT(*) FROM commande WHERE franchise_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, franchiseId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Calculer le montant total des commandes (chiffre d'affaires)
     */
    public double getChiffreAffaires() throws SQLException {
        String req = "SELECT SUM(montant_total) FROM commande WHERE statut = 'VALIDEE'";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    /**
     * Calculer le montant total des commandes pour une période
     */
    public double getChiffreAffairesBetweenDates(LocalDateTime debut, LocalDateTime fin) throws SQLException {
        String req = "SELECT SUM(montant_total) FROM commande WHERE statut = 'VALIDEE' AND date_creation BETWEEN ? AND ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setTimestamp(1, Timestamp.valueOf(debut));
        ps.setTimestamp(2, Timestamp.valueOf(fin));
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    /**
     * Calculer le montant total des commandes d'une franchise
     */
    public double getTotalAchatsByFranchiseId(int franchiseId) throws SQLException {
        String req = "SELECT SUM(montant_total) FROM commande WHERE franchise_id = ? AND statut = 'VALIDEE'";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, franchiseId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    /**
     * Calculer le montant moyen des commandes
     */
    public double getMontantMoyen() throws SQLException {
        String req = "SELECT AVG(montant_total) FROM commande";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getDouble(1);
        }
        return 0;
    }

    /**
     * Obtenir la commande la plus élevée
     */
    public Commande getCommandeMax() throws SQLException {
        String req = "SELECT * FROM commande ORDER BY montant_total DESC LIMIT 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return mapResultSetToCommande(rs);
        }
        return null;
    }

    // ===================== PAGINATION =====================

    /**
     * Récupérer les commandes avec pagination
     */
    public List<Commande> selectWithPagination(int page, int limit) throws SQLException {
        List<Commande> commandeList = new ArrayList<>();
        String req = "SELECT * FROM commande ORDER BY date_creation DESC LIMIT ? OFFSET ?";
        PreparedStatement ps = cnx.prepareStatement(req);

        int offset = (page - 1) * limit;
        ps.setInt(1, limit);
        ps.setInt(2, offset);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            commandeList.add(mapResultSetToCommande(rs));
        }

        return commandeList;
    }

    // ===================== TRANSACTIONS COMPLEXES =====================

    /**
     * Créer une commande complète avec ses lignes (transaction)
     */
    public void createCommandeWithLines(Commande commande, List<LigneCommande> lignes) throws SQLException {
        try {
            cnx.setAutoCommit(false);

            // Insérer la commande
            insertOnePS(commande);

            // Insérer les lignes
            LigneCommandeService ligneService = new LigneCommandeService();
            for (LigneCommande ligne : lignes) {
                ligne.setCommande_id(commande.getId());
                ligneService.insertOnePS(ligne);
            }

            cnx.commit();
            System.out.println("✅ Commande " + commande.getId() + " créée avec " + lignes.size() + " ligne(s)");
        } catch (SQLException e) {
            cnx.rollback();
            System.err.println("❌ Erreur lors de la création de la commande, rollback effectué");
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    /**
     * Valider une commande (changer statut et mettre à jour les stocks)
     */
    public void validerCommande(int commandeId) throws SQLException {
        try {
            cnx.setAutoCommit(false);

            // Récupérer la commande
            Commande commande = selectById(commandeId);
            if (commande == null) {
                throw new SQLException("Commande non trouvée");
            }

            // Vérifier que la commande est en attente
            if (!"EN_ATTENTE".equals(commande.getStatut())) {
                throw new SQLException("Seules les commandes en attente peuvent être validées");
            }

            // Récupérer les lignes de commande
            LigneCommandeService ligneService = new LigneCommandeService();
            List<LigneCommande> lignes = ligneService.selectByCommandeId(commandeId);

            // Vérifier les stocks et mettre à jour
            ProduitService produitService = new ProduitService();
            for (LigneCommande ligne : lignes) {
                Produit produit = produitService.selectById(ligne.getProduit_id());
                if (produit.getStock_dispo() < ligne.getQuantite()) {
                    throw new SQLException("Stock insuffisant pour le produit: " + produit.getNom());
                }
                produit.setStock_dispo(produit.getStock_dispo() - ligne.getQuantite());
                produitService.updateOne(produit);
            }

            // Mettre à jour le statut
            commande.setStatut("VALIDEE");
            updateOne(commande);

            cnx.commit();
            System.out.println("✅ Commande " + commandeId + " validée avec succès !");
        } catch (SQLException e) {
            cnx.rollback();
            System.err.println("❌ Erreur lors de la validation de la commande " + commandeId + ": " + e.getMessage());
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    /**
     * Refuser une commande
     */
    public void refuserCommande(int commandeId) throws SQLException {
        Commande commande = selectById(commandeId);
        if (commande != null) {
            commande.setStatut("REFUSEE");
            updateOne(commande);
            System.out.println("✅ Commande " + commandeId + " refusée");
        }
    }

    /**
     * Annuler une commande (si elle est en attente)
     */
    public void annulerCommande(int commandeId) throws SQLException {
        Commande commande = selectById(commandeId);
        if (commande != null && "EN_ATTENTE".equals(commande.getStatut())) {
            deleteById(commandeId);
            System.out.println("✅ Commande " + commandeId + " annulée et supprimée");
        }
    }

    // ===================== MÉTHODE DE MAPPAGE =====================

    /**
     * Convertir un ResultSet en objet Commande
     */
    private Commande mapResultSetToCommande(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId(rs.getInt("id"));
        c.setDate_creation(rs.getTimestamp("date_creation").toLocalDateTime());
        c.setMontant_total(rs.getDouble("montant_total"));
        c.setStatut(rs.getString("statut"));
        c.setFranchise_id(rs.getInt("franchise_id"));
        return c;
    }

    // ===================== POUR LES TESTS =====================

    /**
     * Insérer des commandes de test
     */
    public void insertTestData() throws SQLException {
        // Vérifier s'il y a des franchises
        FranchiseService franchiseService = new FranchiseService();
        if (franchiseService.countAll() == 0) {
            franchiseService.insertTestData();
        }

        // Créer des commandes test
        Commande c1 = new Commande(LocalDateTime.now().minusDays(2), 1250.00, "VALIDEE", 1);
        Commande c2 = new Commande(LocalDateTime.now().minusDays(1), 850.50, "EN_ATTENTE", 2);
        Commande c3 = new Commande(LocalDateTime.now(), 2300.00, "EN_ATTENTE", 1);
        Commande c4 = new Commande(LocalDateTime.now().minusDays(5), 430.00, "REFUSEE", 3);

        insertOnePS(c1);
        insertOnePS(c2);
        insertOnePS(c3);
        insertOnePS(c4);

        System.out.println("✅ Données de test insérées dans commande");
    }
}