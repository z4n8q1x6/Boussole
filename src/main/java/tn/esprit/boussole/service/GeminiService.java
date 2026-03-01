package tn.esprit.boussole.service; // On garde le package de tes amis

import java.io.InputStream;
import java.util.Properties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class GeminiService {


    private static String API_KEY = loadApiKey();
    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";


    private static String loadApiKey() {
        Properties prop = new Properties();
        try (InputStream input = GeminiService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Désolé, impossible de trouver config.properties");
                return "";
            }
            prop.load(input);
            return prop.getProperty("gemini.api.key");
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public static String getGeminiResponse(String userPrompt) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            return "Erreur : Clé API manquante dans config.properties";
        }

        try {
            String fullUrl = URL_BASE + API_KEY;

            JSONObject jsonRequest = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject().put("text", "Réponds brièvement en tant qu'assistant Boussole : " + userPrompt);
            contents.put(new JSONObject().put("parts", new JSONArray().put(part)));
            jsonRequest.put("contents", contents);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Désolé, erreur API (Code " + response.statusCode() + ").";
            }

            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "Connexion impossible avec l'IA.";
        }
    }
}