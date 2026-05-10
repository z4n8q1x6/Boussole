package tn.esprit.boussole.models;

import com.fasterxml.jackson.annotation.JsonProperty;  // ADDED
import com.fasterxml.jackson.annotation.JsonIgnore;    // ADDED (optional)

public class Produit {

    private int id;
    private String nom;
    private String reference;
    private double prix_achat;
    private int stock_dispo;
    private String image;

    // ==================== CONSTRUCTEURS ====================

    public Produit() {
    }

    /**
     * Constructeur sans id (pour l'insertion)
     */
    public Produit(String nom, String reference, double prix_achat, int stock_dispo, String image) {
        this.nom = nom;
        this.reference = reference;
        this.prix_achat = prix_achat;
        this.stock_dispo = stock_dispo;
        this.image = image;
    }

    /**
     * Constructeur avec id (pour la modification)
     */
    public Produit(int id, String nom, String reference, double prix_achat, int stock_dispo, String image) {
        this.id = id;
        this.nom = nom;
        this.reference = reference;
        this.prix_achat = prix_achat;
        this.stock_dispo = stock_dispo;
        this.image = image;
    }

    // ==================== GETTERS & SETTERS ====================

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

    // ADDED: Jackson annotation for JSON serialization
    @JsonProperty("prix_achat")
    public double getPrix_achat() {
        return prix_achat;
    }

    public void setPrix_achat(double prix_achat) {
        this.prix_achat = prix_achat;
    }

    @JsonProperty("stock_dispo")
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

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Retourne le nom du produit (pour l'affichage dans les ComboBox)
     */
    @Override
    public String toString() {
        return nom;
    }

    /**
     * Retourne une représentation détaillée du produit
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public String toDetailedString() {
        return "Produit{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", reference='" + reference + '\'' +
                ", prix_achat=" + prix_achat +
                ", stock_dispo=" + stock_dispo +
                ", image='" + image + '\'' +
                '}';
    }

    /**
     * Vérifie si deux produits sont égaux (basé sur l'ID)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Produit produit = (Produit) obj;
        return id == produit.id;
    }

    /**
     * Hash code basé sur l'ID
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // ==================== MÉTHODES MÉTIER ====================

    /**
     * Vérifie si le produit est en stock
     */
    @JsonProperty("en_stock")  // ADDED: expose in JSON
    public boolean isEnStock() {
        return stock_dispo > 0;
    }

    /**
     * Vérifie si le produit est en rupture de stock
     */
    @JsonProperty("rupture")  // ADDED: expose in JSON
    public boolean isRupture() {
        return stock_dispo == 0;
    }

    /**
     * Vérifie si le stock est faible (moins de 5 unités)
     */
    @JsonProperty("stock_faible")  // ADDED: expose in JSON
    public boolean isStockFaible() {
        return stock_dispo > 0 && stock_dispo < 5;
    }

    /**
     * Vérifie si une quantité demandée est disponible
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public boolean quantiteDisponible(int quantite) {
        return stock_dispo >= quantite;
    }

    /**
     * Calcule la valeur totale du stock pour ce produit
     */
    @JsonProperty("valeur_stock")  // ADDED: expose in JSON
    public double getValeurStock() {
        return prix_achat * stock_dispo;
    }

    /**
     * Valide que le produit a des informations minimales
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public boolean isValid() {
        return nom != null && !nom.trim().isEmpty() &&
                reference != null && !reference.trim().isEmpty() &&
                prix_achat > 0;
    }

    /**
     * Valide que le nom ne contient pas de chiffres
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public boolean isNomValide() {
        return nom != null && !nom.matches(".*\\d.*");
    }

    /**
     * Valide que la référence n'est pas que des zéros
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public boolean isReferenceValide() {
        return reference != null && !reference.matches("^0+$");
    }

    // ==================== FORMATAGE POUR AFFICHAGE ====================

    /**
     * Retourne le prix formaté avec le sigle DT
     */
    @JsonProperty("prix_formate")  // ADDED: expose in JSON
    public String getPrixFormate() {
        return String.format("%.2f DT", prix_achat);
    }

    /**
     * Retourne le stock avec un libellé
     */
    @JsonProperty("stock_formate")  // ADDED: expose in JSON
    public String getStockFormate() {
        if (stock_dispo <= 0) {
            return "Rupture";
        } else if (stock_dispo < 5) {
            return "Stock faible: " + stock_dispo;
        } else {
            return "En stock: " + stock_dispo;
        }
    }

    /**
     * Retourne la couleur associée à l'état du stock
     */
    @JsonProperty("stock_color")  // ADDED: expose in JSON
    public String getStockColor() {
        if (stock_dispo <= 0) {
            return "#EF4444"; // Rouge
        } else if (stock_dispo < 5) {
            return "#F59E0B"; // Orange
        } else {
            return "#10B981"; // Vert
        }
    }

    /**
     * Retourne l'icône associée à l'état du stock
     */
    @JsonProperty("stock_icon")  // ADDED: expose in JSON
    public String getStockIcon() {
        if (stock_dispo <= 0) {
            return "🔴";
        } else if (stock_dispo < 5) {
            return "🟡";
        } else {
            return "🟢";
        }
    }

    /**
     * Raccourci pour l'affichage dans les listes
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public String getAffichageCourt() {
        return nom + " (" + reference + ")";
    }

    /**
     * Raccourci pour l'affichage dans les listes avec prix
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public String getAffichageAvecPrix() {
        return nom + " - " + getPrixFormate();
    }

    /**
     * Raccourci pour l'affichage dans le panier
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public String getAffichagePanier() {
        return nom + " x " + stock_dispo + " = " + getPrixFormate();
    }

    // ==================== MÉTHODES DE COMPARAISON ====================

    /**
     * Compare deux produits par nom
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public int compareToByNom(Produit autre) {
        return this.nom.compareTo(autre.nom);
    }

    /**
     * Compare deux produits par prix
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public int compareToByPrix(Produit autre) {
        return Double.compare(this.prix_achat, autre.prix_achat);
    }

    /**
     * Compare deux produits par stock
     */
    @JsonIgnore  // ADDED: ignore for JSON serialization
    public int compareToByStock(Produit autre) {
        return Integer.compare(autre.stock_dispo, this.stock_dispo); // Décroissant
    }
}