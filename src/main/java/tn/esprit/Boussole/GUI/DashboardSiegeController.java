package tn.esprit.Boussole.GUI;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import tn.esprit.Boussole.Models.FranchiseData;
import tn.esprit.Boussole.Services.ServiceClustering;
import tn.esprit.Boussole.Services.ServiceTransaction;
import tn.esprit.Boussole.Services.ServiceBilan;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardSiegeController implements Initializable {

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private Label lblSolde;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnBudgets;

    @FXML
    private Button btnBilans;

    @FXML
    private Button btnRefresh;

    @FXML
    private Label lblRevenus;

    @FXML
    private Label lblDepenses;

    @FXML
    private ProgressIndicator progress;

    // Services
    private ServiceTransaction serviceTransaction;
    private ServiceBilan serviceBilan;
    private ServiceClustering serviceClustering; // Service IA

    // IA Components
    @FXML
    private ScatterChart<Number, Number> scatterChartIA;
    @FXML
    private NumberAxis xAxisIA;
    @FXML
    private NumberAxis yAxisIA;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println(">>> DEBUG: DashboardSiegeController initialisé - Version chargée avec succès ! " + System.currentTimeMillis());
        try {
            // Initialisation des services
            serviceTransaction = new ServiceTransaction();
            serviceBilan = new ServiceBilan();
            serviceClustering = new ServiceClustering(); // Init Service IA

            // Vérification session
            int franchiseId = SessionManager.getInstance().getIdFranchise();
            /*
            if (franchiseId == 0) {
                afficherMessageErreur("Session invalide : identifiant manquant.");
                // Fermer la fenêtre ou rediriger vers Login
            }
             */

            // Initialiser la navigation
            btnBudgets.setOnAction(e -> changerPage(e, "/tn/esprit/Boussole/GUI/GestionBudgets.fxml"));
            btnBilans.setOnAction(e -> changerPage(e, "/tn/esprit/Boussole/GUI/GestionBilans.fxml"));
            btnRefresh.setOnAction(e -> chargerDonnees());

            // Chargement initial
            chargerDonnees();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du DashboardSiegeController : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerDonnees() {
        progress.setVisible(true);
        btnRefresh.setDisable(true);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 1. Récupération des données financières globales
                double soldeTotal = serviceTransaction.getSoldeTotalReseau();
                double totalRevenus = serviceTransaction.getTotalRevenus();
                double totalDepenses = serviceTransaction.getTotalDepenses();

                // 2. Récupération données Graphique (3 derniers mois)
                // Ex: 10 = Octobre (juste pour exemple, idéalement dynamique)
                Map<String, Double[]> graphData = serviceBilan.getDonneesGraphique(3);

                // 3. Analyse IA (Clustering)
                List<FranchiseData> rawData = serviceTransaction.getDonneesFinancieresGlobales();
                Map<Integer, List<FranchiseData>> clusters = serviceClustering.analyserDonnees(rawData, 3);

                // Mise à jour de l'UI sur le thread JavaFX
                Platform.runLater(() -> {
                    // KPI
                    lblSolde.setText(String.format("%.2f TND", soldeTotal));
                    lblRevenus.setText(String.format("%.2f TND", totalRevenus));
                    lblDepenses.setText(String.format("%.2f TND", totalDepenses));

                    // BarChart
                    updateBarChart(graphData);

                    // ScatterChart (IA)
                    updateScatterChart(clusters);

                    progress.setVisible(false);
                    btnRefresh.setDisable(false);
                });
                return null;
            }
        };

        // Gestion des erreurs du thread
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                progress.setVisible(false);
                btnRefresh.setDisable(false);
                afficherMessageErreur("Erreur lors du chargement des données : " + task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    private void updateBarChart(Map<String, Double[]> data) {
        barChart.getData().clear();

        XYChart.Series<String, Number> seriesRevenus = new XYChart.Series<>();
        seriesRevenus.setName("Revenus");

        XYChart.Series<String, Number> seriesDepenses = new XYChart.Series<>();
        seriesDepenses.setName("Dépenses");

        for (Map.Entry<String, Double[]> entry : data.entrySet()) {
            seriesRevenus.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
            seriesDepenses.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
        }

        barChart.getData().addAll(seriesRevenus, seriesDepenses);
    }

    private void updateScatterChart(Map<Integer, List<FranchiseData>> clusters) {
        scatterChartIA.getData().clear();

        // Définition des couleurs et noms des clusters
        // Note: L'ordre des clusters (0, 1, 2) dépend de l'algorithme, donc on doit analyser les centres
        // ou pour simplifier ici :
        // On considère que le cluster avec les Recettes les plus hautes est "Performant"
        // Celui avec Dépenses > Recettes est "Risqué"

        // Simple mapping direct pour l'instant, on pourra affiner l'intelligence
        String[] nomsClusters = {"Groupe A", "Groupe B", "Groupe C"};
        String[] styles = {
            "-fx-body-color: #00E5CC;", // Vert/Cyan (Performant ?)
            "-fx-body-color: #FFA726;", // Orange (Neutre)
            "-fx-body-color: #EF5350;"  // Rouge (Risque)
        };

        int index = 0;
        for (Map.Entry<Integer, List<FranchiseData>> entry : clusters.entrySet()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(nomsClusters[index % 3]); // Nom générique

            for (FranchiseData franchise : entry.getValue()) {
                XYChart.Data<Number, Number> point = new XYChart.Data<>(franchise.getRecettes(), franchise.getDepenses());
                series.getData().add(point);

                // Installation du Tooltip APRES l'ajout au graph (node généré)
                point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        Tooltip tooltip = new Tooltip(franchise.getLabel() + "\nRec: " + franchise.getRecettes() + "\nDép: " + franchise.getDepenses());
                        Tooltip.install(newNode, tooltip);
                        // Appliquer style couleur spécifique si besoin
                        // newNode.setStyle(styles[finalIndex % 3]);
                        // Note: ScatterChart gère les couleurs par série par défaut,
                        // pour forcer des couleurs par cluster "sémantique", il faudrait trier les clusters avant.
                    }
                });
            }
            scatterChartIA.getData().add(series);
            index++;
        }
    }

    private void afficherMessageErreur(String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    /**
     * Méthode utilitaire pour changer de page (navigation entre écrans FXML).
     * À réutiliser dans les autres contrôleurs (GestionBudgetsController, GestionBilansController).
     */
    private void changerPage(ActionEvent event, String fxmlPath) {
        try {
            // Charger le nouveau FXML
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Erreur : fichier FXML non trouvé : " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);

            // Charger la feuille CSS
            try {
                URL cssUrl = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css");
                if (cssUrl != null) {
                    String css = cssUrl.toExternalForm();
                    scene.getStylesheets().add(css);
                }
            } catch (Exception e) {
                System.out.println("Attention : CSS non chargée (" + e.getMessage() + ")");
            }

            // Obtenir la stage actuelle depuis le bouton source et changer la scène
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Boussole - " + fxmlPath);
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du FXML : " + e.getMessage());
            // print first stack trace elements to help debugging
            for (StackTraceElement el : e.getStackTrace()) {
                System.err.println(el.toString());
            }
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors du changement de page : " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) {
                System.err.println(el.toString());
            }
        }
    }
}
