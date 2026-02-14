package tn.esprit.boussole.models;

public class LigneCommande {

    private int id;
    private int quantite;
    private double prix_unitaire;
    private int commande_id;
    private int produit_id;

    public LigneCommande() {
    }

    // Constructor without id (for insert)
    public LigneCommande(int quantite, double prix_unitaire, int commande_id, int produit_id) {
        this.quantite = quantite;
        this.prix_unitaire = prix_unitaire;
        this.commande_id = commande_id;
        this.produit_id = produit_id;
    }

    // Constructor with id
    public LigneCommande(int id, int quantite, double prix_unitaire, int commande_id, int produit_id) {
        this.id = id;
        this.quantite = quantite;
        this.prix_unitaire = prix_unitaire;
        this.commande_id = commande_id;
        this.produit_id = produit_id;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public double getPrix_unitaire() {
        return prix_unitaire;
    }

    public void setPrix_unitaire(double prix_unitaire) {
        this.prix_unitaire = prix_unitaire;
    }

    public int getCommande_id() {
        return commande_id;
    }

    public void setCommande_id(int commande_id) {
        this.commande_id = commande_id;
    }

    public int getProduit_id() {
        return produit_id;
    }

    public void setProduit_id(int produit_id) {
        this.produit_id = produit_id;
    }

    @Override
    public String toString() {
        return "LigneCommande{" +
                "id=" + id +
                ", quantite=" + quantite +
                ", prix_unitaire=" + prix_unitaire +
                ", commande_id=" + commande_id +
                ", produit_id=" + produit_id +
                '}';
    }
}
