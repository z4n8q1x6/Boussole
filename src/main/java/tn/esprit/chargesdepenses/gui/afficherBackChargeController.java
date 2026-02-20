package tn.esprit.chargesdepenses.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.services.ChargeService;
import java.io.IOException;
import java.sql.SQLException;

public class afficherBackChargeController {
    @FXML private TableView<Charge> tableCharges;
    @FXML private TableColumn<Charge, String> colTitre;
    @FXML private TableColumn<Charge, Double> colMontant;
    @FXML private TableColumn<Charge, String> colDate;
    @FXML private TableColumn<Charge, Charge.TypeCharge> colType;
    @FXML private TableColumn<Charge, Charge.StatusValidation> colStatus;
    @FXML private TableColumn<Charge, String> colFranchiseId;
    @FXML private TableColumn<Charge, Void> colModifier;
    @FXML private TableColumn<Charge, Void> colSupprimer;
    @FXML private Button btnAjouter;
    @FXML private Button btnFront;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label lblTotal;
    @FXML private TextField txtRecherche;

    private final ChargeService chargeService = new ChargeService();
    private final ObservableList<Charge> chargesList = FXCollections.observableArrayList();
    private FilteredList<Charge> filteredData;
    private SortedList<Charge> sortedData;

    @FXML
    public void initialize() {
        tableCharges.setEditable(true);

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(TextFieldTableCell.forTableColumn());
        colTitre.setOnEditCommit(event -> {
            Charge c = event.getRowValue();
            c.setTitre(event.getNewValue());
            updateChargeInDB(c);
        });

        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMontant.setOnEditCommit(event -> {
            Charge c = event.getRowValue();
            c.setMontant(event.getNewValue());
            updateChargeInDB(c);
            calculerTotal();
        });

        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateCharge().toString()));

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(ComboBoxTableCell.forTableColumn(Charge.TypeCharge.values()));
        colType.setOnEditCommit(event -> {
            Charge c = event.getRowValue();
            c.setType(event.getNewValue());
            updateChargeInDB(c);
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusValidation"));
        colStatus.setCellFactory(ComboBoxTableCell.forTableColumn(Charge.StatusValidation.values()));
        colStatus.setOnEditCommit(event -> {
            Charge c = event.getRowValue();
            c.setStatusValidation(event.getNewValue());
            updateChargeInDB(c);
        });

        // Affichage du NOM de la franchise
        colFranchiseId.setCellValueFactory(new PropertyValueFactory<>("franchiseName"));

        addModifierButtonToTable();
        addSupprimerButtonToTable();

        filteredData = new FilteredList<>(chargesList, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableCharges.comparatorProperty());
        tableCharges.setItems(sortedData);

        txtRecherche.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(charge -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return charge.getTitre().toLowerCase().contains(newValue.toLowerCase());
            });
            calculerTotal();
        });

        comboTri.setItems(FXCollections.observableArrayList("Montant Croissant", "Montant Décroissant"));
        comboTri.setOnAction(e -> trierCharges());

        loadCharges();
        btnAjouter.setOnAction(e -> openAjoutForm());
        if (btnFront != null) btnFront.setOnAction(e -> openFrontOffice());
    }

    private void updateChargeInDB(Charge charge) {
        try {
            chargeService.updateOne(charge);
        } catch (SQLException e) {
            showAlert("Erreur", "MAJ impossible : " + e.getMessage());
            loadCharges();
        }
    }

    private void loadCharges() {
        chargesList.clear();
        try {
            chargesList.addAll(chargeService.selectAll());
            calculerTotal();
        } catch (SQLException e) {
            showAlert("Erreur", "Chargement impossible : " + e.getMessage());
        }
    }

    private void calculerTotal() {
        double total = tableCharges.getItems().stream().mapToDouble(Charge::getMontant).sum();
        lblTotal.setText(String.format("Total : %.2f DT", total));
    }

    private void trierCharges() {
        String selection = comboTri.getValue();
        if ("Montant Croissant".equals(selection)) {
            colMontant.setSortType(TableColumn.SortType.ASCENDING);
        } else {
            colMontant.setSortType(TableColumn.SortType.DESCENDING);
        }
        tableCharges.getSortOrder().setAll(colMontant);
    }

    private void addModifierButtonToTable() {
        colModifier.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("✎");
            {
                btn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> openModifierForm(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addSupprimerButtonToTable() {
        colSupprimer.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("🗑");
            {
                btn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> supprimerCharge(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void supprimerCharge(Charge charge) {
        try {
            chargeService.deleteOne(charge);
            chargesList.remove(charge);
            calculerTotal();
        } catch (SQLException e) {
            showAlert("Erreur", "Suppression impossible : " + e.getMessage());
        }
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCharge.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait();
            loadCharges();
        } catch (IOException e) {
            showAlert("Erreur", "Ouverture impossible : " + e.getMessage());
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
            stage.showAndWait();
            loadCharges();
        } catch (IOException e) {
            showAlert("Erreur", "Ouverture impossible : " + e.getMessage());
        }
    }

    private void openFrontOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherFrontCharge.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Front Office inaccessible : " + e.getMessage());
        }
    }

    private void showAlert(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
