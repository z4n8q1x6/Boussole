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

    // Label optionnel pour afficher spécifiquement le montant en retard
    @FXML private Label lblTotalImpayes;

    private PretService pretService = new PretService();

    @FXML
    public void initialize() {
        chargerStatistiques();
    }

    private void chargerStatistiques() {
        List<Pret> allPrets = pretService.getAllPrets();
        LocalDate today = LocalDate.now();

        // 1. Calcul des nombres par statut pour le graphique
        long countAccorde = allPrets.stream().filter(p -> p.getStatut() == StatutPret.ACCORDE).count();
        long countAttente = allPrets.stream().filter(p -> p.getStatut() == StatutPret.EN_ATTENTE).count();
        long countRefuse = allPrets.stream().filter(p -> p.getStatut() == StatutPret.REFUSE).count();

        // 2. Création et affichage des données du PieChart
        PieChart.Data accordeData = new PieChart.Data("Accordés (" + countAccorde + ")", countAccorde);
        PieChart.Data attenteData = new PieChart.Data("En Attente (" + countAttente + ")", countAttente);
        PieChart.Data refuseData = new PieChart.Data("Refusés (" + countRefuse + ")", countRefuse);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(accordeData, attenteData, refuseData);
        chartStatuts.setData(pieChartData);

        // 3. Personnalisation des COULEURS et ANIMATIONS
        applyStylesAndAnimations(accordeData, "#27ae60"); // Vert
        applyStylesAndAnimations(attenteData, "#f39c12"); // Orange
        applyStylesAndAnimations(refuseData, "#e74c3c");  // Rouge

        // 4. Calcul financier approfondi
        // Somme totale des montants prêtés (pour les prêts accordés)
        double totalPrete = allPrets.stream()
                .filter(p -> p.getStatut() == StatutPret.ACCORDE)
                .mapToDouble(Pret::getMontantDemande).sum();

        // Somme de ce qui a été payé réellement
        double totalRembourse = allPrets.stream()
                .filter(p -> p.getStatut() == StatutPret.ACCORDE)
                .flatMap(p -> pretService.getMensualitesByPret(p.getId()).stream())
                .filter(Mensualite::isEstPaye)
                .mapToDouble(Mensualite::getMontant).sum();

        // LOGIQUE DE RETARD : Somme des mensualités NON PAYÉES dont la date est DEPASSÉE
        double totalImpayes = allPrets.stream()
                .filter(p -> p.getStatut() == StatutPret.ACCORDE)
                .flatMap(p -> pretService.getMensualitesByPret(p.getId()).stream())
                .filter(m -> !m.isEstPaye() && m.getDateEcheance().toLocalDate().isBefore(today))
                .mapToDouble(Mensualite::getMontant).sum();

        // Le reste total à recouvrer (sain + retard)
        double totalRestant = totalPrete - totalRembourse;

        // 5. Mise à jour de l'interface graphique
        lblTotalPrete.setText(String.format("%.2f DT", totalPrete));
        lblTotalRembourse.setText(String.format("%.2f DT", totalRembourse));
        lblTotalRestant.setText(String.format("%.2f DT", totalRestant));

        // Si tu as ajouté lblTotalImpayes dans ton FXML, on l'affiche en rouge
        if (lblTotalImpayes != null) {
            lblTotalImpayes.setText(String.format("%.2f DT", totalImpayes));
            lblTotalImpayes.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    private void applyStylesAndAnimations(PieChart.Data data, String color) {
        // Appliquer la couleur à la tranche
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            // Animation d'éclatement au survol
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