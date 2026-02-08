package tn.esprit.boussole.controllers;

import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.boussole.models.AlerteIA;
import tn.esprit.boussole.services.AlerteIAService;
import tn.esprit.boussole.utils.Gemini;

public class AlerteIAController {
  AlerteIAService service = new AlerteIAService();
  @FXML private TableView<AlerteIA> table;

  @FXML private TableColumn<AlerteIA, String> colType;

  @FXML private TableColumn<AlerteIA, Float> colScore;

  @FXML private TableColumn<AlerteIA, java.util.Date> colDate;

  @FXML private TextArea messageArea;

  // temp
  private int franchise_id = 2;

  public void initialize() {
    colType.setCellValueFactory(new PropertyValueFactory<>("type_alerte"));
    colScore.setCellValueFactory(new PropertyValueFactory<>("score_gravite"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("date_detection"));

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
  public void generate() {
    Optional<AlerteIA> result = Gemini.generate_alerte();
    if (result.isPresent()) {
      AlerteIA alerteIA = result.get();
      alerteIA.setFranchiseId(franchise_id);
      if (service.add(alerteIA)) {
        display();
        System.out.println("Alerte added successfully.");
      } else {
        System.out.println("Failed to add reclamation.");
      }
    } else {
      Alert alert = new Alert(Alert.AlertType.ERROR, "Your API key is invalid.");
      alert.show();
    }
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
    table.setItems(service.getByFranchise(franchise_id));
  }
}
