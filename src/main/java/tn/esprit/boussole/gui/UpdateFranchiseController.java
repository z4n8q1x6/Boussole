package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.franchiseService;

public class UpdateFranchiseController {

    @FXML private TextField txtNom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtAdresse;
    @FXML private TextField txtSolde;
    @FXML private CheckBox checkActif;

    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnUpdate;

    private franchise currentFranchise;
    private Runnable onFranchiseUpdated;
    private franchiseService franchiseService;

    @FXML
    public void initialize() {
        try {
            franchiseService = new franchiseService();
            setupButtonHoverEffects();
        } catch (Exception e) {
            System.err.println("Erreur dans initialize de updateFranchiseController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initData(franchise f) {
        if (f == null) return;
        this.currentFranchise = f;
        
        if (txtNom != null) txtNom.setText(f.getNom());
        if (txtEmail != null) txtEmail.setText(f.getEmail());
        if (txtTelephone != null) txtTelephone.setText(f.getTelephone());
        if (txtAdresse != null) txtAdresse.setText(f.getAdresse());
        if (txtSolde != null) txtSolde.setText(String.valueOf(f.getSoldeActuel()));
        
        if (checkActif != null) {
            checkActif.setSelected(f.getActif() != null && f.getActif());
        }
    }

    @FXML
    private void handleUpdate() {
        if (!validateFields()) return;

        try {
            currentFranchise.setNom(txtNom.getText().trim());
            currentFranchise.setEmail(txtEmail.getText().trim());
            currentFranchise.setTelephone(txtTelephone.getText().trim());
            currentFranchise.setAdresse(txtAdresse.getText().trim());
            currentFranchise.setActif(checkActif.isSelected());
            
            try {
                currentFranchise.setSoldeActuel(Double.parseDouble(txtSolde.getText()));
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Format invalide", "Le solde doit être un nombre valide.");
                return;
            }

            franchiseService.updateone(currentFranchise);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Entreprise mise à jour avec succès !");

            if (onFranchiseUpdated != null) {
                onFranchiseUpdated.run();
            }
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour : " + e.getMessage());
        }
    }

    private boolean validateFields() {
        if (txtNom == null || txtEmail == null) return false;
        if (txtNom.getText().trim().isEmpty() || txtEmail.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Le nom et l'email sont obligatoires.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void closeWindow() {
        if (btnClose != null && btnClose.getScene() != null) {
            Stage stage = (Stage) btnClose.getScene().getWindow();
            stage.close();
        }
    }

    private void setupButtonHoverEffects() {
        if (btnUpdate != null) {
            btnUpdate.setOnMouseEntered(e -> btnUpdate.setStyle("-fx-background-color: #2980B9; -fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;"));
            btnUpdate.setOnMouseExited(e -> btnUpdate.setStyle("-fx-background-color: #3498DB; -fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;"));
        }
        
        if (btnCancel != null) {
            btnCancel.setOnMouseEntered(e -> btnCancel.setStyle("-fx-background-color: #F8F9FA; -fx-border-color: #7F8C8D; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;"));
            btnCancel.setOnMouseExited(e -> btnCancel.setStyle("-fx-background-color: white; -fx-border-color: #BDC3C7; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;"));
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setOnFranchiseUpdated(Runnable callback) {
        this.onFranchiseUpdated = callback;
    }
}
