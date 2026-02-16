package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.prefs.Preferences;

public class dashController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnEntreprises;
    @FXML private Button btnSettings;
    @FXML private Button btnReports;
    @FXML private Button btnLogout;
    @FXML private Label lblUsername;

    @FXML
    public void initialize() {
        // 1. Récupérer les infos de session
        Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
        String email = prefs.get("email", "Utilisateur");
        String role = prefs.get("role", "");
        
        if (lblUsername != null) {
            lblUsername.setText(email);
        }

        // 2. Charger la vue par défaut
        loadView("/users.fxml");
        setActiveButton(btnUsers);

        // --- Configuration des actions des boutons ---

        btnUsers.setOnAction(event -> {
            loadView("/users.fxml");
            setActiveButton(btnUsers);
        });

        btnEntreprises.setOnAction(event -> {
            loadView("/entreprise.fxml");
            setActiveButton(btnEntreprises);
        });

        btnDashboard.setOnAction(event -> setActiveButton(btnDashboard));
        btnSettings.setOnAction(event -> setActiveButton(btnSettings));
        btnReports.setOnAction(event -> setActiveButton(btnReports));

        btnLogout.setOnAction(event -> handleLogout());
    }

    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier FXML introuvable : " + fxmlPath);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            
        } catch (IOException e) {
            e.printStackTrace(); // Garder la trace dans la console pour le débogage
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement", 
                      "Impossible de charger la vue '" + fxmlPath + "'.\n\nDétail: " + e.getMessage() + 
                      "\n\n(Vérifiez la console pour plus de détails)");
        }
    }
    
    private void setActiveButton(Button activeButton) {
        // Réinitialiser le style de tous les boutons du menu
        if (btnDashboard.getParent() instanceof VBox) {
            VBox menuBox = (VBox) btnDashboard.getParent();
            for (Node node : menuBox.getChildren()) {
                if (node instanceof Button && node != btnLogout) {
                     // Style par défaut (transparent)
                     node.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-border-width: 0;");
                }
            }
        }
        
        // Appliquer le style actif au bouton cliqué (bleu avec bordure gauche blanche)
        activeButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: white; -fx-border-width: 0 0 0 5;");
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous vraiment vous déconnecter ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 1. Effacer la session
            Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
            prefs.remove("jwt");
            prefs.remove("email");
            prefs.remove("role");

            // 2. Rediriger vers la page de login
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Parent root = loader.load();
                
                Stage stage = (Stage) btnLogout.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                
                // Garder la fenêtre maximisée
                stage.setMaximized(true);
                
                stage.show();
                
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à l'écran de connexion.");
            }
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
