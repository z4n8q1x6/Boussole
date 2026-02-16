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
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class afficherFrontFournisseurController {

    @FXML
    private GridPane gridFournisseurs;
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

    private final FournisseurService fournisseurService = new FournisseurService();
    private List<Fournisseur> allFournisseurs = new ArrayList<>();
    private List<Fournisseur> displayedFournisseurs = new ArrayList<>();
    
    // Paramètres de pagination
    private static final int ITEMS_PER_PAGE = 3;
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        // Initialisation du ComboBox de tri
        comboTri.setItems(FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
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
            allFournisseurs = fournisseurService.selectAll();
            applyFilters(); // Appliquer les filtres initiaux
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les données: " + e.getMessage());
        }
    }

    private void applyFilters() {
        // 1. Filtrage par recherche
        String searchText = txtRecherche.getText().toLowerCase();
        displayedFournisseurs = allFournisseurs.stream()
                .filter(f -> f.getNom().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        // 2. Tri
        String sortOption = comboTri.getValue();
        if (sortOption != null) {
            if (sortOption.equals("Matricule Croissant")) {
                displayedFournisseurs.sort(Comparator.comparing(Fournisseur::getMatriculeFiscal));
            } else if (sortOption.equals("Matricule Décroissant")) {
                displayedFournisseurs.sort(Comparator.comparing(Fournisseur::getMatriculeFiscal).reversed());
            }
        }

        // 3. Recalcul de la pagination
        totalPages = (int) Math.ceil((double) displayedFournisseurs.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        currentPage = 0; // Revenir à la première page après un filtre

        updateView();
    }

    private void updateView() {
        gridFournisseurs.getChildren().clear();
        
        // Mise à jour des infos de pagination
        lblPageInfo.setText("Page " + (currentPage + 1) + " / " + totalPages);
        btnPrecedent.setDisable(currentPage == 0);
        btnSuivant.setDisable(currentPage >= totalPages - 1);

        // Sélection des items pour la page courante
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, displayedFournisseurs.size());

        int column = 0;
        int row = 1;

        for (int i = start; i < end; i++) {
            Fournisseur fournisseur = displayedFournisseurs.get(i);
            VBox card = createFournisseurCard(fournisseur);
            
            gridFournisseurs.add(card, column++, row);
        }
    }

    private VBox createFournisseurCard(Fournisseur fournisseur) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(250);
        card.setPrefHeight(250); 
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // Nom du Fournisseur
        Label lblNom = new Label(fournisseur.getNom());
        lblNom.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblNom.setTextFill(Color.web("#0e5384"));
        lblNom.setWrapText(true);
        lblNom.setAlignment(Pos.CENTER);

        // Matricule Fiscal
        Label lblMatricule = new Label("Matricule: " + fournisseur.getMatriculeFiscal());
        lblMatricule.setTextFill(Color.GRAY);
        lblMatricule.setFont(Font.font("System", 12));

        // Téléphone
        Label lblPhone = new Label("📞 " + fournisseur.getTelephone());
        lblPhone.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblPhone.setTextFill(Color.web("#27ae60"));
        lblPhone.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 5 10 5 10; -fx-background-radius: 20;");

        // Boutons d'action
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        
        Button btnModifier = new Button("Modifier");
        btnModifier.setStyle("-fx-background-color: #4593cb; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> openModifierForm(fournisseur));
        
        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> supprimerFournisseur(fournisseur));
        
        actionBox.getChildren().addAll(btnModifier, btnSupprimer);

        // Assemblage
        card.getChildren().addAll(lblNom, lblMatricule, lblPhone, actionBox);
        
        // Effet de survol
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(14, 83, 132, 0.4), 15, 0, 0, 0); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"));

        return card;
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter un Fournisseur");
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
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
        }
    }

    private void supprimerFournisseur(Fournisseur fournisseur) {
        try {
            fournisseurService.deleteOne(fournisseur);
            loadData();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de supprimer le fournisseur: " + e.getMessage());
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
