package tn.esprit.boussole.gui.siege;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import tn.esprit.boussole.services.CommandeService;
import tn.esprit.boussole.services.FranchiseService;
import tn.esprit.boussole.services.ProduitService;

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
}