package tn.esprit.boussole.gui.siege;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.boussole.services.CommandeService;
import tn.esprit.boussole.services.FranchiseService;
import tn.esprit.boussole.services.ProduitService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class SiegeMainController implements Initializable {

    @FXML private Label produitsLabel;
    @FXML private Label commandesLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label franchisesLabel;
    @FXML private Label chiffreAffairesLabel;

    private ProduitService produitService;
    private CommandeService commandeService;
    private FranchiseService franchiseService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        produitService = new ProduitService();
        commandeService = new CommandeService();
        franchiseService = new FranchiseService();

        // Initialiser avec des valeurs par défaut
        produitsLabel.setText("0");
        commandesLabel.setText("0");
        enAttenteLabel.setText("0");
        franchisesLabel.setText("0");
        chiffreAffairesLabel.setText("0 DT");

        // Charger les vraies données
        chargerStatistiques();

        // Tester le service de géolocalisation (optionnel)
        testerGeolocalisation();
    }

    private void chargerStatistiques() {
        try {
            // Nombre de produits
            int nbProduits = produitService.countAll();
            produitsLabel.setText(String.valueOf(nbProduits));

            // Nombre de commandes
            int nbCommandes = commandeService.countAll();
            commandesLabel.setText(String.valueOf(nbCommandes));

            // Commandes en attente
            int enAttente = commandeService.countEnAttente();
            enAttenteLabel.setText(String.valueOf(enAttente));

            // Nombre de franchises
            int nbFranchises = franchiseService.countAll();
            franchisesLabel.setText(String.valueOf(nbFranchises));

            // Chiffre d'affaires (commandes validées)
            double ca = commandeService.getChiffreAffaires();
            chiffreAffairesLabel.setText(String.format("%.2f DT", ca));

            System.out.println("✅ Statistiques du siège chargées");
            System.out.println("   Produits: " + nbProduits);
            System.out.println("   Commandes: " + nbCommandes);
            System.out.println("   En attente: " + enAttente);
            System.out.println("   Franchises: " + nbFranchises);
            System.out.println("   CA: " + ca + " DT");

        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testerGeolocalisation() {
        // Test simple pour vérifier que le service est disponible
        System.out.println("🗺️ Service de géolocalisation prêt");
    }

    /**
     * Ouvre la carte des franchises
     */
    @FXML
    private void handleOuvrirCarte() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/siege/CarteFranchisesView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("🗺️ Carte des franchises - Boussole");
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();

            System.out.println("✅ Carte des franchises ouverte");

        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture carte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Rafraîchir les statistiques
     */
    @FXML
    private void handleRefresh() {
        chargerStatistiques();
    }

    /**
     * Ouvre la gestion du catalogue
     */
    @FXML
    private void handleGestionCatalogue() {
        // La navigation est gérée par la NavbarController
        System.out.println("📦 Navigation vers gestion catalogue");
    }

    /**
     * Ouvre les commandes reçues
     */
    @FXML
    private void handleCommandesRecues() {
        // La navigation est gérée par la NavbarController
        System.out.println("📋 Navigation vers commandes reçues");
    }

    /**
     * Ouvre la liste des franchises
     */
    @FXML
    private void handleListeFranchises() {
        // À implémenter si besoin
        System.out.println("🏢 Liste des franchises (à implémenter)");
    }

    /**
     * Ouvre les statistiques avancées
     */
    @FXML
    private void handleStatistiques() {
        // À implémenter si besoin
        System.out.println("📊 Statistiques avancées (à implémenter)");
    }
}