package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
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
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, Double> colMontant;
    @FXML private TableColumn<Commande, String> colStatut;
    @FXML private TableColumn<Commande, Integer> colFranchiseId;  // Affichage de l'ID
    @FXML private TableColumn<Commande, Void> colActions;

    @FXML private TextField montantField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField franchiseIdField;  // Champ pour saisir l'ID

    private CommandeService commandeService;
    private ObservableList<Commande> commandeList;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commandeService = new CommandeService();

        // Initialiser la combo box des statuts
        statutCombo.getItems().addAll("En attente", "Confirmée", "Expédiée", "Livrée", "Annulée");

        // Configuration des colonnes avec édition
        configurerTable();

        // Charger les données
        chargerDonnees();

        // Vider le formulaire au démarrage
        viderFormulaire();

        // Ajouter des listeners pour la validation en temps réel
        ajouterValidations();
    }

    private void ajouterValidations() {
        // Validation du champ Montant (doit être > 0)
        montantField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                montantField.setText(oldValue);
            }
        });

        // Validation du champ Franchise ID (doit être > 0 et entier)
        franchiseIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                franchiseIdField.setText(oldValue);
            }
        });
    }

    private void configurerTable() {
        // Rendre le tableau éditable
        commandeTable.setEditable(true);

        // Permettre la sélection de cellules individuelles
        commandeTable.getSelectionModel().setCellSelectionEnabled(true);

        // Colonne DATE (non éditable)
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate_creation();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(formatter) : ""
            );
        });

        // Colonne MONTANT (éditable avec validation)
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant_total"));
        colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter() {
            @Override
            public Double fromString(String value) {
                try {
                    double montant = Double.parseDouble(value);
                    if (montant <= 0) {
                        showAlert(Alert.AlertType.WARNING, "Attention", "Le montant doit être supérieur à 0");
                        return null;
                    }
                    return montant;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le montant doit être un nombre valide");
                    return null;
                }
            }
        }));
        colMontant.setOnEditCommit(event -> {
            Commande commande = event.getRowValue();
            Double newValue = event.getNewValue();
            if (newValue != null && newValue > 0) {
                commande.setMontant_total(newValue);
                sauvegarderModification(commande);
            } else {
                chargerDonnees();
            }
        });

        // Colonne STATUT (éditable avec ComboBox)
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(ComboBoxTableCell.forTableColumn(
                "En attente", "Confirmée", "Expédiée", "Livrée", "Annulée"
        ));
        colStatut.setOnEditCommit(event -> {
            Commande commande = event.getRowValue();
            commande.setStatut(event.getNewValue());
            sauvegarderModification(commande);
        });

        // Colonne FRANCHISE ID (éditable avec validation)
        colFranchiseId.setCellValueFactory(new PropertyValueFactory<>("franchise_id"));
        colFranchiseId.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                try {
                    int id = Integer.parseInt(value);
                    if (id <= 0) {
                        showAlert(Alert.AlertType.WARNING, "Attention", "L'ID franchise doit être supérieur à 0");
                        return null;
                    }
                    return id;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "L'ID franchise doit être un nombre entier valide");
                    return null;
                }
            }
        }));
        colFranchiseId.setOnEditCommit(event -> {
            Commande commande = event.getRowValue();
            Integer newValue = event.getNewValue();
            if (newValue != null && newValue > 0) {
                commande.setFranchise_id(newValue);
                sauvegarderModification(commande);
            } else {
                chargerDonnees();
            }
        });

        // Colonne Actions (avec bouton Supprimer uniquement)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(deleteBtn);

            {
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");

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

    private void sauvegarderModification(Commande commande) {
        try {
            commandeService.updateOne(commande);
            System.out.println("Commande modifiée: " + commande.getId());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
            chargerDonnees();
        }
    }

    private void chargerDonnees() {
        try {
            commandeList = FXCollections.observableArrayList(commandeService.selectAll());
            commandeTable.setItems(commandeList);
            System.out.println("Commandes chargées: " + commandeList.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjouter() {
        if (!validerChampsAjout()) return;

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
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        chargerDonnees();
    }

    @FXML
    private void handleEffacer() {
        viderFormulaire();
    }

    private void viderFormulaire() {
        montantField.clear();
        statutCombo.setValue(null);
        franchiseIdField.clear();
    }

    private void supprimerCommande(Commande commande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la commande");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette commande ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commandeService.deleteOne(commande);
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande supprimée avec succès!");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validerChampsAjout() {
        // Validation Montant
        String montantStr = montantField.getText();
        if (montantStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le montant est obligatoire");
            return false;
        }
        try {
            double montant = Double.parseDouble(montantStr);
            if (montant <= 0) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le montant doit être supérieur à 0");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le montant doit être un nombre valide");
            return false;
        }

        // Validation Statut
        if (statutCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un statut");
            return false;
        }

        // Validation Franchise ID
        String franchiseStr = franchiseIdField.getText();
        if (franchiseStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "L'ID franchise est obligatoire");
            return false;
        }
        try {
            int id = Integer.parseInt(franchiseStr);
            if (id <= 0) {
                showAlert(Alert.AlertType.WARNING, "Attention", "L'ID franchise doit être supérieur à 0");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "L'ID franchise doit être un nombre entier valide");
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