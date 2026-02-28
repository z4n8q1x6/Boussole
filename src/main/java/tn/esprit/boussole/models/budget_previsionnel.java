package tn.esprit.boussole.models;

public class budget_previsionnel {

    public enum TypeBudget {
        LIMITE_DEPENSE,
        OBJECTIF_REVENU
    }

    private int id;
    private int mois;
    private int annee;
    private double montantCible;
    private TypeBudget type_budget;
    private String categorie;
    private int franchiseId;

    // Constructeur vide
    public budget_previsionnel() {
    }

    // Constructeur sans id
    public budget_previsionnel(int mois, int annee, double montantCible, TypeBudget type_budget, String categorie, int franchiseId) {
        this.mois = mois;
        this.annee = annee;
        this.montantCible = montantCible;
        this.type_budget = type_budget;
        this.categorie = categorie;
        this.franchiseId = franchiseId;
    }

    // Constructeur complet
    public budget_previsionnel(int id, int mois, int annee, double montantCible, TypeBudget type_budget, String categorie, int franchiseId) {
        this.id = id;
        this.mois = mois;
        this.annee = annee;
        this.montantCible = montantCible;
        this.type_budget = type_budget;
        this.categorie = categorie;
        this.franchiseId = franchiseId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMois() {
        return mois;
    }

    public void setMois(int mois) {
        this.mois = mois;
    }

    public int getAnnee() {
        return annee;
    }

    public void setAnnee(int annee) {
        this.annee = annee;
    }

    public double getMontantCible() {
        return montantCible;
    }

    public void setMontantCible(double montantCible) {
        this.montantCible = montantCible;
    }

    public TypeBudget getType_budget() {
        return type_budget;
    }

    public void setType_budget(TypeBudget type_budget) {
        this.type_budget = type_budget;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public int getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(int franchiseId) {
        this.franchiseId = franchiseId;
    }

    @Override
    public String toString() {
        return "budget_previsionnel{" +
                "id=" + id +
                ", mois=" + mois +
                ", annee=" + annee +
                ", montantCible=" + montantCible +
                ", type_budget=" + type_budget +
                ", categorie='" + categorie + '\'' +
                ", franchiseId=" + franchiseId +
                '}';
    }
}
