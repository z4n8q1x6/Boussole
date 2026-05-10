package tn.esprit.boussole.api.models;

/**
 * Modèle pour les coordonnées géographiques
 */
public class Coordonnees {

    private double latitude;
    private double longitude;
    private double altitude; // optionnelle

    public Coordonnees() {}

    public Coordonnees(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = 0;
    }

    public Coordonnees(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    // Getters et Setters
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public String toString() {
        return latitude + "," + longitude;
    }

    /**
     * Formater les coordonnées pour affichage
     */
    public String formatDMS() {
        return latitudeToDMS(latitude) + " " + longitudeToDMS(longitude);
    }

    private String latitudeToDMS(double lat) {
        String direction = lat >= 0 ? "N" : "S";
        lat = Math.abs(lat);
        int degres = (int) lat;
        int minutes = (int) ((lat - degres) * 60);
        double secondes = ((lat - degres) * 60 - minutes) * 60;
        return String.format("%d°%d'%.1f\"%s", degres, minutes, secondes, direction);
    }

    private String longitudeToDMS(double lon) {
        String direction = lon >= 0 ? "E" : "O";
        lon = Math.abs(lon);
        int degres = (int) lon;
        int minutes = (int) ((lon - degres) * 60);
        double secondes = ((lon - degres) * 60 - minutes) * 60;
        return String.format("%d°%d'%.1f\"%s", degres, minutes, secondes, direction);
    }
}