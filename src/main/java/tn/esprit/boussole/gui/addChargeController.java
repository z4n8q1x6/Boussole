package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.service.ChargeService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class addChargeController {

    @FXML private TextField titreInput;
    @FXML private TextField montantInput;
    @FXML private DatePicker dateInput;
    @FXML private ComboBox<Charge.TypeCharge> typeCombo;
    @FXML private TextField preuveImageInput;
    @FXML private ComboBox<Charge.StatusValidation> statusCombo;
    @FXML private TextField franchiseIdInput;
    @FXML private Button btnListe;
    @FXML private Button btnVersFournisseur;
    @FXML private Button btnValider;

    private final ChargeService chargeService = new ChargeService();

    @FXML
    public void initialize() {
        // --- DESIGN DES CHAMPS (CONSERVÉ) ---
        String fieldStyle = "-fx-background-color: #0C0F1A; -fx-text-fill: white; -fx-border-color: #1E293B; -fx-border-radius: 5;";
        titreInput.setStyle(fieldStyle);
        montantInput.setStyle(fieldStyle);
        dateInput.setStyle("-fx-control-inner-background: #0C0F1A; -fx-border-color: #1E293B;");
        typeCombo.setStyle("-fx-background-color: #0C0F1A; -fx-border-color: #1E293B;");
        statusCombo.setStyle("-fx-background-color: #0C0F1A; -fx-border-color: #1E293B;");
        franchiseIdInput.setStyle(fieldStyle);
        preuveImageInput.setStyle(fieldStyle);

        typeCombo.getItems().setAll(Charge.TypeCharge.values());
        statusCombo.getItems().setAll(Charge.StatusValidation.values());
        dateInput.setValue(LocalDate.now());

        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });

        // Action du bouton annuler/liste : on ferme simplement
        if (btnListe != null) {
            btnListe.setOnAction(e -> closeWindow());
        }
    }

    @FXML
    private void handleAjouter() {
        String erreur = validerFormulaire();

        if (erreur != null) {
            showAlert("Erreur de saisie", erreur, Alert.AlertType.WARNING);
            return;
        }

        try {
            String nomFranchise = franchiseIdInput.getText().trim();
            int franchiseId = chargeService.getFranchiseIdByName(nomFranchise);

            if (franchiseId == -1) {
                showAlert("Erreur", "La franchise '" + nomFranchise + "' n'existe pas.", Alert.AlertType.ERROR);
                return;
            }

            Charge nouvelleCharge = new Charge(
                    titreInput.getText().trim(),
                    Double.parseDouble(montantInput.getText()),
                    dateInput.getValue(),
                    typeCombo.getValue(),
                    preuveImageInput.getText().trim(),
                    franchiseId
            );
            nouvelleCharge.setStatusValidation(statusCombo.getValue());

            chargeService.insertone(nouvelleCharge);

            showAlert("Succès", "Dépense enregistrée avec succès !", Alert.AlertType.INFORMATION);

            // --- CHANGEMENT ICI : ON FERME LA FENÊTRE AU LIEU DE REDIRIGER ---
            closeWindow();

        } catch (SQLException e) {
            showAlert("Erreur DB", "Impossible d'enregistrer : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format de nombre invalide.", Alert.AlertType.ERROR);
        }
    }

    // --- MÉTHODE DE FERMETURE PROPRE ---
    private void closeWindow() {
        Stage stage = (Stage) btnValider.getScene().getWindow();
        stage.close();
    }

    // handleAfficherListe() SUPPRIMÉE POUR ÉVITER LA REDIRECTION VERS LE BACK

    @FXML
    private void handleVersFournisseur() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnValider.getScene().getWindow();
            Scene newScene = new Scene(root);

            String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
            newScene.getStylesheets().add(css);

            stage.setScene(newScene);
            stage.setTitle("Ajouter un Fournisseur");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Impossible d'ouvrir ajouterFournisseur.fxml", Alert.AlertType.ERROR);
        }
    }

    private String validerFormulaire() {
        String titre = titreInput.getText().trim();
        if (titre.isEmpty() || montantInput.getText().isEmpty() ||
                dateInput.getValue() == null || typeCombo.getValue() == null ||
                statusCombo.getValue() == null || franchiseIdInput.getText().isEmpty()) {
            return "Tous les champs sont obligatoires.";
        }
        if (!titre.matches(".*[a-zA-Z].*")) {
            return "Le titre de la charge doit contenir au moins quelques lettres.";
        }
        return null;
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }
}