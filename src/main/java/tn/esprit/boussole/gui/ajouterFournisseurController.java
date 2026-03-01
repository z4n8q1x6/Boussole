package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Fournisseur;
import tn.esprit.boussole.service.FournisseurService;

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

    private final FournisseurService fournisseurService = new FournisseurService();

    @FXML
    public void initialize() {
        // --- STYLE DES CHAMPS (CONSERVÉ) ---
        String fieldStyle = "-fx-background-color: #0C0F1A; -fx-text-fill: white; -fx-border-color: #1E293B; -fx-border-radius: 5; -fx-padding: 5;";
        nomInput.setStyle(fieldStyle);
        matriculeInput.setStyle(fieldStyle);
        telephoneInput.setStyle(fieldStyle);
        franchiseIdInput.setStyle(fieldStyle);

        // --- GESTION DU BOUTON RETOUR ---
        if (btnListe != null) {
            btnListe.setOnAction(e -> closeWindow());
        }
    }

    @FXML
    private void handleAjouter() {
        String erreur = validerFormulaire();
        if (erreur != null) {
            showAlert("Erreur de saisie", erreur, Alert.AlertType.WARNING);
            return;
        }

        try {
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
                    franchiseId
            );

            fournisseurService.insertone(nouveauFournisseur);
            showAlert("Succès", "Fournisseur enregistré avec succès !", Alert.AlertType.INFORMATION);

            // --- CHANGEMENT ICI : FERMETURE DE LA FENÊTRE ---
            closeWindow();

        } catch (SQLException e) {
            showAlert("Erreur Base de Données", "Impossible d'enregistrer le fournisseur : " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format invalide.", Alert.AlertType.ERROR);
        }
    }

    // --- MÉTHODE DE FERMETURE PROPRE ---
    private void closeWindow() {
        Stage stage = (Stage) btnValider.getScene().getWindow();
        stage.close();
    }

    // handleAfficherListe() SUPPRIMÉE POUR ÉVITER LA REDIRECTION VERS LE BACK

    @FXML
    private void handleVersCharge() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCharge.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnValider.getScene().getWindow();
            Scene newScene = new Scene(root);

            String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
            newScene.getStylesheets().add(css);

            stage.setScene(newScene);
            stage.setTitle("Ajouter une Charge");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur Navigation", "Impossible d'ouvrir ajouterCharge.fxml", Alert.AlertType.ERROR);
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

        DialogPane dialogPane = alert.getDialogPane();
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }
}
