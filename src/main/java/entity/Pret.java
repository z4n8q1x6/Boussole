package entity;

public class Pret {

    private int id;
    private double montantDemande;
    private int dureeMois;
    private float taux;
    private StatutPret statut;
    private String motif;


    public Pret() {
    }


    public Pret(int id, double montantDemande, int dureeMois, float taux, StatutPret statut, String motif) {
        this.id = id;
        this.montantDemande = montantDemande;
        this.dureeMois = dureeMois;
        this.taux = taux;
        this.statut = statut;
        this.motif = motif;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getMontantDemande() {
        return montantDemande;
    }

    public void setMontantDemande(double montantDemande) {
        this.montantDemande = montantDemande;
    }

    public int getDureeMois() {
        return dureeMois;
    }

    public void setDureeMois(int dureeMois) {
        this.dureeMois = dureeMois;
    }

    public float getTaux() {
        return taux;
    }

    public void setTaux(float taux) {
        this.taux = taux;
    }

    public StatutPret getStatut() {
        return statut;
    }

    public void setStatut(StatutPret statut) {
        this.statut = statut;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }


    @Override
    public String toString() {
        return "Pret{" +
                "id=" + id +
                ", montantDemande=" + montantDemande +
                ", dureeMois=" + dureeMois +
                ", taux=" + taux +
                ", statut=" + statut +
                ", motif='" + motif + '\'' +
                '}';
    }
}
