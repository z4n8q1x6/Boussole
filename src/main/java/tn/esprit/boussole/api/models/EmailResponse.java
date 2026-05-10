package tn.esprit.boussole.api.models;

import java.time.LocalDateTime;

/**
 * Modèle pour la réponse d'envoi d'email
 */
public class EmailResponse {

    private boolean succes;
    private String message;
    private String destinataire;
    private LocalDateTime dateEnvoi;
    private String erreur;
    private String messageId;

    public EmailResponse() {
        this.dateEnvoi = LocalDateTime.now();
    }

    // Getters et Setters
    public boolean isSucces() {
        return succes;
    }

    public void setSucces(boolean succes) {
        this.succes = succes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(String destinataire) {
        this.destinataire = destinataire;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public String getErreur() {
        return erreur;
    }

    public void setErreur(String erreur) {
        this.erreur = erreur;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return "EmailResponse{" +
                "succes=" + succes +
                ", message='" + message + '\'' +
                ", destinataire='" + destinataire + '\'' +
                ", dateEnvoi=" + dateEnvoi +
                '}';
    }
}