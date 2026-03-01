package tn.esprit.boussole.api.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tn.esprit.boussole.api.models.Coordonnees;
import tn.esprit.boussole.api.models.Localisation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client pour l'API de géolocalisation OpenStreetMap (Nominatim)
 */
public class GeoClient {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "BoussoleApp/1.0 (contact@boussole.tn)";

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    private static GeoClient instance;

    /**
     * Constructeur privé (singleton)
     */
    private GeoClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build();
                    return chain.proceed(request);
                })
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Obtenir l'instance unique
     */
    public static synchronized GeoClient getInstance() {
        if (instance == null) {
            instance = new GeoClient();
        }
        return instance;
    }

    /**
     * Géocoder une adresse (texte -> coordonnées)
     * @param adresse L'adresse à géocoder
     * @return Localisation avec coordonnées
     */
    public Localisation geocoder(String adresse) throws IOException {
        String url = NOMINATIM_URL + "/search?q=" + adresse.replace(" ", "+") +
                "&format=json&addressdetails=1&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Erreur API: " + response.code());
            }

            String jsonResponse = response.body().string();
            JsonNode root = mapper.readTree(jsonResponse);

            if (root.isArray() && root.size() > 0) {
                return jsonToLocalisation(root.get(0));
            }
        }

        return null;
    }

    /**
     * Géocoder avec plusieurs résultats
     */
    public List<Localisation> geocoderMultiple(String adresse, int limit) throws IOException {
        String url = NOMINATIM_URL + "/search?q=" + adresse.replace(" ", "+") +
                "&format=json&addressdetails=1&limit=" + limit;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        List<Localisation> resultats = new ArrayList<>();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Erreur API: " + response.code());
            }

            String jsonResponse = response.body().string();
            JsonNode root = mapper.readTree(jsonResponse);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    resultats.add(jsonToLocalisation(node));
                }
            }
        }

        return resultats;
    }

    /**
     * Géocodage inversé (coordonnées -> adresse)
     * @param lat Latitude
     * @param lon Longitude
     * @return Localisation avec adresse
     */
    public Localisation geocoderInverse(double lat, double lon) throws IOException {
        String url = NOMINATIM_URL + "/reverse?lat=" + lat + "&lon=" + lon +
                "&format=json&addressdetails=1";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Erreur API: " + response.code());
            }

            String jsonResponse = response.body().string();
            JsonNode root = mapper.readTree(jsonResponse);

            return jsonToLocalisation(root);
        }
    }

    /**
     * Convertir JSON en objet Localisation
     */
    private Localisation jsonToLocalisation(JsonNode node) {
        Localisation loc = new Localisation();

        // Coordonnées
        if (node.has("lat") && node.has("lon")) {
            double lat = Double.parseDouble(node.get("lat").asText());
            double lon = Double.parseDouble(node.get("lon").asText());
            loc.setCoordonnees(new Coordonnees(lat, lon));
        }

        // Adresse complète
        if (node.has("display_name")) {
            loc.setAdresseComplete(node.get("display_name").asText());
        }

        // Détails de l'adresse
        if (node.has("address")) {
            JsonNode address = node.get("address");

            if (address.has("road")) loc.setRue(address.get("road").asText());
            if (address.has("city")) loc.setVille(address.get("city").asText());
            if (address.has("town")) loc.setVille(address.get("town").asText());
            if (address.has("village")) loc.setVille(address.get("village").asText());
            if (address.has("postcode")) loc.setCodePostal(address.get("postcode").asText());
            if (address.has("country")) loc.setPays(address.get("country").asText());
            if (address.has("state")) loc.setRegion(address.get("state").asText());
        }

        // Métadonnées OpenStreetMap
        if (node.has("osm_type")) loc.setOsmType(node.get("osm_type").asText());
        if (node.has("osm_id")) loc.setOsmId(node.get("osm_id").asLong());

        return loc;
    }

    /**
     * Calculer la distance entre deux points (formule de Haversine)
     * @param loc1 Première localisation
     * @param loc2 Deuxième localisation
     * @return Distance en kilomètres
     */
    public double calculerDistance(Localisation loc1, Localisation loc2) {
        if (loc1.getCoordonnees() == null || loc2.getCoordonnees() == null) {
            return -1;
        }

        double lat1 = loc1.getCoordonnees().getLatitude();
        double lon1 = loc1.getCoordonnees().getLongitude();
        double lat2 = loc2.getCoordonnees().getLatitude();
        double lon2 = loc2.getCoordonnees().getLongitude();

        double R = 6371; // Rayon de la Terre en km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Vérifier si le service est disponible
     */
    public boolean testConnexion() {
        try {
            String url = NOMINATIM_URL + "/status.php?format=json";
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            System.err.println("❌ Service de géolocalisation indisponible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtenir une instance de test (simulation)
     */
    public static GeoClient getTestInstance() {
        return new GeoClient() {
            @Override
            public Localisation geocoder(String adresse) {
                System.out.println("🗺️ [TEST] Géocodage de: " + adresse);
                Localisation loc = new Localisation();
                loc.setAdresseComplete(adresse + ", Tunisie");

                // Coordonnées approximatives pour les grandes villes tunisiennes
                if (adresse.toLowerCase().contains("tunis")) {
                    loc.setCoordonnees(new Coordonnees(36.8065, 10.1815));
                    loc.setVille("Tunis");
                } else if (adresse.toLowerCase().contains("sfax")) {
                    loc.setCoordonnees(new Coordonnees(34.7478, 10.7662));
                    loc.setVille("Sfax");
                } else if (adresse.toLowerCase().contains("sousse")) {
                    loc.setCoordonnees(new Coordonnees(35.8256, 10.6411));
                    loc.setVille("Sousse");
                } else {
                    loc.setCoordonnees(new Coordonnees(36.8065, 10.1815)); // Tunis par défaut
                }

                return loc;
            }

            @Override
            public boolean testConnexion() {
                return true;
            }
        };
    }
}