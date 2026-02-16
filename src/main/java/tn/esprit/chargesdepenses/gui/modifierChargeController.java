package tn.esprit.chargesdepenses.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.models.enums.TypeCharge;
import tn.esprit.chargesdepenses.models.enums.StatusValidation;
import tn.esprit.chargesdepenses.services.ChargeService;

public class modifierChargeController {
    @FXML
    private TextField titreInput;
    @FXML
    private TextField montantInput;
    @FXML
    private DatePicker dateInput;
    @FXML
    private ComboBox<TypeCharge> typeCombo;
    @FXML
    private TextField preuveImageInput;
    @FXML
    private ComboBox<StatusValidation> statusCombo;
    @FXML
    private TextField franchiseIdInput;

    private final ChargeService chargeService = new ChargeService();
    private Charge chargeActuelle;

    public void initialize() {
        typeCombo.getItems().setAll(TypeCharge.values());
        statusCombo.getItems().setAll(StatusValidation.values());
    }

    @FXML
    public void handleModifier() {
        try {
            String titre = titreInput.getText();
            double montant = Double.parseDouble(montantInput.getText());
            java.time.LocalDate date = dateInput.getValue();
            TypeCharge type = typeCombo.getValue();
            String preuve = preuveImageInput.getText();
            StatusValidation status = statusCombo.getValue();
            int franchiseId = Integer.parseInt(franchiseIdInput.getText());

            if (chargeActuelle == null) chargeActuelle = new Charge();
            chargeActuelle.setTitre(titre);
            chargeActuelle.setMontant(montant);
            chargeActuelle.setDateCharge(date);
            chargeActuelle.setType(type);
            chargeActuelle.setPreuveImage(preuve);
            chargeActuelle.setStatusValidation(status);
            chargeActuelle.setFranchiseId(franchiseId);

            chargeService.updateOne(chargeActuelle);
            showAlert("Succès", "La charge a été mise à jour avec succès");
            clearForm();
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le montant et l'ID de franchise doivent être des nombres");
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // Ajout d'un setter pour permettre la modification depuis le contrôleur d'affichage
    public void setChargeActuelle(Charge charge) {
        this.chargeActuelle = charge;
        // Remplir les champs du formulaire avec les valeurs de la charge sélectionnée
        titreInput.setText(charge.getTitre());
        montantInput.setText(String.valueOf(charge.getMontant()));
        dateInput.setValue(charge.getDateCharge());
        typeCombo.setValue(charge.getType());
        preuveImageInput.setText(charge.getPreuveImage());
        statusCombo.setValue(charge.getStatusValidation());
        franchiseIdInput.setText(String.valueOf(charge.getFranchiseId()));
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        titreInput.clear();
        montantInput.clear();
        dateInput.setValue(null);
        typeCombo.setValue(null);
        preuveImageInput.clear();
        statusCombo.setValue(null);
        franchiseIdInput.clear();
    }
}
