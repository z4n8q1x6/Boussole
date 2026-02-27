package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class GeminiService {
    private static final String API_KEY = "AIzaSyDxwuzzTfpXH5vs5vkboxKI0n4Lo3UHI-Q";

    // Changement d'URL pour correspondre exactement au modèle de votre console
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public static String getGeminiResponse(String userPrompt) {
        try {
            JSONObject jsonRequest = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject().put("text", "Réponds brièvement en tant qu'assistant Boussole : " + userPrompt);
            contents.put(new JSONObject().put("parts", new JSONArray().put(part)));
            jsonRequest.put("contents", contents);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Si le code n'est pas 200 (Succès), on affiche l'erreur dans la console pour comprendre
            if (response.statusCode() != 200) {
                System.out.println("DEBUG - Code Erreur: " + response.statusCode());
                System.out.println("DEBUG - Réponse brute: " + response.body());
                return "Désolé, une erreur serveur (Code " + response.statusCode() + ") empêche la réponse.";
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