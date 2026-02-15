package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.services.CommandeService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class CommandeController implements Initializable {

    @FXML private TableView<Commande> commandeTable;
    @FXML private TableColumn<Commande, Integer> colId;
    @FXML private TableColumn<Commande, String> colDate;  // Changé de LocalDateTime à String
    @FXML private TableColumn<Commande, Double> colMontant;
    @FXML private TableColumn<Commande, String> colStatut;
    @FXML private TableColumn<Commande, Integer> colFranchise;
    @FXML private TableColumn<Commande, Void> colActions;

    @FXML private TextField montantField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField franchiseIdField;

    private CommandeService commandeService;
    private ObservableList<Commande> commandeList;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commandeService = new CommandeService();

        // Initialiser la combo box des statuts
        statutCombo.getItems().addAll("En attente", "Confirmée", "Expédiée", "Livrée", "Annulée");

        // Configuration des colonnes
        configurerTable();

        // Charger les données
        chargerDonnees();

        // Listener pour la sélection
        commandeTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        remplirFormulaire(newSelection);
                    }
                }
        );
    }

    private void configurerTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant_total"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colFranchise.setCellValueFactory(new PropertyValueFactory<>("franchise_id"));

        // Formatage de la date - CORRECTION ICI
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate_creation();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(formatter) : ""
            );
        });

        // Colonne Actions avec boutons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

                editBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    remplirFormulaire(commande);
                    commandeTable.getSelectionModel().select(commande);
                });

                deleteBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    supprimerCommande(commande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // Le reste du code reste identique...
    private void chargerDonnees() {
        try {
            commandeList = FXCollections.observableArrayList(commandeService.selectAll());
            commandeTable.setItems(commandeList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes: " + e.getMessage());
        }
    }

    private void remplirFormulaire(Commande commande) {
        montantField.setText(String.valueOf(commande.getMontant_total()));
        statutCombo.setValue(commande.getStatut());
        franchiseIdField.setText(String.valueOf(commande.getFranchise_id()));
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
            Commande commande = new Commande(
                    LocalDateTime.now(),
                    Double.parseDouble(montantField.getText()),
                    statutCombo.getValue(),
                    Integer.parseInt(franchiseIdField.getText())
            );

            commandeService.insertOnePS(commande);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande ajoutée avec succès!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        Commande selected = commandeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une commande à modifier");
            return;
        }

        if (!validerChamps()) return;

        try {
            selected.setMontant_total(Double.parseDouble(montantField.getText()));
            selected.setStatut(statutCombo.getValue());
            selected.setFranchise_id(Integer.parseInt(franchiseIdField.getText()));

            commandeService.updateOne(selected);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande modifiée avec succès!");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        Commande selected = commandeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une commande à supprimer");
            return;
        }
        supprimerCommande(selected);
    }

    private void supprimerCommande(Commande commande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la commande");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer la commande #" + commande.getId() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commandeService.deleteOne(commande);
                viderFormulaire();
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande supprimée avec succès!");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEffacer() {
        viderFormulaire();
    }

    private void viderFormulaire() {
        montantField.clear();
        statutCombo.setValue(null);
        franchiseIdField.clear();
        commandeTable.getSelectionModel().clearSelection();
    }

    private boolean validerChamps() {
        if (montantField.getText().isEmpty() || statutCombo.getValue() == null ||
                franchiseIdField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs");
            return false;
        }

        try {
            Double.parseDouble(montantField.getText());
            Integer.parseInt(franchiseIdField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le montant et l'ID franchise doivent être des nombres valides");
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