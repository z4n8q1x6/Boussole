package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.boussole.models.user;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.userService;

import java.time.LocalDateTime;

public class addUserController {

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtAdresse;
    @FXML private TextField txtSolde;
    @FXML private TextField txtNomEntreprise;
    @FXML private TextField txtAdresseEntreprise;
    @FXML private CheckBox checkActif;

    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnCreate;

    private Runnable onUserCreated;

    @FXML
    public void initialize() {
        // Valeurs par défaut
        txtSolde.setText("0.00");
        checkActif.setSelected(true);

        setupButtonHoverEffects();

        // Validation de l'email en temps réel
        txtEmail.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateEmail();
        });

        // Validation numérique pour le solde
        txtSolde.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) txtSolde.setText(oldVal);
        });
        
        // Validation pour le téléphone (uniquement des chiffres)
        txtTelephone.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtTelephone.setText(oldVal);
            }
        });
    }

    @FXML
    private void handleCreate() {
        if (!validateFields()) return;

        try {
            // Création de l'objet user
            user u = new user();
            u.setNom(txtNom.getText().trim());
            u.setPrenom(txtPrenom.getText().trim());
            u.setEmail(txtEmail.getText().trim());
            u.setMotDePasse(txtPassword.getText()); // ⚠️ Hash en production avec BCrypt
            u.setRole("ENTREPRISE"); // Rôle forcé
            u.setActif(checkActif.isSelected());
            u.setDateCreation(LocalDateTime.now());

            userService service = new userService();

            // Créer franchise + user
            franchise f = new franchise();
            f.setNom(txtNomEntreprise.getText().trim());
            f.setAdresse(txtAdresseEntreprise.getText().trim());
            f.setEmail(txtEmail.getText().trim());
            f.setTelephone(txtTelephone.getText().trim());
            f.setActif(checkActif.isSelected());
            f.setSoldeActuel(Double.parseDouble(txtSolde.getText()));
            f.setDateCreation(LocalDateTime.now());

            service.insertUserWithFranchise(u, f);

            showAlert(Alert.AlertType.INFORMATION,
                    "Succès",
                    "L'entreprise " + f.getNom() + " et son responsable ont été créés avec succès !");

            // Appeler le callback pour rafraîchir la liste
            if (onUserCreated != null) onUserCreated.run();

            // Fermer la fenêtre
            closeWindow();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Format numérique invalide pour le solde.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de créer l'entreprise : " + e.getMessage());
        }
    }

    private boolean validateFields() {
        // Validation du nom
        if (txtNom.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Nom obligatoire", "Le nom du responsable est obligatoire.");
            txtNom.requestFocus();
            return false;
        }

        // Validation du prénom
        if (txtPrenom.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Prénom obligatoire", "Le prénom du responsable est obligatoire.");
            txtPrenom.requestFocus();
            return false;
        }

        // Validation de l'email
        if (txtEmail.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Email obligatoire", "L'email est obligatoire.");
            txtEmail.requestFocus();
            return false;
        }

        if (!isValidEmail(txtEmail.getText())) {
            showAlert(Alert.AlertType.WARNING, "Email invalide", "Veuillez entrer une adresse email valide.");
            txtEmail.requestFocus();
            return false;
        }

        // Validation du mot de passe
        if (txtPassword.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Mot de passe obligatoire", "Le mot de passe est obligatoire.");
            txtPassword.requestFocus();
            return false;
        }

        if (txtPassword.getText().length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Mot de passe faible",
                    "Le mot de passe doit contenir au moins 6 caractères.");
            txtPassword.requestFocus();
            return false;
        }
        
        // Validation du téléphone
        String telephone = txtTelephone.getText().trim();
        if (!telephone.isEmpty() && (telephone.length() != 8 || !telephone.matches("\\d+"))) {
            showAlert(Alert.AlertType.WARNING, "Téléphone invalide", "Le numéro de téléphone doit contenir exactement 8 chiffres.");
            txtTelephone.requestFocus();
            return false;
        }

        // Validation du nom de l'entreprise
        String nomEntreprise = txtNomEntreprise.getText().trim();
        if (nomEntreprise.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Nom d'entreprise obligatoire",
                    "Le nom de l'entreprise est obligatoire.");
            txtNomEntreprise.requestFocus();
            return false;
        }
        
        // Vérifie qu'il n'y a pas de chiffres (lettres, espaces, symboles autorisés)
        if (nomEntreprise.matches(".*\\d.*")) {
             showAlert(Alert.AlertType.WARNING, "Nom d'entreprise invalide",
                    "Le nom de l'entreprise ne doit pas contenir de chiffres.");
            txtNomEntreprise.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void validateEmail() {
        if (!txtEmail.getText().isEmpty() && !isValidEmail(txtEmail.getText())) {
            txtEmail.setStyle("-fx-border-color: #E74C3C; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        } else {
            txtEmail.setStyle("-fx-border-color: #BDC3C7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        }
    }

    @FXML
    private void handleCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler");
        alert.setHeaderText("Annuler la création");
        alert.setContentText("Êtes-vous sûr de vouloir annuler ? Les données saisies seront perdues.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                closeWindow();
            }
        });
    }

    @FXML
    private void handleClose() {
        handleCancel();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    private void setupButtonHoverEffects() {
        // Effet hover pour le bouton Créer
        btnCreate.setOnMouseEntered(e ->
                btnCreate.setStyle("-fx-background-color: linear-gradient(to right, #2980B9, #21618C); " +
                        "-fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;")
        );
        btnCreate.setOnMouseExited(e ->
                btnCreate.setStyle("-fx-background-color: linear-gradient(to right, #3498DB, #2980B9); " +
                        "-fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;")
        );

        // Effet hover pour le bouton Annuler
        btnCancel.setOnMouseEntered(e ->
                btnCancel.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; " +
                        "-fx-border-color: #7F8C8D; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;")
        );
        btnCancel.setOnMouseExited(e ->
                btnCancel.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-border-color: #BDC3C7; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;")
        );

        // Effet hover pour le bouton Fermer
        btnClose.setOnMouseEntered(e ->
                btnClose.setStyle("-fx-background-color: #E74C3C; -fx-background-radius: 50; " +
                        "-fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 20px; -fx-font-weight: bold;")
        );
        btnClose.setOnMouseExited(e ->
                btnClose.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 50; " +
                        "-fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 20px; -fx-font-weight: bold;")
        );
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setOnUserCreated(Runnable callback) {
        this.onUserCreated = callback;
    }
}
