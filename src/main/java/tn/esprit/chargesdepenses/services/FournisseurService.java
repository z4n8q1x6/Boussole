package tn.esprit.chargesdepenses.services;

import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.utils.MyBDConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FournisseurService implements CRUD<Fournisseur> {

    private Connection cnx;

    public FournisseurService() {
        // Récupération de la connexion via ton Singleton
        cnx = MyBDConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Fournisseur fournisseur) throws SQLException {
        // On n'insère pas l'ID car il est AUTO_INCREMENT
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
    public void updateOne(Fournisseur fournisseur) throws SQLException {
        String req = "UPDATE `fournisseur` SET `nom` = ?, `matricule_fiscal` = ?, `telephone` = ? WHERE `id` = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, fournisseur.getNom());
        ps.setString(2, fournisseur.getMatriculeFiscal());
        ps.setString(3, fournisseur.getTelephone());
        ps.setLong(4, fournisseur.getId()); // Utilise Long car ton entité utilise Long pour l'ID

        ps.executeUpdate();
        System.out.println("Fournisseur mis à jour !");
    }

    @Override
    public void deleteOne(Fournisseur fournisseur) throws SQLException {
        String req = "DELETE FROM `fournisseur` WHERE `id` = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setLong(1, fournisseur.getId());

        ps.executeUpdate();
        System.out.println("Fournisseur supprimé !");
    }

    @Override
    public List<Fournisseur> selectAll() throws SQLException {
        List<Fournisseur> fournisseurs = new ArrayList<>();
        String req = "SELECT * FROM `fournisseur`";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Fournisseur f = new Fournisseur();
            // Mapping des colonnes SQL vers l'objet Java
            f.setId(rs.getLong("id"));
            f.setNom(rs.getString("nom"));
            f.setMatriculeFiscal(rs.getString("matricule_fiscal"));
            f.setTelephone(rs.getString("telephone"));
            f.setFranchiseId(rs.getInt("franchise_id"));

            fournisseurs.add(f);
        }

        return fournisseurs;
    }
}