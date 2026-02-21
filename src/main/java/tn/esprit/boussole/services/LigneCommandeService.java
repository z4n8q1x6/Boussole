package tn.esprit.boussole.services;

import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LigneCommandeService implements CRUD<LigneCommande> {

    private Connection cnx;

    public LigneCommandeService() {
        cnx = Database.getInstance().getConnection();
    }

    // ===================== CRUD DE BASE =====================

    @Override
    public void insertOne(LigneCommande ligneCommande) throws SQLException {
        String req = "INSERT INTO `ligne_commande`(`quantite`, `prix_unitaire`, `commande_id`, `produit_id`) " +
                "VALUES ('" + ligneCommande.getQuantite() + "','" + ligneCommande.getPrix_unitaire() + "','" +
                ligneCommande.getCommande_id() + "','" + ligneCommande.getProduit_id() + "')";

        Statement st = cnx.createStatement();
        st.executeUpdate(req);
        System.out.println("✅ LigneCommandeService.insertOne: ligne insérée");
    }

    /**
     * Insère une ligne de commande et récupère l'ID généré automatiquement
     * @param ligneCommande La ligne à insérer
     * @throws SQLException Erreur SQL
     */
    public void insertOnePS(LigneCommande ligneCommande) throws SQLException {
        String req = "INSERT INTO `ligne_commande`(`quantite`, `prix_unitaire`, `commande_id`, `produit_id`) " +
                "VALUES (?,?,?,?)";

        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, ligneCommande.getQuantite());
        ps.setDouble(2, ligneCommande.getPrix_unitaire());
        ps.setInt(3, ligneCommande.getCommande_id());
        ps.setInt(4, ligneCommande.getProduit_id());

        int affectedRows = ps.executeUpdate();
        System.out.println("✅ LigneCommandeService.insertOnePS: " + affectedRows + " ligne(s) insérée(s)");

        // Récupérer l'ID généré
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int generatedId = rs.getInt(1);
            ligneCommande.setId(generatedId);
            System.out.println("✅ ID généré pour la ligne: " + generatedId);
        } else {
            System.out.println("⚠️ Aucun ID généré récupéré");
        }
    }

    @Override
    public void updateOne(LigneCommande ligneCommande) throws SQLException {
        String req = "UPDATE ligne_commande SET quantite = ?, prix_unitaire = ?, commande_id = ?, produit_id = ? " +
                "WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setInt(1, ligneCommande.getQuantite());
        ps.setDouble(2, ligneCommande.getPrix_unitaire());
        ps.setInt(3, ligneCommande.getCommande_id());
        ps.setInt(4, ligneCommande.getProduit_id());
        ps.setInt(5, ligneCommande.getId());

        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ LigneCommandeService.updateOne: " + rowsAffected + " ligne(s) affectée(s) pour l'ID " + ligneCommande.getId());
    }

    @Override
    public void deleteOne(LigneCommande ligneCommande) throws SQLException {
        String req = "DELETE FROM ligne_commande WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, ligneCommande.getId());
        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ LigneCommandeService.deleteOne: " + rowsAffected + " ligne(s) supprimée(s)");
    }

    public void deleteById(int id) throws SQLException {
        String req = "DELETE FROM ligne_commande WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ LigneCommandeService.deleteById: " + rowsAffected + " ligne(s) supprimée(s) pour l'ID " + id);
    }

    @Override
    public List<LigneCommande> selectAll() throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT * FROM ligne_commande ORDER BY id DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            ligneCommandeList.add(mapResultSetToLigneCommande(rs));
        }

        System.out.println("✅ LigneCommandeService.selectAll: " + ligneCommandeList.size() + " ligne(s) trouvée(s)");
        return ligneCommandeList;
    }

    // ===================== RECHERCHE PAR ID =====================

    /**
     * Récupérer une ligne de commande par son ID
     */
    public LigneCommande selectById(int id) throws SQLException {
        String req = "SELECT * FROM ligne_commande WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapResultSetToLigneCommande(rs);
        }
        return null;
    }

    // ===================== RECHERCHE PAR COMMANDE =====================

    /**
     * Récupérer toutes les lignes d'une commande spécifique
     */
    public List<LigneCommande> selectByCommandeId(int commandeId) throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT * FROM ligne_commande WHERE commande_id = ? ORDER BY id";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commandeId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            ligneCommandeList.add(mapResultSetToLigneCommande(rs));
        }

        System.out.println("✅ LigneCommandeService.selectByCommandeId: " + ligneCommandeList.size() + " ligne(s) pour la commande " + commandeId);
        return ligneCommandeList;
    }

    /**
     * Récupérer les lignes d'une commande avec les informations produit
     */
    public List<LigneCommande> selectByCommandeIdWithDetails(int commandeId) throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT lc.*, p.nom as produit_nom, p.reference as produit_reference, p.image as produit_image " +
                "FROM ligne_commande lc " +
                "JOIN produit p ON lc.produit_id = p.id " +
                "WHERE lc.commande_id = ? " +
                "ORDER BY lc.id";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commandeId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            LigneCommande lc = mapResultSetToLigneCommande(rs);
            lc.setProduitNom(rs.getString("produit_nom"));
            lc.setProduitReference(rs.getString("produit_reference"));
            ligneCommandeList.add(lc);
        }

        return ligneCommandeList;
    }

    // ===================== RECHERCHE PAR PRODUIT =====================

    /**
     * Récupérer toutes les lignes d'un produit spécifique
     */
    public List<LigneCommande> selectByProduitId(int produitId) throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT * FROM ligne_commande WHERE produit_id = ? ORDER BY id";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produitId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            ligneCommandeList.add(mapResultSetToLigneCommande(rs));
        }

        return ligneCommandeList;
    }

    /**
     * Récupérer les lignes d'un produit avec les informations commande
     */
    public List<LigneCommande> selectByProduitIdWithDetails(int produitId) throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT lc.*, c.date_creation as commande_date, c.statut as commande_statut " +
                "FROM ligne_commande lc " +
                "JOIN commande c ON lc.commande_id = c.id " +
                "WHERE lc.produit_id = ? " +
                "ORDER BY c.date_creation DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produitId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            LigneCommande lc = mapResultSetToLigneCommande(rs);
            lc.setCommandeDate(rs.getTimestamp("commande_date").toLocalDateTime().toString());
            ligneCommandeList.add(lc);
        }

        return ligneCommandeList;
    }

    // ===================== STATISTIQUES =====================

    /**
     * Compter le nombre total de lignes
     */
    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM ligne_commande";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compter le nombre de lignes pour une commande
     */
    public int countByCommandeId(int commandeId) throws SQLException {
        String req = "SELECT COUNT(*) FROM ligne_commande WHERE commande_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commandeId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Compter le nombre de lignes pour un produit
     */
    public int countByProduitId(int produitId) throws SQLException {
        String req = "SELECT COUNT(*) FROM ligne_commande WHERE produit_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produitId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    /**
     * Calculer le montant total d'une commande à partir de ses lignes
     */
    public double calculerTotalCommande(int commandeId) throws SQLException {
        String req = "SELECT SUM(quantite * prix_unitaire) as total FROM ligne_commande WHERE commande_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commandeId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble("total");
        }
        return 0;
    }

    /**
     * Calculer la quantité totale vendue d'un produit
     */
    public int getQuantiteTotaleVendue(int produitId) throws SQLException {
        String req = "SELECT SUM(quantite) as total FROM ligne_commande WHERE produit_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produitId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    /**
     * Calculer le chiffre d'affaires généré par un produit
     */
    public double getChiffreAffairesByProduit(int produitId) throws SQLException {
        String req = "SELECT SUM(quantite * prix_unitaire) as total FROM ligne_commande WHERE produit_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produitId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble("total");
        }
        return 0;
    }

    /**
     * Obtenir le produit le plus vendu (quantité)
     */
    public int getProduitPlusVendu() throws SQLException {
        String req = "SELECT produit_id, SUM(quantite) as total FROM ligne_commande GROUP BY produit_id ORDER BY total DESC LIMIT 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        if (rs.next()) {
            return rs.getInt("produit_id");
        }
        return -1;
    }

    // ===================== VÉRIFICATIONS =====================

    /**
     * Vérifier si un produit est utilisé dans des commandes
     */
    public boolean isProduitUtilise(int produitId) throws SQLException {
        return countByProduitId(produitId) > 0;
    }

    /**
     * Vérifier si une commande a des lignes
     */
    public boolean isCommandeVide(int commandeId) throws SQLException {
        return countByCommandeId(commandeId) == 0;
    }

    /**
     * Vérifier la cohérence des prix (prix unitaire = prix du produit au moment de la commande)
     */
    public boolean verifierCohérencePrix(int ligneId, double prixActuelProduit) throws SQLException {
        LigneCommande ligne = selectById(ligneId);
        return ligne != null && Math.abs(ligne.getPrix_unitaire() - prixActuelProduit) < 0.01;
    }

    // ===================== PAGINATION =====================

    /**
     * Récupérer les lignes avec pagination
     */
    public List<LigneCommande> selectWithPagination(int page, int limit) throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT * FROM ligne_commande ORDER BY id DESC LIMIT ? OFFSET ?";
        PreparedStatement ps = cnx.prepareStatement(req);

        int offset = (page - 1) * limit;
        ps.setInt(1, limit);
        ps.setInt(2, offset);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            ligneCommandeList.add(mapResultSetToLigneCommande(rs));
        }

        return ligneCommandeList;
    }

    // ===================== SUPPRESSIONS GROUPÉES =====================

    /**
     * Supprimer toutes les lignes d'une commande
     */
    public void deleteByCommandeId(int commandeId) throws SQLException {
        String req = "DELETE FROM ligne_commande WHERE commande_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commandeId);
        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ LigneCommandeService.deleteByCommandeId: " + rowsAffected + " ligne(s) supprimée(s) pour la commande " + commandeId);
    }

    /**
     * Supprimer toutes les lignes d'un produit
     */
    public void deleteByProduitId(int produitId) throws SQLException {
        String req = "DELETE FROM ligne_commande WHERE produit_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produitId);
        int rowsAffected = ps.executeUpdate();
        System.out.println("✅ LigneCommandeService.deleteByProduitId: " + rowsAffected + " ligne(s) supprimée(s) pour le produit " + produitId);
    }

    // ===================== MÉTHODE DE MAPPAGE =====================

    /**
     * Convertir un ResultSet en objet LigneCommande
     */
    private LigneCommande mapResultSetToLigneCommande(ResultSet rs) throws SQLException {
        LigneCommande lc = new LigneCommande();
        lc.setId(rs.getInt("id"));
        lc.setQuantite(rs.getInt("quantite"));
        lc.setPrix_unitaire(rs.getDouble("prix_unitaire"));
        lc.setCommande_id(rs.getInt("commande_id"));
        lc.setProduit_id(rs.getInt("produit_id"));
        return lc;
    }

    // ===================== POUR LES TESTS =====================

    /**
     * Insérer des lignes de commande de test
     */
    public void insertTestData() throws SQLException {
        // Vérifier s'il y a des commandes et des produits
        CommandeService commandeService = new CommandeService();
        ProduitService produitService = new ProduitService();

        List<Commande> commandes = commandeService.selectAll();
        List<Produit> produits = produitService.selectAll();

        if (!commandes.isEmpty() && !produits.isEmpty()) {
            // Première commande
            if (produits.size() >= 2) {
                LigneCommande lc1 = new LigneCommande(2, produits.get(0).getPrix_achat(), commandes.get(0).getId(), produits.get(0).getId());
                LigneCommande lc2 = new LigneCommande(1, produits.get(1).getPrix_achat(), commandes.get(0).getId(), produits.get(1).getId());

                insertOnePS(lc1);
                insertOnePS(lc2);
                System.out.println("✅ Lignes de test insérées pour la commande " + commandes.get(0).getId());
            }

            // Deuxième commande
            if (produits.size() >= 3 && commandes.size() >= 2) {
                LigneCommande lc3 = new LigneCommande(3, produits.get(2).getPrix_achat(), commandes.get(1).getId(), produits.get(2).getId());
                insertOnePS(lc3);
                System.out.println("✅ Ligne de test insérée pour la commande " + commandes.get(1).getId());
            }
        } else {
            System.out.println("⚠️ Pas assez de commandes ou de produits pour insérer des lignes de test");
        }
    }
}