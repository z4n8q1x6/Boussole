package tn.esprit.boussole.api.models;

/**
 * Modèle pour une localisation complète avec adresse
 */
public class Localisation {

    private Coordonnees coordonnees;
    private String adresseComplete;
    private String rue;
    private String ville;
    private String codePostal;
    private String pays;
    private String region;
    private String osmType; // Type OpenStreetMap
    private long osmId;     // ID OpenStreetMap

    public Localisation() {}

    public Localisation(Coordonnees coordonnees, String adresseComplete) {
        this.coordonnees = coordonnees;
        this.adresseComplete = adresseComplete;
    }

    // Getters et Setters
    public Coordonnees getCoordonnees() {
        return coordonnees;
    }

    public void setCoordonnees(Coordonnees coordonnees) {
        this.coordonnees = coordonnees;
    }

    public String getAdresseComplete() {
        return adresseComplete;
    }

    public void setAdresseComplete(String adresseComplete) {
        this.adresseComplete = adresseComplete;
    }

    public String getRue() {
        return rue;
    }

    public void setRue(String rue) {
        this.rue = rue;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public long getOsmId() {
        return osmId;
    }

    public void setOsmId(long osmId) {
        this.osmId = osmId;
    }

    @Override
    public String toString() {
        return adresseComplete;
    }
}