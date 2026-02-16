package tn.esprit.boussole.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.franchiseService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class entrepriseController {

    @FXML private TableView<franchise> tableEntreprises;
    @FXML private TableColumn<franchise, Integer> colId;
    @FXML private TableColumn<franchise, String> colNom;
    @FXML private TableColumn<franchise, String> colEmail;
    @FXML private TableColumn<franchise, String> colTelephone;
    @FXML private TableColumn<franchise, String> colAdresse;
    @FXML private TableColumn<franchise, Boolean> colActif;
    @FXML private TableColumn<franchise, Double> colSolde;
    @FXML private TableColumn<franchise, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Label lblPagination;

    private ObservableList<franchise> entrepriseList = FXCollections.observableArrayList();
    private franchiseService franchiseService;

    @FXML
    public void initialize() {
        System.out.println("Initialisation de entrepriseController...");
        franchiseService = new franchiseService();

        // Remplacer l'ID de la base par un numéro séquentiel (1, 2, 3...)
        colId.setText("N°");
        colId.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tableEntreprises.getItems().indexOf(column.getValue()) + 1));
        colId.setSortable(false);

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        colActif.setCellValueFactory(new PropertyValueFactory<>("actif"));
        colSolde.setCellValueFactory(new PropertyValueFactory<>("soldeActuel"));

        // Ajouter les boutons d'actions
        addActionButtons();

        // Charger les entreprises
        loadEntreprises();
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);

                editBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; " +
                        "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold;");
                deleteBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                        "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 5 10; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold;");

                editBtn.setOnAction(event -> {
                    System.out.println("Clic sur le bouton Modifier");
                    franchise selectedEntreprise = getTableView().getItems().get(getIndex());
                    if (selectedEntreprise != null) {
                        System.out.println("Entreprise sélectionnée : " + selectedEntreprise.getNom());
                        handleEditEntreprise(selectedEntreprise);
                    } else {
                        System.err.println("Erreur : Aucune entreprise sélectionnée !");
                    }
                });

                deleteBtn.setOnAction(event -> {
                    franchise selectedEntreprise = getTableView().getItems().get(getIndex());
                    if (selectedEntreprise != null) {
                        handleDeleteEntreprise(selectedEntreprise);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadEntreprises() {
        entrepriseList.clear();
        try {
            List<franchise> entreprises = franchiseService.selectAll(new franchise());
            entrepriseList.addAll(entreprises);
            tableEntreprises.setItems(entrepriseList);
            updatePaginationLabel();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les entreprises.");
        }
    }

    private void handleEditEntreprise(franchise selectedEntreprise) {
        try {
            System.out.println("Début handleEditEntreprise pour : " + selectedEntreprise.getNom());
            
            URL fxmlUrl = getClass().getResource("/updateFranchise.fxml");
            if (fxmlUrl == null) {
                System.err.println("ERREUR CRITIQUE : Fichier /updateFranchise.fxml introuvable !");
                showAlert(Alert.AlertType.ERROR, "Erreur Fichier", "Le fichier updateFranchise.fxml est introuvable.");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("FXML chargé avec succès");

            UpdateFranchiseController controller = loader.getController();
            if (controller == null) {
                System.err.println("ERREUR : Le contrôleur est null !");
                throw new IllegalStateException("Le contrôleur updateFranchiseController est null !");
            }
            
            controller.initData(selectedEntreprise);
            controller.setOnFranchiseUpdated(this::loadEntreprises); 

            Stage stage = new Stage();
            stage.setTitle("Modifier l'Entreprise");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            System.out.println("Fenêtre de modification fermée");

        } catch (Exception e) {
            System.err.println("EXCEPTION dans handleEditEntreprise :");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Critique", 
                      "Impossible d'ouvrir le formulaire de modification.\n\n" +
                      "Cause : " + e.getClass().getSimpleName() + "\n" +
                      "Message : " + e.getMessage());
        }
    }

    private void handleDeleteEntreprise(franchise selectedEntreprise) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'entreprise");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer l'entreprise " + selectedEntreprise.getNom() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteEntreprise(selectedEntreprise);
        }
    }

    private void deleteEntreprise(franchise selectedEntreprise) {
        try {
            franchiseService.deleteone(selectedEntreprise);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Entreprise supprimée avec succès !");
            loadEntreprises();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'entreprise.");
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();

        if (searchText.isEmpty()) {
            tableEntreprises.setItems(entrepriseList);
        } else {
            ObservableList<franchise> filteredList = FXCollections.observableArrayList();
            for (franchise f : entrepriseList) {
                if (f.getNom().toLowerCase().contains(searchText) ||
                        f.getEmail().toLowerCase().contains(searchText) ||
                        f.getAdresse().toLowerCase().contains(searchText)) {
                    filteredList.add(f);
                }
            }
            tableEntreprises.setItems(filteredList);
        }
        updatePaginationLabel();
    }

    private void updatePaginationLabel() {
        int total = tableEntreprises.getItems().size();
        lblPagination.setText("Affichage de 1 à " + total + " sur " + total + " entreprises");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
