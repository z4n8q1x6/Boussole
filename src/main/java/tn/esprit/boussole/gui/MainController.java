package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentArea;

    // Boutons fonctionnels (vos 3 entités)
    @FXML private Button btnDashboard;
    @FXML private Button btnProduit;
    @FXML private Button btnCommande;
    @FXML private Button btnLigneCommande;

    // Autres boutons (désactivés pour l'instant)
    @FXML private Button btnFournisseur;
    @FXML private Button btnFranchises;
    @FXML private Button btnAlerteias;
    @FXML private Button btnReclamations;
    @FXML private Button btnBilan;
    @FXML private Button btnBudget;
    @FXML private Button btnCharge;
    @FXML private Button btnMensualite;
    @FXML private Button btnPret;
    @FXML private Button btnTransaction;
    @FXML private Button btnUtilisateur;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Ajouter des tooltips aux boutons désactivés
        ajouterTooltips();

        // Afficher le tableau de bord par défaut
        showDashboard();
    }

    private void ajouterTooltips() {
        btnFournisseur.setTooltip(new Tooltip("Bientôt disponible"));
        btnFranchises.setTooltip(new Tooltip("Bientôt disponible"));
        btnAlerteias.setTooltip(new Tooltip("Bientôt disponible"));
        btnReclamations.setTooltip(new Tooltip("Bientôt disponible"));
        btnBilan.setTooltip(new Tooltip("Bientôt disponible"));
        btnBudget.setTooltip(new Tooltip("Bientôt disponible"));
        btnCharge.setTooltip(new Tooltip("Bientôt disponible"));
        btnMensualite.setTooltip(new Tooltip("Bientôt disponible"));
        btnPret.setTooltip(new Tooltip("Bientôt disponible"));
        btnTransaction.setTooltip(new Tooltip("Bientôt disponible"));
        btnUtilisateur.setTooltip(new Tooltip("Bientôt disponible"));
    }

    @FXML
    private void showDashboard() {
        // Page d'accueil simple
        Label label = new Label("🏠 Tableau de bord");
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: #2c3e50;");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(label);
    }

    @FXML
    private void showProduits() {
        chargerVue("/tn/esprit/boussole/views/ProduitView.fxml");
    }

    @FXML
    private void showCommandes() {
        chargerVue("/tn/esprit/boussole/views/CommandeView.fxml");
    }

    @FXML
    private void showLignesCommande() {
        chargerVue("/tn/esprit/boussole/views/LigneCommandeView.fxml");
    }

    private void chargerVue(String fxmlPath) {
        try {
            Parent vue = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(vue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}