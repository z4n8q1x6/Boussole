package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.models.user;
import tn.esprit.boussole.service.franchiseService;
import tn.esprit.boussole.service.userService;

import java.sql.SQLException;

public class updateUserController {

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> comboRole;
    @FXML private CheckBox checkActif;
    
    @FXML private TextField txtNomEntreprise;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtAdresse;

    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnUpdate;

    private user currentUser;
    private Runnable onUserUpdated;
    private franchiseService franchiseService;

    @FXML
    public void initialize() {
        try {
            franchiseService = new franchiseService();
            
            if (comboRole != null) {
                comboRole.getItems().addAll("SIEGE", "ENTREPRISE");
            } else {
                System.err.println("Erreur: comboRole est null dans updateUserController");
            }
            
            setupButtonHoverEffects();
        } catch (Exception e) {
            System.err.println("Erreur dans initialize de updateUserController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initData(user u) {
        if (u == null) return;
        this.currentUser = u;
        
        if (txtNom != null) txtNom.setText(u.getNom());
        if (txtPrenom != null) txtPrenom.setText(u.getPrenom());
        if (txtEmail != null) txtEmail.setText(u.getEmail());
        if (comboRole != null) comboRole.setValue(u.getRole());
        
        if (checkActif != null) {
            checkActif.setSelected(u.getActif() != null && u.getActif());
        }
        
        if (u.getidFranchise() != null && u.getidFranchise() > 0) {
            loadFranchiseInfo(u.getidFranchise());
        } else {
            clearFranchiseInfo();
        }
    }

    private void loadFranchiseInfo(int idFranchise) {
        try {
            if (franchiseService == null) return;
            franchise f = franchiseService.getById(idFranchise);
            if (f != null) {
                if (txtNomEntreprise != null) txtNomEntreprise.setText(f.getNom());
                if (txtTelephone != null) txtTelephone.setText(f.getTelephone());
                if (txtAdresse != null) txtAdresse.setText(f.getAdresse());
            } else {
                if (txtNomEntreprise != null) txtNomEntreprise.setText("Franchise introuvable (ID: " + idFranchise + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (txtNomEntreprise != null) txtNomEntreprise.setText("Erreur chargement");
        }
    }

    private void clearFranchiseInfo() {
        if (txtNomEntreprise != null) txtNomEntreprise.setText("Aucune franchise liée");
        if (txtTelephone != null) txtTelephone.setText("");
        if (txtAdresse != null) txtAdresse.setText("");
    }

    @FXML
    private void handleUpdate() {
        if (!validateFields()) return;

        try {
            currentUser.setNom(txtNom.getText().trim());
            currentUser.setPrenom(txtPrenom.getText().trim());
            currentUser.setEmail(txtEmail.getText().trim());
            currentUser.setRole(comboRole.getValue());
            currentUser.setActif(checkActif.isSelected());
            
            // L'ID franchise reste inchangé (on ne le modifie pas ici)

            if (txtPassword != null && !txtPassword.getText().isEmpty()) {
                currentUser.setMotDePasse(txtPassword.getText());
            }

            userService service = new userService();
            service.updateone(currentUser);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur mis à jour avec succès !");

            if (onUserUpdated != null) onUserUpdated.run();
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour : " + e.getMessage());
        }
    }

    private boolean validateFields() {
        if (txtNom == null || txtPrenom == null || txtEmail == null) return false;
        if (txtNom.getText().trim().isEmpty() || txtPrenom.getText().trim().isEmpty() || txtEmail.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Veuillez remplir tous les champs obligatoires.");
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
            btnUpdate.setOnMouseEntered(e -> btnUpdate.setStyle("-fx-background-color: linear-gradient(to right, #2980B9, #21618C); -fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;"));
            btnUpdate.setOnMouseExited(e -> btnUpdate.setStyle("-fx-background-color: linear-gradient(to right, #3498DB, #2980B9); -fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;"));
        }
        
        if (btnCancel != null) {
            btnCancel.setOnMouseEntered(e -> btnCancel.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-border-color: #7F8C8D; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;"));
            btnCancel.setOnMouseExited(e -> btnCancel.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #BDC3C7; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;"));
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setOnUserUpdated(Runnable callback) {
        this.onUserUpdated = callback;
    }
}
