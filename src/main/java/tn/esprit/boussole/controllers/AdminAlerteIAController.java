package tn.esprit.boussole.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.boussole.models.AlerteIA;
import tn.esprit.boussole.services.AlerteIAService;
import tn.esprit.boussole.utils.AlertUtil;
import tn.esprit.boussole.utils.PDFGenerator;

public class AdminAlerteIAController {
  AlerteIAService service = new AlerteIAService();
  @FXML private TableView<AlerteIA> table;

  @FXML private TableColumn<AlerteIA, String> colType;

  @FXML private TableColumn<AlerteIA, Float> colScore;

  @FXML private TableColumn<AlerteIA, java.util.Date> colDate;

  @FXML private TableColumn<AlerteIA, String> colFranchise;

  @FXML private TextArea messageArea;
  @FXML private Button pdfButton;

  public void initialize() {
    colType.setCellValueFactory(new PropertyValueFactory<>("type_alerte"));
    colScore.setCellValueFactory(new PropertyValueFactory<>("score_gravite"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("date_detection"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("date_detection"));
    colFranchise.setCellValueFactory(
        cellData -> {
          return new SimpleStringProperty("nom/ville#" + cellData.getValue().getFranchiseId());
        });

    // When a user clicks a row, update the "Details" TextArea on the right
    table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSelection, newSelection) -> {
              if (newSelection != null) {
                messageArea.setText(newSelection.getMessage());
              } else {
                messageArea.setText("Veuillez sélectionner une alerte pour voir les détails...");
              }
            });

    display();
  }

  @FXML
  public void delete() {
    AlerteIA selected = table.getSelectionModel().getSelectedItem();

    if (selected != null) {
      ButtonType result = AlertUtil.showConfirmation("Confirmation", "Supprimer cette alerte?");

      if (result == ButtonType.YES) {
        if (service.delete(selected.getId())) {
          display();
          System.out.println("Alerte deleted successfully.");
        }
      }
    } else {
      AlertUtil.showWarning("Aucune sélection", "Veuillez sélectionner une alerte à supprimer.");
    }
  }

  void display() {
    table.setItems(service.getAll());
  }

  @FXML
  public void pdf() {
    Stage stage = (Stage) pdfButton.getScene().getWindow();
    String result = PDFGenerator.generateAlertePDF(stage);
    if (result.equals("generated")) {
      AlertUtil.showInformation("Export Success", "PDF saved successfully!");
      System.out.println("PDF generated.");
    } else if (!result.isEmpty()) {
      AlertUtil.showError("Export Failed", "Error: " + result);
      System.out.println("PDF NOT generated.");
    }
  }
}
