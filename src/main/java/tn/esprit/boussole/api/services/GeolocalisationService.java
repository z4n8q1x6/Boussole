package tn.esprit.boussole.api.services;

import tn.esprit.boussole.models.franchise;  // CHANGED: lowercase 'f'
import tn.esprit.boussole.api.clients.GeoClient;
import tn.esprit.boussole.api.models.Localisation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service métier pour la géolocalisation
 */
public class GeolocalisationService {

    private final GeoClient geoClient;
    private boolean testMode = false;

    // Cache des localisations pour éviter de surcharger l'API
    private final Map<Integer, Localisation> cache = new HashMap<>();

    public GeolocalisationService() {
        this.geoClient = GeoClient.getInstance();
    }

    public GeolocalisationService(boolean testMode) {
        this.testMode = testMode;
        if (testMode) {
            this.geoClient = GeoClient.getTestInstance();
        } else {
            this.geoClient = GeoClient.getInstance();
        }
    }

    /**
     * Obtenir la localisation d'une franchise
     */
    public Localisation getLocalisationFranchise(franchise franchise) throws IOException {  // CHANGED: lowercase 'f'
        // Vérifier le cache
        if (cache.containsKey(franchise.getId())) {
            return cache.get(franchise.getId());
        }

        // Construire l'adresse complète
        String adresse = construireAdresse(franchise);

        // Géocoder
        Localisation loc = geoClient.geocoder(adresse);

        // Mettre en cache
        if (loc != null) {
            cache.put(franchise.getId(), loc);
        }

        return loc;
    }

    /**
     * Obtenir les localisations de toutes les franchises
     */
    public List<Localisation> getLocalisationsToutesFranchises(List<franchise> franchises) {  // CHANGED: lowercase 'f'
        List<Localisation> localisations = new ArrayList<>();

        for (franchise f : franchises) {  // CHANGED: lowercase 'f'
            try {
                Localisation loc = getLocalisationFranchise(f);
                if (loc != null) {
                    localisations.add(loc);
                }
                // Pause pour respecter les limites de l'API (1 requête par seconde)
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("Erreur pour la franchise " + f.getId() + ": " + e.getMessage());
            }
        }

        return localisations;
    }

    /**
     * Construire l'adresse complète à partir des données franchise
     */
    private String construireAdresse(franchise franchise) {  // CHANGED: lowercase 'f'
        StringBuilder sb = new StringBuilder();

        // Utiliser l'adresse complète si disponible
        if (franchise.getAdresse() != null && !franchise.getAdresse().isEmpty()) {
            sb.append(franchise.getAdresse());
        }

        // Ajouter le pays par défaut si l'adresse est trop courte
        if (sb.length() < 10) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Tunisie");
        }

