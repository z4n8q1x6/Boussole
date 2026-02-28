package tn.esprit.boussole.gui;

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

// Imports pour JFreeChart (Utilisation de PiePlot pour le Flat Design)
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.service.ChargeService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class afficherFrontChargeController {

    @FXML
    private GridPane gridCharges;
    @FXML
    private StackPane chartContainer;
    @FXML
    private Button btnPrecedent;
    @FXML
    private Button btnSuivant;
    @FXML
    private Label lblPageInfo;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnIA;
    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> comboTri;

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

        btnPrecedent.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                updateView();
            }
        });
        btnSuivant.setOnAction(e -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateView();
            }
        });
        btnAjouter.setOnAction(e -> openAjoutForm());

        btnIA.setOnAction(e -> handleConseilsLocaux());
    }

    // --- LOGIQUE STATISTIQUE CORRIGÉE (DEUX GRAPHIQUES FLAT) ---

    private void updateStatistics() {
        if (allCharges.isEmpty()) {
            chartContainer.getChildren().clear();
            return;
        }

        // 1. Dataset pour la Répartition par TYPE (SOMME DES MONTANTS)
        DefaultPieDataset typeDataset = new DefaultPieDataset();

        // On regroupe par type et on additionne les montants (getMontant)
        Map<Charge.TypeCharge, Double> typeStats = allCharges.stream()
                .collect(Collectors.groupingBy(
                        Charge::getType,
                        Collectors.summingDouble(Charge::getMontant)
                ));

        // On remplit le dataset avec les sommes réelles
        typeStats.forEach((type, sommeDT) -> {
            typeDataset.setValue(type.toString(), sommeDT);
        });

        // 2. Dataset pour le STATUT (COMPTAGE DU NOMBRE)
        DefaultPieDataset statusDataset = new DefaultPieDataset();
        Map<String, Long> statusStats = allCharges.stream()
                .collect(Collectors.groupingBy(c -> c.getStatusValidation().toString(), Collectors.counting()));
        statusStats.forEach(statusDataset::setValue);

        // 3. Création des graphiques
        JFreeChart typeChart = createFlatChart("Répartition des dépenses (DT)", typeDataset);
        JFreeChart statusChart = createFlatChart("État des validations", statusDataset);

        // 4. Configuration des couleurs (Vérifie bien que tes Enums sont en MAJUSCULES)
        PiePlot typePlot = (PiePlot) typeChart.getPlot();
        // On utilise les noms exacts que tu m'as donnés
        typePlot.setSectionPaint("CHARGES_EXPLOITATIONS", new java.awt.Color(0, 229, 204));
        typePlot.setSectionPaint("CHARGES_FINANCIERES", new java.awt.Color(14, 165, 233));
        typePlot.setSectionPaint("CHARGES_EXCEPTIONNELLES", new java.awt.Color(139, 92, 246));

        // 5. Configuration des couleurs Statuts
        PiePlot statusPlot = (PiePlot) statusChart.getPlot();
        statusPlot.setSectionPaint("VALIDE", new java.awt.Color(16, 185, 129));
        statusPlot.setSectionPaint("EN_ATTENTE", new java.awt.Color(245, 158, 11));
        statusPlot.setSectionPaint("REJETTE", new java.awt.Color(239, 68, 68));

        // 6. Affichage
        ChartViewer typeViewer = new ChartViewer(typeChart);
        ChartViewer statusViewer = new ChartViewer(statusChart);
        HBox chartsBox = new HBox(20, typeViewer, statusViewer);
        chartsBox.setAlignment(Pos.CENTER);

        typeViewer.prefWidthProperty().bind(chartContainer.widthProperty().divide(2).subtract(15));
        statusViewer.prefWidthProperty().bind(chartContainer.widthProperty().divide(2).subtract(15));

        chartContainer.getChildren().setAll(chartsBox);
    }

    private JFreeChart createFlatChart(String title, DefaultPieDataset dataset) {
        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);

        chart.setBackgroundPaint(null);
        chart.getTitle().setPaint(java.awt.Color.WHITE);
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        chart.setBorderVisible(false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(null);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelGenerator(null); // Retire les étiquettes blanches avec bordures
        plot.setCircular(true);

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(null);
            chart.getLegend().setItemPaint(java.awt.Color.LIGHT_GRAY);
            chart.getLegend().setFrame(org.jfree.chart.block.BlockBorder.NONE);
        }

        return chart;
    }

    // --- LOGIQUE MÉTIER & CONSEILS ---

    private void handleConseilsLocaux() {
        if (allCharges.isEmpty()) {
            showAlert("Info", "Aucune donnée à analyser pour le moment.");
            return;
        }

        StringBuilder rapport = new StringBuilder();
        rapport.append("📊 ANALYSE FINANCIÈRE AUTOMATISÉE\n\n");

        double totalDepenses = allCharges.stream().mapToDouble(Charge::getMontant).sum();
        double moyenneParCharge = totalDepenses / allCharges.size();

        Map<Charge.TypeCharge, Double> parCategorie = allCharges.stream()
                .collect(Collectors.groupingBy(Charge::getType, Collectors.summingDouble(Charge::getMontant)));

        Map.Entry<Charge.TypeCharge, Double> topCategorie = parCategorie.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        rapport.append(String.format("💰 Total des dépenses : %.2f DT\n", totalDepenses));
        rapport.append(String.format("📉 Moyenne par dépense : %.2f DT\n\n", moyenneParCharge));

        rapport.append("🔍 OBSERVATIONS :\n");
        if (topCategorie != null) {
            double pourcentage = (topCategorie.getValue() / totalDepenses) * 100;
            rapport.append(String.format("- Votre poste de dépense principal est '%s' (%.1f%% du total).\n",
                    topCategorie.getKey(), pourcentage));
            if (pourcentage > 50)
                rapport.append("  ⚠️ Attention : Cette catégorie consomme plus de la moitié de votre budget !\n");
        }

        rapport.append("\n💡 CONSEILS PERSONNALISÉS :\n");
        if (parCategorie.containsKey(Charge.TypeCharge.CHARGES_EXPLOITATIONS))
            rapport.append("- Exploitation : Vérifiez vos contrats fournisseurs.\n");
        if (parCategorie.containsKey(Charge.TypeCharge.CHARGES_FINANCIERES))
            rapport.append("- Financier : Analysez les frais bancaires.\n");
        if (parCategorie.containsKey(Charge.TypeCharge.CHARGES_EXCEPTIONNELLES))
            rapport.append("- Exceptionnel : Pensez à constituer un fonds de réserve.\n");

        showAdviceDialog(rapport.toString());
    }

    private void showAdviceDialog(String conseils) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Analyse Financière");
        alert.setHeaderText("Rapport de vos dépenses");

        // Style du TextArea pour qu'il s'intègre au mode sombre
        TextArea textArea = new TextArea(conseils);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(40);

        // On applique des couleurs sombres directement au TextArea
        textArea.setStyle(
                "-fx-control-inner-background: #0C0F1A; " +
                        "-fx-text-fill: #CBD5F5; " +
                        "-fx-font-family: 'Consolas', monospace; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #1E293B;"
        );

        GridPane expContent = new GridPane();
        expContent.setPadding(new Insets(10));
        expContent.add(textArea, 0, 0);
        alert.getDialogPane().setContent(expContent);

        // --- APPLICATION DU THÈME GLOBAL ---
        DialogPane dialogPane = alert.getDialogPane();
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }

    // --- CRUD & AFFICHAGE ---

    private void loadData() {
        try {
            allCharges = chargeService.selectAll(null);
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
            if (sortOption.equals("Montant Croissant"))
                displayedCharges.sort(Comparator.comparingDouble(Charge::getMontant));
            else if (sortOption.equals("Montant Décroissant"))
                displayedCharges.sort(Comparator.comparingDouble(Charge::getMontant).reversed());
        }

        totalPages = (int) Math.ceil((double) displayedCharges.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        currentPage = 0;

        updateView();
        updateStatistics();
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

        String defaultStyle = "-fx-background-color: #0C0F1A; -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15;";
        String hoverStyle = "-fx-background-color: #1E293B; -fx-background-radius: 15; -fx-border-color: #0EA5E9; -fx-border-radius: 15; -fx-cursor: hand;";

        card.setStyle(defaultStyle);

        ImageView iv = new ImageView();
        iv.setFitHeight(120);
        iv.setFitWidth(220);
        iv.setPreserveRatio(true);
        try {
            if (charge.getPreuveImage() != null) iv.setImage(new Image(charge.getPreuveImage()));
        } catch (Exception e) {
        }

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
        btnMod.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-background-radius: 8;");
        btnMod.setOnAction(e -> openModifierForm(charge));

        Button btnSup = new Button("🗑");
        btnSup.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 8;");
        btnSup.setOnAction(e -> supprimerCharge(charge));

        Button btnPdf = new Button("PDF");
        btnPdf.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 8;");
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
        File file = fc.showSaveDialog(gridCharges.getScene().getWindow());

        if (file != null) {
            try (PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document doc = new Document(pdf)) {
                doc.add(new Paragraph("Rapport de Charge - Boussole").setBold().setFontSize(18));
                Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth();
                table.addCell("Titre :");
                table.addCell(charge.getTitre());
                table.addCell("Montant :");
                table.addCell(charge.getMontant() + " DT");
                table.addCell("Catégorie :");
                table.addCell(charge.getType().toString());
                table.addCell("Statut :");
                table.addCell(charge.getStatusValidation().toString());
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
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void openModifierForm(Charge charge) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierCharge.fxml"));
            Parent root = loader.load();
            ((modifierChargeController) loader.getController()).setChargeActuelle(charge);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void supprimerCharge(Charge charge) {
        try {
            chargeService.deleteone(charge);
            loadData();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);

        // --- STYLE PERSONNALISÉ ---
        DialogPane dialogPane = a.getDialogPane();
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("dialog-pane");

        a.showAndWait();
    }
}