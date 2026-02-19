package tn.esprit.chargesdepenses.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.services.ChargeService;

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
    private TextField franchiseIdInput; // Retour au TextField pour l'ID

    private final ChargeService chargeService = new ChargeService();
    private Charge chargeActuelle;

    public void initialize() {
        typeCombo.getItems().setAll(Charge.TypeCharge.values());
        statusCombo.getItems().setAll(Charge.StatusValidation.values());

        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });
        
        // Validation pour l'ID franchise (chiffres uniquement)
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
            String titre = titreInput.getText().trim();
            double montant = Double.parseDouble(montantInput.getText());
            LocalDate date = dateInput.getValue();
            Charge.TypeCharge type = typeCombo.getValue();
            String preuve = preuveImageInput.getText().trim();
            Charge.StatusValidation status = statusCombo.getValue();
            int franchiseId = Integer.parseInt(franchiseIdInput.getText().trim());

            if (chargeActuelle == null) {
                showAlert("Erreur", "Aucune charge sélectionnée pour la modification.", Alert.AlertType.ERROR);
                return;
            }
            
            chargeActuelle.setTitre(titre);
            chargeActuelle.setMontant(montant);
            chargeActuelle.setDateCharge(date);
            chargeActuelle.setType(type);
            chargeActuelle.setPreuveImage(preuve);
            chargeActuelle.setStatusValidation(status);
            chargeActuelle.setFranchiseId(franchiseId);

            chargeService.updateOne(chargeActuelle);
            showAlert("Succès", "La charge a été mise à jour avec succès", Alert.AlertType.INFORMATION);
            
            ((Stage) titreInput.getScene().getWindow()).close();

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le montant et l'ID de franchise doivent être des nombres valides.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
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

        if (titreInput.getText().matches("^\\d+$")) {
            return "Le titre ne peut pas contenir uniquement des chiffres.";
        }

        try {
            double montant = Double.parseDouble(montantInput.getText());
            if (montant <= 0) {
                return "Le montant doit être strictement supérieur à 0.";
            }
        } catch (NumberFormatException e) {
            return "Le montant est invalide.";
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
