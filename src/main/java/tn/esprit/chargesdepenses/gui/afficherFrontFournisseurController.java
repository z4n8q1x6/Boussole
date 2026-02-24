package tn.esprit.chargesdepenses.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class afficherFrontFournisseurController {

    @FXML private GridPane gridFournisseurs;
    @FXML private Button btnPrecedent, btnSuivant, btnAjouter, btnIA;
    @FXML private Label lblPageInfo, lblMeteo, lblNews; // lblNews correspond à fx:id="lblNews" dans ton FXML
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private TextArea txtReponseIA;

    private final FournisseurService fournisseurService = new FournisseurService();
    private List<Fournisseur> allFournisseurs = new ArrayList<>();
    private List<Fournisseur> displayedFournisseurs = new ArrayList<>();

    // URL pour l'option "Lire plus"
    private String businessNewsUrl = "https://www.boursier.com/actualites/economie";

    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        comboTri.setItems(FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
        comboTri.setOnAction(e -> applyFilters());
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        loadData();

        // --- LANCEMENT DES APIS EXTERNES ---
        startExternalAPIs();

        btnPrecedent.setOnAction(e -> { if (currentPage > 0) { currentPage--; updateView(); } });
        btnSuivant.setOnAction(e -> { if (currentPage < totalPages - 1) { currentPage++; updateView(); } });
        btnAjouter.setOnAction(e -> openAjoutForm());

        // Rendre le label news cliquable pour "Lire Plus"
        lblNews.setCursor(javafx.scene.Cursor.HAND);
        lblNews.setOnMouseClicked(e -> openBusinessLink());
    }

    private void startExternalAPIs() {
        // Task pour la Météo corrigée (Gestion UTF-8)
        Task<String> weatherTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                URL url = new URL("https://wttr.in/Tunis?format=%c+%t");
                // On précise explicitement StandardCharsets.UTF_8 ici
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    return reader.readLine();
                }
            }
        };
        weatherTask.setOnSucceeded(e -> lblMeteo.setText(weatherTask.getValue()));
        weatherTask.setOnFailed(e -> lblMeteo.setText("Météo indisponible"));

        // 2. Task Flash Business (Simulation de News Réelles)
        Task<String> newsTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // On pourrait utiliser un flux RSS ici, voici un titre d'exemple
                return "Inflation : Les prix à la production chutent de 0.5% ce mois. (Lire plus...)";
            }
        };
        newsTask.setOnSucceeded(e -> lblNews.setText(newsTask.getValue()));

        new Thread(weatherTask).start();
        new Thread(newsTask).start();
    }

    private void openBusinessLink() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(businessNewsUrl));
            }
        } catch (Exception e) {
            System.err.println("Erreur lien : " + e.getMessage());
        }
    }

    @FXML
    private void handleAnalyseIA() {
        // Version locale pour éviter l'erreur 403 (Gemini désactivé temporairement)
        if (allFournisseurs.isEmpty()) {
            showAlert("Information", "Aucun fournisseur à analyser.");
            return;
        }
        txtReponseIA.setText("💡 Analyse Boussole : Avec vos " + allFournisseurs.size() +
                " partenaires, nous suggérons de consolider vos achats chez le fournisseur le plus ancien pour obtenir des remises sur volume.");
    }

    // --- LOGIQUE DE CHARGEMENT & CRUD (INCHANGÉE POUR LE DESIGN) ---

    private void loadData() {
        try {
            allFournisseurs = fournisseurService.selectAll();
            applyFilters();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String searchText = txtRecherche.getText().toLowerCase();
        displayedFournisseurs = allFournisseurs.stream()
                .filter(f -> f.getNom().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        String sortOption = comboTri.getValue();
        if (sortOption != null) {
            if (sortOption.equals("Matricule Croissant")) displayedFournisseurs.sort(Comparator.comparing(Fournisseur::getMatriculeFiscal));
            else if (sortOption.equals("Matricule Décroissant")) displayedFournisseurs.sort(Comparator.comparing(Fournisseur::getMatriculeFiscal).reversed());
        }

        totalPages = (int) Math.ceil((double) displayedFournisseurs.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        currentPage = 0;
        updateView();
    }

    private void updateView() {
        gridFournisseurs.getChildren().clear();
        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + totalPages);
        btnPrecedent.setDisable(currentPage == 0);
        btnSuivant.setDisable(currentPage >= totalPages - 1);

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, displayedFournisseurs.size());

        int column = 0;
        for (int i = start; i < end; i++) {
            gridFournisseurs.add(createFournisseurCard(displayedFournisseurs.get(i)), column++, 1);
        }
    }

    private VBox createFournisseurCard(Fournisseur fournisseur) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(250);
        card.setPrefHeight(250);

        String defaultStyle = "-fx-background-color: #0C0F1A; -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15;";
        String hoverStyle = "-fx-background-color: #1E293B; -fx-background-radius: 15; -fx-border-color: #0EA5E9; -fx-border-radius: 15; -fx-cursor: hand;";

        card.setStyle(defaultStyle);

        Label lblNom = new Label(fournisseur.getNom());
        lblNom.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblNom.setTextFill(Color.web("#E8EDF5"));
        lblNom.setWrapText(true);
        lblNom.setAlignment(Pos.CENTER);

        Label lblMatricule = new Label("Matricule: " + fournisseur.getMatriculeFiscal());
        lblMatricule.setTextFill(Color.web("#8892A4"));

        Label lblPhone = new Label("📞 " + fournisseur.getTelephone());
        lblPhone.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblPhone.setTextFill(Color.web("#00E5CC"));
        lblPhone.setStyle("-fx-background-color: rgba(0, 229, 204, 0.1); -fx-padding: 5 10; -fx-background-radius: 20;");

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        Button btnMod = new Button("Modifier"); btnMod.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-background-radius: 8;");
        btnMod.setOnAction(e -> openModifierForm(fournisseur));
        Button btnSup = new Button("Supprimer"); btnSup.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 8;");
        btnSup.setOnAction(e -> supprimerFournisseur(fournisseur));

        actionBox.getChildren().addAll(btnMod, btnSup);
        card.getChildren().addAll(lblNom, lblMatricule, lblPhone, actionBox);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

        return card;
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir l'ajout.");
        }
    }

    private void openModifierForm(Fournisseur f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierFournisseur.fxml"));
            Parent root = loader.load();
            modifierFournisseurController controller = loader.getController();
            controller.setFournisseurActuel(f);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir la modification.");
        }
    }

    private void supprimerFournisseur(Fournisseur fournisseur) {
        try {
            fournisseurService.deleteOne(fournisseur);
            loadData();
        } catch (SQLException e) {
            showAlert("Erreur", "Échec de suppression.");
        }
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}