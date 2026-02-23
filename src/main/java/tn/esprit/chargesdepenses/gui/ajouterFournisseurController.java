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
    @FXML private TextField franchiseIdInput; // Saisie du NOM de la franchise
    @FXML private Button btnListe;
    @FXML private Button btnValider;
    @FXML private Button btnVersCharge;

    private final FournisseurService fournisseurService = new FournisseurService();

    @FXML
    public void initialize() {
        // Pas de validation numérique sur franchiseIdInput car c'est un nom
    }

    @FXML
    private void handleAjouter() {
        String erreur = validerFormulaire();
        if (erreur != null) {
            showAlert("Erreur de saisie", erreur, Alert.AlertType.WARNING);
            return;
        }

        try {
            // Récupération de l'ID à partir du NOM saisi
            String nomFranchise = franchiseIdInput.getText().trim();
            int franchiseId = fournisseurService.getFranchiseIdByName(nomFranchise);

            if (franchiseId == -1) {
                showAlert("Erreur", "La franchise '" + nomFranchise + "' n'existe pas.", Alert.AlertType.ERROR);
                return;
            }

            Fournisseur nouveauFournisseur = new Fournisseur(
                    nomInput.getText().trim(),
                    matriculeInput.getText().trim(),
                    telephoneInput.getText().trim(),
                    franchiseId // Utilisation de l'ID trouvé
            );

            fournisseurService.insertOne(nouveauFournisseur);
            showAlert("Succès", "Fournisseur enregistré avec succès !", Alert.AlertType.INFORMATION);
            handleAfficherListe();

        } catch (SQLException e) {
            showAlert("Erreur Base de Données", "Impossible d'enregistrer le fournisseur : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format invalide.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackFournisseur.fxml")); 
            Parent root = loader.load();
            
            Scene currentScene = btnListe.getScene();
            if (currentScene == null) currentScene = nomInput.getScene();
            
            Stage stage = (Stage) currentScene.getWindow();
            Scene newScene = new Scene(root);
            
            String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
            newScene.getStylesheets().add(css);
            
            stage.setScene(newScene);
            stage.setTitle("Boussole - Liste des Fournisseurs");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur Navigation", "Impossible de charger la liste : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleVersCharge() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCharge.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnListe.getScene().getWindow();
            Scene newScene = new Scene(root);
            
            String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
            newScene.getStylesheets().add(css);
            
            stage.setScene(newScene);
            stage.setTitle("Ajouter une Charge");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Impossible d'ouvrir ajouterCharge.fxml : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String validerFormulaire() {
        if (nomInput.getText().trim().isEmpty() || franchiseIdInput.getText().trim().isEmpty()) {
            return "Le nom et la franchise sont obligatoires.";
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
