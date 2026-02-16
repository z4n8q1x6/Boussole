package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.services.ProduitService;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProduitController implements Initializable {

    @FXML private TableView<Produit> produitTable;
    @FXML private TableColumn<Produit, Integer> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colReference;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, String> colImage;
    @FXML private TableColumn<Produit, Void> colActions;

    @FXML private TextField nomField;
    @FXML private TextField referenceField;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private TextField imageField;

    private ProduitService produitService;
    private ObservableList<Produit> produitList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        produitService = new ProduitService();

        // Configuration des colonnes
        configurerTable();

        // Charger les données
        chargerDonnees();

        // Listener pour la sélection
        produitTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        remplirFormulaire(newSelection);
                    }
                }
        );
    }

    private void configurerTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colReference.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_achat"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock_dispo"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));

        // Colonne Actions avec boutons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");

                editBtn.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    remplirFormulaire(produit);
                    produitTable.getSelectionModel().select(produit);
                });

                deleteBtn.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    supprimerProduit(produit);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void chargerDonnees() {
        try {
            produitList = FXCollections.observableArrayList(produitService.selectAll());
            produitTable.setItems(produitList);
            System.out.println("Produits chargés: " + produitList.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void remplirFormulaire(Produit produit) {
        nomField.setText(produit.getNom());
        referenceField.setText(produit.getReference());
        prixField.setText(String.valueOf(produit.getPrix_achat()));
        stockField.setText(String.valueOf(produit.getStock_dispo()));
        imageField.setText(produit.getImage());
    }

    @FXML
    private void handleRefresh() {
        chargerDonnees();
        viderFormulaire();
    }

    @FXML
    private void handleAjouter() {
        if (!validerChamps()) return;

        try {
            Produit produit = new Produit(
                    nomField.getText(),
                    referenceField.getText(),
                    Double.parseDouble(prixField.getText()),
                    Integer.parseInt(stockField.getText()),
                    imageField.getText()
            );

            produitService.insertOnePS(produit);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit ajouté avec succès!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleModifier() {
        Produit selected = produitTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un produit à modifier");
            return;
        }

        if (!validerChamps()) return;

        try {
            selected.setNom(nomField.getText());
            selected.setReference(referenceField.getText());
            selected.setPrix_achat(Double.parseDouble(prixField.getText()));
            selected.setStock_dispo(Integer.parseInt(stockField.getText()));
            selected.setImage(imageField.getText());

            produitService.updateOne(selected);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit modifié avec succès!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSupprimer() {
        Produit selected = produitTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un produit à supprimer");
            return;
        }
        supprimerProduit(selected);
    }

    private void supprimerProduit(Produit produit) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le produit");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer le produit \"" + produit.getNom() + "\" ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                produitService.deleteOne(produit);
                viderFormulaire();
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit supprimé avec succès!");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleEffacer() {
        viderFormulaire();
    }

    private void viderFormulaire() {
        nomField.clear();
        referenceField.clear();
        prixField.clear();
        stockField.clear();
        imageField.clear();
        produitTable.getSelectionModel().clearSelection();
    }

    private boolean validerChamps() {
        if (nomField.getText().isEmpty() || referenceField.getText().isEmpty() ||
                prixField.getText().isEmpty() || stockField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs obligatoires");
            return false;
        }

        try {
            Double.parseDouble(prixField.getText());
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le prix et le stock doivent être des nombres valides");
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}