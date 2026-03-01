package models;
import java.sql.Date;

public class Mensualite {

    private int id;
    private Date dateEcheance;
    private double montant;
    private boolean estPaye;
    private int pretId;

    // 🔹 Constructeur vide
    public Mensualite() {
    }

    // 🔹 Constructeur avec paramètres
    public Mensualite(int id, Date dateEcheance, double montant, boolean estPaye, int pretId) {
        this.id = id;
        this.dateEcheance = dateEcheance;
        this.montant = montant;
        this.estPaye = estPaye;
        this.pretId = pretId;
    }

    // 🔹 Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(Date dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public boolean isEstPaye() {
        return estPaye;
    }

    public void setEstPaye(boolean estPaye) {
        this.estPaye = estPaye;
    }

    public int getPretId() {
        return pretId;
    }

    public void setPretId(int pretId) {
        this.pretId = pretId;
    }

    @Override
    public String toString() {
        return "Mensualite{" +
                "id=" + id +
                ", dateEcheance=" + dateEcheance +
                ", montant=" + montant +
                ", estPaye=" + estPaye +
                ", pretId=" + pretId +
                '}';
    }
}