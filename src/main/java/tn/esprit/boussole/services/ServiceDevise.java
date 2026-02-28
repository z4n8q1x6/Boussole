package tn.esprit.boussole.services;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServiceDevise {

    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/TND";

    public double convertir(double montantTND, String codeDeviseCible) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parsing JSON
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("rates")) {
                    JSONObject rates = jsonResponse.getJSONObject("rates");
                    if (rates.has(codeDeviseCible)) {
                        double taux = rates.getDouble(codeDeviseCible);
                        return montantTND * taux;
                    }
                }
            } else {
                System.err.println("Erreur API Devise : Code " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la conversion : " + e.getMessage());
        }
        return 0.0; // Retourne 0 en cas d'erreur
    }
}

