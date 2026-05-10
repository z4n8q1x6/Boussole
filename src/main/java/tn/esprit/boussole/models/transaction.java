package tn.esprit.boussole.models;

import java.util.Date;

public class transaction {

    public enum Type {
        RECETTE,
        DEPENSE
    }

    private int id;

    private Date date;

    private double montant;

    private Type type;

    private String description;

    private int franchiseId;

    // Constructeur par défaut
    public transaction() {
    }

    // Constructeur sans id (pour création)
    public transaction(Date date, double montant, Type type, String description, int franchiseId) {
        this.date = date;
        this.montant = montant;
        this.type = type;
        this.description = description;
        this.franchiseId = franchiseId;
    }

    // Constructeur complet
    public transaction(int id, Date date, double montant, Type type, String description, int franchiseId) {
        this.id = id;
        this.date = date;
        this.montant = montant;
        this.type = type;
        this.description = description;
        this.franchiseId = franchiseId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(int franchiseId) {
        this.franchiseId = franchiseId;
    }
}
