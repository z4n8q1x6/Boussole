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
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import tn.esprit.Boussole.Services.ServiceTransaction;
import tn.esprit.Boussole.Services.ServiceBilan;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // instantiate services
        serviceTransaction = new ServiceTransaction();
        serviceBilan = new ServiceBilan();

        // Verify session (ROLE CHECK instead of ID check)
        String role = SessionManager.getInstance().getRole();
        if (role == null || !role.equals("SIEGE")) {
            afficherMessageErreur("Accès Refusé : Vous n'avez pas les droits d'accès au Siège.");
            // Close window for security
            Platform.runLater(() -> {
                try {
                    Stage s = (Stage) (btnDashboard != null ? btnDashboard.getScene().getWindow() : null);
                    if (s != null) s.close();
                } catch (Exception ignored) {
                }
            });
            return;
        }

        // Load real data
        chargerDonnees();

        // Configure barChart axes labels
        try {
            if (xAxis != null) xAxis.setLabel("Mois");
            if (yAxis != null) yAxis.setLabel("Montant (TND)");
        } catch (Exception e) {
            System.err.println("Erreur configuration axes: " + e.getMessage());
        }

        // Configurer les boutons de navigation
        btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"));
        btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBudgets.fxml"));
        btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBilans.fxml"));

        // Refresh button
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> chargerDonnees());
        }
    }

    private void chargerDonnees() {
        // disable refresh and show progress
        if (btnRefresh != null) btnRefresh.setDisable(true);
        if (progress != null) progress.setVisible(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    int franchiseId = SessionManager.getInstance().getIdFranchise();

                    // Get totals (either network-wide or per franchise depending on session)
                    final double solde;
                    final double totalRevenus;
                    final double totalDepenses;

                    if (franchiseId == 0) {
                        solde = serviceTransaction.getSoldeTotalReseau();
                        totalRevenus = serviceTransaction.getTotalRevenus();
                        totalDepenses = serviceTransaction.getTotalDepenses();
                    } else {
                        solde = serviceTransaction.calculerSolde(franchiseId);
                        // per franchise totals by summing transactions
                        double rv = 0.0;
                        double dp = 0.0;
                        java.util.List<tn.esprit.Boussole.Models.transaction> txs = serviceTransaction.getAllByFranchise(franchiseId);
                        for (tn.esprit.Boussole.Models.transaction t : txs) {
                            if (t.getType() == tn.esprit.Boussole.Models.transaction.Type.RECETTE) rv += t.getMontant();
                            else if (t.getType() == tn.esprit.Boussole.Models.transaction.Type.DEPENSE) dp += t.getMontant();
                        }
                        totalRevenus = rv;
                        totalDepenses = dp;
                    }

                    // Get last 3 months data (for franchise or network)
                    Map<String, Double[]> donnees;
                    // We want the chart to show network totals if session is siege, or franchise totals otherwise
                    if (franchiseId == 0) {
                        donnees = serviceBilan.getDonneesGraphique(3);
                    } else {
                        // get per-franchise by querying serviceTransaction for each month
                        java.util.LinkedHashMap<String, Double[]> map = new java.util.LinkedHashMap<>();
                        for (int i = 2; i >= 0; i--) {
                            java.util.Calendar c = (java.util.Calendar) java.util.Calendar.getInstance().clone();
                            c.add(java.util.Calendar.MONTH, -i);
                            int month = c.get(java.util.Calendar.MONTH) + 1;
                            int year = c.get(java.util.Calendar.YEAR);
                            String key = String.format("%02d/%d", month, year);
                            double rec = 0.0, dep = 0.0;
                            java.util.List<tn.esprit.Boussole.Models.transaction> txs = serviceTransaction.getAllByFranchise(franchiseId);
                            for (tn.esprit.Boussole.Models.transaction t : txs) {
                                java.util.Date d = t.getDate();
                                if (d != null) {
                                    java.util.Calendar dc = java.util.Calendar.getInstance();
                                    dc.setTime(d);
                                    int tm = dc.get(java.util.Calendar.MONTH) + 1;
                                    int ty = dc.get(java.util.Calendar.YEAR);
                                    if (tm == month && ty == year) {
                                        if (t.getType() == tn.esprit.Boussole.Models.transaction.Type.RECETTE) rec += t.getMontant();
                                        else if (t.getType() == tn.esprit.Boussole.Models.transaction.Type.DEPENSE) dep += t.getMontant();
                                    }
                                }
                            }
                            map.put(key, new Double[]{rec, dep});
                        }
                        donnees = map;
                    }

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            lblSolde.setText(String.format("%.2f TND", solde));
                            lblRevenus.setText(String.format("%.2f TND", totalRevenus));
                            lblDepenses.setText(String.format("%.2f TND", totalDepenses));

                            XYChart.Series<String, Number> seriesRevenus = new XYChart.Series<>();
                            seriesRevenus.setName("Revenus");
                            XYChart.Series<String, Number> seriesDepenses = new XYChart.Series<>();
                            seriesDepenses.setName("Dépenses");

                            barChart.getData().clear();

                            for (Map.Entry<String, Double[]> entry : donnees.entrySet()) {
                                String mois = entry.getKey();
                                Double[] vals = entry.getValue();
                                double rec = vals[0] != null ? vals[0] : 0.0;
                                double dep = vals[1] != null ? vals[1] : 0.0;
                                seriesRevenus.getData().add(new XYChart.Data<>(mois, rec));
                                seriesDepenses.getData().add(new XYChart.Data<>(mois, dep));
                            }

                            // add series individually to avoid unchecked varargs warning
                            barChart.getData().add(seriesRevenus);
                            barChart.getData().add(seriesDepenses);
                        } catch (Exception e) {
                            System.err.println("Erreur mise à jour UI: " + e.getMessage());
                        } finally {
                            if (btnRefresh != null) btnRefresh.setDisable(false);
                            if (progress != null) progress.setVisible(false);
                        }
                    });

                } catch (Exception e) {
                    // Log and show fallback in UI thread
                    System.err.println("Erreur lors du chargement des données: " + e.getMessage());
                    Platform.runLater(() -> {
                        lblSolde.setText("—");
                        if (btnRefresh != null) btnRefresh.setDisable(false);
                        if (progress != null) progress.setVisible(false);
                        afficherMessageErreur("Erreur lors du chargement des données : " + e.getMessage());
                    });
                }
                return null;
            }
        };

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
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
