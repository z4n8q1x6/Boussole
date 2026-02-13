package tn.esprit.boussole.models;

import java.time.LocalDateTime;

public class Commande {

    private int id;
    private LocalDateTime date_creation;
    private double montant_total;
    private String statut;
    private int franchise_id;
    public Commande(){
    }
    // Constructor without id (for insert)
    public Commande(LocalDateTime date_creation, double montant_total, String statut, int franchise_id) {
        this.montant_total = montant_total;
        this.statut = statut;
        this.franchise_id = franchise_id;
        this.date_creation = LocalDateTime.now();
    }

    // Constructor with id
    // testtt
    public Commande(int id, LocalDateTime date_creation, double montant_total, String statut, int franchise_id) {
        this.id = id;
        this.date_creation = date_creation;
        this.montant_total = montant_total;
        this.statut = statut;
        this.franchise_id = franchise_id;
    }

    // Getters & Setters
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
}

