package controller;

import entity.Pret;
import entity.StatutPret;
import entity.Mensualite;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import service.PretService;

import java.time.LocalDate;
import java.util.List;

public class DashboardRisqueController {

    @FXML private PieChart chartStatuts;
    @FXML private Label lblTotalPrete;
    @FXML private Label lblTotalRembourse;
    @FXML private Label lblTotalRestant;

    // Label pour afficher spécifiquement le montant en retard
    @FXML private Label lblTotalImpayes;

    private PretService pretService = new PretService();

    @FXML
    public void initialize() {
        // 1. Charger les données et remplir le PieChart
        chargerStatistiques();

        // 2. Appliquer les couleurs Néon (Forçage après rendu)
        chartStatuts.getData().forEach(data -> {
            if (data.getName().startsWith("Accordés")) {
                applyStylesAndAnimations(data, "#10B981"); // Vert émeraude
            } else if (data.getName().startsWith("En Attente")) {
                applyStylesAndAnimations(data, "#00E5CC"); // Cyan Boussole
            } else if (data.getName().startsWith("Refusés")) {
                applyStylesAndAnimations(data, "#EF4444"); // Rouge alerte
            }
        });
    }

    private void chargerStatistiques() {
        List<Pret> allPrets = pretService.getAllPrets();
        LocalDate today = LocalDate.now();

        // --- 1. CALCUL DES NOMBRES PAR STATUT ---
        long countAccorde = allPrets.stream().filter(p -> p.getStatut() == StatutPret.ACCORDE).count();
        long countAttente = allPrets.stream().filter(p -> p.getStatut() == StatutPret.EN_ATTENTE).count();
        long countRefuse = allPrets.stream().filter(p -> p.getStatut() == StatutPret.REFUSE).count();

        // --- 2. CRÉATION DES DONNÉES DU GRAPHIQUE ---
        PieChart.Data accordeData = new PieChart.Data("Accordés (" + countAccorde + ")", countAccorde);
        PieChart.Data attenteData = new PieChart.Data("En Attente (" + countAttente + ")", countAttente);
        PieChart.Data refuseData = new PieChart.Data("Refusés (" + countRefuse + ")", countRefuse);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(accordeData, attenteData, refuseData);
        chartStatuts.setData(pieChartData);

        // --- 3. CALCUL FINANCIER ---
        // Capital total prêté (Accordés uniquement)
        double totalPrete = allPrets.stream()
                .filter(p -> p.getStatut() == StatutPret.ACCORDE)
                .mapToDouble(Pret::getMontantDemande).sum();

        // Total déjà remboursé (Mensualités payées)
        double totalRembourse = allPrets.stream()
                .filter(p -> p.getStatut() == StatutPret.ACCORDE)
                .flatMap(p -> pretService.getMensualitesByPret(p.getId()).stream())
                .filter(Mensualite::isEstPaye)
                .mapToDouble(Mensualite::getMontant).sum();

        // Risque d'impayés (Mensualités non payées et date < aujourd'hui)
        double totalImpayes = allPrets.stream()
                .filter(p -> p.getStatut() == StatutPret.ACCORDE)
                .flatMap(p -> pretService.getMensualitesByPret(p.getId()).stream())
                .filter(m -> !m.isEstPaye() && m.getDateEcheance().toLocalDate().isBefore(today))
                .mapToDouble(Mensualite::getMontant).sum();

        // Reste à recouvrer
        double totalRestant = totalPrete - totalRembourse;

        // --- 4. MISE À JOUR DE L'INTERFACE ---
        lblTotalPrete.setText(String.format("%.2f DT", totalPrete));
        lblTotalRembourse.setText(String.format("%.2f DT", totalRembourse));
        lblTotalRestant.setText(String.format("%.2f DT", totalRestant));

        if (lblTotalImpayes != null) {
            lblTotalImpayes.setText(String.format("%.2f DT", totalImpayes));
        }
    }

    private void applyStylesAndAnimations(PieChart.Data data, String color) {
        // Appliquer la couleur à la tranche via CSS inline
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            // Animation d'éclatement (Zoom) au survol de la souris
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}