package tn.esprit.boussole.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.boussole.models.AlerteIA;

import java.io.IOException;
import java.util.Optional;

public class Gemini {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY"); // Assure-toi que c'est le bon nom dans .env
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Optional<AlerteIA> generate_alerte() {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("GEMINI_API_KEY not set in .env");
            return Optional.empty();
        }

        // Données simulées (à remplacer par des données réelles)
        String franchiseName = "Tunis Downtown";
        int year = 2024;
        int month = 2;
        double totalRecettes = 450000.00;
        double totalCharges = 240000.00;
        double resultatNet = totalRecettes - totalCharges;
        double budgetRevenuCible = 500000.00;
        double variance = ((totalRecettes - budgetRevenuCible) / budgetRevenuCible) * 100;

        String promptText = String.format(
            """
            Analyse les données financières de la franchise %s pour %s %d:
            - Chiffre d'affaires: %,.2f TND
            - Charges totales: %,.2f TND
            - Résultat net: %,.2f TND
            - Objectif revenu: %,.2f TND
            - Variance: %+.1f%%

            Détecte les anomalies ou risques.
            Réponds UNIQUEMENT avec un objet JSON valide respectant ce format, sans texte autour (pas de markdown ```json) :
            {
              "type_alerte": "Type de l'alerte (ex: Risque Financier)",
              "message": "Description détaillée du problème et recommandation (max 500 caractères)",
              "score_gravite": 8.5
            }
            """,
            franchiseName, "Février", year, totalRecettes, totalCharges, resultatNet, budgetRevenuCible, variance
        );

        // Construction du corps de la requête JSON
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", promptText);
        parts.put(part);
        
        JSONObject contents = new JSONObject();
        content.put("contents", new JSONArray().put(new JSONObject().put("parts", parts)));

        // Configuration de la génération (JSON mode si supporté, sinon prompt engineering)
        // Pour gemini-2.0-flash, on peut forcer le JSON via le prompt comme fait ci-dessus

        RequestBody body = RequestBody.create(content.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Erreur API Gemini: " + response.body().string());
                return Optional.empty();
            }

            String responseBody = response.body().string();
            // Parsing de la réponse Gemini
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray candidates = jsonResponse.optJSONArray("candidates");
            
            if (candidates != null && candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject contentResp = candidate.getJSONObject("content");
                JSONArray partsResp = contentResp.getJSONArray("parts");
                String textResp = partsResp.getJSONObject(0).getString("text");

                // Nettoyage du markdown éventuel (```json ... ```)
                textResp = textResp.replaceAll("```json", "").replaceAll("```", "").trim();

                // Conversion en objet AlerteIA
                AlerteIA alerte = mapper.readValue(textResp, AlerteIA.class);
                return Optional.of(alerte);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public static Optional<String> generateAdvice(String prompt) {
        if (API_KEY == null || API_KEY.isEmpty()) return Optional.empty();

        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));
        content.put("contents", new JSONArray().put(new JSONObject().put("parts", parts)));

        RequestBody body = RequestBody.create(content.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                String text = jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                return Optional.of(text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
