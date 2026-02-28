package tn.esprit.boussole.models;

public class bilan {

    private int id;
    private int mois;
    private int annee;
    private double totalRecettes;
    private double totalCharges;
    private double resultatNet;
    private int franchiseId;

    // Constructeur vide
    public bilan() {
    }

    // Constructeur complet
    public bilan(int id, int mois, int annee, double totalRecettes, double totalCharges, double resultatNet, int franchiseId) {
        this.id = id;
        this.mois = mois;
        this.annee = annee;
        this.totalRecettes = totalRecettes;
        this.totalCharges = totalCharges;
        this.resultatNet = resultatNet;
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

    public double getTotalRecettes() {
        return totalRecettes;
    }

    public void setTotalRecettes(double totalRecettes) {
        this.totalRecettes = totalRecettes;
    }

    public double getTotalCharges() {
        return totalCharges;
    }

    public void setTotalCharges(double totalCharges) {
        this.totalCharges = totalCharges;
    }

    public double getResultatNet() {
        return resultatNet;
    }

    public void setResultatNet(double resultatNet) {
        this.resultatNet = resultatNet;
    }

    public int getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(int franchiseId) {
        this.franchiseId = franchiseId;
    }

    @Override
    public String toString() {
        return "bilan{" +
                "id=" + id +
                ", mois=" + mois +
                ", annee=" + annee +
                ", totalRecettes=" + totalRecettes +
                ", totalCharges=" + totalCharges +
                ", resultatNet=" + resultatNet +
                ", franchiseId=" + franchiseId +
                '}';
    }
}
