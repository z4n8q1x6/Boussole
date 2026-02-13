package tn.esprit.boussole.models;

public class Produit {

    private int id;
    private String nom;
    private String reference;
    private double prix_achat;
    private int stock_dispo;
    private String image;

    // Constructor without id (for insert)
    public Produit(String ordinater, String aze88az4e8, double v, int i, String azeazeaze) {
        this.nom = nom;
        this.reference = reference;
        this.prix_achat = prix_achat;
        this.stock_dispo = stock_dispo;
        this.image = image;
    }

    // Constructor with id
    public Produit(int id, String nom, String reference, double prix_achat, int stock_dispo, String image) {
        this.id = id;
        this.nom = nom;
        this.reference = reference;
        this.prix_achat = prix_achat;
        this.stock_dispo = stock_dispo;
        this.image = image;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public double getPrix_achat() {
        return prix_achat;
    }

    public void setPrix_achat(double prix_achat) {
        this.prix_achat = prix_achat;
    }

    public int getStock_dispo() {
        return stock_dispo;
    }

    public void setStock_dispo(int stock_dispo) {
        this.stock_dispo = stock_dispo;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

