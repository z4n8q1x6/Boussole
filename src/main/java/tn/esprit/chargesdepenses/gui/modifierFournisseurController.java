package tn.esprit.chargesdepenses.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
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
    @FXML private TextField franchiseIdInput; // Saisie du NOM
    @FXML private Button btnValider;
    @FXML private Button btnListe;

    private final FournisseurService fournisseurService = new FournisseurService();
    private Fournisseur fournisseurActuel;

    @FXML
    public void initialize() {
        // --- DESIGN SOMBRE DES CHAMPS ---
        String fieldStyle = "-fx-background-color: #0C0F1A; -fx-text-fill: white; -fx-border-color: #1E293B; -fx-border-radius: 5; -fx-padding: 5;";
        nomInput.setStyle(fieldStyle);
        matriculeInput.setStyle(fieldStyle);
        telephoneInput.setStyle(fieldStyle);
        franchiseIdInput.setStyle(fieldStyle);
    }

    public void setFournisseurActuel(Fournisseur fournisseur) {
        this.fournisseurActuel = fournisseur;
        if (fournisseur != null) {
            nomInput.setText(fournisseur.getNom());
            matriculeInput.setText(fournisseur.getMatriculeFiscal());
            telephoneInput.setText(fournisseur.getTelephone());

            // Pré-remplir le NOM de la franchise (Logique d'origine préservée)
            try {
                String franchiseName = fournisseurService.getFranchiseNameById(fournisseur.getFranchiseId());
                franchiseIdInput.setText(franchiseName);
            } catch (SQLException e) {
                franchiseIdInput.setText("");
            }
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
            String nomFranchise = franchiseIdInput.getText().trim();
            int franchiseId = fournisseurService.getFranchiseIdByName(nomFranchise);

            if (franchiseId == -1) {
                showAlert("Erreur", "La franchise '" + nomFranchise + "' n'existe pas.", Alert.AlertType.ERROR);
                return;
            }

            fournisseurActuel.setNom(nomInput.getText().trim());
            fournisseurActuel.setMatriculeFiscal(matriculeInput.getText().trim());
            fournisseurActuel.setTelephone(telephoneInput.getText().trim());
            fournisseurActuel.setFranchiseId(franchiseId);

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

        // --- INJECTION DU STYLE SOMBRE ---
        DialogPane dialogPane = alert.getDialogPane();
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }
}