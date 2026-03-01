package tn.esprit.boussole.gui;

import tn.esprit.boussole.models.Pret;
import tn.esprit.boussole.models.StatutPret;
import tn.esprit.boussole.service.PretService;
import tn.esprit.boussole.org.example.Chatbot;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.net.URL;

public class DemandePretController {

    @FXML private TextField txtMontant, txtDuree, txtTaux, txtMotif;
    @FXML private Label lblMessage;

    private PretService pretService = new PretService();

    @FXML
    private void ajouterPret() {
        try {
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

            pretService.insertone(p);

            lblMessage.setText("Prêt ajouté avec succès !");
            lblMessage.setStyle("-fx-text-fill: #00E5CC;");

            txtMontant.clear();
            txtDuree.clear();
            txtTaux.clear();
            txtMotif.clear();

        } catch (Exception e) {
            lblMessage.setText("Erreur : " + e.getMessage());
            lblMessage.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    @FXML
    private void ouvrirChatbot(ActionEvent event) {
        try {
            // Ces méthodes existent maintenant dans PretService
            double totalAccorde = pretService.getMontantTotalParStatut(StatutPret.ACCORDE);
            long nbAccorde = pretService.countPretsParStatut(StatutPret.ACCORDE);
            double totalAttente = pretService.getMontantTotalParStatut(StatutPret.EN_ATTENTE);

            StringBuilder contexte = new StringBuilder();
            contexte.append("Tu es Boussole IA, l'assistant expert en crédits.\n");
            contexte.append("Données SQL :\n");
            contexte.append("- Total Accordé : ").append(totalAccorde).append(" DT\n");
            contexte.append("- Nombre de dossiers : ").append(nbAccorde).append("\n");
            contexte.append("- En attente : ").append(totalAttente).append(" DT\n\n");
            contexte.append("Saisie actuelle : ").append(txtMontant.getText()).append(" DT.");

            Chatbot chatbotApp = new Chatbot(contexte.toString());
            Stage stage = new Stage();
            stage.setScene(chatbotApp.creerSceneChatbot());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Assistant IA Boussole");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void ouvrirListe() {
        chargerPage("/view/ListePrets.fxml");
    }

    private void chargerPage(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) return;

            Parent nextRoot = FXMLLoader.load(resource);
            Scene currentScene = txtMontant.getScene();
            Parent currentRoot = currentScene.getRoot();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                currentScene.setRoot(nextRoot);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), nextRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (Exception e) { e.printStackTrace(); }
    }
}