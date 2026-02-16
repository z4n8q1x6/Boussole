package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.boussole.utils.MyBdConnexion;
import tn.esprit.boussole.service.AuthService;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.prefs.Preferences;

public class loginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;

    private final Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        String remembered = prefs.get("rememberedEmail", "");
        if (!remembered.isEmpty()) {
            emailField.setText(remembered);
            rememberMeCheckbox.setSelected(true);
        }

        loginButton.setOnAction(e -> handleLogin());
        forgotPasswordLink.setOnAction(e -> handleForgotPassword());

        loginButton.setOnMouseEntered(e ->
                loginButton.setStyle("-fx-background-color: #2980B9; -fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;")
        );
        loginButton.setOnMouseExited(e ->
                loginButton.setStyle("-fx-background-color: #3498DB; -fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand;")
        );
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs vides", "Veuillez remplir tous les champs.");
            return;
        }

        AuthInfo info = fetchAuthInfo(email);
        if (info == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de connexion", "Email ou mot de passe incorrect.");
            passwordField.clear();
            return;
        }

        if (!verifyPassword(password, info.storedPassword)) {
            showAlert(Alert.AlertType.ERROR, "Erreur de connexion", "Email ou mot de passe incorrect.");
            passwordField.clear();
            return;
        }

        // Génère token et stocke session
        String token = authService.generateToken(email, info.role == null ? "" : info.role);
        Preferences session = Preferences.userRoot().node(loginController.class.getName());
        session.put("jwt", token);
        session.put("email", email);
        session.put("role", info.role == null ? "" : info.role);

        if (rememberMeCheckbox.isSelected()) prefs.put("rememberedEmail", email);
        else prefs.remove("rememberedEmail");

        showAlert(Alert.AlertType.INFORMATION, "Connexion réussie", "Bienvenue " + email + " !");
        navigateToRoleInterface(info.role);
    }

    private AuthInfo fetchAuthInfo(String email) {
        String sql = "SELECT mot_de_passe, role FROM utilisateur WHERE email = ? LIMIT 1";
        Connection conn = MyBdConnexion.getinstance().getCnx();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String pwd = rs.getString("mot_de_passe");
                    String role = rs.getString("role");
                    return new AuthInfo(pwd, role);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur DB", "Impossible de se connecter à la base de données.");
        }
        return null;
    }

    private boolean verifyPassword(String plain, String stored) {
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            try {
                Class<?> bc = Class.forName("org.mindrot.jbcrypt.BCrypt");
                Method checkpw = bc.getMethod("checkpw", String.class, String.class);
                Object result = checkpw.invoke(null, plain, stored);
                return result instanceof Boolean && (Boolean) result;
            } catch (ClassNotFoundException cnfe) {
                // BCrypt absent
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
        return plain.equals(stored);
    }

    private void navigateToRoleInterface(String role) {
        try {
            String fxml;
            if (role != null && role.equalsIgnoreCase("SIEGE")) {
                fxml = "/dash.fxml"; // Dashboard Admin
            } else {
                fxml = "/dashUser.fxml"; // Dashboard Entreprise/Utilisateur
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            
            // Forcer le plein écran ou maximisé
            stage.setMaximized(true);
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir l'interface.\n\nDétail: " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        showAlert(Alert.AlertType.INFORMATION, "Mot de passe oublié", "Fonctionnalité à implémenter.");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class AuthInfo {
        final String storedPassword;
        final String role;
        AuthInfo(String storedPassword, String role) {
            this.storedPassword = storedPassword;
            this.role = role;
        }
    }
}
