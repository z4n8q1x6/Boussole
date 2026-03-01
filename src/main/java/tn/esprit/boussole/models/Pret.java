package tn.esprit.boussole.models;

public class Pret {
    private int id;
    private double montantDemande;
    private int dureeMois;
    private float taux;
    private StatutPret statut; // Maintenant reconnu car dans le même package
    private String motif;

    public Pret() {}

    public Pret(int id, double montantDemande, int dureeMois, float taux, StatutPret statut, String motif) {
        this.id = id;
        this.montantDemande = montantDemande;
        this.dureeMois = dureeMois;
        this.taux = taux;
        this.statut = statut;
        this.motif = motif;
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public double getMontantDemande() { return montantDemande; }
    public int getDureeMois() { return dureeMois; }
    public float getTaux() { return taux; }
    public StatutPret getStatut() { return statut; }
    public String getMotif() { return motif; }

    // --- SETTERS ---
    public void setId(int id) { this.id = id; }
    public void setMontantDemande(double montantDemande) { this.montantDemande = montantDemande; }
    public void setDureeMois(int dureeMois) { this.dureeMois = dureeMois; }
    public void setTaux(float taux) { this.taux = taux; }
    public void setStatut(StatutPret statut) { this.statut = statut; }
    public void setMotif(String motif) { this.motif = motif; }

    @Override
    public String toString() {
        return "Pret{id=" + id + ", motif='" + motif + "', montant=" + montantDemande + "}";
    }
}