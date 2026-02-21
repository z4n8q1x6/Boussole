package tn.esprit.Boussole.Models;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Commande {

    private int id;
    private LocalDateTime date_creation;
    private double montant_total;
    private String statut; // EN_ATTENTE, VALIDEE, REFUSEE
    private int franchise_id;

    // ==================== CONSTRUCTEURS ====================

    public Commande() {
    }

    /**
     * Constructeur sans id (pour l'insertion)
     */
    public Commande(LocalDateTime date_creation, double montant_total, String statut, int franchise_id) {
        this.date_creation = date_creation;
        this.montant_total = montant_total;
        this.statut = statut;
        this.franchise_id = franchise_id;
    }

    /**
     * Constructeur avec id (pour la modification)
     */
    public Commande(int id, LocalDateTime date_creation, double montant_total, String statut, int franchise_id) {
        this.id = id;
        this.date_creation = date_creation;
        this.montant_total = montant_total;
        this.statut = statut;
        this.franchise_id = franchise_id;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(LocalDateTime date_creation) {
        this.date_creation = date_creation;
    }

    public double getMontant_total() {
        return montant_total;
    }

    public void setMontant_total(double montant_total) {
        this.montant_total = montant_total;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getFranchise_id() {
        return franchise_id;
    }

    public void setFranchise_id(int franchise_id) {
        this.franchise_id = franchise_id;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Retourne une représentation courte de la commande
     */
    @Override
    public String toString() {
        return "Commande #" + id + " - " + getDateFormatee();
    }

    /**
     * Retourne une représentation détaillée de la commande
     */
    public String toDetailedString() {
        return "Commande{" +
                "id=" + id +
                ", date_creation=" + date_creation +
                ", montant_total=" + montant_total +
                ", statut='" + statut + '\'' +
                ", franchise_id=" + franchise_id +
                '}';
    }

    /**
     * Vérifie si deux commandes sont égales (basé sur l'ID)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Commande commande = (Commande) obj;
        return id == commande.id;
    }

    /**
     * Hash code basé sur l'ID
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // ==================== MÉTHODES DE FORMATAGE ====================

    /**
     * Formate la date au format français
     */
    public String getDateFormatee() {
        if (date_creation == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return date_creation.format(formatter);
    }

    /**
     * Formate la date au format court (jj/mm/aaaa)
     */
    public String getDateCourte() {
        if (date_creation == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date_creation.format(formatter);
    }

    /**
     * Formate le montant total avec le sigle DT
     */
    public String getMontantFormate() {
        return String.format("%.2f DT", montant_total);
    }

    // ==================== MÉTHODES DE STATUT ====================

    /**
     * Vérifie si la commande est en attente
     */
    public boolean isEnAttente() {
        return "EN_ATTENTE".equals(statut);
    }

    /**
     * Vérifie si la commande est validée
     */
    public boolean isValidee() {
        return "VALIDEE".equals(statut);
    }

    /**
     * Vérifie si la commande est refusée
     */
    public boolean isRefusee() {
        return "REFUSEE".equals(statut);
    }

    /**
     * Retourne la couleur associée au statut (pour le CSS)
     */
    public String getStatutColor() {
        switch (statut) {
            case "EN_ATTENTE":
                return "#F59E0B"; // Orange
            case "VALIDEE":
                return "#10B981"; // Vert
            case "REFUSEE":
                return "#EF4444"; // Rouge
            default:
                return "#6B7280"; // Gris
        }
    }

    /**
     * Retourne l'icône associée au statut
     */
    public String getStatutIcon() {
        switch (statut) {
            case "EN_ATTENTE":
                return "⏳";
            case "VALIDEE":
                return "✅";
            case "REFUSEE":
                return "❌";
            default:
                return "❓";
        }
    }

    /**
     * Retourne le libellé du statut en français
     */
    public String getStatutLibelle() {
        switch (statut) {
            case "EN_ATTENTE":
                return "En attente";
            case "VALIDEE":
                return "Validée";
            case "REFUSEE":
                return "Refusée";
            default:
                return statut;
        }
    }

    // ==================== MÉTHODES DE VALIDATION ====================

    /**
     * Vérifie que la commande a des informations minimales
     */
    public boolean isValid() {
        return date_creation != null &&
                montant_total > 0 &&
                statut != null && !statut.isEmpty() &&
                franchise_id > 0;
    }

    /**
     * Vérifie que le statut est valide
     */
    public boolean isStatutValide() {
        return statut != null && (
                "EN_ATTENTE".equals(statut) ||
                        "VALIDEE".equals(statut) ||
                        "REFUSEE".equals(statut)
        );
    }

    // ==================== MÉTHODES DE COMPARAISON ====================

    /**
     * Compare deux commandes par date (récente d'abord)
     */
    public int compareToByDate(Commande autre) {
        if (this.date_creation == null && autre.date_creation == null) return 0;
        if (this.date_creation == null) return 1;
        if (autre.date_creation == null) return -1;
        return autre.date_creation.compareTo(this.date_creation); // Décroissant
    }

    /**
     * Compare deux commandes par montant
     */
    public int compareToByMontant(Commande autre) {
        return Double.compare(autre.montant_total, this.montant_total); // Décroissant
    }

    /**
     * Compare deux commandes par statut
     */
    public int compareToByStatut(Commande autre) {
        if (this.statut == null && autre.statut == null) return 0;
        if (this.statut == null) return 1;
        if (autre.statut == null) return -1;
        return this.statut.compareTo(autre.statut);
    }
}