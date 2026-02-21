package tn.esprit.boussole.models;

import java.time.LocalDateTime;

public class Franchise {
    private int id;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private LocalDateTime dateCreation;
    private boolean actif;
    private double soldeActuel;

    public Franchise() {}

    public Franchise(int id, String nom, String email, String telephone, String adresse,
                     LocalDateTime dateCreation, boolean actif, double soldeActuel) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.adresse = adresse;
        this.dateCreation = dateCreation;
        this.actif = actif;
        this.soldeActuel = soldeActuel;
    }

    public Franchise(String nom, String email, String telephone, String adresse) {
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.adresse = adresse;
        this.actif = true;
        this.soldeActuel = 0;
        this.dateCreation = LocalDateTime.now();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public double getSoldeActuel() { return soldeActuel; }
    public void setSoldeActuel(double soldeActuel) { this.soldeActuel = soldeActuel; }

    @Override
    public String toString() {
        return nom;
    }
}