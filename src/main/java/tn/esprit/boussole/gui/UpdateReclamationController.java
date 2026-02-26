package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Reclamation;
import tn.esprit.boussole.service.ReclamationService;

public class UpdateReclamationController {

  @FXML private ComboBox<String> comboStatut;
  @FXML private Label lblSujet;
  @FXML private Label lblError;
  @FXML private Button btnUpdate;

  private final ReclamationService service = new ReclamationService();
  private Reclamation reclamation;

  public void initialize() {
    comboStatut.setItems(FXCollections.observableArrayList("EN_ATTENTE", "EN_COURS", "RESOLU"));
  }

  public void setReclamation(Reclamation reclamation) {
    this.reclamation = reclamation;
    lblSujet.setText(reclamation.getSujet());
    comboStatut.setValue(reclamation.getStatut().toString());
  }

  @FXML
  private void handleUpdate() {
    if (comboStatut.getValue() == null) {
      showError("Veuillez sélectionner un statut.");
      return;
    }
    if (service.updateStatus(reclamation.getId(), comboStatut.getValue())) {
      closeWindow();
    } else {
      showError("Erreur lors de la mise à jour.");
    }
  }

  @FXML
  private void handleCancel() {
    closeWindow();
  }

  private void showError(String message) {
    lblError.setText(message);
    lblError.setVisible(true);
    lblError.setManaged(true);
  }

  private void closeWindow() {
    Stage stage = (Stage) btnUpdate.getScene().getWindow();
    stage.close();
  }
}
