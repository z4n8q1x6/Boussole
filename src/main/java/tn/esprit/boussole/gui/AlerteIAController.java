package tn.esprit.boussole.gui;

import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.boussole.models.AlerteIA;
import tn.esprit.boussole.service.AlerteIAService;
import tn.esprit.boussole.utils.AlertUtil;
import tn.esprit.boussole.utils.Gemini;
import tn.esprit.boussole.utils.UserManager;

public class AlerteIAController {
  AlerteIAService service = new AlerteIAService();
  @FXML private TableView<AlerteIA> table;

  @FXML private TableColumn<AlerteIA, String> colType;

  @FXML private TableColumn<AlerteIA, Float> colScore;

  @FXML private TableColumn<AlerteIA, java.util.Date> colDate;

  @FXML private TextArea messageArea;

  private int franchise_id = -1;


  public void initialize() {
    this.franchise_id = UserManager.getCurrentUserFranchiseId();
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
      AlertUtil.showError("Erreur", "Votre clé API est invalide.");
    }
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
      AlertUtil.showWarning("Aucune séléction", "Veuillez sélectionner une alerte à supprimer.");
    }
  }

  void display() {
    table.setItems(service.getByFranchise(franchise_id));
  }
}
