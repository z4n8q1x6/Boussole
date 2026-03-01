package tn.esprit.boussole.service;

import tn.esprit.boussole.models.Fournisseur;
import tn.esprit.boussole.utils.MyBdConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FournisseurService implements crud<Fournisseur> {

    private Connection cnx;

    public FournisseurService() {
        cnx = MyBdConnexion.getinstance().getCnx();
    }

    @Override
    public void insertone(Fournisseur fournisseur) throws SQLException {
        String req = "INSERT INTO `fournisseur` (`nom`, `matricule_fiscal`, `telephone`, `franchise_id`) VALUES (?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, fournisseur.getNom());
        ps.setString(2, fournisseur.getMatriculeFiscal());
        ps.setString(3, fournisseur.getTelephone());
        ps.setInt(4, fournisseur.getFranchiseId());

        ps.executeUpdate();
        System.out.println("Fournisseur ajouté avec succès !");
    }

    @Override
    public void updateone(Fournisseur fournisseur) throws SQLException {
        // Ajout de franchise_id à la clause SET
        String req = "UPDATE `fournisseur` SET `nom` = ?, `matricule_fiscal` = ?, `telephone` = ?, `franchise_id` = ? WHERE `id` = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, fournisseur.getNom());
        ps.setString(2, fournisseur.getMatriculeFiscal());
        ps.setString(3, fournisseur.getTelephone());
        ps.setInt(4, fournisseur.getFranchiseId()); // Ajout de la franchise_id
        ps.setLong(5, fournisseur.getId());

        ps.executeUpdate();
        System.out.println("Fournisseur mis à jour !");
    }

    @Override
    public void deleteone(Fournisseur fournisseur) throws SQLException {
        String req = "DELETE FROM `fournisseur` WHERE `id` = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setLong(1, fournisseur.getId());

        ps.executeUpdate();
        System.out.println("Fournisseur supprimé !");
    }

    @Override
    public List<Fournisseur> selectAll(Fournisseur fournisseur) throws SQLException {
        List<Fournisseur> fournisseurs = new ArrayList<>();
        // Jointure pour récupérer le nom de la franchise
        String req = "SELECT f.*, fr.nom as franchise_nom FROM `fournisseur` f JOIN `franchises` fr ON f.franchise_id = fr.id";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Fournisseur f = new Fournisseur();
            f.setId(rs.getLong("id"));
            f.setNom(rs.getString("nom"));
            f.setMatriculeFiscal(rs.getString("matricule_fiscal"));
            f.setTelephone(rs.getString("telephone"));
            f.setFranchiseId(rs.getInt("franchise_id"));
            f.setFranchiseName(rs.getString("franchise_nom")); // Récupération du nom

            fournisseurs.add(f);
        }

        return fournisseurs;
    }

<<<<<<< HEAD
    @Override
    public List<Fournisseur> selectAll() {
        return List.of();
    }

=======
>>>>>>> 2118e9cc01de212c47c7cbfda8004c4fa0bea0f9
    // Nouvelle méthode pour trouver l'ID par le nom
    public int getFranchiseIdByName(String name) throws SQLException {
        String req = "SELECT id FROM franchises WHERE nom = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        return -1; 
    }
    
    // Nouvelle méthode pour trouver le nom par l'ID
    public String getFranchiseNameById(int id) throws SQLException {
        String req = "SELECT nom FROM franchises WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("nom");
        }
        return "Inconnu";
    }
}
