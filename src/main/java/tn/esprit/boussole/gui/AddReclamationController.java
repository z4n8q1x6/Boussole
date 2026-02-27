package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Reclamation;
import tn.esprit.boussole.service.ReclamationService;
import tn.esprit.boussole.utils.UserManager;

public class AddReclamationController {

  @FXML private TextField txtSujet;
  @FXML private TextArea txtDescription;
  @FXML private Label lblError;
  @FXML private Button btnCreate;

  private final ReclamationService reclamationService = new ReclamationService();
  private int franchiseId = -1;

  // Flag pour s'assurer que UserManager a la priorité sur setFranchiseId
  private boolean initializedByUserManager = false;

  public void initialize() {
    System.out.println("AddReclamationController DEBUG: initialize() appelée.");

    // Auto-populate franchise ID from logged-in user
    int fetchedFranchiseId = UserManager.getCurrentUserFranchiseId();
    System.out.println("AddReclamationController DEBUG: ID récupéré par UserManager = " + fetchedFranchiseId);

    if (!UserManager.isValidFranchiseId(fetchedFranchiseId)) {
      System.err.println("AddReclamationController DEBUG: Franchise ID invalide (-1 ou 0)");
      showError("Erreur: Aucune franchise assignée à votre compte.");
    } else {
      this.franchiseId = fetchedFranchiseId;
      this.initializedByUserManager = true; // --- Marquer comme initialisé ---
      System.out.println("AddReclamationController DEBUG: Variable classe franchiseId mise à jour avec: " + this.franchiseId);
    }
  }

  // Call this before showing the dialog so the controller knows which franchise
  public void setFranchiseId(int franchiseId) {
    // --- Ignorer si UserManager a déjà fait le travail ---
    if (this.initializedByUserManager) {
      System.out.println("AddReclamationController DEBUG: setFranchiseId(" + franchiseId + ") ignoré (déjà initialisé avec " + this.franchiseId + ")");
      return;
    }
    // -----------------------------------------------------

    System.out.println("AddReclamationController DEBUG: setFranchiseId() appelée avec: " + franchiseId);
    this.franchiseId = franchiseId;
  }

  @FXML
  private void handleCreate() {
    if (!validate()) return;

    // --- LOG DE DÉBOGAGE ---
    System.out.println("AddReclamationController DEBUG: Tentative d'ajout réclamation avec franchiseId = " + this.franchiseId);
    // -----------------------

    if (!UserManager.isValidFranchiseId(franchiseId)) {
      showError("Erreur: Franchise invalide. Veuillez vous reconnecter.");
      return;
    }

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
    String sujet = txtSujet.getText().trim();
    if (sujet.isEmpty()) {
      showError("Le sujet est obligatoire.");
      return false;
    }
    if (Character.isDigit(sujet.charAt(0))) {
      showError("Le sujet ne doit pas commencer par un chiffre.");
      return false;
    }

    String description = txtDescription.getText().trim();
    if (description.isEmpty()) {
      showError("La description est obligatoire.");
      return false;
    }
    if (Character.isDigit(description.charAt(0))) {
      showError("La description ne doit pas commencer par un chiffre.");
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