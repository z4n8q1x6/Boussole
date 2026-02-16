package tn.esprit.chargesdepenses.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.io.IOException;
import java.sql.SQLException;

public class ajouterFournisseurController {

    @FXML private TextField nomInput;
    @FXML private TextField matriculeInput;
    @FXML private TextField telephoneInput;
    @FXML private TextField franchiseIdInput;
    @FXML private Button btnListe;
    @FXML private Button btnValider;
    @FXML private Button btnVersCharge;

    // Assurez-vous que cette classe existe dans votre package services
    private final FournisseurService fournisseurService = new FournisseurService();

    @FXML
    public void initialize() {
        // Initialisation si nécessaire
        // Par exemple, forcer franchiseId à être numérique
        franchiseIdInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*")) {
                franchiseIdInput.setText(newValue.replaceAll("[^\\d]", ""));
            }
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
            Fournisseur nouveauFournisseur = new Fournisseur(
                    nomInput.getText().trim(),
                    matriculeInput.getText().trim(),
                    telephoneInput.getText().trim(),
                    Integer.parseInt(franchiseIdInput.getText().trim())
            );

            fournisseurService.insertOne(nouveauFournisseur);

            showAlert("Succès", "Fournisseur enregistré avec succès !", Alert.AlertType.INFORMATION);
            
            // Rediriger vers la liste des fournisseurs après l'ajout
            handleAfficherListe();

        } catch (SQLException e) {
            showAlert("Erreur Base de Données", "Impossible d'enregistrer le fournisseur : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NumberFormatException e) {
            showAlert("Erreur de Format", "L'ID de la franchise doit être un nombre entier valide.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            // Redirection vers la liste des fournisseurs
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackFournisseur.fxml")); 
            Parent root = loader.load();
            Stage stage = (Stage) btnListe.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Boussole - Liste des Fournisseurs");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Impossible de charger afficherBackFournisseur.fxml : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVersCharge() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCharge.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnListe.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter une Charge");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Impossible d'ouvrir ajouterCharge.fxml : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String validerFormulaire() {
        if (nomInput.getText().trim().isEmpty()) {
            return "Le nom du fournisseur est obligatoire.";
        }
        if (franchiseIdInput.getText().trim().isEmpty()) {
            return "L'ID de la franchise est obligatoire.";
        }
        // Vous pouvez ajouter d'autres validations ici (format téléphone, etc.)
        return null;
    }

    private void viderChamps() {
        nomInput.clear();
        matriculeInput.clear();
        telephoneInput.clear();
        franchiseIdInput.clear();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
