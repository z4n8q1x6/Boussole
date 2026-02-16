package tn.esprit.chargesdepenses.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Charge;
import tn.esprit.chargesdepenses.models.enums.StatusValidation;
import tn.esprit.chargesdepenses.models.enums.TypeCharge;
import tn.esprit.chargesdepenses.services.ChargeService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class addChargeController {

    @FXML private TextField titreInput;
    @FXML private TextField montantInput;
    @FXML private DatePicker dateInput;
    @FXML private ComboBox<TypeCharge> typeCombo;
    @FXML private TextField preuveImageInput;
    @FXML private ComboBox<StatusValidation> statusCombo;
    @FXML private TextField franchiseIdInput;
    @FXML private Button btnListe; 
    @FXML private Button btnVersFournisseur;

    private final ChargeService chargeService = new ChargeService();

    @FXML
    public void initialize() {
        // Remplissage des ComboBox avec les Enums
        typeCombo.getItems().setAll(TypeCharge.values());
        statusCombo.getItems().setAll(StatusValidation.values());

        // Date par défaut à aujourd'hui
        dateInput.setValue(LocalDate.now());

        // Restriction : que des chiffres pour le montant
        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });
    }

    @FXML
    private void handleAjouter() {
        String erreur = validerFormulaire();

        if (erreur != null) {
            showAlert("Erreur de saisie", erreur, Alert.AlertType.WARNING);
            return;
        }

        try {
            Charge nouvelleCharge = new Charge(
                    titreInput.getText().trim(),
                    Double.parseDouble(montantInput.getText()),
                    dateInput.getValue(),
                    typeCombo.getValue(),
                    preuveImageInput.getText().trim(),
                    Integer.parseInt(franchiseIdInput.getText())
            );
            nouvelleCharge.setStatusValidation(statusCombo.getValue());

            chargeService.insertOne(nouvelleCharge);

            showAlert("Succès", "Dépense enregistrée avec succès !", Alert.AlertType.INFORMATION);

            // Rediriger vers la liste des charges après l'ajout
            handleAfficherListe();

        } catch (SQLException e) {
            showAlert("Erreur DB", "Impossible d'enregistrer : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            // Chargement de la page de la liste des charges
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackCharge.fxml"));
            Parent root = loader.load();

            // Récupération de la fenêtre (Stage) actuelle
            Stage stage = (Stage) titreInput.getScene().getWindow();

            // Changement de scène
            stage.setScene(new Scene(root));
            stage.setTitle("Boussole - Liste des Charges");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Fichier afficherBackCharge.fxml introuvable: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVersFournisseur() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titreInput.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter un Fournisseur");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Impossible d'ouvrir ajouterFournisseur.fxml : " + e.getMessage(), Alert.AlertType.ERROR);
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

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
