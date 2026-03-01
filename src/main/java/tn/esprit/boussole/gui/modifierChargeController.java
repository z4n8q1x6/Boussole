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

public class modifierChargeController {
    @FXML
    private TextField titreInput;
    @FXML
    private TextField montantInput;
    @FXML
    private DatePicker dateInput;
    @FXML
    private ComboBox<Charge.TypeCharge> typeCombo;
    @FXML
    private TextField preuveImageInput;
    @FXML
    private ComboBox<Charge.StatusValidation> statusCombo;
    @FXML
    private TextField franchiseIdInput;
    @FXML
    private Button btnListe;

    private final ChargeService chargeService = new ChargeService();
    private Charge chargeActuelle;

    @FXML
    public void initialize() {
        // --- STYLE DES CHAMPS (CONSERVÉ) ---
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

        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });

        // --- GESTION DU BOUTON RETOUR/LISTE ---
        // On ferme simplement la fenêtre au lieu de recharger le Back
        if (btnListe != null) {
            btnListe.setOnAction(e -> closeWindow());
        }
    }

    @FXML
    public void handleModifier() {
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

            chargeActuelle.setTitre(titreInput.getText().trim());
            chargeActuelle.setMontant(Double.parseDouble(montantInput.getText()));
            chargeActuelle.setDateCharge(dateInput.getValue());
            chargeActuelle.setType(typeCombo.getValue());
            chargeActuelle.setPreuveImage(preuveImageInput.getText().trim());
            chargeActuelle.setStatusValidation(statusCombo.getValue());
            chargeActuelle.setFranchiseId(franchiseId);

            chargeService.updateone(chargeActuelle);
            showAlert("Succès", "La charge a été mise à jour avec succès", Alert.AlertType.INFORMATION);

            // --- CHANGEMENT ICI : FERMETURE DE LA FENÊTRE ---
            closeWindow();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur SQL: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // --- MÉTHODE DE FERMETURE PROPRE ---
    private void closeWindow() {
        Stage stage = (Stage) titreInput.getScene().getWindow();
        stage.close();
    }

    // handleAfficherListe() SUPPRIMÉE POUR ÉVITER LA REDIRECTION VERS LE BACK

    public void setChargeActuelle(Charge charge) {
        this.chargeActuelle = charge;
        if (charge != null) {
            titreInput.setText(charge.getTitre());
            montantInput.setText(String.valueOf(charge.getMontant()));
            dateInput.setValue(charge.getDateCharge());
            typeCombo.setValue(charge.getType());
            preuveImageInput.setText(charge.getPreuveImage());
            statusCombo.setValue(charge.getStatusValidation());

            try {
                String franchiseName = chargeService.getFranchiseNameById(charge.getFranchiseId());
                franchiseIdInput.setText(franchiseName);
            } catch (SQLException e) {
                franchiseIdInput.setText("");
            }
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

    private void showAlert(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }
}