package tn.esprit.Boussole.GUI;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardSiegeController implements Initializable {

    @FXML
    private BarChart<String, Number> barChartComparatif;

    @FXML
    private Label lblSolde;

    @FXML
    private CategoryAxis xAxisComp;

    @FXML
    private NumberAxis yAxisComp;

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
            if (btnRefresh != null) {
                btnRefresh.setOnAction(e -> chargerDonnees());
            }

            // Chargement initial
            chargerDonnees();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du DashboardSiegeController : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerDonnees() {
        progress.setVisible(true);
            if (btnRefresh != null) btnRefresh.setDisable(true);

        // DEBUG : vérifier les tables existantes
        try {
            java.sql.Connection testCnx = tn.esprit.Boussole.Utilis.MyBDConnexion.getInstance().getCnx();
            System.out.println("=== DEBUG: Connexion DB active ? " + (testCnx != null && !testCnx.isClosed()));
            java.sql.DatabaseMetaData meta = testCnx.getMetaData();
            java.sql.ResultSet tables = meta.getTables(null, null, "%transaction%", null);
            System.out.println("=== DEBUG: Tables contenant 'transaction' :");
            while (tables.next()) {
                System.out.println("    -> Table: " + tables.getString("TABLE_NAME"));
            }
            tables.close();
            // Test direct
            java.sql.Statement stmt = testCnx.createStatement();
            java.sql.ResultSet rsTest = stmt.executeQuery("SELECT COUNT(*) as cnt FROM transaction");
            if (rsTest.next()) {
                System.out.println("=== DEBUG: Nombre de lignes dans 'transaction' : " + rsTest.getInt("cnt"));
            }
            rsTest.close();
            stmt.close();
        } catch (Exception dbg) {
            System.out.println("=== DEBUG ERREUR: " + dbg.getMessage());
            // Peut-être la table s'appelle autrement ? Essayons 'transactions'
            try {
                java.sql.Connection testCnx2 = tn.esprit.Boussole.Utilis.MyBDConnexion.getInstance().getCnx();
                java.sql.Statement stmt2 = testCnx2.createStatement();
                java.sql.ResultSet rsTest2 = stmt2.executeQuery("SELECT COUNT(*) as cnt FROM transactions");
                if (rsTest2.next()) {
                    System.out.println("=== DEBUG: Nombre de lignes dans 'transactions' (PLURIEL) : " + rsTest2.getInt("cnt"));
                    System.out.println("=== !!! LA TABLE S'APPELLE 'transactions' PAS 'transaction' !!!");
                }
                rsTest2.close();
                stmt2.close();
            } catch (Exception dbg2) {
                System.out.println("=== DEBUG: 'transactions' non plus : " + dbg2.getMessage());
            }
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 1. Récupération des données financières globales (KPI)
                double soldeTotal = serviceTransaction.getSoldeTotalReseau();
                double totalRevenus = serviceTransaction.getTotalRevenus();
                double totalDepenses = serviceTransaction.getTotalDepenses();

                System.out.println("=== DEBUG Dashboard: Solde=" + soldeTotal + " Revenus=" + totalRevenus + " Depenses=" + totalDepenses);

                // 2. Réel vs Budget par mois (3 derniers mois) – TOUT le réseau
                java.sql.Connection cnx = tn.esprit.Boussole.Utilis.MyBDConnexion.getInstance().getCnx();
                Map<String, Double[]> reelVsBudget = new java.util.LinkedHashMap<>();
                String[] nomsMois = {"", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                                     "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

                for (int i = 2; i >= 0; i--) {
                    java.util.Calendar c = (java.util.Calendar) java.util.Calendar.getInstance().clone();
                    c.add(java.util.Calendar.MONTH, -i);
                    int month = c.get(java.util.Calendar.MONTH) + 1;
                    int year = c.get(java.util.Calendar.YEAR);
                    String key = nomsMois[month] + " " + year;

                    // Réel : somme de toutes les transactions du mois
                    double totalReel = 0.0;
                    String sqlReel = "SELECT COALESCE(SUM(montant), 0.0) AS total FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ?";
                    try (PreparedStatement ps = cnx.prepareStatement(sqlReel)) {
                        ps.setInt(1, month);
                        ps.setInt(2, year);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) totalReel = rs.getDouble("total");
                        }
                    }

                    // Budget : somme des montants cibles du mois
                    double totalBudget = 0.0;
                    String sqlBudget = "SELECT COALESCE(SUM(montant_cible), 0.0) AS total FROM budget_previsionnel WHERE mois = ? AND annee = ?";
                    try (PreparedStatement ps = cnx.prepareStatement(sqlBudget)) {
                        ps.setInt(1, month);
                        ps.setInt(2, year);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) totalBudget = rs.getDouble("total");
                        }
                    }

                    reelVsBudget.put(key, new Double[]{totalReel, totalBudget});
                    System.out.println("=== DEBUG: " + key + " -> Réel=" + totalReel + " Budget=" + totalBudget);
                }

                // 3. Analyse IA (Clustering)
                List<FranchiseData> rawData = serviceTransaction.getDonneesFinancieresGlobales();
                System.out.println("=== DEBUG Dashboard: rawData franchises=" + rawData.size());
                Map<Integer, List<FranchiseData>> clusters = serviceClustering.analyserDonnees(rawData, 3);
                System.out.println("=== DEBUG Dashboard: clusters=" + clusters.size());

                // Mise à jour de l'UI sur le thread JavaFX
                Platform.runLater(() -> {
                    // KPI
                    lblSolde.setText(String.format("%.2f TND", soldeTotal));
                    lblRevenus.setText(String.format("%.2f TND", totalRevenus));
                    lblDepenses.setText(String.format("%.2f TND", totalDepenses));

                    // BarChart Réel vs Budget
                    updateBarChart(reelVsBudget);

                    // ScatterChart (IA)
                    updateScatterChart(clusters);

                    progress.setVisible(false);
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
                return null;
            }
        };

        // Gestion des erreurs du thread
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                progress.setVisible(false);
                if (btnRefresh != null) btnRefresh.setDisable(false);
                System.out.println("=== DEBUG TASK FAILED: " + task.getException().getMessage());
                task.getException().printStackTrace();
                afficherMessageErreur("Erreur lors du chargement des données : " + task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    private void updateBarChart(Map<String, Double[]> data) {
        barChartComparatif.getData().clear();

        XYChart.Series<String, Number> seriesReel = new XYChart.Series<>();
        seriesReel.setName("Réel (Total Réseau)");

        XYChart.Series<String, Number> seriesBudget = new XYChart.Series<>();
        seriesBudget.setName("Budget Prévu (Réseau)");

        for (Map.Entry<String, Double[]> entry : data.entrySet()) {
            seriesReel.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
            seriesBudget.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
        }

        barChartComparatif.getData().addAll(seriesReel, seriesBudget);

        // Rotation des labels pour meilleure lisibilité
        xAxisComp.setTickLabelRotation(-20);
        xAxisComp.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
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
