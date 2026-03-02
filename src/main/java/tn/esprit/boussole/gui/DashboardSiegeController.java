package tn.esprit.boussole.gui;

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
import tn.esprit.boussole.models.FranchiseData;
import tn.esprit.boussole.service.ServiceClustering;
import tn.esprit.boussole.service.ServiceTransaction;
import tn.esprit.boussole.service.ServiceBilan;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import tn.esprit.boussole.utils.MyBdConnexion;
import tn.esprit.boussole.utils.ThemeManagerS;

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


    // services
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
            Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
            int franchiseId = fetchFranchiseId(prefs.get("email", ""));
            /*
            if (franchiseId == 0) {
                afficherMessageErreur("Session invalide : identifiant manquant.");
                // Fermer la fenêtre ou rediriger vers Login
            }
             */

            // Initialiser la navigation
            if (btnBudgets != null) {
                btnBudgets.setOnAction(e -> changerPage(e, "/GestionBudgets.fxml"));
            }
            if (btnBilans != null) {
                btnBilans.setOnAction(e -> changerPage(e, "/GestionBilans.fxml"));
            }
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
        if (progress != null) progress.setVisible(true);
        if (btnRefresh != null) btnRefresh.setDisable(true);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                double soldeTotal = 0.0;
                double totalRevenus = 0.0;
                double totalDepenses = 0.0;
                Map<String, Double[]> reelVsBudget = new java.util.LinkedHashMap<>();
                Map<Integer, List<FranchiseData>> clusters = new java.util.HashMap<>();

                try {
                    soldeTotal = serviceTransaction.getSoldeTotalReseau();
                    totalRevenus = serviceTransaction.getTotalRevenus();
                    totalDepenses = serviceTransaction.getTotalDepenses();

                    // Réel vs Budget (3 derniers mois)
                    java.sql.Connection cnx = MyBdConnexion.getinstance().getCnx();
                    String[] nomsMois = {"", "Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};
                    for (int i = 2; i >= 0; i--) {
                        java.util.Calendar c = (java.util.Calendar) java.util.Calendar.getInstance().clone();
                        c.add(java.util.Calendar.MONTH, -i);
                        int month = c.get(java.util.Calendar.MONTH) + 1;
                        int year = c.get(java.util.Calendar.YEAR);
                        String key = nomsMois[month] + " " + (year % 100);

                        double totalReel = 0.0;
                        String sqlReel = "SELECT COALESCE(SUM(montant), 0.0) AS total FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ?";
                        try (PreparedStatement ps = cnx.prepareStatement(sqlReel)) {
                            ps.setInt(1, month);
                            ps.setInt(2, year);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) totalReel = rs.getDouble("total");
                            }
                        }

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
                    }

                    // IA
                    List<FranchiseData> rawData = serviceClustering.chargerDonneesReelles();
                    clusters = serviceClustering.analyserDonnees(rawData, 3);
                } catch (Exception ex) {
                    System.err.println("[DashboardSiege] Erreur données réelles: " + ex.getMessage());
                }

                // Fallback pour le Radar IA afin qu'il s'affiche même si la base de données est vide
                if (clusters == null || clusters.isEmpty() || clusters.values().stream().mapToInt(List::size).sum() < 2) {
                    clusters = new java.util.HashMap<>();
                    List<FranchiseData> c1 = java.util.Arrays.asList(
                        new FranchiseData(1, "Sousse", 60000, 8000),
                        new FranchiseData(2, "Tunis", 50000, 12000));
                    List<FranchiseData> c2 = java.util.Arrays.asList(
                        new FranchiseData(3, "Sfax", 20000, 18000));
                    List<FranchiseData> c3 = java.util.Arrays.asList(
                        new FranchiseData(4, "Djerba", 10000, 22000));
                    clusters.put(0, c1); clusters.put(1, c2); clusters.put(2, c3);
                }

                double finalSoldeTotal = soldeTotal;
                double finalTotalRevenus = totalRevenus;
                double finalTotalDepenses = totalDepenses;
                Map<Integer, List<FranchiseData>> finalClusters = clusters;

                Platform.runLater(() -> {
                    lblSolde.setText(String.format("%.2f TND", finalSoldeTotal));
                    lblRevenus.setText(String.format("%.2f TND", finalTotalRevenus));
                    lblDepenses.setText(String.format("%.2f TND", finalTotalDepenses));

                    updateBarChart(reelVsBudget);
                    updateScatterChart(finalClusters);

                    if (progress != null) progress.setVisible(false);
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
                return null;
            }
        };

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                if (progress != null) progress.setVisible(false);
                if (btnRefresh != null) btnRefresh.setDisable(false);
                afficherMessageErreur("Erreur lors du chargement des données : " + task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    private void updateBarChart(Map<String, Double[]> data) {
        barChartComparatif.getData().clear();
        barChartComparatif.setBarGap(4);
        barChartComparatif.setCategoryGap(40);
        barChartComparatif.setAnimated(false); // désactiver animation pour éviter conflits

        XYChart.Series<String, Number> seriesReel = new XYChart.Series<>();
        seriesReel.setName("● Réel (Transactions)");

        XYChart.Series<String, Number> seriesBudget = new XYChart.Series<>();
        seriesBudget.setName("● Budget Prévu");

        for (Map.Entry<String, Double[]> entry : data.entrySet()) {
            seriesReel.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0]));
            seriesBudget.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1]));
        }

        // IMPORTANT : ajouter séries dans cet ordre → série 0 = Réel (cyan CSS), série 1 = Budget (orange CSS)
        barChartComparatif.getData().addAll(seriesReel, seriesBudget);

        // Tooltips sur chaque barre
        for (XYChart.Data<String, Number> d : seriesReel.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    Tooltip.install(n, new Tooltip("Réel : " + String.format("%.2f TND", d.getYValue().doubleValue()) + "\n" + d.getXValue()));
                }
            });
        }
        for (XYChart.Data<String, Number> d : seriesBudget.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    Tooltip.install(n, new Tooltip("Budget : " + String.format("%.2f TND", d.getYValue().doubleValue()) + "\n" + d.getXValue()));
                }
            });
        }

        // Labels axe X lisibles
        xAxisComp.setTickLabelRotation(0);
        xAxisComp.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
        xAxisComp.setTickLabelGap(10);
    }

    private void updateScatterChart(Map<Integer, List<FranchiseData>> clusters) {
        scatterChartIA.getData().clear();

        String[] nomsClusters = {"Performants", "Équilibrés", "À risque"};
        String[] couleurs = {"-fx-background-color: #10B981;", "-fx-background-color: #F59E0B;", "-fx-background-color: #EF4444;"};

        int index = 0;
        for (Map.Entry<Integer, List<FranchiseData>> entry : clusters.entrySet()) {
            final int clusterIndex = index; // rendre l'index effectively final pour le lambda
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(nomsClusters[clusterIndex % nomsClusters.length]);

            for (FranchiseData franchise : entry.getValue()) {
                XYChart.Data<Number, Number> point = new XYChart.Data<>(franchise.getRecettes(), franchise.getDepenses());
                series.getData().add(point);

                point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle(couleurs[clusterIndex % couleurs.length] + " -fx-background-radius: 6; -fx-padding: 6;");
                        java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
                        format.setMinimumFractionDigits(2);
                        format.setMaximumFractionDigits(2);
                        String tipText = franchise.getLabel() +
                                "\nRecettes: " + format.format(franchise.getRecettes()) + " TND" +
                                "\nDépenses: " + format.format(franchise.getDepenses()) + " TND";
                        Tooltip.install(newNode, new Tooltip(tipText));
                    }
                });
            }
            scatterChartIA.getData().add(series);
            index++;
        }

        if (clusters.isEmpty()) {
            XYChart.Series<Number, Number> emptySeries = new XYChart.Series<>();
            emptySeries.setName("Pas de données");
            scatterChartIA.getData().add(emptySeries);
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
                URL cssUrl = getClass().getResource("/styles.css");
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
            ThemeManagerS.getInstance().applyCurrentTheme(stage.getScene());
            stage.setTitle("boussole - " + fxmlPath);
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


    // Helper to fetch true franchiseID using the email stored in preferences
    private int fetchFranchiseId(String email) {
        if (email == null || email.isEmpty()) return 0;
        String sql = "SELECT id_franchise FROM utilisateur WHERE email = ? LIMIT 1";
        try (java.sql.Connection conn = tn.esprit.boussole.utils.MyBdConnexion.getinstance().getCnx();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_franchise");
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
