package tn.esprit.boussole.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.boussole.utils.EmailService; // Import du nouveau service
import tn.esprit.boussole.utils.MyBdConnexion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class forgotPasswordController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImage;
    @FXML private VBox formVBox;
    @FXML private TextField emailField;
    @FXML private Button btnSend;
    @FXML private Hyperlink linkBack;

    @FXML
    public void initialize() {
        // Animation de fond
        if (rootPane != null && backgroundImage != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
            
            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(20), backgroundImage);
            scaleTransition.setFromX(1.1);
            scaleTransition.setFromY(1.1);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.setCycleCount(ScaleTransition.INDEFINITE);
            scaleTransition.setAutoReverse(true);
            scaleTransition.play();
        }
        
        // Animation d'entrée pour le formulaire
        if (formVBox != null) {
            formVBox.setOpacity(0);
            formVBox.setTranslateY(20);
            
            FadeTransition fade = new FadeTransition(Duration.seconds(0.5), formVBox);
            fade.setFromValue(0);
            fade.setToValue(1);
            
            TranslateTransition translate = new TranslateTransition(Duration.seconds(0.5), formVBox);
            translate.setFromY(20);
            translate.setToY(0);
            
            fade.play();
            translate.play();
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

        if (!checkEmailExists(email)) {
            showAlert(Alert.AlertType.ERROR, "Email inconnu", "Aucun compte n'est associé à cet email.");
            return;
        }

        String newPassword = generateRandomPassword(10);
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        if (updatePasswordInDB(email, hashedPassword)) {
            // Utilisation du nouveau service d'email
            String subject = "Réinitialisation de votre mot de passe - Boussole";
            String title = "Votre nouveau mot de passe";
            String body = "Vous avez demandé une réinitialisation de mot de passe. Voici votre nouveau mot de passe temporaire. Veuillez vous connecter et le changer dès que possible.";
            EmailService.sendHtmlEmail(email, subject, title, body, newPassword);

            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                    "Un nouveau mot de passe a été envoyé à " + email + ".\nVeuillez vérifier votre boîte de réception.");
            handleBack();
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le mot de passe. Veuillez réessayer.");
        }
    }

    private boolean checkEmailExists(String email) {
        String sql = "SELECT email FROM utilisateur WHERE email = ?";
        Connection conn = MyBdConnexion.getinstance().getCnx();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updatePasswordInDB(String email, String hashedPassword) {
        String sql = "UPDATE utilisateur SET mot_de_passe = ? WHERE email = ?";
        Connection conn = MyBdConnexion.getinstance().getCnx();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void handleBack() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), formVBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        TranslateTransition slideOut = new TranslateTransition(Duration.seconds(0.5), formVBox);
        slideOut.setToY(20);
        
        fadeOut.setOnFinished(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Parent root = loader.load();
                
                Stage stage = (Stage) linkBack.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
                
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à la page de connexion.");
            }
        });
        
        fadeOut.play();
        slideOut.play();
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