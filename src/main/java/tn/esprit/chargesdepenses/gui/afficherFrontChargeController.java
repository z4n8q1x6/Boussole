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
    @FXML private StackPane chartContainer;
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;
    @FXML private Label lblPageInfo;
    @FXML private Button btnAjouter;
    @FXML private Button btnIA;
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
        
        // Changement : Appel de la logique locale au lieu de l'API Gemini
        btnIA.setOnAction(e -> handleConseilsLocaux());
    }

    /**
     * LOGIQUE DE CONSEILS LOCALE (Remplacement de l'IA)
     * Analyse les données et génère des recommandations basées sur des règles métiers.
     */
    private void handleConseilsLocaux() {
        if (allCharges.isEmpty()) {
            showAlert("Info", "Aucune donnée à analyser pour le moment.");
            return;
        }

        StringBuilder rapport = new StringBuilder();
        rapport.append("📊 ANALYSE FINANCIÈRE AUTOMATISÉE\n\n");

        // 1. Calculs de base
        double totalDepenses = allCharges.stream().mapToDouble(Charge::getMontant).sum();
        double moyenneParCharge = totalDepenses / allCharges.size();
        
        Map<Charge.TypeCharge, Double> parCategorie = allCharges.stream()
                .collect(Collectors.groupingBy(Charge::getType, Collectors.summingDouble(Charge::getMontant)));

        // Trouver la catégorie la plus coûteuse
        Map.Entry<Charge.TypeCharge, Double> topCategorie = parCategorie.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        // 2. Génération du rapport
        rapport.append(String.format("💰 Total des dépenses : %.2f DT\n", totalDepenses));
        rapport.append(String.format("📉 Moyenne par dépense : %.2f DT\n\n", moyenneParCharge));

        rapport.append("🔍 OBSERVATIONS :\n");
        
        if (topCategorie != null) {
            double pourcentage = (topCategorie.getValue() / totalDepenses) * 100;
            rapport.append(String.format("- Votre poste de dépense principal est '%s' (%.1f%% du total).\n", 
                    topCategorie.getKey(), pourcentage));
            
            if (pourcentage > 50) {
                rapport.append("  ⚠️ Attention : Cette catégorie consomme plus de la moitié de votre budget !\n");
            }
        }

        // 3. Conseils spécifiques par catégorie
        rapport.append("\n💡 CONSEILS PERSONNALISÉS :\n");
        
        if (parCategorie.containsKey(Charge.TypeCharge.CHARGES_EXPLOITATIONS)) {
            rapport.append("- Exploitation : Vérifiez vos contrats fournisseurs et négociez les tarifs récurrents.\n");
        }
        
        if (parCategorie.containsKey(Charge.TypeCharge.CHARGES_FINANCIERES)) {
            rapport.append("- Financier : Analysez les frais bancaires et les intérêts d'emprunt pour les optimiser.\n");
        }
        
        if (parCategorie.containsKey(Charge.TypeCharge.CHARGES_EXCEPTIONNELLES)) {
            rapport.append("- Exceptionnel : Ces dépenses sont imprévues. Pensez à constituer un fonds de réserve.\n");
        }

        if (totalDepenses > 10000) { // Seuil arbitraire d'exemple
            rapport.append("- Votre volume de dépenses est élevé. Un audit détaillé ligne par ligne est recommandé.\n");
        } else {
            rapport.append("- Votre gestion semble maîtrisée. Continuez à surveiller les petits écarts.\n");
        }

        showAdviceDialog(rapport.toString());
    }

    private void showAdviceDialog(String conseils) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Analyse Financière");
        alert.setHeaderText("Rapport de vos dépenses");
        
        TextArea textArea = new TextArea(conseils);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 14px;"); // Police monospace pour l'alignement
        
        GridPane.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
        
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setContent(expContent);
        alert.showAndWait();
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
        updateStatistics();
    }

    private void updateStatistics() {
        if (displayedCharges.isEmpty()) {
            chartContainer.getChildren().clear();
            return;
        }

        DefaultPieDataset dataset = new DefaultPieDataset();
        Map<Charge.TypeCharge, Double> stats = displayedCharges.stream()
                .collect(Collectors.groupingBy(Charge::getType, Collectors.summingDouble(Charge::getMontant)));

        stats.forEach((type, total) -> dataset.setValue(type.toString(), total));

        JFreeChart chart = ChartFactory.createPieChart3D("Répartition des charges (DT)", dataset, true, true, false);

        chart.setBackgroundPaint(null);
        chart.getTitle().setPaint(java.awt.Color.WHITE);
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));

        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setBackgroundPaint(null);
        plot.setOutlineVisible(false);
        plot.setLabelPaint(java.awt.Color.WHITE);
        plot.setLabelBackgroundPaint(new java.awt.Color(12, 15, 26, 200));

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(null);
            chart.getLegend().setItemPaint(java.awt.Color.WHITE);
        }

        ChartViewer viewer = new ChartViewer(chart);
        viewer.setStyle("-fx-background-color: transparent;");
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
        
        String defaultStyle = "-fx-background-color: #0C0F1A; -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);";
        String hoverStyle = "-fx-background-color: #1E293B; -fx-background-radius: 15; -fx-border-color: #0EA5E9; -fx-border-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(14, 165, 233, 0.4), 15, 0, 0, 0); -fx-cursor: hand;";
        
        card.setStyle(defaultStyle);

        ImageView iv = new ImageView();
        iv.setFitHeight(120); iv.setFitWidth(220); iv.setPreserveRatio(true);
        try { if (charge.getPreuveImage() != null) iv.setImage(new Image(charge.getPreuveImage())); } catch (Exception e) {}

        Label lblTitre = new Label(charge.getTitre());
        lblTitre.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblTitre.setTextFill(Color.web("#E8EDF5"));

        Label lblMontant = new Label(charge.getMontant() + " DT");
        lblMontant.setFont(Font.font("System", FontWeight.BOLD, 20));
        lblMontant.setTextFill(Color.web("#00E5CC"));

        Label lblType = new Label(charge.getType().toString());
        lblType.setTextFill(Color.web("#8892A4"));
        lblType.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 5 10; -fx-background-radius: 10;");

        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER);

        Button btnMod = new Button("✎");
        btnMod.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8;");
        btnMod.setOnAction(e -> openModifierForm(charge));

        Button btnSup = new Button("🗑");
        btnSup.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8;");
        btnSup.setOnAction(e -> supprimerCharge(charge));

        Button btnPdf = new Button("PDF");
        btnPdf.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 8;");
        btnPdf.setOnAction(e -> genererFicheChargePDF(charge));

        actionBox.getChildren().addAll(btnMod, btnSup, btnPdf);
        card.getChildren().addAll(iv, lblTitre, lblMontant, lblType, actionBox);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

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