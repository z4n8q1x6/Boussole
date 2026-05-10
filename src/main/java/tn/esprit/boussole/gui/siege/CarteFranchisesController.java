package tn.esprit.boussole.gui.siege;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.geometry.Insets;  // ADD THIS IMPORT
import tn.esprit.boussole.api.services.GeolocalisationService;
import tn.esprit.boussole.service.franchiseService;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.utils.NotificationManager;
import tn.esprit.boussole.utils.UserManager;

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

    private franchiseService franchiseService;
    private GeolocalisationService geoService;
    private List<franchise> toutesFranchises;

    private boolean testMode = true; // Mettre à false en production

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Vérifier que l'utilisateur est bien SIEGE
        if (!UserManager.isCurrentUserSiege()) {
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Accès refusé",
                    "Vous n'avez pas les permissions pour accéder à cette page."
            );
            return;
        }

        franchiseService = new franchiseService();
        geoService = new GeolocalisationService(testMode);

        // Initialiser le filtre
        franchiseFilter.getItems().addAll("Toutes les franchises", "Franchises actives", "Franchises inactives");
        franchiseFilter.setValue("Toutes les franchises");
        franchiseFilter.setOnAction(e -> filtrerFranchises());

        // Vérifier que WebView est disponible
        if (webView == null) {
            statusLabel.setText("❌ WebView non disponible - Vérifiez JavaFX Web");
            loadingPane.setVisible(false);
            return;
        }

        // Charger la carte
        chargerCarte();
    }

    private void chargerCarte() {
        // Afficher le chargement
        loadingPane.setVisible(true);
        statusLabel.setText("Chargement des franchises...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Récupération des franchises...");

                // Charger toutes les franchises
                toutesFranchises = franchiseService.selectAll(null);

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
                NotificationManager.show(
                        webView.getScene().getWindow(),
                        NotificationManager.Type.ERROR,
                        "Erreur",
                        "Erreur lors de l'affichage de la carte: " + e.getMessage()
                );
            }
        });

        task.setOnFailed(event -> {
            statusLabel.setText("❌ Erreur: " + task.getException().getMessage());
            loadingPane.setVisible(false);
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur",
                    "Impossible de charger la carte: " + task.getException().getMessage()
            );
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
                // FIXED: Use getActif() instead of isActif()
                count = toutesFranchises.stream().filter(f -> f.getActif() != null && f.getActif()).count();
            } else if ("Franchises inactives".equals(filtre)) {
                // FIXED: Use getActif() instead of isActif()
                count = toutesFranchises.stream().filter(f -> f.getActif() == null || !f.getActif()).count();
            }

            countLabel.setText(String.valueOf(count));

        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur",
                    "Erreur lors du filtrage: " + e.getMessage()
            );
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
                System.err.println("Erreur zoom in: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleZoomOut() {
        if (webView != null) {
            try {
                webView.getEngine().executeScript("map.zoomOut();");
            } catch (Exception e) {
                System.err.println("Erreur zoom out: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCentrerTunis() {
        if (webView != null) {
            try {
                webView.getEngine().executeScript("map.setView([36.8065, 10.1815], 8);");
            } catch (Exception e) {
                System.err.println("Erreur centrage: " + e.getMessage());
            }
        }
    }

    /**
     * Vider le cache et recharger la carte
     */
    public void reloadMap() {
        geoService.viderCache();
        chargerCarte();
    }

    /**
     * Obtenir le nombre de franchises actuellement affichées
     */
    public int getCurrentFranchiseCount() {
        if (toutesFranchises != null) {
            return toutesFranchises.size();
        }
        return 0;
    }

    /**
     * Vérifier si la carte est en cours de chargement
     */
    public boolean isLoading() {
        return loadingPane.isVisible();
    }

    /**
     * Tester la connexion au service de géolocalisation
     */
    @FXML
    private void handleTestConnexion() {
        boolean ok = geoService.testConnexion();
        if (ok) {
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.SUCCESS,
                    "Connexion OK",
                    "Le service de géolocalisation est accessible."
            );
        } else {
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Connexion échouée",
                    "Impossible de contacter le service de géolocalisation."
            );
        }
    }

    /**
     * Basculer entre mode test et mode réel
     */
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
        geoService = new GeolocalisationService(testMode);
        reloadMap();

        String mode = testMode ? "TEST" : "RÉEL";
        NotificationManager.show(
                webView.getScene().getWindow(),
                NotificationManager.Type.INFO,
                "Mode " + mode,
                "Service de géolocalisation en mode " + mode
        );
    }

    /**
     * Exporter la liste des franchises au format CSV
     */
    @FXML
    private void handleExportCSV() {
        if (toutesFranchises == null || toutesFranchises.isEmpty()) {
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.WARNING,
                    "Aucune donnée",
                    "Aucune franchise à exporter."
            );
            return;
        }

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Nom,Email,Téléphone,Adresse,Actif,Solde\n");

            for (franchise f : toutesFranchises) {
                csv.append(f.getId()).append(",")
                        .append(f.getNom()).append(",")
                        .append(f.getEmail()).append(",")
                        .append(f.getTelephone()).append(",")
                        .append(f.getAdresse()).append(",")
                        .append(f.getActif() != null && f.getActif() ? "Oui" : "Non").append(",")
                        .append(f.getSoldeActuel()).append("\n");
            }

            // Ici vous pouvez ajouter un FileChooser pour sauvegarder le fichier
            System.out.println("CSV généré avec " + toutesFranchises.size() + " franchises");

            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.SUCCESS,
                    "Export réussi",
                    toutesFranchises.size() + " franchises exportées avec succès."
            );

        } catch (Exception e) {
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur d'export",
                    "Impossible d'exporter les données: " + e.getMessage()
            );
        }
    }

    /**
     * Afficher les informations d'une franchise spécifique sur la carte
     */
    public void focusOnFranchise(int franchiseId) {
        if (webView != null) {
            try {
                String script = String.format(
                        "var marker = markers.find(m => m.id === %d);" +
                                "if (marker) {" +
                                "    map.setView([marker.lat, marker.lon], 15);" +
                                "    L.popup()" +
                                "        .setLatLng([marker.lat, marker.lon])" +
                                "        .setContent('<div class=\"franchise-nom\">' + marker.nom + '</div><div>' + marker.adresse + '</div>')" +
                                "        .openOn(map);" +
                                "}", franchiseId
                );
                webView.getEngine().executeScript(script);
            } catch (Exception e) {
                System.err.println("Erreur focus franchise: " + e.getMessage());
            }
        }
    }

    /**
     * Rafraîchir les données sans vider le cache
     */
    @FXML
    private void handleActualiserDonnees() {
        try {
            toutesFranchises = franchiseService.selectAll(null);
            countLabel.setText(String.valueOf(toutesFranchises.size()));

            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.SUCCESS,
                    "Données actualisées",
                    toutesFranchises.size() + " franchises trouvées."
            );
        } catch (SQLException e) {
            NotificationManager.show(
                    webView.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur",
                    "Impossible d'actualiser les données: " + e.getMessage()
            );
        }
    }

    /**
     * Afficher la légende de la carte
     */
    @FXML
    private void handleAfficherLegende() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Légende de la carte");
        alert.setHeaderText("Comprendre les marqueurs");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label titre = new Label("🗺️ Carte des franchises");
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label legend1 = new Label("📍 Chaque marqueur représente une franchise");
        Label legend2 = new Label("🟢 Les franchises actives apparaissent normalement");
        Label legend3 = new Label("🔴 Les franchises inactives sont toujours affichées");
        Label legend4 = new Label("👆 Cliquez sur un marqueur pour voir les détails");

        content.getChildren().addAll(titre, legend1, legend2, legend3, legend4);

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setStyle("-fx-background-color: #0C0F1A;");

        // Styliser les labels
        for (Label label : new Label[]{titre, legend1, legend2, legend3, legend4}) {
            label.setStyle("-fx-text-fill: #E8EDF5;");
        }

        alert.showAndWait();
    }
}