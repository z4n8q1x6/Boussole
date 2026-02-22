package controller;

import entity.Pret;
import entity.StatutPret;
import service.PretService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class DemandePretController {

    @FXML
    private TextField txtMontant, txtDuree, txtTaux, txtMotif;

    @FXML
    private Label lblMessage;

    private PretService pretService = new PretService();

    @FXML
    private void ajouterPret() {
        try {
            // Validation des champs numériques et texte
            if (!txtMontant.getText().matches("\\d+(\\.\\d+)?")) {
                lblMessage.setText("Montant invalide !");
                lblMessage.setStyle("-fx-text-fill: red;");
                return;
            }
            if (!txtDuree.getText().matches("\\d+")) {
                lblMessage.setText("Durée invalide !");
                lblMessage.setStyle("-fx-text-fill: red;");
                return;
            }
            if (!txtTaux.getText().matches("\\d+(\\.\\d+)?")) {
                lblMessage.setText("Taux invalide !");
                lblMessage.setStyle("-fx-text-fill: red;");
                return;
            }
            if (txtMotif.getText().isEmpty()) {
                lblMessage.setText("Veuillez saisir un motif !");
                lblMessage.setStyle("-fx-text-fill: red;");
                return;
            }

            Pret p = new Pret();
            p.setMontantDemande(Double.parseDouble(txtMontant.getText()));
            p.setDureeMois(Integer.parseInt(txtDuree.getText()));
            p.setTaux(Float.parseFloat(txtTaux.getText()));
            p.setMotif(txtMotif.getText());
            p.setStatut(StatutPret.EN_ATTENTE);

            // Appel du service
            pretService.ajouterPret(p);

            // Message de succès (vert)
            lblMessage.setText("Prêt ajouté avec succès !");
            lblMessage.setStyle("-fx-text-fill: green;");

            // Nettoyage des champs
            txtMontant.clear();
            txtDuree.clear();
            txtTaux.clear();
            txtMotif.clear();

        } catch (Exception e) {
            // Message d'erreur (rouge)
            lblMessage.setText("Erreur : " + e.getMessage());
            lblMessage.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }


    @FXML
    private void ouvrirListe() throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/view/ListePrets.fxml"));


        Stage stage = (Stage) txtMontant.getScene().getWindow();


        stage.setScene(new Scene(root));
        stage.setTitle("Boussole - Liste des Prêts");
    }
}