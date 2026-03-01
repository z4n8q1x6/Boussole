package tn.esprit.boussole.api.models;

/**
 * Modèle pour une requête d'envoi d'email
 */
public class EmailRequest {

    private String destinataire;
    private String sujet;
    private String contenuHTML;
    private String contenuTexte; // Version texte simple (optionnelle)

    public EmailRequest() {}

    public EmailRequest(String destinataire, String sujet, String contenuHTML) {
        this.destinataire = destinataire;
        this.sujet = sujet;
        this.contenuHTML = contenuHTML;
    }

    // Getters et Setters
    public String getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(String destinataire) {
        this.destinataire = destinataire;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getContenuHTML() {
        return contenuHTML;
    }

    public void setContenuHTML(String contenuHTML) {
        this.contenuHTML = contenuHTML;
    }

    public String getContenuTexte() {
        return contenuTexte;
    }

    public void setContenuTexte(String contenuTexte) {
        this.contenuTexte = contenuTexte;
    }
}