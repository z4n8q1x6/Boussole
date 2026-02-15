package tn.esprit.Boussole.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private Button btnSiege;

    @FXML
    private Button btnFranchise;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Liaison des actions
        btnSiege.setOnAction(this::actionSiege);
        btnFranchise.setOnAction(this::actionFranchise);
    }

    @FXML
    private void actionSiege(ActionEvent event) {
        System.out.println(">>> Action: Connexion SIÈGE demandée.");
        
        // 1. Configuration de la Session
        SessionManager session = SessionManager.getInstance();
        session.cleanSession();
        session.setIdUtilisateur(1); // Simulé
        session.setRole("SIEGE");
        session.setIdFranchise(0); // Pas de franchise pour le Siège

        System.out.println("Session configurée : " + session);

        // 2. Chargement de l'interface
        changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml");
    }

    @FXML
    private void actionFranchise(ActionEvent event) {
        System.out.println(">>> Action: Connexion FRANCHISE demandée.");

        // 1. Configuration de la Session
        SessionManager session = SessionManager.getInstance();
        session.cleanSession();
        session.setIdUtilisateur(2); // Simulé
        session.setRole("FRANCHISE");
        session.setIdFranchise(1); // ID Franchise = 1

        System.out.println("Session configurée : " + session);

        // 2. Chargement de l'interface
        changerPage(event, "/tn/esprit/Boussole/GUI/DashboardFranchise.fxml");
    }

    /**
     * Méthode utilitaire pour changer de page (navigation entre écrans FXML).
     * @param event ActionEvent du bouton cliqué
     * @param fxmlPath chemin absolu de la ressource FXML
     */
    private void changerPage(ActionEvent event, String fxmlPath) {
        System.out.println(">>> Tentative de chargement du FXML : " + fxmlPath);

        try {
            // A. Vérification de l'existence du fichier
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                String errorMsg = "Fichier FXML introuvable : " + fxmlPath;
                System.err.println("!!! ERREUR CRITIQUE : " + errorMsg);
                afficherMessageErreur("Erreur Critique : " + errorMsg);
                return;
            }

            // B. Chargement du FXML
            Parent root = FXMLLoader.load(fxmlUrl);
            System.out.println(">>> Chargement FXML réussi.");

            // C. Changement de Scène
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            
            Scene scene = new Scene(root);
            
            // Ajout CSS global si présent
            URL cssUrl = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            
            // Définition d'un titre plus propre
            String title = fxmlPath.contains("Siege") ? "Espace SIÈGE" : "Espace FRANCHISE";
            stage.setTitle("Boussole - " + title);
            
            stage.centerOnScreen();
            stage.show();
            System.out.println(">>> Interface affichée : " + fxmlPath);

        } catch (IOException e) {
            // D. Gestion des erreurs de chargement (controlleur, fxml invalide, etc.)
            System.err.println("!!! EXCEPTION FXML : " + e.getMessage());
            e.printStackTrace();
            afficherMessageErreur("Erreur de chargement de l'interface : " + e.getMessage());
        } catch (Exception e) {
            // E. Autres erreurs imprévues
            System.err.println("!!! EXCEPTION INCONNUE : " + e.getMessage());
            e.printStackTrace();
            afficherMessageErreur("Erreur système inattendue : " + e.getMessage());
        }
    }


    /**
     * Helper method to show error alert (consistent with other controllers)
     */
    private void afficherMessageErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
