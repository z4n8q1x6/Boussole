package tn.esprit.boussole.services;

import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LigneCommandeService implements CRUD<LigneCommande> {

    private Connection cnx;

    public LigneCommandeService() {
        cnx = Database.getInstance().getConnection();
    }

    @Override
    public void insertOne(LigneCommande ligneCommande) throws SQLException {
        String req = "INSERT INTO `ligne_commande`(`quantite`, `prix_unitaire`, `commande_id`, `produit_id`) " +
                "VALUES ('"+ligneCommande.getQuantite()+"','"+ligneCommande.getPrix_unitaire()+"','"+ligneCommande.getCommande_id()+"','"+ligneCommande.getProduit_id()+"')";

        Statement st = cnx.createStatement();
        st.executeUpdate(req);
    }

    public void insertOnePS(LigneCommande ligneCommande) throws SQLException {
        String req = "INSERT INTO `ligne_commande`(`quantite`, `prix_unitaire`, `commande_id`, `produit_id`) " +
                "VALUES (?,?,?,?)";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setInt(1, ligneCommande.getQuantite());
        ps.setDouble(2, ligneCommande.getPrix_unitaire());
        ps.setInt(3, ligneCommande.getCommande_id());
        ps.setInt(4, ligneCommande.getProduit_id());

        ps.executeUpdate();
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

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(LigneCommande ligneCommande) throws SQLException {
        String req = "DELETE FROM ligne_commande WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, ligneCommande.getId());

        ps.executeUpdate();
    }

    @Override
    public List<LigneCommande> selectAll() throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT * FROM ligne_commande";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            LigneCommande lc = new LigneCommande();
            lc.setId(rs.getInt("id"));
            lc.setQuantite(rs.getInt("quantite"));
            lc.setPrix_unitaire(rs.getDouble("prix_unitaire"));
            lc.setCommande_id(rs.getInt("commande_id"));
            lc.setProduit_id(rs.getInt("produit_id"));

            ligneCommandeList.add(lc);
        }

        return ligneCommandeList;
    }

    // Méthodes supplémentaires utiles pour LigneCommande

    public List<LigneCommande> selectByCommandeId(int commandeId) throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT * FROM ligne_commande WHERE commande_id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commandeId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            LigneCommande lc = new LigneCommande();
            lc.setId(rs.getInt("id"));
            lc.setQuantite(rs.getInt("quantite"));
            lc.setPrix_unitaire(rs.getDouble("prix_unitaire"));
            lc.setCommande_id(rs.getInt("commande_id"));
            lc.setProduit_id(rs.getInt("produit_id"));

            ligneCommandeList.add(lc);
        }

        return ligneCommandeList;
    }

    public List<LigneCommande> selectByProduitId(int produitId) throws SQLException {
        List<LigneCommande> ligneCommandeList = new ArrayList<>();
        String req = "SELECT * FROM ligne_commande WHERE produit_id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produitId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            LigneCommande lc = new LigneCommande();
            lc.setId(rs.getInt("id"));
            lc.setQuantite(rs.getInt("quantite"));
            lc.setPrix_unitaire(rs.getDouble("prix_unitaire"));
            lc.setCommande_id(rs.getInt("commande_id"));
            lc.setProduit_id(rs.getInt("produit_id"));

            ligneCommandeList.add(lc);
        }

        return ligneCommandeList;
    }

    public double calculerTotalLigne(int ligneId) throws SQLException {
        String req = "SELECT quantite, prix_unitaire FROM ligne_commande WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, ligneId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int quantite = rs.getInt("quantite");
            double prix = rs.getDouble("prix_unitaire");
            return quantite * prix;
        }
        return 0;
    }

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
}
