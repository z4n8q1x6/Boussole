package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;
import java.util.prefs.Preferences;

public class dashController {

    @FXML private Button btnDashboard, btnUsers, btnEntreprises, btnSettings, btnReports, btnLogout;
    @FXML private Label lblUsername;
    @FXML private StackPane contentArea;

    private final String ACTIVE_STYLE = "-fx-background-color: rgba(0,229,204,0.12); -fx-text-fill: #00E5CC; -fx-border-color: transparent transparent transparent #00E5CC; -fx-border-width: 0 0 0 3;";
    private final String INACTIVE_STYLE = "-fx-background-color: transparent; -fx-text-fill: #8892A4; -fx-border-width: 0;";

    @FXML
    public void initialize() {
        // 1. Session
        Preferences prefs = Preferences.userRoot().node("tn.esprit.boussole.gui.loginController");
        lblUsername.setText(prefs.get("email", "Administrateur"));

        // 2. Actions des boutons
        btnUsers.setOnAction(e -> handleMenuClick(btnUsers, "/users.fxml"));
        btnEntreprises.setOnAction(e -> handleMenuClick(btnEntreprises, "/entreprise.fxml"));
        btnDashboard.setOnAction(e -> handleMenuClick(btnDashboard, null));
        btnReports.setOnAction(e -> handleMenuClick(btnReports, null));
        btnSettings.setOnAction(e -> handleMenuClick(btnSettings, null));
        btnLogout.setOnAction(e -> handleLogout());

        // 3. Hover effects (Optionnel si tu utilises un CSS externe, mais gardé ici pour ton code)
        setupHoverEffects();

        // 4. Page par défaut
        handleMenuClick(btnUsers, "/users.fxml");
    }

    private void handleMenuClick(Button button, String fxmlPath) {
        updateButtonStyle(button);
        if (fxmlPath != null) {
            loadView(fxmlPath);
        } else {
            showPlaceholder(button.getText());
        }
    }

    private void loadView(String fxmlPath) {
        try {
            // Sécurité : Vérifier si la ressource existe
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("Erreur: Fichier " + fxmlPath + " introuvable dans resources.");
                return;
            }
            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur de chargement", "Impossible d'ouvrir : " + fxmlPath);
        }
    }

    private void updateButtonStyle(Button activeButton) {
        Button[] allButtons = {btnDashboard, btnUsers, btnEntreprises, btnReports, btnSettings};
        for (Button b : allButtons) {
            b.setStyle(b == activeButton ? ACTIVE_STYLE : INACTIVE_STYLE);
        }
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vous déconnecter ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
                    Stage stage = (Stage) btnLogout.getScene().getWindow();
                    stage.setScene(new Scene(root));
                } catch (IOException e) {
                    showAlert("Erreur", "Retour au login impossible.");
                }
            }
        });
    }

    private void showPlaceholder(String text) {
        contentArea.getChildren().setAll(new Label("Page : " + text + " (Bientôt disponible)"));
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    private void setupHoverEffects() {
        // Implémentation simplifiée : change l'opacité au survol
        Button[] allButtons = {btnDashboard, btnUsers, btnEntreprises, btnReports, btnSettings};
        for (Button b : allButtons) {
            b.setOnMouseEntered(e -> { if(!b.getStyle().contains("0.12")) b.setOpacity(0.7); });
            b.setOnMouseExited(e -> b.setOpacity(1.0));
        }
    }
}