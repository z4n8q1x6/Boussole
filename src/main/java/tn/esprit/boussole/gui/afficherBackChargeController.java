package tn.esprit.boussole.gui;

import java.io.IOException;
import java.sql.SQLException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
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
import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.service.ChargeService;
import tn.esprit.boussole.service.CurrencyService;

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
  @FXML private ComboBox<String> comboTri;
  @FXML private Label lblTotal;
  @FXML private TextField txtRecherche;

  @FXML private Label lblTaux;
  @FXML private Label lblTotalEur;
  private final CurrencyService currencyService = new CurrencyService();
  private double tauxActuel = 0.30;

  private final ChargeService chargeService = new ChargeService();
  private final ObservableList<Charge> chargesList = FXCollections.observableArrayList();
  private FilteredList<Charge> filteredData;
  private SortedList<Charge> sortedData;

  @FXML
  public void initialize() {
    tableCharges.setEditable(true);

    // --- CONFIGURATION COLONNE TITRE ---
    colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
    colTitre.setCellFactory(TextFieldTableCell.forTableColumn());
    colTitre.setOnEditCommit(
        event -> {
          String nouveauTitre = event.getNewValue();
          Charge c = event.getRowValue();
          if (nouveauTitre != null && nouveauTitre.trim().matches(".*[a-zA-Z].*")) {
            c.setTitre(nouveauTitre.trim());
            updateChargeInDB(c);
          } else {
            showAlert(
                "Erreur de saisie", "Le titre ne peut pas être composé uniquement de chiffres.");
            tableCharges.refresh();
          }
        });

    // --- CONFIGURATION COLONNE MONTANT ---
    colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
    colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
    colMontant.setOnEditCommit(
        event -> {
          Charge c = event.getRowValue();
          c.setMontant(event.getNewValue());
          updateChargeInDB(c);
          calculerTotal();
        });

    colDate.setCellValueFactory(
        cellData -> new SimpleStringProperty(cellData.getValue().getDateCharge().toString()));

    colType.setCellValueFactory(new PropertyValueFactory<>("type"));
    colType.setCellFactory(ComboBoxTableCell.forTableColumn(Charge.TypeCharge.values()));
    colType.setOnEditCommit(
        event -> {
          Charge c = event.getRowValue();
          c.setType(event.getNewValue());
          updateChargeInDB(c);
        });

    colStatus.setCellValueFactory(new PropertyValueFactory<>("statusValidation"));
    colStatus.setCellFactory(ComboBoxTableCell.forTableColumn(Charge.StatusValidation.values()));
    colStatus.setOnEditCommit(
        event -> {
          Charge c = event.getRowValue();
          c.setStatusValidation(event.getNewValue());
          updateChargeInDB(c);
        });

    colFranchiseId.setCellValueFactory(new PropertyValueFactory<>("franchiseName"));

    addModifierButtonToTable();
    addSupprimerButtonToTable();

    // --- RECHERCHE ET TRI ---
    filteredData = new FilteredList<>(chargesList, p -> true);
    sortedData = new SortedList<>(filteredData);
    sortedData.comparatorProperty().bind(tableCharges.comparatorProperty());
    tableCharges.setItems(sortedData);

    txtRecherche
        .textProperty()
        .addListener(
            (obs, old, newValue) -> {
              filteredData.setPredicate(
                  charge -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    return charge.getTitre().toLowerCase().contains(newValue.toLowerCase());
                  });
              calculerTotal();
            });

    comboTri.setItems(
        FXCollections.observableArrayList("Montant Croissant", "Montant Décroissant"));
    comboTri.setOnAction(e -> trierCharges());

    loadCharges();
    chargerTauxDeChange();

    btnAjouter.setOnAction(e -> openAjoutForm());
    // LOGIQUE btnFront SUPPRIMÉE
  }

  private void chargerTauxDeChange() {
    Task<Double> task =
        new Task<>() {
          @Override
          protected Double call() {
            return currencyService.getTauxTndVersEur();
          }
        };
    task.setOnSucceeded(
        e -> {
          tauxActuel = task.getValue();
          if (lblTaux != null) lblTaux.setText(String.format("1 TND = %.4f EUR", tauxActuel));
          calculerTotal();
        });
    task.setOnFailed(
        e -> {
          if (lblTaux != null) lblTaux.setText("1 TND = 0.3000 EUR (Hors ligne)");
          calculerTotal();
        });
    new Thread(task).start();
  }

  private void calculerTotal() {
    double total = tableCharges.getItems().stream().mapToDouble(Charge::getMontant).sum();
    lblTotal.setText(String.format("Total : %.2f DT", total));
    if (lblTotalEur != null) lblTotalEur.setText(String.format("%.2f €", total * tauxActuel));
  }

  private void updateChargeInDB(Charge charge) {
    try {
      chargeService.updateone(charge);
    } catch (SQLException e) {
      showAlert("Erreur", "MAJ impossible : " + e.getMessage());
      loadCharges();
    }
  }

  private void loadCharges() {
    chargesList.clear();
    try {
      chargesList.addAll(chargeService.selectAll(null));
      calculerTotal();
    } catch (SQLException e) {
      showAlert("Erreur", "Chargement impossible : " + e.getMessage());
    }
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
    colModifier.setCellFactory(
        param ->
            new TableCell<>() {
              private final Button btn = new Button("✎");

              {
                btn.setStyle(
                    "-fx-background-color: rgba(0, 200, 180, 0.15);"
                        + "-fx-text-fill: #00C4B4;"
                        + "-fx-cursor: hand;"
                        + "-fx-background-radius: 6;"
                        + "-fx-font-size: 14px;"
                        + "-fx-padding: 4 10 4 10;");
                btn.setOnAction(e -> openModifierForm(getTableView().getItems().get(getIndex())));
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
              }
            });
  }

  private void addSupprimerButtonToTable() {
    colSupprimer.setCellFactory(
        param ->
            new TableCell<>() {
              private final Button btn = new Button("⊖");

              {
                btn.setStyle(
                    "-fx-background-color: rgba(231, 76, 60, 0.15);"
                        + "-fx-text-fill: #E74C3C;"
                        + "-fx-cursor: hand;"
                        + "-fx-background-radius: 6;"
                        + "-fx-font-size: 14px;"
                        + "-fx-padding: 4 10 4 10;");
                btn.setOnAction(e -> supprimerCharge(getTableView().getItems().get(getIndex())));
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
              }
            });
  }

  private void supprimerCharge(Charge charge) {
    try {
      chargeService.deleteone(charge);
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

  private void showAlert(String titre, String msg) {
    javafx.application.Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle(titre);
      alert.setHeaderText(null);
      alert.setContentText(msg);
      DialogPane dialogPane = alert.getDialogPane();
      String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
      dialogPane.getStylesheets().add(css);
      dialogPane.getStyleClass().add("dialog-pane");
      alert.showAndWait();
    });
  }
}
