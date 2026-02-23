package tn.esprit.chargesdepenses.gui;

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
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class afficherFrontFournisseurController {

    @FXML private GridPane gridFournisseurs;
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;
    @FXML private Label lblPageInfo;
    @FXML private Button btnAjouter;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;

    @FXML private Button btnIA;
    @FXML private TextArea txtReponseIA;

    private final FournisseurService fournisseurService = new FournisseurService();
    private List<Fournisseur> allFournisseurs = new ArrayList<>();
    private List<Fournisseur> displayedFournisseurs = new ArrayList<>();

    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 0;
    private int totalPages = 0;

    // --- CONFIGURATION IA VALIDÉE ---
    private static final String GEMINI_API_KEY = "AIzaSyDDrRFJ92kzJe17C31zdVHmfjpT9j8fv_Q";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    @FXML
    public void initialize() {
        comboTri.setItems(FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
        comboTri.setOnAction(e -> applyFilters());
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        loadData();

        btnPrecedent.setOnAction(e -> { if (currentPage > 0) { currentPage--; updateView(); } });
        btnSuivant.setOnAction(e -> { if (currentPage < totalPages - 1) { currentPage++; updateView(); } });
        btnAjouter.setOnAction(e -> openAjoutForm());
    }

    @FXML
    private void handleAnalyseIA() {
        if (allFournisseurs.isEmpty()) {
            showAlert("Information", "Aucun fournisseur à analyser.");
            return;
        }

        btnIA.setDisable(true);
        txtReponseIA.setText("L'IA examine vos partenaires commerciaux...");

        StringBuilder context = new StringBuilder("Voici ma liste de fournisseurs :\n");
        for (Fournisseur f : allFournisseurs) {
            context.append("- ").append(f.getNom()).append(" (Matricule: ").append(f.getMatriculeFiscal()).append(")\n");
        }
        context.append("\nEn tant qu'expert financier, donne-moi 2 conseils brefs pour optimiser mes relations avec eux.");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                JSONObject jsonBody = new JSONObject();
                JSONArray partsArray = new JSONArray();
                partsArray.put(new JSONObject().put("text", context.toString()));
                JSONArray contentsArray = new JSONArray();
                contentsArray.put(new JSONObject().put("parts", partsArray));
                jsonBody.put("contents", contentsArray);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());
                    return responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                } else if (response.statusCode() == 429) {
                    throw new Exception("Quota dépassé. Attendez 1 minute.");
                } else {
                    throw new Exception("Erreur API : " + response.statusCode());
                }
            }
        };

        task.setOnSucceeded(e -> {
            txtReponseIA.setText(task.getValue());
            btnIA.setDisable(false);
        });

        task.setOnFailed(e -> {
            txtReponseIA.setText("Note : " + task.getException().getMessage());
            btnIA.setDisable(false);
        });

        new Thread(task).start();
    }

    // --- DESIGN ORIGINAL RÉTABLI ---

    private VBox createFournisseurCard(Fournisseur fournisseur) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(250);
        card.setPrefHeight(250);

        String defaultStyle = "-fx-background-color: #0C0F1A; -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);";
        String hoverStyle = "-fx-background-color: #1E293B; -fx-background-radius: 15; -fx-border-color: #0EA5E9; -fx-border-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(14, 165, 233, 0.4), 15, 0, 0, 0); -fx-cursor: hand;";

        card.setStyle(defaultStyle);

        Label lblNom = new Label(fournisseur.getNom());
        lblNom.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblNom.setTextFill(Color.web("#E8EDF5"));
        lblNom.setWrapText(true);
        lblNom.setAlignment(Pos.CENTER);

        Label lblMatricule = new Label("Matricule: " + fournisseur.getMatriculeFiscal());
        lblMatricule.setTextFill(Color.web("#8892A4"));
        lblMatricule.setFont(Font.font("System", 12));

        Label lblPhone = new Label("📞 " + fournisseur.getTelephone());
        lblPhone.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblPhone.setTextFill(Color.web("#00E5CC"));
        lblPhone.setStyle("-fx-background-color: rgba(0, 229, 204, 0.1); -fx-padding: 5 10; -fx-background-radius: 20;");

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        Button btnMod = new Button("Modifier");
        btnMod.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnMod.setOnAction(e -> openModifierForm(fournisseur));

        Button btnSup = new Button("Supprimer");
        btnSup.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSup.setOnAction(e -> supprimerFournisseur(fournisseur));

        actionBox.getChildren().addAll(btnMod, btnSup);
        card.getChildren().addAll(lblNom, lblMatricule, lblPhone, actionBox);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

        return card;
    }

    // --- LOGIQUE DE CHARGEMENT ---

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

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage());
        }
    }

    private void openModifierForm(Fournisseur fournisseur) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierFournisseur.fxml"));
            Parent root = loader.load();
            modifierFournisseurController controller = loader.getController();
            controller.setFournisseurActuel(fournisseur);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier le Fournisseur");
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification.");
        }
    }

    private void supprimerFournisseur(Fournisseur fournisseur) {
        try {
            fournisseurService.deleteOne(fournisseur);
            loadData();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de supprimer le fournisseur.");
        }
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}