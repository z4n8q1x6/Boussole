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
    @FXML private TextField franchiseIdInput;
    @FXML private Button btnListe; 
    @FXML private Button btnVersFournisseur;
    @FXML private Button btnValider;

    private final ChargeService chargeService = new ChargeService();

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll(Charge.TypeCharge.values());
        statusCombo.getItems().setAll(Charge.StatusValidation.values());
        dateInput.setValue(LocalDate.now());

        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });
        
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
                    Integer.parseInt(franchiseIdInput.getText().trim())
            );
            nouvelleCharge.setStatusValidation(statusCombo.getValue());

            chargeService.insertOne(nouvelleCharge);

            showAlert("Succès", "Dépense enregistrée avec succès !", Alert.AlertType.INFORMATION);

            handleAfficherListe();

        } catch (SQLException e) {
            showAlert("Erreur DB", "Impossible d'enregistrer : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format de nombre invalide.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackCharge.fxml"));
            Parent root = loader.load();
            
            Scene currentScene = btnListe.getScene();
            if (currentScene == null) {
                currentScene = titreInput.getScene();
            }
            
            Stage stage = (Stage) currentScene.getWindow();
            Scene newScene = new Scene(root);
            
            // Application explicite du CSS
            String css = getClass().getResource("/styles/dash.css").toExternalForm();
            newScene.getStylesheets().add(css);
            
            stage.setScene(newScene);
            stage.setTitle("Boussole - Liste des Charges");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur Navigation", "Impossible de charger la liste : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Une erreur inattendue est survenue : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleVersFournisseur() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnListe.getScene().getWindow();
            Scene newScene = new Scene(root);
            
            // Application explicite du CSS
            String css = getClass().getResource("/styles/dash.css").toExternalForm();
            newScene.getStylesheets().add(css);
            
            stage.setScene(newScene);
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
