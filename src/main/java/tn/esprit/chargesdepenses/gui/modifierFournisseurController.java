package tn.esprit.chargesdepenses.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.sql.SQLException;

public class modifierFournisseurController {

    @FXML private TextField nomInput;
    @FXML private TextField matriculeInput;
    @FXML private TextField telephoneInput;
    @FXML private TextField franchiseIdInput; // Retour au TextField pour l'ID
    @FXML private Button btnValider;

    private final FournisseurService fournisseurService = new FournisseurService();
    private Fournisseur fournisseurActuel;

    @FXML
    public void initialize() {
        // Validation pour l'ID franchise (chiffres uniquement)
        franchiseIdInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*")) franchiseIdInput.setText(old);
        });
    }

    public void setFournisseurActuel(Fournisseur fournisseur) {
        this.fournisseurActuel = fournisseur;
        if (fournisseur != null) {
            nomInput.setText(fournisseur.getNom());
            matriculeInput.setText(fournisseur.getMatriculeFiscal());
            telephoneInput.setText(fournisseur.getTelephone());
            franchiseIdInput.setText(String.valueOf(fournisseur.getFranchiseId()));
        }
    }

    @FXML
    private void handleModifier() {
        String erreur = validerFormulaire();

        if (erreur != null) {
            showAlert("Erreur de saisie", erreur, Alert.AlertType.WARNING);
            return;
        }

        try {
            // Mise à jour de l'objet fournisseurActuel avec les nouvelles valeurs
            fournisseurActuel.setNom(nomInput.getText().trim());
            fournisseurActuel.setMatriculeFiscal(matriculeInput.getText().trim());
            fournisseurActuel.setTelephone(telephoneInput.getText().trim());
            fournisseurActuel.setFranchiseId(Integer.parseInt(franchiseIdInput.getText().trim()));

            // Appel au service pour la mise à jour en base
            fournisseurService.updateOne(fournisseurActuel);

            showAlert("Succès", "Fournisseur mis à jour avec succès !", Alert.AlertType.INFORMATION);
            
            // Fermer la fenêtre après succès
            closeWindow();

        } catch (SQLException e) {
            showAlert("Erreur Base de Données", "Impossible de mettre à jour le fournisseur : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "L'ID de franchise doit être un nombre valide.", Alert.AlertType.ERROR);
        }
    }

    private String validerFormulaire() {
        if (nomInput.getText().trim().isEmpty() || franchiseIdInput.getText().trim().isEmpty()) {
            return "Le nom et la franchise sont obligatoires.";
        }

        if (nomInput.getText().matches("^\\d+$")) {
            return "Le nom du fournisseur ne peut pas contenir uniquement des chiffres.";
        }

        if (!matriculeInput.getText().trim().isEmpty() && matriculeInput.getText().matches("^\\d+$")) {
            return "Le matricule fiscal ne peut pas contenir uniquement des chiffres.";
        }

        return null;
    }

    private void closeWindow() {
        Stage stage = (Stage) btnValider.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
