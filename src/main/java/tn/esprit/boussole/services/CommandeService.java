package tn.esprit.boussole.services;


import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.utils.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeService implements CRUD<Commande> {

    private Connection cnx;

    public CommandeService() {
        cnx = Database.getInstance().getConnection();
    }

    @Override
    public void insertOne(Commande commande) throws SQLException {

        String req = "INSERT INTO `commande`(`date_creation`, `montant_total`, `statut`, `franchise_id`) " +
                "VALUES ('"+commande.getDate_creation()+"','"+commande.getMontant_total()+"','"+commande.getStatut()+"','"+commande.getFranchise_id()+"')";

        Statement st = cnx.createStatement();
        st.executeUpdate(req);
    }

    public void insertOnePS(Commande commande) throws SQLException {

        String req = "INSERT INTO `commande`(`date_creation`, `montant_total`, `statut`, `franchise_id`) " +
                "VALUES (?,?,?,?)";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setTimestamp(1, Timestamp.valueOf(commande.getDate_creation()));
        ps.setDouble(2, commande.getMontant_total());
        ps.setString(3, commande.getStatut());
        ps.setInt(4, commande.getFranchise_id());

        ps.executeUpdate();
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

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Commande commande) throws SQLException {

        String req = "DELETE FROM commande WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, commande.getId());

        ps.executeUpdate();
    }

    @Override
    public List<Commande> selectAll() throws SQLException {

        List<Commande> commandeList = new ArrayList<>();
        String req = "SELECT * FROM commande";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            Commande c = new Commande();
            c.setId(rs.getInt("id"));
            c.setDate_creation(rs.getTimestamp("date_creation").toLocalDateTime());
            c.setMontant_total(rs.getDouble("montant_total"));
            c.setStatut(rs.getString("statut"));
            c.setFranchise_id(rs.getInt("franchise_id"));

            commandeList.add(c);
        }

        return commandeList;
    }
}
