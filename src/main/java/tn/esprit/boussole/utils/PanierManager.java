package tn.esprit.boussole.utils;  // CHANGED: package name

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.boussole.gui.franchise.PanierController;
import tn.esprit.boussole.models.Produit;

/**
 * Gestionnaire singleton du panier
 * Permet de partager le panier entre les différents contrôleurs
 */
public class PanierManager {

    // Instance unique (singleton)
    private static PanierManager instance;

    // Liste des articles dans le panier
    private ObservableList<PanierController.ProduitPanier> panierItems;

    // Référence au contrôleur du panier pour rafraîchir l'affichage
    private PanierController panierController;

    /**
     * Constructeur privé (pattern singleton)
     */
    private PanierManager() {
        panierItems = FXCollections.observableArrayList();
    }

    /**
     * Récupérer l'instance unique du PanierManager
     * @return L'instance unique
     */
    public static synchronized PanierManager getInstance() {
        if (instance == null) {
            instance = new PanierManager();
        }
        return instance;
    }

    /**
     * Récupérer la liste des articles du panier
     * @return ObservableList des articles
     */
    public ObservableList<PanierController.ProduitPanier> getPanierItems() {
        return panierItems;
    }

    /**
     * Enregistrer le contrôleur du panier pour pouvoir rafraîchir l'affichage
     * @param controller Le contrôleur du panier
     */
    public void setPanierController(PanierController controller) {
        this.panierController = controller;
    }

    /**
     * Ajouter un produit au panier
     * @param produit Le produit à ajouter
     * @param quantite La quantité à ajouter
     */
    public void ajouterProduit(Produit produit, int quantite) {
        // Vérifier si le produit existe déjà dans le panier
        for (PanierController.ProduitPanier item : panierItems) {
            if (item.getProduit().getId() == produit.getId()) {
                // Augmenter la quantité
                item.setQuantite(item.getQuantite() + quantite);
                rafraichirPanier();
                return;
            }
        }

        // Ajouter un nouvel article
        PanierController.ProduitPanier nouvelArticle = new PanierController.ProduitPanier(produit, quantite);
        panierItems.add(nouvelArticle);
        rafraichirPanier();
    }

    /**
     * Modifier la quantité d'un produit dans le panier
     * @param produitId L'ID du produit
     * @param nouvelleQuantite La nouvelle quantité
     * @return true si modifié, false si produit non trouvé
     */
    public boolean modifierQuantite(int produitId, int nouvelleQuantite) {
        for (PanierController.ProduitPanier item : panierItems) {
            if (item.getProduit().getId() == produitId) {
                if (nouvelleQuantite <= 0) {
                    // Si quantité = 0, supprimer l'article
                    panierItems.remove(item);
                } else {
                    item.setQuantite(nouvelleQuantite);
                }
                rafraichirPanier();
                return true;
            }
        }
        return false;
    }

    /**
     * Supprimer un produit du panier
     * @param produitId L'ID du produit à supprimer
     * @return true si supprimé, false si non trouvé
     */
    public boolean supprimerProduit(int produitId) {
        for (PanierController.ProduitPanier item : panierItems) {
            if (item.getProduit().getId() == produitId) {
                panierItems.remove(item);
                rafraichirPanier();
                return true;
            }
        }
        return false;
    }

    /**
     * Vider complètement le panier
     */
    public void viderPanier() {
        panierItems.clear();
        rafraichirPanier();
    }

    /**
     * Obtenir le nombre total d'articles dans le panier
     * @return Le nombre d'articles
     */
    public int getNombreArticles() {
        return panierItems.size();
    }

    /**
     * Obtenir la quantité totale de produits (somme des quantités)
     * @return La quantité totale
     */
    public int getQuantiteTotale() {
        return panierItems.stream()
                .mapToInt(PanierController.ProduitPanier::getQuantite)
                .sum();
    }

    /**
     * Calculer le montant total du panier
     * @return Le montant total
     */
    public double getTotalPanier() {
        return panierItems.stream()
                .mapToDouble(PanierController.ProduitPanier::getTotalLigne)
                .sum();
    }

    /**
     * Vérifier si le panier est vide
     * @return true si vide, false sinon
     */
    public boolean estVide() {
        return panierItems.isEmpty();
    }

    /**
     * Rafraîchir l'affichage du panier si le contrôleur est disponible
     */
    private void rafraichirPanier() {
        if (panierController != null) {
            panierController.rafraichir();
        }
    }

    /**
     * Obtenir un résumé textuel du panier
     * @return String décrivant le contenu du panier
     */
    public String getResumePanier() {
        if (panierItems.isEmpty()) {
            return "Panier vide";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Panier (").append(getQuantiteTotale()).append(" articles) :\n");

        for (PanierController.ProduitPanier item : panierItems) {
            sb.append("• ").append(item.getNom())
                    .append(" x ").append(item.getQuantite())
                    .append(" = ").append(String.format("%.2f DT", item.getTotalLigne()))
                    .append("\n");
        }

        sb.append("Total : ").append(String.format("%.2f DT", getTotalPanier()));

        return sb.toString();
    }

    /**
     * Réinitialiser le manager (pour les tests)
     */
    public void reset() {
        panierItems.clear();
        panierController = null;
    }
}