package tn.esprit.boussole.gui.franchise;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import tn.esprit.boussole.services.CommandeService;
import tn.esprit.boussole.services.LigneCommandeService;
import tn.esprit.boussole.services.ProduitService;
import tn.esprit.boussole.utils.PanierManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FranchiseMainController implements Initializable {

    @FXML private Label commandesLabel;
    @FXML private Label panierLabel;
    @FXML private Label totalDepenseLabel;
    @FXML private Label produitsLabel;

    private CommandeService commandeService;
    private LigneCommandeService ligneCommandeService;
    private ProduitService produitService;

    // ID de la franchise connectée (à remplacer par la session)
    private int franchiseId = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commandeService = new CommandeService();
        ligneCommandeService = new LigneCommandeService();
        produitService = new ProduitService();

        chargerStatistiques();
    }

    private void chargerStatistiques() {
        try {
            // Nombre de commandes de la franchise
            int nbCommandes = 0;
            try {
                nbCommandes = commandeService.selectByFranchiseId(franchiseId).size();
            } catch (SQLException e) {
                System.err.println("Erreur chargement commandes: " + e.getMessage());
            }
            commandesLabel.setText(String.valueOf(nbCommandes));

            // Nombre d'articles dans le panier
            int nbPanier = PanierManager.getInstance().getQuantiteTotale();
            panierLabel.setText(String.valueOf(nbPanier));

            // Total dépensé (commandes validées)
            double totalDepense = 0;
            try {
                totalDepense = commandeService.getTotalAchatsByFranchiseId(franchiseId);
            } catch (SQLException e) {
                System.err.println("Erreur chargement total dépense: " + e.getMessage());
            }
            totalDepenseLabel.setText(String.format("%.2f DT", totalDepense));

            // Nombre de produits disponibles
            int nbProduits = 0;
            try {
                nbProduits = produitService.countAll();
            } catch (SQLException e) {
                System.err.println("Erreur chargement produits: " + e.getMessage());
            }
            produitsLabel.setText(String.valueOf(nbProduits));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}