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
import java.sql.SQLException;
import java.time.LocalDate;

public class addChargeController {

    @FXML private TextField titreInput;
    @FXML private TextField montantInput;
    @FXML private DatePicker dateInput;
    @FXML private ComboBox<Charge.TypeCharge> typeCombo;
    @FXML private TextField preuveImageInput;
    @FXML private ComboBox<Charge.StatusValidation> statusCombo;
    @FXML private TextField franchiseIdInput; // Renommé pour clarté (correspond au FXML si je le change aussi, sinon je garde franchiseInput mais je traite comme ID)
    // Note: Dans le FXML précédent j'avais mis franchiseInput (TextField). Je vais garder ce nom pour éviter de toucher au FXML si possible, 
    // ou je remets franchiseIdInput si c'était le nom d'origine.
    // Le FXML actuel a "franchiseCombo" (ComboBox) ou "franchiseInput" (TextField) selon mes dernières modifs.
    // Je vais vérifier le FXML actuel.
    // Ah, j'avais remis des TextField nommés "franchiseInput" dans les dernières étapes.
    // Je vais utiliser "franchiseIdInput" pour être cohérent avec l'ID.
    
    @FXML private Button btnListe; 
    @FXML private Button btnVersFournisseur;

    private final ChargeService chargeService = new ChargeService();

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll(Charge.TypeCharge.values());
        statusCombo.getItems().setAll(Charge.StatusValidation.values());
        dateInput.setValue(LocalDate.now());

        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });
        
        // Validation pour l'ID franchise (chiffres uniquement)
        franchiseIdInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*")) franchiseIdInput.setText(old);
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
                    Integer.parseInt(franchiseIdInput.getText().trim()) // Directement l'ID
            );
            nouvelleCharge.setStatusValidation(statusCombo.getValue());

            chargeService.insertOne(nouvelleCharge);
            showAlert("Succès", "Dépense enregistrée avec succès !", Alert.AlertType.INFORMATION);
            handleAfficherListe();

        } catch (SQLException e) {
            showAlert("Erreur DB", "Impossible d'enregistrer : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "L'ID de franchise doit être un nombre valide.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackCharge.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titreInput.getScene().getWindow();
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

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
