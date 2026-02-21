package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Reclamation;
import tn.esprit.boussole.service.ReclamationService;

public class AddReclamationController {

  @FXML private TextField txtSujet;
  @FXML private TextArea txtDescription;
  @FXML private Label lblError;
  @FXML private Button btnCreate;

  private final ReclamationService reclamationService = new ReclamationService();
  private int franchiseId;

  public void initialize() {}

  // Call this before showing the dialog so the controller knows which franchise
  public void setFranchiseId(int franchiseId) {
    this.franchiseId = franchiseId;
  }

  @FXML
  private void handleCreate() {
    if (!validate()) return;

    Reclamation reclamation = new Reclamation();
    reclamation.setSujet(txtSujet.getText().trim());
    reclamation.setDescription(txtDescription.getText().trim());
    reclamation.setFranchiseId(franchiseId);

    boolean success = reclamationService.add(reclamation);

    if (success) {
      closeWindow();
    } else {
      showError("Erreur lors de la création. Veuillez réessayer.");
    }
  }

  @FXML
  private void handleCancel() {
    closeWindow();
  }

  private boolean validate() {
    if (txtSujet.getText().trim().isEmpty()) {
      showError("Le sujet est obligatoire.");
      return false;
    }
    if (txtDescription.getText().trim().isEmpty()) {
      showError("La description est obligatoire.");
      return false;
    }
    hideError();
    return true;
  }

  private void showError(String message) {
    lblError.setText(message);
    lblError.setVisible(true);
    lblError.setManaged(true);
  }

  private void hideError() {
    lblError.setVisible(false);
    lblError.setManaged(false);
  }

  private void closeWindow() {
    Stage stage = (Stage) btnCreate.getScene().getWindow();
    stage.close();
  }
}
