package tn.esprit.chargesdepenses.gui;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// Imports pour JFreeChart
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.services.ChargeService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class afficherFrontChargeController {

    @FXML private GridPane gridCharges;
    @FXML private StackPane chartContainer; // Injecté depuis le nouveau FXML
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;
    @FXML private Label lblPageInfo;
    @FXML private Button btnAjouter;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;

    private final ChargeService chargeService = new ChargeService();
    private List<Charge> allCharges = new ArrayList<>();
    private List<Charge> displayedCharges = new ArrayList<>();

    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        comboTri.setItems(FXCollections.observableArrayList("Montant Croissant", "Montant Décroissant"));
        comboTri.setOnAction(e -> applyFilters());
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        loadData();

        btnPrecedent.setOnAction(e -> { if (currentPage > 0) { currentPage--; updateView(); } });
        btnSuivant.setOnAction(e -> { if (currentPage < totalPages - 1) { currentPage++; updateView(); } });
        btnAjouter.setOnAction(e -> openAjoutForm());
    }

    private void loadData() {
        try {
            allCharges = chargeService.selectAll();
            applyFilters();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String searchText = txtRecherche.getText().toLowerCase();
        displayedCharges = allCharges.stream()
                .filter(c -> c.getTitre().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        String sortOption = comboTri.getValue();
        if (sortOption != null) {
            if (sortOption.equals("Montant Croissant")) displayedCharges.sort(Comparator.comparingDouble(Charge::getMontant));
            else if (sortOption.equals("Montant Décroissant")) displayedCharges.sort(Comparator.comparingDouble(Charge::getMontant).reversed());
        }

        totalPages = (int) Math.ceil((double) displayedCharges.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        currentPage = 0;

        updateView();
        updateStatistics(); // Calculer les stats sur les données filtrées
    }

    /**
     * LOGIQUE DE L'API STATISTIQUE (JFreeChart)
     */
    private void updateStatistics() {
        if (displayedCharges.isEmpty()) {
            chartContainer.getChildren().clear();
            return;
        }

        // 1. Préparer les données (Somme des montants par Type de Charge)
        DefaultPieDataset dataset = new DefaultPieDataset();
        Map<Charge.TypeCharge, Double> stats = displayedCharges.stream()
                .collect(Collectors.groupingBy(Charge::getType, Collectors.summingDouble(Charge::getMontant)));

        stats.forEach((type, total) -> dataset.setValue(type.toString(), total));

        // 2. Créer le graphique via l'API
        JFreeChart chart = ChartFactory.createPieChart3D("Répartition des charges (DT)", dataset, true, true, false);

        // 3. Personnaliser le style pour le mode sombre
        chart.setBackgroundPaint(null); // Fond transparent
        chart.getTitle().setPaint(java.awt.Color.WHITE); // Titre en BLANC
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));

        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setBackgroundPaint(null);
        plot.setOutlineVisible(false);
        plot.setLabelPaint(java.awt.Color.WHITE); // Labels en BLANC
        plot.setLabelBackgroundPaint(new java.awt.Color(12, 15, 26, 200)); // Fond des labels sombre

        // Légende en blanc
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(null);
            chart.getLegend().setItemPaint(java.awt.Color.WHITE);
        }

        // 4. Afficher dans le conteneur JavaFX
        ChartViewer viewer = new ChartViewer(chart);
        chartContainer.getChildren().setAll(viewer);
    }

    private void updateView() {
        gridCharges.getChildren().clear();
        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + totalPages);
        btnPrecedent.setDisable(currentPage == 0);
        btnSuivant.setDisable(currentPage >= totalPages - 1);

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, displayedCharges.size());

        int column = 0;
        for (int i = start; i < end; i++) {
            gridCharges.add(createChargeCard(displayedCharges.get(i)), column++, 1);
        }
    }

    private VBox createChargeCard(Charge charge) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));
        card.setPrefSize(250, 380);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        ImageView iv = new ImageView();
        iv.setFitHeight(120); iv.setFitWidth(220); iv.setPreserveRatio(true);
        try { if (charge.getPreuveImage() != null) iv.setImage(new Image(charge.getPreuveImage())); } catch (Exception e) {}

        Label lblTitre = new Label(charge.getTitre());
        lblTitre.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblTitre.setTextFill(Color.web("#0e5384"));

        Label lblMontant = new Label(charge.getMontant() + " DT");
        lblMontant.setFont(Font.font("System", FontWeight.BOLD, 20));
        lblMontant.setTextFill(Color.web("#27ae60"));

        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER);

        Button btnMod = new Button("✎");
        btnMod.setStyle("-fx-background-color: #4593cb; -fx-text-fill: white; -fx-cursor: hand;");
        btnMod.setOnAction(e -> openModifierForm(charge));

        Button btnSup = new Button("🗑");
        btnSup.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
        btnSup.setOnAction(e -> supprimerCharge(charge));

        Button btnPdf = new Button("PDF");
        btnPdf.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        btnPdf.setOnAction(e -> genererFicheChargePDF(charge));

        actionBox.getChildren().addAll(btnMod, btnSup, btnPdf);
        card.getChildren().addAll(iv, lblTitre, lblMontant, new Label(charge.getType().toString()), actionBox);

        return card;
    }

    private void genererFicheChargePDF(Charge charge) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter PDF");
        fc.setInitialFileName("Charge_" + charge.getTitre().replace(" ", "_") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(gridCharges.getScene().getWindow());

        if (file != null) {
            try (PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document doc = new Document(pdf)) {

                doc.add(new Paragraph("Rapport de Charge").setBold().setFontSize(18));
                doc.add(new Paragraph("Boussole Management System\n\n"));

                Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth();
                table.addCell("Titre :"); table.addCell(charge.getTitre());
                table.addCell("Montant :"); table.addCell(charge.getMontant() + " DT");
                table.addCell("Date :"); table.addCell(charge.getDateCharge().toString());
                table.addCell("Catégorie :"); table.addCell(charge.getType().toString());
                table.addCell("Statut :"); table.addCell(charge.getStatusValidation().toString());

                doc.add(table);
                showAlert("Succès", "PDF généré !");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur PDF: " + e.getMessage());
            }
        }
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCharge.fxml"));
            Stage stage = new Stage(); stage.setScene(new Scene(loader.load()));
            stage.showAndWait(); loadData();
        } catch (IOException e) { showAlert("Erreur", e.getMessage()); }
    }

    private void openModifierForm(Charge charge) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierCharge.fxml"));
            Parent root = loader.load();
            ((modifierChargeController)loader.getController()).setChargeActuelle(charge);
            Stage stage = new Stage(); stage.setScene(new Scene(root));
            stage.showAndWait(); loadData();
        } catch (IOException e) { showAlert("Erreur", e.getMessage()); }
    }

    private void supprimerCharge(Charge charge) {
        try { chargeService.deleteOne(charge); loadData(); } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}