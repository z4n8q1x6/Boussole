package tn.esprit.chargesdepenses.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.services.ChargeService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class afficherFrontChargeController {

    @FXML
    private GridPane gridCharges;
    @FXML
    private Button btnPrecedent;
    @FXML
    private Button btnSuivant;
    @FXML
    private Label lblPageInfo;
    @FXML
    private Button btnAjouter;
    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> comboTri;

    private final ChargeService chargeService = new ChargeService();
    private List<Charge> allCharges = new ArrayList<>();
    private List<Charge> displayedCharges = new ArrayList<>();
    
    // Paramètres de pagination
    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        // Initialisation du ComboBox de tri
        comboTri.setItems(FXCollections.observableArrayList("Montant Croissant", "Montant Décroissant"));
        comboTri.setOnAction(e -> applyFilters());

        // Listener pour la recherche
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });

        loadData();
        
        // Configuration des boutons de pagination
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

        // Configuration du bouton Ajouter
        btnAjouter.setOnAction(e -> openAjoutForm());
    }

    private void loadData() {
        try {
            allCharges = chargeService.selectAll();
            applyFilters(); // Appliquer les filtres initiaux (ou aucun)
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void applyFilters() {
        // 1. Filtrage par recherche
        String searchText = txtRecherche.getText().toLowerCase();
        displayedCharges = allCharges.stream()
                .filter(c -> c.getTitre().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        // 2. Tri
        String sortOption = comboTri.getValue();
        if (sortOption != null) {
            if (sortOption.equals("Montant Croissant")) {
                displayedCharges.sort(Comparator.comparingDouble(Charge::getMontant));
            } else if (sortOption.equals("Montant Décroissant")) {
                displayedCharges.sort(Comparator.comparingDouble(Charge::getMontant).reversed());
            }
        }

        // 3. Recalcul de la pagination
        totalPages = (int) Math.ceil((double) displayedCharges.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        currentPage = 0; // Revenir à la première page après un filtre

        updateView();
    }

    private void updateView() {
        gridCharges.getChildren().clear();
        
        // Mise à jour des infos de pagination
        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + totalPages);
        btnPrecedent.setDisable(currentPage == 0);
        btnSuivant.setDisable(currentPage >= totalPages - 1);

        // Sélection des items pour la page courante
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, displayedCharges.size());

        int column = 0;
        int row = 1;

        for (int i = start; i < end; i++) {
            Charge charge = displayedCharges.get(i);
            VBox card = createChargeCard(charge);
            
            gridCharges.add(card, column++, row);
        }
    }

    private VBox createChargeCard(Charge charge) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(15));
        card.setPrefWidth(250);
        card.setPrefHeight(350); 
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitHeight(120);
        imageView.setFitWidth(220);
        imageView.setPreserveRatio(true);
        
        try {
            String imageUrl = charge.getPreuveImage();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                 imageView.setImage(new Image(imageUrl)); 
            }
        } catch (Exception e) {
            // Ignorer erreur image
        }

        // Titre
        Label lblTitre = new Label(charge.getTitre());
        lblTitre.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblTitre.setTextFill(Color.web("#0e5384"));
        lblTitre.setWrapText(true);

        // Montant
        Label lblMontant = new Label(charge.getMontant() + " DT");
        lblMontant.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblMontant.setTextFill(Color.web("#27ae60"));

        // Date
        Label lblDate = new Label("Date: " + charge.getDateCharge());
        lblDate.setTextFill(Color.GRAY);

        // Type
        Label lblType = new Label(charge.getType().toString());
        lblType.setStyle("-fx-background-color: #e1f5fe; -fx-text-fill: #0288d1; -fx-background-radius: 5; -fx-padding: 3 8 3 8;");

        // Boutons d'action
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        
        Button btnModifier = new Button("Modifier");
        btnModifier.setStyle("-fx-background-color: #4593cb; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> openModifierForm(charge));
        
        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> supprimerCharge(charge));
        
        actionBox.getChildren().addAll(btnModifier, btnSupprimer);

        // Assemblage
        card.getChildren().addAll(imageView, lblTitre, lblMontant, lblDate, lblType, actionBox);
        
        // Effet de survol
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(14, 83, 132, 0.4), 15, 0, 0, 0); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"));

        return card;
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCharge.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter une charge");
            stage.showAndWait();
            loadData(); 
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage());
        }
    }

    private void openModifierForm(Charge charge) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierCharge.fxml"));
            Parent root = loader.load();
            modifierChargeController controller = loader.getController();
            controller.setChargeActuelle(charge); 
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier la charge");
            stage.showAndWait();
            loadData(); 
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
        }
    }

    private void supprimerCharge(Charge charge) {
        try {
            chargeService.deleteOne(charge);
            loadData();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de supprimer la charge: " + e.getMessage());
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
