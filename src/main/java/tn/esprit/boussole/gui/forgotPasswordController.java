package tn.esprit.boussole.gui;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class forgotPasswordController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImage;
    @FXML private TextField emailField;
    @FXML private Button btnSend;
    @FXML private Hyperlink linkBack;

    @FXML
    public void initialize() {
        // Animation de fond (comme sur le login)
        if (rootPane != null && backgroundImage != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
            
            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(20), backgroundImage);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.setCycleCount(ScaleTransition.INDEFINITE);
            scaleTransition.setAutoReverse(true);
            scaleTransition.play();
        }

        // Actions
        btnSend.setOnAction(e -> handleSend());
        linkBack.setOnAction(e -> handleBack());

        // Effets de survol
        setupButtonHoverEffects();
    }

    private void handleSend() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ vide", "Veuillez entrer votre adresse email.");
            return;
        }

        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Email invalide", "Veuillez entrer une adresse email valide.");
            return;
        }

        // TODO: Intégrer la logique d'envoi d'email ici
        // Pour l'instant, on simule l'envoi
        showAlert(Alert.AlertType.INFORMATION, "Email envoyé", 
                "Si un compte est associé à " + email + ", vous recevrez un lien de réinitialisation.");
    }

    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) linkBack.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true); // Garder le plein écran
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à la page de connexion.");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void setupButtonHoverEffects() {
        btnSend.setOnMouseEntered(e -> 
            btnSend.setStyle("-fx-background-color: #0284C7; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;")
        );
        btnSend.setOnMouseExited(e -> 
            btnSend.setStyle("-fx-background-color: #0EA5E9; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;")
        );
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
