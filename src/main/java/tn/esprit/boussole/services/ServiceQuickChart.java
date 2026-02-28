package tn.esprit.boussole.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ServiceQuickChart {

    /**
     * Génère une URL d'image pour un graphique Doughnut (Donut) avec QuickChart.io
     * @param recettes Le total des recettes (en vert)
     * @param depenses Le total des dépenses (en rouge)
     * @return L'URL directe de l'image du graphique
     */
    public String genererUrlGraphique(double recettes, double depenses) {
        try {
            // Création du JSON de configuration du graphique (Chart.js syntax)
            JSONObject chartConfig = new JSONObject();
            chartConfig.put("type", "doughnut");

            JSONObject data = new JSONObject();
            data.put("labels", new JSONArray(new String[]{"Recettes", "Charges"}));

            JSONArray datasets = new JSONArray();
            JSONObject datasetObj = new JSONObject();
            datasetObj.put("data", new JSONArray(new double[]{recettes, depenses}));
            // Vert pour les recettes (#10B981) et Rouge pour les dépenses (#EF4444)
            datasetObj.put("backgroundColor", new JSONArray(new String[]{"#10B981", "#EF4444"}));
            datasetObj.put("borderWidth", 0);
            datasets.put(datasetObj);

            data.put("datasets", datasets);
            chartConfig.put("data", data);

            // Options pour cacher la légende par défaut (ou la styliser)
            JSONObject options = new JSONObject();
            JSONObject plugins = new JSONObject();
            
            JSONObject datalabels = new JSONObject();
            datalabels.put("display", true);
            datalabels.put("color", "#fff");
            datalabels.put("font", new JSONObject().put("weight", "bold").put("size", 14));
            
            plugins.put("datalabels", datalabels);
            
            // Ajouter un titre
            JSONObject title = new JSONObject();
            title.put("display", true);
            title.put("text", "Répartition Financière");
            title.put("color", "#0F172A");
            title.put("font", new JSONObject().put("size", 18).put("weight", "bold"));
            plugins.put("title", title);
            
            options.put("plugins", plugins);
            chartConfig.put("options", options);

            // Encoder le JSON en paramètre d'URL
            String jsonString = chartConfig.toString();
            String encodedJson = URLEncoder.encode(jsonString, StandardCharsets.UTF_8.toString());

            // Construction de l'URL QuickChart (Format: https://quickchart.io/chart?c=...)
            // On définit aussi la taille de l'image (ex: 400x300)
            return "https://quickchart.io/chart?w=500&h=300&c=" + encodedJson;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
