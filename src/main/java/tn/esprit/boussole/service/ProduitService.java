package tn.esprit.boussole.service;  // CHANGED: package name

import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.utils.MyBdConnexion;  // CHANGED: database utility

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements crud<Produit> {  // CHANGED: lowercase crud interface

    private Connection cnx;

    public ProduitService() {
        cnx = MyBdConnexion.getinstance().getCnx();  // CHANGED: database connection
    }

    // ===================== CRUD DE BASE =====================

    @Override
    public void insertone(Produit produit) throws SQLException {  // CHANGED: method name
        // Using PreparedStatement directly (skip the Statement version)
        String req = "INSERT INTO `produit`(`nom`, `reference`, `prix_achat`, `stock_dispo`, `image`) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, produit.getNom());
            ps.setString(2, produit.getReference());
            ps.setDouble(3, produit.getPrix_achat());
            ps.setInt(4, produit.getStock_dispo());
            ps.setString(5, produit.getImage());
            ps.executeUpdate();

            // Récupérer l'ID généré
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    produit.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateone(Produit produit) throws SQLException {  // CHANGED: method name
        String req = "UPDATE produit SET nom = ?, reference = ?, prix_achat = ?, stock_dispo = ?, image = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, produit.getNom());
            ps.setString(2, produit.getReference());
            ps.setDouble(3, produit.getPrix_achat());
            ps.setInt(4, produit.getStock_dispo());
            ps.setString(5, produit.getImage());
            ps.setInt(6, produit.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteone(Produit produit) throws SQLException {  // CHANGED: method name
        String req = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, produit.getId());
            ps.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        String req = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Produit> selectAll(Produit ignored) throws SQLException {  // CHANGED: method signature with parameter
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit ORDER BY id DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                produitList.add(mapResultSetToProduit(rs));
            }
        }

        return produitList;
    }

    @Override
    public List<Produit> selectAll() {
        return List.of();
    }

    // ===================== MÉTHODES DE RECHERCHE =====================

    /**
     * Récupérer un produit par son ID
     */
    public Produit selectById(int id) throws SQLException {
        String req = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupérer un produit par sa référence
     */
    public Produit selectByReference(String reference) throws SQLException {
        String req = "SELECT * FROM produit WHERE reference = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);
                }
            }
        }
        return null;
    }

    /**
     * Rechercher des produits par nom (recherche partielle)
     */
    public List<Produit> searchByNom(String recherche) throws SQLException {
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE nom LIKE ? ORDER BY nom";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, "%" + recherche + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produitList.add(mapResultSetToProduit(rs));
                }
            }
        }
        return produitList;
    }

    /**
     * Rechercher des produits par référence (recherche partielle)
     */
    public List<Produit> searchByReference(String recherche) throws SQLException {
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE reference LIKE ? ORDER BY reference";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, "%" + recherche + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produitList.add(mapResultSetToProduit(rs));
                }
            }
        }
        return produitList;
    }

    /**
     * Recherche générale (nom ou référence)
     */
    public List<Produit> search(String recherche) throws SQLException {
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE nom LIKE ? OR reference LIKE ? ORDER BY nom";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            String pattern = "%" + recherche + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produitList.add(mapResultSetToProduit(rs));
                }
            }
        }
        return produitList;
    }

    // ===================== MÉTHODES DE FILTRAGE =====================

    /**
     * Récupérer les produits en stock (stock > 0)
     */
    public List<Produit> selectEnStock() throws SQLException {
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE stock_dispo > 0 ORDER BY nom";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                produitList.add(mapResultSetToProduit(rs));
            }
        }
        return produitList;
    }

    /**
     * Récupérer les produits en rupture de stock (stock = 0)
     */
    public List<Produit> selectRuptureStock() throws SQLException {
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE stock_dispo = 0 ORDER BY nom";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                produitList.add(mapResultSetToProduit(rs));
            }
        }
        return produitList;
    }

    /**
     * Récupérer les produits par prix minimum et maximum
     */
    public List<Produit> selectByPrixRange(double min, double max) throws SQLException {
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit WHERE prix_achat BETWEEN ? AND ? ORDER BY prix_achat";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setDouble(1, min);
            ps.setDouble(2, max);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produitList.add(mapResultSetToProduit(rs));
                }
            }
        }
        return produitList;
    }

    // ===================== MÉTHODES DE STATISTIQUES =====================

    /**
     * Compter le nombre total de produits
     */
    public int countAll() throws SQLException {
        String req = "SELECT COUNT(*) FROM produit";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de produits en stock
     */
    public int countEnStock() throws SQLException {
        String req = "SELECT COUNT(*) FROM produit WHERE stock_dispo > 0";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de produits en rupture
     */
    public int countRupture() throws SQLException {
        String req = "SELECT COUNT(*) FROM produit WHERE stock_dispo = 0";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Obtenir la valeur totale du stock (prix_achat * stock_dispo)
     */
    public double getValeurTotaleStock() throws SQLException {
        String req = "SELECT SUM(prix_achat * stock_dispo) FROM produit";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }

    /**
     * Obtenir le prix moyen des produits
     */
    public double getPrixMoyen() throws SQLException {
        String req = "SELECT AVG(prix_achat) FROM produit";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }

    /**
     * Obtenir le produit le plus cher
     */
    public Produit getPlusCher() throws SQLException {
        String req = "SELECT * FROM produit ORDER BY prix_achat DESC LIMIT 1";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return mapResultSetToProduit(rs);
            }
        }
        return null;
    }

    /**
     * Obtenir le produit le moins cher
     */
    public Produit getMoinsCher() throws SQLException {
        String req = "SELECT * FROM produit ORDER BY prix_achat ASC LIMIT 1";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return mapResultSetToProduit(rs);
            }
        }
        return null;
    }

    // ===================== MÉTHODES DE PAGINATION =====================

    /**
     * Récupérer les produits avec pagination
     */
    public List<Produit> selectWithPagination(int page, int limit) throws SQLException {
        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit ORDER BY id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            int offset = (page - 1) * limit;
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produitList.add(mapResultSetToProduit(rs));
                }
            }
        }
        return produitList;
    }

    // ===================== MÉTHODES DE VÉRIFICATION =====================

    /**
     * Vérifier si une référence existe déjà
     */
    public boolean referenceExists(String reference) throws SQLException {
        String req = "SELECT COUNT(*) FROM produit WHERE reference = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Vérifier si une référence existe déjà (pour un ID différent)
     */
    public boolean referenceExistsForOther(String reference, int id) throws SQLException {
        String req = "SELECT COUNT(*) FROM produit WHERE reference = ? AND id != ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, reference);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Vérifier si le stock est suffisant pour une quantité donnée
     */
    public boolean stockSuffisant(int produitId, int quantite) throws SQLException {
        Produit p = selectById(produitId);
        return p != null && p.getStock_dispo() >= quantite;
    }

    /**
     * Mettre à jour le stock (ajouter ou enlever)
     */
    public void updateStock(int produitId, int quantite, boolean ajouter) throws SQLException {
        Produit p = selectById(produitId);
        if (p != null) {
            if (ajouter) {
                p.setStock_dispo(p.getStock_dispo() + quantite);
            } else {
                p.setStock_dispo(p.getStock_dispo() - quantite);
            }
            updateone(p);
        }
    }

    // ===================== MÉTHODE DE MAPPAGE =====================

    /**
     * Convertir un ResultSet en objet Produit
     */
    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setReference(rs.getString("reference"));
        p.setPrix_achat(rs.getDouble("prix_achat"));
        p.setStock_dispo(rs.getInt("stock_dispo"));
        p.setImage(rs.getString("image"));
        return p;
    }

    // ===================== POUR LES TESTS =====================

    /**
     * Insérer des produits de test
     */
    public void insertTestData() throws SQLException {
        Produit p1 = new Produit("Ordinateur Portable HP", "REF001", 2500.00, 10, "hp_laptop.jpg");
        Produit p2 = new Produit("Souris Logitech", "REF002", 45.00, 50, "souris.jpg");
        Produit p3 = new Produit("Clavier Mécanique", "REF003", 120.00, 30, "clavier.jpg");
        Produit p4 = new Produit("Écran Samsung 24\"", "REF004", 650.00, 5, "ecran.jpg");
        Produit p5 = new Produit("Disque Dur Externe 1To", "REF005", 280.00, 0, "dd.jpg");

        insertone(p1);
        insertone(p2);
        insertone(p3);
        insertone(p4);
        insertone(p5);
    }
}