        return sb.toString();
    }

    /**
     * Calculer la distance entre deux franchises
     */
    public double distanceEntreFranchises(franchise f1, franchise f2) throws IOException {  // CHANGED: lowercase 'f'
        Localisation loc1 = getLocalisationFranchise(f1);
        Localisation loc2 = getLocalisationFranchise(f2);

        if (loc1 == null || loc2 == null) {
            return -1;
        }

        return geoClient.calculerDistance(loc1, loc2);
    }

    /**
     * Trouver les franchises proches d'un point
     */
    public List<FranchiseProche> trouverFranchisesProches(List<franchise> franchises,  // CHANGED: lowercase 'f'
                                                          double lat, double lon,
                                                          double rayonKm) throws IOException {
        List<FranchiseProche> resultats = new ArrayList<>();

        // Créer une localisation pour le point de référence
        Localisation pointRef = geoClient.geocoderInverse(lat, lon);

        for (franchise f : franchises) {  // CHANGED: lowercase 'f'
            Localisation loc = getLocalisationFranchise(f);
            if (loc != null) {
                double distance = geoClient.calculerDistance(pointRef, loc);
                if (distance <= rayonKm) {
                    resultats.add(new FranchiseProche(f, distance));
                }
            }
        }

        // Trier par distance
        resultats.sort((a, b) -> Double.compare(a.distance, b.distance));

        return resultats;
    }

    /**
     * Générer une carte HTML pour afficher les franchises
     */
    public String genererCarteHTML(List<franchise> franchises) throws IOException {  // CHANGED: lowercase 'f'
        StringBuilder markers = new StringBuilder();
        markers.append("[");

        for (int i = 0; i < franchises.size(); i++) {
            franchise f = franchises.get(i);  // CHANGED: lowercase 'f'
            Localisation loc = getLocalisationFranchise(f);

            if (loc != null && loc.getCoordonnees() != null) {
                if (i > 0) markers.append(",");

                // Extraire une ville approximative depuis l'adresse
                String ville = extraireVille(f.getAdresse());

                markers.append("{")
                        .append("\"lat\":").append(loc.getCoordonnees().getLatitude()).append(",")
                        .append("\"lon\":").append(loc.getCoordonnees().getLongitude()).append(",")
                        .append("\"id\":").append(f.getId()).append(",")
                        .append("\"nom\":\"").append(escapeJson(f.getNom())).append("\",")
                        .append("\"adresse\":\"").append(escapeJson(f.getAdresse())).append("\",")
                        .append("\"ville\":\"").append(escapeJson(ville)).append("\"")
                        .append("}");
            }
        }

        markers.append("]");

        return genererHTMLCarte(markers.toString());
    }

    /**
     * Extraire une ville depuis l'adresse (méthode simplifiée)
     */
    private String extraireVille(String adresse) {
        if (adresse == null || adresse.isEmpty()) {
            return "";
        }
        // Prendre la première partie avant la virgule
        String[] parties = adresse.split(",");
        return parties[0].trim();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String genererHTMLCarte(String markers) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "body { margin: 0; padding: 0; }" +
                "#map { width: 100%; height: 100vh; }" +
                ".leaflet-popup-content { font-family: Arial, sans-serif; }" +
                ".franchise-nom { font-weight: bold; color: #0EA5E9; }" +
                ".franchise-adresse { font-size: 12px; color: #64748B; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map = L.map('map').setView([36.8065, 10.1815], 8);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "    attribution: '© OpenStreetMap contributors'" +
                "}).addTo(map);" +
                "var markers = " + markers + ";" +
                "markers.forEach(function(m) {" +
                "    var marker = L.marker([m.lat, m.lon]).addTo(map);" +
                "    marker.bindPopup(" +
                "        '<div class=\"franchise-nom\">' + m.nom + '</div>' +" +
                "        '<div class=\"franchise-adresse\">' + m.adresse + '</div>' +" +
                "        '<div>' + m.ville + '</div>'" +
                "    );" +
                "});" +
                "if (markers.length === 1) {" +
                "    map.setView([markers[0].lat, markers[0].lon], 13);" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
    }

    /**
     * Tester la connexion au service de géolocalisation
     */
    public boolean testConnexion() {
        try {
            if (testMode) {
                System.out.println("🗺️ [MODE TEST] Service de géolocalisation simulé");
                return true;
            }

            Localisation test = geoClient.geocoder("Tunis, Tunisie");
            boolean ok = test != null && test.getCoordonnees() != null;

            if (ok) {
                System.out.println("✅ Service de géolocalisation OK");
            } else {
                System.out.println("❌ Service de géolocalisation indisponible");
            }

            return ok;

        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion au service de géolocalisation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vider le cache
     */
    public void viderCache() {
        cache.clear();
        System.out.println("🗺️ Cache vidé");
    }

    /**
     * Classe interne pour les franchises proches
     */
    public static class FranchiseProche {
        private franchise franchise;  // CHANGED: lowercase 'f'
        private double distance;

        public FranchiseProche(franchise franchise, double distance) {  // CHANGED: lowercase 'f'
            this.franchise = franchise;
            this.distance = distance;
        }

        public franchise getFranchise() { return franchise; }  // CHANGED: lowercase 'f'
        public double getDistance() { return distance; }
        public String getDistanceFormatee() {
            if (distance < 1) {
                return String.format("%.0f m", distance * 1000);
            }
            return String.format("%.2f km", distance);
        }
    }
}