package controller;

import entity.Pret;
import entity.StatutPret;
import service.PretService;
import org.example.Chatbot;
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

    @FXML
    private TextField txtMontant, txtDuree, txtTaux, txtMotif;

    @FXML
    private Label lblMessage;

    private PretService pretService = new PretService();

    /**
     * Ajoute un nouveau prêt après validation des champs.
     */
    @FXML
    private void ajouterPret() {
        try {
            // 1. Validation des champs numériques et texte
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

            // 2. Création de l'objet Pret (utilise les Setters ajoutés dans Pret.java)
            Pret p = new Pret();
            p.setMontantDemande(Double.parseDouble(txtMontant.getText()));
            p.setDureeMois(Integer.parseInt(txtDuree.getText()));
            p.setTaux(Float.parseFloat(txtTaux.getText()));
            p.setMotif(txtMotif.getText());
            p.setStatut(StatutPret.EN_ATTENTE);

            // 3. Appel du service pour l'insertion en BDD
            pretService.ajouterPret(p);

            // 4. Message de succès et nettoyage
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

    /**
     * Ouvre la fenêtre pop-up du Chatbot Gemini avec le contexte du formulaire.
     */
    @FXML
    private void ouvrirChatbot(ActionEvent event) {
        try {
            // 1. ALLER CHERCHER LES INFOS EN BDD (C'est ce qui manquait !)
            double totalAccorde = pretService.getMontantTotalParStatut(StatutPret.ACCORDE);
            long nbAccorde = pretService.countPretsParStatut(StatutPret.ACCORDE);
            double totalAttente = pretService.getMontantTotalParStatut(StatutPret.EN_ATTENTE);

            // 2. PRÉPARER LE CONTEXTE COMPLET
            StringBuilder contexte = new StringBuilder();
            contexte.append("Tu es Boussole IA, l'assistant expert en crédits.\n");
            contexte.append("Voici les chiffres réels de la base de données :\n");
            contexte.append("- Total Accordé : ").append(totalAccorde).append(" DT\n");
            contexte.append("- Nombre de dossiers : ").append(nbAccorde).append("\n");
            contexte.append("- En attente : ").append(totalAttente).append(" DT\n\n");

            contexte.append("L'utilisateur est AUSSI en train de remplir ce formulaire :\n");
            contexte.append("- Montant saisi : ").append(txtMontant.getText()).append(" DT\n");
            contexte.append("- Motif : ").append(txtMotif.getText()).append("\n");
            contexte.append("Réponds aux questions en utilisant ces données.");

            // 3. LANCER LE CHATBOT
            Chatbot chatbotApp = new Chatbot(contexte.toString());
            Stage stage = new Stage();
            stage.setScene(chatbotApp.creerSceneChatbot());
            stage.initModality(Modality.APPLICATION_MODAL); // Optionnel : bloque la fenêtre arrière
            stage.setTitle("Assistant IA Boussole");
            stage.show();

        } catch (Exception e) {
            System.err.println("Erreur IA : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Redirige vers la liste des prêts.
     */
    @FXML
    private void ouvrirListe() {
        chargerPage("/view/ListePrets.fxml");
    }

    /**
     * Méthode utilitaire pour naviguer entre les vues avec une transition fluide.
     */
    private void chargerPage(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("ERREUR : Fichier FXML introuvable -> " + fxmlPath);
                return;
            }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}