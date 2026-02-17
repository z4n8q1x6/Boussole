package tn.esprit.boussole.gui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.boussole.utils.MyBdConnexion;
import tn.esprit.boussole.service.AuthService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

public class loginController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImage;
    @FXML private VBox brandingVBox;
    @FXML private VBox loginFormVBox;
    @FXML private Canvas particleCanvas;
    
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button loginButton;
    @FXML private Button googleLoginButton;
    @FXML private Hyperlink forgotPasswordLink;

    private final Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private final AuthService authService = new AuthService();
    
    private GraphicsContext gc;
    private List<Particle> particles;
    private Random random = new Random();

    @FXML
    public void initialize() {
        // Binding pour l'image de fond responsive
        if (rootPane != null && backgroundImage != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
            
            // Animation Ken Burns (Zoom lent) sur l'image de fond
            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(20), backgroundImage);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.setCycleCount(ScaleTransition.INDEFINITE);
            scaleTransition.setAutoReverse(true);
            scaleTransition.play();
        }
        
        // Animation d'entrée pour le branding (gauche)
        if (brandingVBox != null) {
            brandingVBox.setOpacity(0);
            brandingVBox.setTranslateY(20);
            
            FadeTransition fade = new FadeTransition(Duration.seconds(1), brandingVBox);
            fade.setFromValue(0);
            fade.setToValue(1);
            
            TranslateTransition translate = new TranslateTransition(Duration.seconds(1), brandingVBox);
            translate.setFromY(20);
            translate.setToY(0);
            
            fade.play();
            translate.play();
        }
        
        // Animation d'entrée pour le formulaire (droite) - avec un léger délai
        if (loginFormVBox != null) {
            loginFormVBox.setOpacity(0);
            loginFormVBox.setTranslateY(20);
            
            FadeTransition fade = new FadeTransition(Duration.seconds(1), loginFormVBox);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.seconds(0.3)); // Délai pour effet cascade
            
            TranslateTransition translate = new TranslateTransition(Duration.seconds(1), loginFormVBox);
            translate.setFromY(20);
            translate.setToY(0);
            translate.setDelay(Duration.seconds(0.3));
            
            fade.play();
            translate.play();
        }
        
        // Initialisation des particules
        if (particleCanvas != null) {
            gc = particleCanvas.getGraphicsContext2D();
            particles = new ArrayList<>();
            for (int i = 0; i < 50; i++) { // 50 particules
                particles.add(new Particle());
            }
            
            // Lier la taille du canvas à la taille du rootPane
            particleCanvas.widthProperty().bind(rootPane.widthProperty());
            particleCanvas.heightProperty().bind(rootPane.heightProperty());

            // Lancer l'animation des particules
            new AnimationTimer() {
                @Override
                public void handle(long now) {
                    updateParticles();
                    drawParticles();
                }
            }.start();
        }

        String remembered = prefs.get("rememberedEmail", "");
        if (!remembered.isEmpty()) {
            emailField.setText(remembered);
            rememberMeCheckbox.setSelected(true);
        }

        loginButton.setOnAction(e -> handleLogin());
        forgotPasswordLink.setOnAction(e -> handleForgotPassword());
        
        if (googleLoginButton != null) {
            googleLoginButton.setOnAction(e -> handleGoogleLogin());
        }

        setupButtonHoverEffects();
    }
    
    private void updateParticles() {
        gc.clearRect(0, 0, particleCanvas.getWidth(), particleCanvas.getHeight()); // Effacer le canvas
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
            
            // Revenir en haut si la particule sort par le bas
            if (p.y > particleCanvas.getHeight()) {
                p.y = -p.radius;
                p.x = random.nextDouble() * particleCanvas.getWidth();
            }
            // Revenir à gauche si la particule sort par la droite
            if (p.x > particleCanvas.getWidth()) {
                p.x = -p.radius;
                p.y = random.nextDouble() * particleCanvas.getHeight();
            }
        }
    }
    
    private void drawParticles() {
        for (Particle p : particles) {
            gc.setFill(Color.rgb(255, 255, 255, p.opacity));
            gc.fillOval(p.x, p.y, p.radius * 2, p.radius * 2);
        }
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
    
    private void handleGoogleLogin() {
        showAlert(Alert.AlertType.INFORMATION, "Google Login", "Authentification Google en cours de développement...");
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgotPassword.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) forgotPasswordLink.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true); // Garder le plein écran
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de récupération.");
        }
    }
    
    private void setupButtonHoverEffects() {
        if (loginButton != null) {
            loginButton.setOnMouseEntered(e ->
                    loginButton.setStyle("-fx-background-color: #0284C7; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;")
            );
            loginButton.setOnMouseExited(e ->
                    loginButton.setStyle("-fx-background-color: #0EA5E9; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;")
            );
        }
        
        if (googleLoginButton != null) {
            googleLoginButton.setOnMouseEntered(e ->
                    googleLoginButton.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 10; -fx-text-fill: #020617; -fx-cursor: hand; -fx-font-weight: bold;")
            );
            googleLoginButton.setOnMouseExited(e ->
                    googleLoginButton.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-text-fill: #020617; -fx-cursor: hand; -fx-font-weight: bold;")
            );
        }
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
    
    // Classe interne pour les particules
    private class Particle {
        double x, y;
        double vx, vy;
        double radius;
        double opacity;
        
        public Particle() {
            this.x = random.nextDouble() * particleCanvas.getWidth();
            this.y = random.nextDouble() * particleCanvas.getHeight();
            this.vx = (random.nextDouble() - 0.5) * 0.5; // Petite vitesse aléatoire
            this.vy = (random.nextDouble() - 0.5) * 0.5;
            this.radius = random.nextDouble() * 2 + 1; // Rayon entre 1 et 3
            this.opacity = random.nextDouble() * 0.5 + 0.2; // Opacité entre 0.2 et 0.7
        }
    }
}
