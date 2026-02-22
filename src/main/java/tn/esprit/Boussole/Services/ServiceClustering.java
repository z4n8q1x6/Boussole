package tn.esprit.Boussole.Services;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import tn.esprit.Boussole.Models.FranchiseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceClustering {

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

