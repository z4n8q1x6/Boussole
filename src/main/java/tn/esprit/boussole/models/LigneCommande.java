package tn.esprit.boussole.models;

public class LigneCommande {

    private int id;
    private int quantite;
    private double prix_unitaire;
    private int commande_id;
    private int produit_id;

    // Champs supplémentaires pour l'affichage (non persistants)
    private String produitNom;
    private String produitReference;
    private String commandeDate;

    // ==================== CONSTRUCTEURS ====================

    public LigneCommande() {
    }

    /**
     * Constructeur sans id (pour l'insertion)
     */
    public LigneCommande(int quantite, double prix_unitaire, int commande_id, int produit_id) {
        this.quantite = quantite;
        this.prix_unitaire = prix_unitaire;
        this.commande_id = commande_id;
        this.produit_id = produit_id;
    }

    /**
     * Constructeur avec id
     */
    public LigneCommande(int id, int quantite, double prix_unitaire, int commande_id, int produit_id) {
        this.id = id;
        this.quantite = quantite;
        this.prix_unitaire = prix_unitaire;
        this.commande_id = commande_id;
        this.produit_id = produit_id;
    }

    // ==================== GETTERS & SETTERS ====================

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

    // ==================== GETTERS & SETTERS POUR L'AFFICHAGE ====================

    public String getProduitNom() {
        return produitNom;
    }

    public void setProduitNom(String produitNom) {
        this.produitNom = produitNom;
    }

    public String getProduitReference() {
        return produitReference;
    }

    public void setProduitReference(String produitReference) {
        this.produitReference = produitReference;
    }

    public String getCommandeDate() {
        return commandeDate;
    }

    public void setCommandeDate(String commandeDate) {
        this.commandeDate = commandeDate;
    }

    // ==================== MÉTHODES MÉTIER ====================

    /**
     * Calcule le total de la ligne (quantité * prix unitaire)
     */
    public double getTotal() {
        return quantite * prix_unitaire;
    }

    /**
     * Retourne le total formaté avec le sigle DT
     */
    public String getTotalFormate() {
        return String.format("%.2f DT", getTotal());
    }

    /**
     * Retourne le prix unitaire formaté
     */
    public String getPrixUnitaireFormate() {
        return String.format("%.2f DT", prix_unitaire);
    }

    /**
     * Vérifie si la ligne est valide (quantité > 0, prix > 0)
     */
    public boolean isValid() {
        return quantite > 0 && prix_unitaire > 0 && commande_id > 0 && produit_id > 0;
    }

    /**
     * Vérifie si la quantité est suffisante par rapport au stock
     */
    public boolean quantiteDisponible(int stockDispo) {
        return quantite <= stockDispo;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Retourne une représentation courte de la ligne
     */
    @Override
    public String toString() {
        if (produitNom != null) {
            return produitNom + " x " + quantite + " = " + getTotalFormate();
        }
        return "Ligne #" + id + " - Produit ID: " + produit_id + " x " + quantite;
    }

    /**
     * Retourne une représentation détaillée
     */
    public String toDetailedString() {
        return "LigneCommande{" +
                "id=" + id +
                ", quantite=" + quantite +
                ", prix_unitaire=" + prix_unitaire +
                ", total=" + getTotal() +
                ", commande_id=" + commande_id +
                ", produit_id=" + produit_id +
                '}';
    }

    /**
     * Vérifie si deux lignes sont égales (basé sur l'ID)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LigneCommande that = (LigneCommande) obj;
        return id == that.id;
    }

    /**
     * Hash code basé sur l'ID
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // ==================== MÉTHODES DE COMPARAISON ====================

    /**
     * Compare deux lignes par produit
     */
    public int compareToByProduit(LigneCommande autre) {
        if (this.produitNom == null && autre.produitNom == null) return 0;
        if (this.produitNom == null) return 1;
        if (autre.produitNom == null) return -1;
        return this.produitNom.compareTo(autre.produitNom);
    }

    /**
     * Compare deux lignes par quantité
     */
    public int compareToByQuantite(LigneCommande autre) {
        return Integer.compare(autre.quantite, this.quantite); // Décroissant
    }

    /**
     * Compare deux lignes par total
     */
    public int compareToByTotal(LigneCommande autre) {
        return Double.compare(autre.getTotal(), this.getTotal()); // Décroissant
    }
}