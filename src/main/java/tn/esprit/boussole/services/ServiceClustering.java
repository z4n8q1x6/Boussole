package tn.esprit.boussole.services;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import tn.esprit.boussole.Utilis.MyBdConnexion;
import tn.esprit.boussole.models.FranchiseData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceClustering {

    /**
     * Récupère les vraies données depuis la base de données pour le clustering.
     */
    public List<FranchiseData> chargerDonneesReelles() {
        List<FranchiseData> list = new ArrayList<>();
        Connection cnx = MyBdConnexion.getinstance().getCnx();
        
        String sql = "SELECT t.franchise_id, f.nom, " +
                     "SUM(CASE WHEN t.type='RECETTE' THEN t.montant ELSE 0 END) as Recettes, " +
                     "SUM(CASE WHEN t.type='DEPENSE' THEN t.montant ELSE 0 END) as Depenses " +
                     "FROM transaction t " +
                     "LEFT JOIN franchises f ON t.franchise_id = f.id " +
                     "GROUP BY t.franchise_id, f.nom";
                     
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int id = rs.getInt("franchise_id");
                String nom = rs.getString("nom");
                if (nom == null || nom.trim().isEmpty()) {
                    nom = "Franchise " + id;
                }
                
                double recettes = rs.getDouble("Recettes");
                double depenses = rs.getDouble("Depenses");
                
                list.add(new FranchiseData(id, nom, recettes, depenses));
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargerDonneesReelles : " + e.getMessage());
        }
        return list;
    }

    /**
     * Analyse les données des franchises et les regroupe en clusters.
     * @param data Liste des données financières des franchises.
     * @param k Nombre de clusters souhaités (par défaut 3).
     * @return Une Map associant l'index du cluster à la liste des franchises qu'il contient.
     */
    public Map<Integer, List<FranchiseData>> analyserDonnees(List<FranchiseData> data, int k) {
        Map<Integer, List<FranchiseData>> result = new HashMap<>();

        // Gestion des cas limites : pas assez de données pour le clustering
        if (data == null || data.isEmpty()) {
            return result;
        }

        // Si moins de points que de clusters demandés, on réduit K
        int actualK = Math.min(k, data.size());
        if (actualK < 2) {
            // Pas assez de données pour clusteriser, tout le monde dans le groupe 0
            result.put(0, new ArrayList<>(data));
            return result;
        }

        try {
            // Initialisation de l'algorithme K-Means++
            // -1 : max iterations illimité
            KMeansPlusPlusClusterer<FranchiseData> clusterer = new KMeansPlusPlusClusterer<>(actualK, -1);

            // Exécution du clustering
            List<CentroidCluster<FranchiseData>> clusterResults = clusterer.cluster(data);

            // Transformation des résultats en Map simple
            for (int i = 0; i < clusterResults.size(); i++) {
                CentroidCluster<FranchiseData> cluster = clusterResults.get(i);
                result.put(i, cluster.getPoints());
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du clustering : " + e.getMessage());
            // Fallback : tout le monde dans le groupe 0 en cas d'erreur mathématique
            result.put(0, new ArrayList<>(data));
        }

        return result;
    }
}

