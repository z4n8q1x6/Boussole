package tn.esprit.chargesdepenses.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.services.ChargeService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;

public class afficherBackChargeController {
    @FXML
    private TableView<Charge> tableCharges;
    @FXML
    private TableColumn<Charge, String> colTitre;
    @FXML
    private TableColumn<Charge, Double> colMontant;
    @FXML
    private TableColumn<Charge, String> colDate;
    @FXML
    private TableColumn<Charge, String> colType;
    @FXML
    private TableColumn<Charge, String> colPreuve;
    @FXML
    private TableColumn<Charge, String> colStatus;
    @FXML
    private TableColumn<Charge, Integer> colFranchiseId;
    @FXML
    private TableColumn<Charge, Void> colModifier;
    @FXML
    private TableColumn<Charge, Void> colSupprimer;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnFront;
    @FXML
    private ComboBox<String> comboTri;
    @FXML
    private Label lblTotal;
    @FXML
    private TextField txtRecherche;

    private final ChargeService chargeService = new ChargeService();
    private final ObservableList<Charge> chargesList = FXCollections.observableArrayList();
    private FilteredList<Charge> filteredData;
    private SortedList<Charge> sortedData;

    @FXML
    public void initialize() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDate.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().getDateCharge().toString()));
        colType.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().getType().name()));
        colPreuve.setCellValueFactory(new PropertyValueFactory<>("preuveImage"));
        colStatus.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().getStatusValidation().name()));
        colFranchiseId.setCellValueFactory(new PropertyValueFactory<>("franchiseId"));

        addModifierButtonToTable();
        addSupprimerButtonToTable();

        // Initialisation de la liste filtrée et triée
        filteredData = new FilteredList<>(chargesList, p -> true);
        sortedData = new SortedList<>(filteredData);
        
        // Lier le comparateur de la SortedList au TableView
        sortedData.comparatorProperty().bind(tableCharges.comparatorProperty());
        
        tableCharges.setItems(sortedData);

        // Listener pour la recherche
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(charge -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return charge.getTitre().toLowerCase().contains(lowerCaseFilter);
            });
            calculerTotal(); // Recalculer le total basé sur les éléments filtrés
        });

        // Initialisation du ComboBox de tri
        comboTri.setItems(FXCollections.observableArrayList("Montant Croissant", "Montant Décroissant"));
        comboTri.setOnAction(e -> trierCharges());

        loadCharges();
        btnAjouter.setOnAction(e -> openAjoutForm());
        btnFront.setOnAction(e -> openFrontOffice());
    }

    private void openFrontOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherFrontCharge.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Charges & Dépenses - Front Office");
            stage.show();
            // Optionnel : fermer la fenêtre actuelle
            // ((Stage) btnFront.getScene().getWindow()).close();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le Front Office: " + e.getMessage());
        }
    }

    private void trierCharges() {
        String selection = comboTri.getValue();
        if (selection != null) {
            if (selection.equals("Montant Croissant")) {
                tableCharges.getSortOrder().clear();
                colMontant.setSortType(TableColumn.SortType.ASCENDING);
                tableCharges.getSortOrder().add(colMontant);
                tableCharges.sort();
            } else if (selection.equals("Montant Décroissant")) {
                tableCharges.getSortOrder().clear();
                colMontant.setSortType(TableColumn.SortType.DESCENDING);
                tableCharges.getSortOrder().add(colMontant);
                tableCharges.sort();
            }
        }
    }

    private void loadCharges() {
        chargesList.clear();
        try {
            chargesList.addAll(chargeService.selectAll());
            calculerTotal();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les charges: " + e.getMessage());
        }
    }

    private void calculerTotal() {
        // Calculer le total uniquement sur les éléments visibles (filtrés)
        double total = tableCharges.getItems().stream()
                .mapToDouble(Charge::getMontant)
                .sum();
        lblTotal.setText(String.format("%.2f DT", total));
    }

    private void addModifierButtonToTable() {
        colModifier.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.control.Button btn = new javafx.scene.control.Button("✎");
            {
                btn.setStyle("-fx-background-color: #4593cb; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                btn.setOnAction(event -> {
                    Charge charge = getTableView().getItems().get(getIndex());
                    openModifierForm(charge);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addSupprimerButtonToTable() {
        colSupprimer.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.control.Button btn = new javafx.scene.control.Button("🗑");
            {
                btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                btn.setOnAction(event -> {
                    Charge charge = getTableView().getItems().get(getIndex());
                    supprimerCharge(charge);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
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
            loadCharges(); 
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
        }
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCharge.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter une charge");
            stage.showAndWait();
            loadCharges(); 
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage());
        }
    }

    private void supprimerCharge(Charge charge) {
        try {
            chargeService.deleteOne(charge);
            chargesList.remove(charge);
            calculerTotal();
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
