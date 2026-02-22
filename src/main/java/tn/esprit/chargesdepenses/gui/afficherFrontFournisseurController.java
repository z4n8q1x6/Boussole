package tn.esprit.chargesdepenses.gui;

import javafx.collections.FXCollections;
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
import javafx.concurrent.Task;
import org.json.JSONObject;

import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
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

    @FXML private Label lblMeteo;
    @FXML private Label lblNews;

    private final FournisseurService fournisseurService = new FournisseurService();
    private List<Fournisseur> allFournisseurs = new ArrayList<>();
    private List<Fournisseur> displayedFournisseurs = new ArrayList<>();

    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        // Lancer les services API
        chargerMeteoOpenMeteo();
        chargerNewsBusiness();

        comboTri.setItems(FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
        comboTri.setOnAction(e -> applyFilters());
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        loadData();

        btnPrecedent.setOnAction(e -> { if (currentPage > 0) { currentPage--; updateView(); } });
        btnSuivant.setOnAction(e -> { if (currentPage < totalPages - 1) { currentPage++; updateView(); } });
        btnAjouter.setOnAction(e -> openAjoutForm());
    }

    private void chargerMeteoOpenMeteo() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=36.81&longitude=10.18&current_weather=true";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject json = new JSONObject(response.body());
                    JSONObject current = json.getJSONObject("current_weather");
                    double temp = current.getDouble("temperature");
                    int code = current.getInt("weathercode");
                    return String.format("%.1f°C %s", temp, getEmojiForCode(code));
                }
                return "Erreur";
            }
        };
        task.setOnSucceeded(e -> { if (lblMeteo != null) lblMeteo.setText("Tunis : " + task.getValue()); });
        new Thread(task).start();
    }

    // MODIFICATION ICI : Méthode de découpage (Split) ultra-fiable
    private void chargerNewsBusiness() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    // Flux Business Google News
                    String url = "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=fr&gl=FR&ceid=FR:fr";

                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .followRedirects(HttpClient.Redirect.ALWAYS) // Important pour Google
                            .build();

                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        String body = response.body();

                        // On passe tout en minuscule pour éviter les problèmes de balises <TITLE> vs <title>
                        String lowerBody = body.toLowerCase();

                        if (lowerBody.contains("<item>")) {
                            // On coupe au premier item
                            int itemIndex = lowerBody.indexOf("<item>");
                            String itemSection = body.substring(itemIndex);

                            // On cherche le titre à l'intérieur de cet item
                            String lowerSection = itemSection.toLowerCase();
                            int startTitle = lowerSection.indexOf("<title>") + 7;
                            int endTitle = lowerSection.indexOf("</title>");

                            if (startTitle > 6 && endTitle > startTitle) {
                                String title = itemSection.substring(startTitle, endTitle);

                                // Nettoyage final
                                return title.replace("&quot;", "\"")
                                        .replace("&amp;", "&")
                                        .replace("&#39;", "'")
                                        .replaceAll("(?i) - .*", "") // Supprime la source proprement
                                        .trim();
                            }
                        }
                    }
                    return "Format de flux inconnu";
                } catch (Exception e) {
                    return "Erreur réseau : " + e.getMessage();
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (lblNews != null) lblNews.setText(task.getValue());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String getEmojiForCode(int code) {
        if (code == 0) return "☀️";
        if (code >= 1 && code <= 3) return "☁️";
        if (code >= 51 && code <= 67) return "🌧️";
        if (code >= 95) return "⛈️";
        return "🌡️";
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

        String style = "-fx-background-color: #0C0F1A; -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15;";
        card.setStyle(style);

        Label lblNom = new Label(fournisseur.getNom());
        lblNom.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblNom.setTextFill(Color.web("#E8EDF5"));
        lblNom.setWrapText(true);

        Label lblMatricule = new Label("Matricule: " + fournisseur.getMatriculeFiscal());
        lblMatricule.setTextFill(Color.web("#8892A4"));

        Label lblPhone = new Label("📞 " + fournisseur.getTelephone());
        lblPhone.setTextFill(Color.web("#00E5CC"));
        lblPhone.setStyle("-fx-background-color: rgba(0, 229, 204, 0.1); -fx-padding: 5 10; -fx-background-radius: 20;");

        HBox actions = new HBox(10, createBtn("Modifier", "#0EA5E9"), createBtn("Supprimer", "#EF4444"));
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(lblNom, lblMatricule, lblPhone, actions);
        return card;
    }

    private Button createBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        return b;
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout");
        }
    }

    private void openModifierForm(Fournisseur f) { /* Logique modifier */ }
    private void supprimerFournisseur(Fournisseur f) { /* Logique supprimer */ }
    private void showAlert(String t, String m) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(t);
        alert.setContentText(m);
        alert.show();
    }
}