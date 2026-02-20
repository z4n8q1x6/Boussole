package tn.esprit.chargesdepenses.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.services.ChargeService;

import java.io.IOException;
import java.time.LocalDate;

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
    private Button btnListe; // Bouton Retour

    private final ChargeService chargeService = new ChargeService();
    private Charge chargeActuelle;

    public void initialize() {
        typeCombo.getItems().setAll(Charge.TypeCharge.values());
        statusCombo.getItems().setAll(Charge.StatusValidation.values());

        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });
        
        franchiseIdInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*")) franchiseIdInput.setText(old);
        });
    }

    @FXML
    public void handleModifier() {
        String erreur = validerFormulaire();

        if (erreur != null) {
            showAlert("Erreur de saisie", erreur, Alert.AlertType.WARNING);
            return;
        }

        try {
            chargeActuelle.setTitre(titreInput.getText().trim());
            chargeActuelle.setMontant(Double.parseDouble(montantInput.getText()));
            chargeActuelle.setDateCharge(dateInput.getValue());
            chargeActuelle.setType(typeCombo.getValue());
            chargeActuelle.setPreuveImage(preuveImageInput.getText().trim());
            chargeActuelle.setStatusValidation(statusCombo.getValue());
            chargeActuelle.setFranchiseId(Integer.parseInt(franchiseIdInput.getText().trim()));

            chargeService.updateOne(chargeActuelle);
            showAlert("Succès", "La charge a été mise à jour avec succès", Alert.AlertType.INFORMATION);
            
            // Redirection automatique vers la liste
            handleAfficherListe();

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackCharge.fxml"));
            Parent root = loader.load();
            
            // On récupère la scène actuelle (peu importe le bouton utilisé)
            Stage stage = (Stage) titreInput.getScene().getWindow();
            Scene newScene = new Scene(root);
            
            String css = getClass().getResource("/styles/dash.css").toExternalForm();
            newScene.getStylesheets().add(css);
            
            stage.setScene(newScene);
            stage.setTitle("Boussole - Liste des Charges");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur Navigation", "Impossible de charger la liste : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void setChargeActuelle(Charge charge) {
        this.chargeActuelle = charge;
        if (charge != null) {
            titreInput.setText(charge.getTitre());
            montantInput.setText(String.valueOf(charge.getMontant()));
            dateInput.setValue(charge.getDateCharge());
            typeCombo.setValue(charge.getType());
            preuveImageInput.setText(charge.getPreuveImage());
            statusCombo.setValue(charge.getStatusValidation());
            franchiseIdInput.setText(String.valueOf(charge.getFranchiseId()));
        }
    }

    private String validerFormulaire() {
        if (titreInput.getText().isEmpty() || montantInput.getText().isEmpty() ||
                dateInput.getValue() == null || typeCombo.getValue() == null ||
                statusCombo.getValue() == null || franchiseIdInput.getText().isEmpty()) {
            return "Tous les champs sont obligatoires.";
        }
        return null;
    }

    private void showAlert(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
