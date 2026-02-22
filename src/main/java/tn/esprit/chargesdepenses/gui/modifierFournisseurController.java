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

public class modifierFournisseurController {

    @FXML private TextField nomInput;
    @FXML private TextField matriculeInput;
    @FXML private TextField telephoneInput;
    @FXML private TextField franchiseIdInput;
    @FXML private Button btnValider;
    @FXML private Button btnListe; // Bouton Retour

    private final FournisseurService fournisseurService = new FournisseurService();
    private Fournisseur fournisseurActuel;

    @FXML
    public void initialize() {
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
            fournisseurActuel.setNom(nomInput.getText().trim());
            fournisseurActuel.setMatriculeFiscal(matriculeInput.getText().trim());
            fournisseurActuel.setTelephone(telephoneInput.getText().trim());
            fournisseurActuel.setFranchiseId(Integer.parseInt(franchiseIdInput.getText().trim()));

            fournisseurService.updateOne(fournisseurActuel);
            showAlert("Succès", "Fournisseur mis à jour avec succès !", Alert.AlertType.INFORMATION);
            
            handleAfficherListe();

        } catch (SQLException e) {
            showAlert("Erreur Base de Données", "Impossible de mettre à jour le fournisseur : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackFournisseur.fxml")); 
            Parent root = loader.load();
            
            Stage stage = (Stage) btnListe.getScene().getWindow();
            Scene newScene = new Scene(root);
            
            // CSS mis à jour
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
