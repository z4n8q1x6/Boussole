package tn.esprit.chargesdepenses.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class CurrencyService {
    // Remplacez par votre clé obtenue sur exchangerate-api.com
    private static final String API_KEY = "5ce8f7bd74ca160456f40705";
    private static final String URL_API = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/TND";

    public double getTauxTndVersEur() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_API))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                // On récupère le taux spécifique pour l'Euro
                return json.getJSONObject("conversion_rates").getDouble("EUR");
            }
        } catch (Exception e) {
            System.err.println("Erreur conversion : " + e.getMessage());
        }
        return 0.30; // Valeur de secours (fallback) si l'API est hors ligne
    }
}