package tn.esprit.Boussole.Models;

import java.time.LocalDate;

public class Charge {

    public enum StatusValidation {
        EN_ATTENTE,
        VALIDE,
        REJETTE
    }

    public enum TypeCharge {
        CHARGES_EXPLOITATIONS,
        CHARGES_FINANCIERES,
        CHARGES_EXCEPTIONNELLES
    }

    private int id;
    private String titre;
    private Double montant;
    private LocalDate dateCharge;
    private TypeCharge type;
    private String preuveImage;
    private StatusValidation statusValidation = StatusValidation.EN_ATTENTE;
    private int franchiseId;
    private String franchiseName; // Champ pour afficher le nom

    // Constructeurs
    public Charge() {}

    public Charge(int id, String titre, Double montant, LocalDate dateCharge, TypeCharge type, String preuveImage, StatusValidation statusValidation, int franchiseId) {
        this.id = id;
        this.titre = titre;
        this.montant = montant;
        this.dateCharge = dateCharge;
        this.type = type;
        this.preuveImage = preuveImage;
        this.statusValidation = statusValidation;
        this.franchiseId = franchiseId;
    }

    public Charge(String titre, Double montant, LocalDate dateCharge, TypeCharge type, String preuveImage, int franchiseId) {
        this.titre = titre;
        this.montant = montant;
        this.dateCharge = dateCharge;
        this.type = type;
        this.preuveImage = preuveImage;
        this.franchiseId = franchiseId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public LocalDate getDateCharge() { return dateCharge; }
    public void setDateCharge(LocalDate dateCharge) { this.dateCharge = dateCharge; }

    public TypeCharge getType() { return type; }
    public void setType(TypeCharge type) { this.type = type; }

    public String getPreuveImage() { return preuveImage; }
    public void setPreuveImage(String preuveImage) { this.preuveImage = preuveImage; }

    public StatusValidation getStatusValidation() { return statusValidation; }
    public void setStatusValidation(StatusValidation statusValidation) { this.statusValidation = statusValidation; }

    public int getFranchiseId() { return franchiseId; }
    public void setFranchiseId(int franchiseId) { this.franchiseId = franchiseId; }

    public String getFranchiseName() { return franchiseName; }
    public void setFranchiseName(String franchiseName) { this.franchiseName = franchiseName; }

    @Override
    public String toString() {
        return "Charge{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", montant=" + montant +
                ", dateCharge=" + dateCharge +
                ", type=" + type +
                ", statusValidation=" + statusValidation +
                ", franchiseId=" + franchiseId +
                '}';
    }
}