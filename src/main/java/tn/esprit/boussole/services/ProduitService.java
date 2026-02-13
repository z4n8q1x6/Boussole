package tn.esprit.boussole.services;

import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements CRUD<Produit> {

    private Connection cnx;

    public ProduitService() {
        cnx = Database.getInstance().getConnection();
    }

    @Override
    public void insertOne(Produit produit) throws SQLException {

        String req = "INSERT INTO `produit`(`nom`, `reference`, `prix_achat`, `stock_dispo`, `image`) " +
                "VALUES ('"+produit.getNom()+"','"+produit.getReference()+"','"+produit.getPrix_achat()+"','"+produit.getStock_dispo()+"','"+produit.getImage()+"')";

        Statement st = cnx.createStatement();
        st.executeUpdate(req);
    }

    public void insertOnePS(Produit produit) throws SQLException {

        String req = "INSERT INTO `produit`(`nom`, `reference`, `prix_achat`, `stock_dispo`, `image`) " +
                "VALUES (?,?,?,?,?)";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, produit.getNom());
        ps.setString(2, produit.getReference());
        ps.setDouble(3, produit.getPrix_achat());
        ps.setInt(4, produit.getStock_dispo());
        ps.setString(5, produit.getImage());

        ps.executeUpdate();
    }

    @Override
    public void updateOne(Produit produit) throws SQLException {

        String req = "UPDATE produit SET nom = ?, reference = ?, prix_achat = ?, stock_dispo = ?, image = ? " +
                "WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, produit.getNom());
        ps.setString(2, produit.getReference());
        ps.setDouble(3, produit.getPrix_achat());
        ps.setInt(4, produit.getStock_dispo());
        ps.setString(5, produit.getImage());
        ps.setInt(6, produit.getId());

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Produit produit) throws SQLException {

        String req = "DELETE FROM produit WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, produit.getId());

        ps.executeUpdate();
    }

    @Override
    public List<Produit> selectAll() throws SQLException {

        List<Produit> produitList = new ArrayList<>();
        String req = "SELECT * FROM produit";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Produit p = new Produit("ordinater", "aze88az4e8", 800.99, 1, "azeazeaze");
            p.setId(rs.getInt("id"));
            p.setNom(rs.getString("nom"));
            p.setReference(rs.getString("reference"));
            p.setPrix_achat(rs.getDouble("prix_achat"));
            p.setStock_dispo(rs.getInt("stock_dispo"));
            p.setImage(rs.getString("image"));

            produitList.add(p);
        }

        return produitList;
    }
}


