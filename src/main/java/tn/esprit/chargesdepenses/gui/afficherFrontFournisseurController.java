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
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
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
    @FXML private Button btnPrecedent, btnSuivant, btnAjouter, btnIA;
    @FXML private Label lblPageInfo, lblMeteo, lblNews;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private TextArea txtReponseIA;

    private final FournisseurService fournisseurService = new FournisseurService();
    private List<Fournisseur> allFournisseurs = new ArrayList<>();
    private List<Fournisseur> displayedFournisseurs = new ArrayList<>();

    // --- CONFIGURATION OPENROUTER ---
    private static final String OPENROUTER_API_KEY = "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";
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    private String businessNewsUrl = "https://www.boursier.com/actualites/economie";
    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        // --- STYLE IA ---
        txtReponseIA.setStyle("-fx-control-inner-background: #0C0F1A; -fx-text-fill: #00E5CC; -fx-font-family: 'Segoe UI';");
        txtReponseIA.setPromptText("L'analyse de l'IA apparaîtra ici...");

        comboTri.setItems(FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
        comboTri.setOnAction(e -> applyFilters());
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        loadData();
        startExternalAPIs();

        btnPrecedent.setOnAction(e -> { if (currentPage > 0) { currentPage--; updateView(); } });
        btnSuivant.setOnAction(e -> { if (currentPage < totalPages - 1) { currentPage++; updateView(); } });
        btnAjouter.setOnAction(e -> openAjoutForm());

        lblNews.setCursor(javafx.scene.Cursor.HAND);
        lblNews.setOnMouseClicked(e -> openBusinessLink());
    }

    private void startExternalAPIs() {
        // 1. Tâche Météo via Open-Meteo (Stable et sans clé)
        Task<String> weatherTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    // Coordonnées de Tunis
                    String urlStr = "https://api.open-meteo.com/v1/forecast?latitude=36.8188&longitude=10.1659&current_weather=true";
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlStr)).build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        JSONObject current = json.getJSONObject("current_weather");
                        double temp = current.getDouble("temperature");
                        int code = current.getInt("weathercode");
                        String desc = traduireCodeMeteo(code);
                        return "🌍 Tunis : " + temp + "°C, " + desc;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "Météo indisponible";
            }
        };

        // Liaison du résultat au Label FXML
        weatherTask.setOnSucceeded(e -> lblMeteo.setText(weatherTask.getValue()));
        weatherTask.setOnFailed(e -> lblMeteo.setText("Météo indisponible"));

        // 2. Tâche Flash Business (News)
        Task<String> newsTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return "Analyse Marché : Les coûts de logistique sont en baisse de 3%. (Lire plus...)";
            }
        };
        newsTask.setOnSucceeded(e -> lblNews.setText(newsTask.getValue()));

        // Lancement des threads
        new Thread(weatherTask).start();
        new Thread(newsTask).start();
    }

    @FXML
    private void handleAnalyseIA() {
        if (allFournisseurs.isEmpty()) {
            showAlert("Information", "Aucun fournisseur à analyser.");
            return;
        }

        btnIA.setDisable(true);
        txtReponseIA.setText("🤖 Analyse OpenRouter en cours...");

        String noms = allFournisseurs.stream()
                .map(Fournisseur::getNom)
                .collect(Collectors.joining(", "));

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                JSONObject payload = new JSONObject();
                payload.put("model", "minimax/minimax-m2.5");
                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", "En tant qu'expert logistique, donne 2 conseils courts et stratégiques pour gérer ces fournisseurs : " + noms);
                messages.put(msg);
                payload.put("messages", messages);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(OPENROUTER_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + OPENROUTER_API_KEY.trim())
                        .header("HTTP-Referer", "http://localhost")
                        .header("X-Title", "Gestion Fournisseurs Esprit")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject resObj = new JSONObject(response.body());
                    return resObj.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content").trim();
                } else {
                    return "Erreur technique (Code " + response.statusCode() + ")";
                }
            }
        };

        task.setOnSucceeded(e -> {
            txtReponseIA.setText("💡 CONSEILS IA :\n" + task.getValue());
            btnIA.setDisable(false);
        });

        task.setOnFailed(e -> {
            txtReponseIA.setText("❌ Erreur de connexion au service OpenRouter.");
            btnIA.setDisable(false);
        });

        new Thread(task).start();
    }

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
        Button btnMod = new Button("Modifier");
        btnMod.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-background-radius: 8;");
        btnMod.setOnAction(e -> openModifierForm(fournisseur));

        Button btnSup = new Button("Supprimer");
        btnSup.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 8;");
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
        } catch (Exception e) { showAlert("Erreur", "Impossible d'ouvrir l'ajout."); }
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
        } catch (Exception e) { showAlert("Erreur", "Impossible d'ouvrir la modification."); }
    }

    private void supprimerFournisseur(Fournisseur fournisseur) {
        try {
            fournisseurService.deleteOne(fournisseur);
            loadData();
        } catch (SQLException e) { showAlert("Erreur", "Échec de suppression."); }
    }

    private void openBusinessLink() {
        try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(businessNewsUrl)); }
        catch (Exception e) { System.err.println("Erreur lien : " + e.getMessage()); }
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        String cssPath = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(cssPath);
        dialogPane.getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    private String traduireCodeMeteo(int code) {
        if (code == 0) return "Ciel dégagé";
        if (code >= 1 && code <= 3) return "Partiellement nuageux";
        if (code >= 45 && code <= 48) return "Brouillard";
        if (code >= 51 && code <= 67) return "Pluie légère";
        if (code >= 71 && code <= 77) return "Neige";
        if (code >= 80 && code <= 82) return "Averses de pluie";
        if (code >= 95) return "Orageux";
        return "Variable";
    }
}