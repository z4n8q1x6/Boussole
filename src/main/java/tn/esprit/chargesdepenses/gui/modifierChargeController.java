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

public class modifierChargeController {
    @FXML
    private TextField titreInput;
    @FXML
    private TextField montantInput;
    @FXML
    private DatePicker dateInput;
    @FXML
    private ComboBox<Charge.TypeCharge> typeCombo;
    @FXML
    private TextField preuveImageInput;
    @FXML
    private ComboBox<Charge.StatusValidation> statusCombo;
    @FXML
    private TextField franchiseIdInput; // Saisie du NOM
    @FXML
    private Button btnListe;

    private final ChargeService chargeService = new ChargeService();
    private Charge chargeActuelle;

    @FXML
    public void initialize() {
        // --- STYLE DES CHAMPS (SANS TOUCHER AU FXML) ---
        String fieldStyle = "-fx-background-color: #0C0F1A; -fx-text-fill: white; -fx-border-color: #1E293B; -fx-border-radius: 5;";
        titreInput.setStyle(fieldStyle);
        montantInput.setStyle(fieldStyle);
        dateInput.setStyle("-fx-control-inner-background: #0C0F1A; -fx-border-color: #1E293B;");
        typeCombo.setStyle("-fx-background-color: #0C0F1A; -fx-border-color: #1E293B;");
        statusCombo.setStyle("-fx-background-color: #0C0F1A; -fx-border-color: #1E293B;");
        franchiseIdInput.setStyle(fieldStyle);
        preuveImageInput.setStyle(fieldStyle);

        // --- LOGIQUE D'ORIGINE ---
        typeCombo.getItems().setAll(Charge.TypeCharge.values());
        statusCombo.getItems().setAll(Charge.StatusValidation.values());

        montantInput.textProperty().addListener((obs, old, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) montantInput.setText(old);
        });
    }

    @FXML
    public void handleModifier() {
        String erreur = validerFormulaire();

        if (erreur != null) {
            showAlert("Erreur de saisie", erreur, Alert.AlertType.WARNING);
            return;
        }

        try {
            // Récupération de l'ID à partir du NOM saisi
            String nomFranchise = franchiseIdInput.getText().trim();
            int franchiseId = chargeService.getFranchiseIdByName(nomFranchise);

            if (franchiseId == -1) {
                showAlert("Erreur", "La franchise '" + nomFranchise + "' n'existe pas.", Alert.AlertType.ERROR);
                return;
            }

            chargeActuelle.setTitre(titreInput.getText().trim());
            chargeActuelle.setMontant(Double.parseDouble(montantInput.getText()));
            chargeActuelle.setDateCharge(dateInput.getValue());
            chargeActuelle.setType(typeCombo.getValue());
            chargeActuelle.setPreuveImage(preuveImageInput.getText().trim());
            chargeActuelle.setStatusValidation(statusCombo.getValue());
            chargeActuelle.setFranchiseId(franchiseId);

            chargeService.updateOne(chargeActuelle);
            showAlert("Succès", "La charge a été mise à jour avec succès", Alert.AlertType.INFORMATION);
            
            handleAfficherListe();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur SQL: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleAfficherListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherBackCharge.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnListe.getScene().getWindow();
            Scene newScene = new Scene(root);
            
            String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
            newScene.getStylesheets().add(css);
            
            stage.setScene(newScene);
            stage.setTitle("Boussole - Liste des Charges");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur Navigation", "Impossible de charger la liste : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void setChargeActuelle(Charge charge) {
        this.chargeActuelle = charge;
        if (charge != null) {
            titreInput.setText(charge.getTitre());
            montantInput.setText(String.valueOf(charge.getMontant()));
            dateInput.setValue(charge.getDateCharge());
            typeCombo.setValue(charge.getType());
            preuveImageInput.setText(charge.getPreuveImage());
            statusCombo.setValue(charge.getStatusValidation());
            
            // Pré-remplir le NOM de la franchise
            try {
                String franchiseName = chargeService.getFranchiseNameById(charge.getFranchiseId());
                franchiseIdInput.setText(franchiseName);
            } catch (SQLException e) {
                franchiseIdInput.setText("");
            }
        }
    }

    private String validerFormulaire() {
        String titre = titreInput.getText().trim();

        // 1. Vérification des champs vides
        if (titre.isEmpty() || montantInput.getText().isEmpty() ||
                dateInput.getValue() == null || typeCombo.getValue() == null ||
                statusCombo.getValue() == null || franchiseIdInput.getText().isEmpty()) {
            return "Tous les champs sont obligatoires.";
        }

        // 2. Nouveau contrôle : Empêcher un titre uniquement numérique
        // La regex ".*[a-zA-Z].*" garantit la présence d'au moins une lettre
        if (!titre.matches(".*[a-zA-Z].*")) {
            return "Le titre de la charge doit contenir au moins quelques lettres (ne peut pas être uniquement des chiffres).";
        }

        return null;
    }

    private void showAlert(String titre, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // --- APPLICATION DU DESIGN SOMBRE BOUSSOLE ---
        DialogPane dialogPane = alert.getDialogPane();
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("dialog-pane");

        alert.showAndWait();
    }
}
