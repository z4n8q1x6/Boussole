package tn.esprit.boussole.gui;

import tn.esprit.boussole.models.Pret;
import tn.esprit.boussole.models.StatutPret;
import tn.esprit.boussole.models.Mensualite;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.boussole.service.PretService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardRisqueController {

    @FXML private PieChart chartStatuts;
    @FXML private Label lblTotalPrete;
    @FXML private Label lblTotalRembourse;
    @FXML private Label lblTotalRestant;
    @FXML private Label lblTotalImpayes;

    private PretService pretService = new PretService();

    @FXML
    public void initialize() {
        chargerStatistiques();

        // Appliquer les couleurs après que les données soient liées au graphique
        chartStatuts.getData().forEach(data -> {
            if (data.getName().startsWith("Accordés")) {
                applyStylesAndAnimations(data, "#10B981");
            } else if (data.getName().startsWith("En Attente")) {
                applyStylesAndAnimations(data, "#00E5CC");
            } else if (data.getName().startsWith("Refusés")) {
                applyStylesAndAnimations(data, "#EF4444");
            }
        });
    }

    private void chargerStatistiques() {
        try {
            List<Pret> allPrets = pretService.getAllPrets();
            LocalDate today = LocalDate.now();

            // --- 1. CALCUL DES NOMBRES PAR STATUT ---
            long countAccorde = allPrets.stream().filter(p -> p.getStatut() == StatutPret.ACCORDE).count();
            long countAttente = allPrets.stream().filter(p -> p.getStatut() == StatutPret.EN_ATTENTE).count();
            long countRefuse = allPrets.stream().filter(p -> p.getStatut() == StatutPret.REFUSE).count();

            // --- 2. GRAPHIQUE ---
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Accordés (" + countAccorde + ")", countAccorde),
                    new PieChart.Data("En Attente (" + countAttente + ")", countAttente),
                    new PieChart.Data("Refusés (" + countRefuse + ")", countRefuse)
            );
            chartStatuts.setData(pieChartData);

            // --- 3. CALCUL FINANCIER ---
            double totalPrete = 0;
            double totalRembourse = 0;
            double totalImpayes = 0;

            for (Pret p : allPrets) {
                if (p.getStatut() == StatutPret.ACCORDE) {
                    totalPrete += p.getMontantDemande();

                    // Récupération des mensualités avec gestion de l'exception SQL
                    try {
                        List<Mensualite> mensualites = pretService.getMensualitesByPret(p.getId());

                        for (Mensualite m : mensualites) {
                            if (m.isEstPaye()) {
                                totalRembourse += m.getMontant();
                            } else if (m.getDateEcheance().toLocalDate().isBefore(today)) {
                                totalImpayes += m.getMontant();
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Erreur SQL pour le prêt ID " + p.getId() + ": " + e.getMessage());
                    }
                }
            }

            double totalRestant = totalPrete - totalRembourse;

            // --- 4. MISE À JOUR DE L'INTERFACE ---
            lblTotalPrete.setText(String.format("%.2f DT", totalPrete));
            lblTotalRembourse.setText(String.format("%.2f DT", totalRembourse));
            lblTotalRestant.setText(String.format("%.2f DT", totalRestant));

            if (lblTotalImpayes != null) {
                lblTotalImpayes.setText(String.format("%.2f DT", totalImpayes));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyStylesAndAnimations(PieChart.Data data, String color) {
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            data.getNode().setOnMouseEntered(event -> {
                data.getNode().setScaleX(1.1);
                data.getNode().setScaleY(1.1);
            });

            data.getNode().setOnMouseExited(event -> {
                data.getNode().setScaleX(1.0);
                data.getNode().setScaleY(1.0);
            });
        }
    }

    @FXML
    private void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/ListePrets.fxml"));
            Stage stage = (Stage) chartStatuts.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Liste des Prêts");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}