package tn.esprit.boussole.gui.siege;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import tn.esprit.boussole.api.services.GeolocalisationService;
import tn.esprit.boussole.models.Franchise;
import tn.esprit.boussole.services.FranchiseService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class CarteFranchisesController implements Initializable {

    @FXML private WebView webView;
    @FXML private VBox loadingPane;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private ComboBox<String> franchiseFilter;
    @FXML private Button refreshButton;
    @FXML private Label countLabel;

    private FranchiseService franchiseService;
    private GeolocalisationService geoService;
    private List<Franchise> toutesFranchises;

    private boolean testMode = true; // Mettre à false en production

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        franchiseService = new FranchiseService();
        geoService = new GeolocalisationService(testMode);

        // Initialiser le filtre
        franchiseFilter.getItems().addAll("Toutes les franchises", "Franchises actives", "Franchises inactives");
        franchiseFilter.setValue("Toutes les franchises");
        franchiseFilter.setOnAction(e -> filtrerFranchises());

        // Charger la carte
        chargerCarte();
    }

    private void chargerCarte() {
        // Vérifier que WebView est disponible
        if (webView == null) {
            statusLabel.setText("❌ WebView non disponible - Vérifiez JavaFX Web");
            loadingPane.setVisible(false);
            return;
        }

        // Afficher le chargement
        loadingPane.setVisible(true);
        statusLabel.setText("Chargement des franchises...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Récupération des franchises...");

                // Charger toutes les franchises
                toutesFranchises = franchiseService.selectAll();

                updateMessage("Génération de la carte...");

                // Générer la carte HTML
                String html = geoService.genererCarteHTML(toutesFranchises);

                updateMessage("Carte générée avec " + toutesFranchises.size() + " franchises");

                return html;
            }
        };

        task.setOnSucceeded(event -> {
            try {
                String html = task.getValue();
                WebEngine engine = webView.getEngine();
                engine.loadContent(html);

                // Mettre à jour l'interface
                loadingPane.setVisible(false);
                statusLabel.setText("✅ " + toutesFranchises.size() + " franchises affichées");
                countLabel.setText(String.valueOf(toutesFranchises.size()));

            } catch (Exception e) {
                statusLabel.setText("❌ Erreur d'affichage: " + e.getMessage());
                loadingPane.setVisible(false);
            }
        });

        task.setOnFailed(event -> {
            statusLabel.setText("❌ Erreur: " + task.getException().getMessage());
            loadingPane.setVisible(false);
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la carte: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void filtrerFranchises() {
        String filtre = franchiseFilter.getValue();

        if (toutesFranchises == null) return;

        try {
            long count = 0;

            if ("Toutes les franchises".equals(filtre)) {
                count = toutesFranchises.size();
            } else if ("Franchises actives".equals(filtre)) {
                count = toutesFranchises.stream().filter(Franchise::isActif).count();
            } else if ("Franchises inactives".equals(filtre)) {
                count = toutesFranchises.stream().filter(f -> !f.isActif()).count();
            }

            countLabel.setText(String.valueOf(count));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        geoService.viderCache();
        chargerCarte();
    }

    @FXML
    private void handleZoomIn() {
        if (webView != null) {
            try {
                webView.getEngine().executeScript("map.zoomIn();");
            } catch (Exception e) {
                // Ignorer
            }
        }
    }

    @FXML
    private void handleZoomOut() {
        if (webView != null) {
            try {
                webView.getEngine().executeScript("map.zoomOut();");
            } catch (Exception e) {
                // Ignorer
            }
        }
    }

    @FXML
    private void handleCentrerTunis() {
        if (webView != null) {
            try {
                webView.getEngine().executeScript("map.setView([36.8065, 10.1815], 8);");
            } catch (Exception e) {
                // Ignorer
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1E293B;");
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white;");
        }

        alert.showAndWait();
    }
}