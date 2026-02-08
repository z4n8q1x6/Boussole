package com.boussole.app.controllers;

import com.boussole.app.models.AlerteIA;
import com.boussole.app.services.AlerteIAService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import com.boussole.app.utils.PDFGenerator;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

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
    colFranchise.setCellValueFactory(cellData -> {
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
      Alert alert =
          new Alert(
              Alert.AlertType.CONFIRMATION,
              "Supprimer cette alerte?",
              ButtonType.YES,
              ButtonType.NO);
      alert.showAndWait();

      if (alert.getResult() == ButtonType.YES) {
        if (service.delete(selected.getId())) {
          display();
          System.out.println("Alerte deleted successfully.");
        }
      }
    } else {
      Alert alert =
          new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une alerte à supprimer.");
      alert.setHeaderText("Aucune sélection");
      alert.showAndWait();
    }
  }

  void display() {
    table.setItems(service.getAll());
  }

  @FXML
  public void pdf() {
    Stage stage = (Stage) pdfButton.getScene().getWindow();
    String result = PDFGenerator.generateAlertePDF(stage);
    if(result.equals("generated")){
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle("Export Success");
      alert.setContentText("PDF saved successfully!");
      alert.showAndWait();
      System.out.println("PDF generated.");
    }else if(!result.isEmpty()){
      Alert alert = new Alert(AlertType.ERROR);
      alert.setTitle("Export Failed");
      alert.setContentText("Error: " + result);
      alert.showAndWait();
      System.out.println("PDF NOT generated.");
    }
  }
}
