package tn.esprit.boussole.gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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
    @FXML private Canvas particleCanvas;

    @FXML private VBox brandingVBox; // Partie Gauche (Logo)
    @FXML private VBox loginFormVBox; // Partie Droite (Formulaire)

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
    private final Random random = new Random();

    @FXML
    public void initialize() {
        // 1. Animation d'entrée "Porte qui se ferme" (Inverse de la sortie)
        // On place les éléments hors de l'écran initialement
        if (brandingVBox != null) brandingVBox.setTranslateX(-500);
        if (loginFormVBox != null) loginFormVBox.setTranslateX(500);

        Platform.runLater(() -> {
            // Animation : Ils reviennent à leur place (0)
            if (brandingVBox != null) {
                TranslateTransition slideInLeft = new TranslateTransition(Duration.seconds(0.8), brandingVBox);
                slideInLeft.setToX(0);
                slideInLeft.play();
            }
            if (loginFormVBox != null) {
                TranslateTransition slideInRight = new TranslateTransition(Duration.seconds(0.8), loginFormVBox);
                slideInRight.setToX(0);
                slideInRight.play();
            }
        });

        // Animation Ken Burns et Initialisation des particules
        setupVisuals();

        // Chargement de l'email mémorisé
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

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs vides", "Veuillez remplir tous les champs.");
            return;
        }

        AuthInfo info = fetchAuthInfo(email);

        if (info == null || !verifyPassword(password, info.storedPassword)) {
            showAlert(Alert.AlertType.ERROR, "Erreur de connexion", "Email ou mot de passe incorrect.");
            passwordField.clear();
            return;
        }

        if (!info.isActif) {
            showAlert(Alert.AlertType.ERROR, "Compte Verrouillé",
                    "Bonjour " + info.prenom + ",\n\nVotre compte est actuellement désactivé. " +
                            "Veuillez contacter l'administrateur pour plus d'informations.");
            passwordField.clear();
            return;
        }

        String token = authService.generateToken(email, info.role == null ? "" : info.role);

        Preferences session = Preferences.userRoot().node(loginController.class.getName());
        session.put("jwt", token);
        session.put("email", email);
        session.put("role", info.role == null ? "" : info.role);
        session.put("prenom", info.prenom == null ? "" : info.prenom);

        if (rememberMeCheckbox.isSelected()) prefs.put("rememberedEmail", email);
        else prefs.remove("rememberedEmail");

        showSuccessAlert(info.prenom);
    }

    private AuthInfo fetchAuthInfo(String email) {
        String sql = "SELECT mot_de_passe, role, prenom, actif FROM utilisateur WHERE email = ? LIMIT 1";
        Connection conn = MyBdConnexion.getinstance().getCnx();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthInfo(
                            rs.getString("mot_de_passe"),
                            rs.getString("role"),
                            rs.getString("prenom"),
                            rs.getBoolean("actif")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur DB", "Impossible de contacter la base de données.");
        }
        return null;
    }

    private static class AuthInfo {
        final String storedPassword, role, prenom;
        final boolean isActif;

        AuthInfo(String storedPassword, String role, String prenom, boolean isActif) {
            this.storedPassword = storedPassword;
            this.role = role;
            this.prenom = prenom;
            this.isActif = isActif;
        }
    }

    private void showSuccessAlert(String prenom) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Succès");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #111827; -fx-background-radius: 14;");
        dialogPane.getButtonTypes().add(ButtonType.OK);
        dialogPane.lookupButton(ButtonType.OK).setVisible(false);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30, 50, 30, 50));
        Label icon = new Label("✅");
        icon.setFont(Font.font(48));
        Label message = new Label("Connexion réussie, " + (prenom != null ? prenom : "") + " !");
        message.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        content.getChildren().addAll(icon, message);
        dialogPane.setContent(content);

        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
        delay.setOnFinished(e -> {
            dialog.close();
            navigateToRoleInterface(Preferences.userRoot().node(loginController.class.getName()).get("role", ""));
        });
        dialog.setOnShown(e -> delay.play());
        dialog.showAndWait();
    }

    private void navigateToRoleInterface(String role) {
        try {
            String fxml = (role != null && role.equalsIgnoreCase("SIEGE")) ? "/dash.fxml" : "/dashUser.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean verifyPassword(String plain, String stored) {
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            try {
                Class<?> bc = Class.forName("org.mindrot.jbcrypt.BCrypt");
                Method checkpw = bc.getMethod("checkpw", String.class, String.class);
                return (Boolean) checkpw.invoke(null, plain, stored);
            } catch (Exception ex) { return false; }
        }
        return plain.equals(stored);
    }

    private void setupVisuals() {
        // Animation Ken Burns
        if (backgroundImage != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
            ScaleTransition st = new ScaleTransition(Duration.seconds(20), backgroundImage);
            st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.1); st.setToY(1.1);
            st.setCycleCount(Animation.INDEFINITE); st.setAutoReverse(true); st.play();
        }

        // Initialisation des particules
        if (particleCanvas != null) {
            particleCanvas.widthProperty().bind(rootPane.widthProperty());
            particleCanvas.heightProperty().bind(rootPane.heightProperty());
            particleCanvas.setMouseTransparent(true);
            particleCanvas.toFront();

            gc = particleCanvas.getGraphicsContext2D();
            particles = new ArrayList<>();

            // Utilisation de Platform.runLater pour attendre que le Canvas ait une taille
            Platform.runLater(() -> {
                for (int i = 0; i < 80; i++) {
                    particles.add(new Particle());
                }
                AnimationTimer timer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        updateParticles();
                    }
                };
                timer.start();
            });
        }
    }

    private void updateParticles() {
        if (gc == null || particleCanvas == null) return;
        gc.clearRect(0, 0, particleCanvas.getWidth(), particleCanvas.getHeight());
        for (Particle p : particles) {
            p.update();
            p.draw(gc);
        }
    }

    private void setupButtonHoverEffects() {
        loginButton.setOnMouseEntered(e -> loginButton.setStyle("-fx-background-color: #0284C7; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle("-fx-background-color: #0EA5E9; -fx-background-radius: 10; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;"));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleForgotPassword() {
        // Animation "Porte qui s'ouvre" (Sortie)
        ParallelTransition pt = new ParallelTransition();

        if (brandingVBox != null) {
            TranslateTransition slideLeft = new TranslateTransition(Duration.seconds(0.8), brandingVBox);
            slideLeft.setToX(-brandingVBox.getWidth() - 200); // Sort vers la gauche
            pt.getChildren().add(slideLeft);
        }

        if (loginFormVBox != null) {
            TranslateTransition slideRight = new TranslateTransition(Duration.seconds(0.8), loginFormVBox);
            slideRight.setToX(loginFormVBox.getWidth() + 200); // Sort vers la droite
            pt.getChildren().add(slideRight);
        }

        if (pt.getChildren().isEmpty()) {
            // Fallback si les VBox ne sont pas liées
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), rootPane);
            fadeOut.setToValue(0);
            pt.getChildren().add(fadeOut);
        }

        pt.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/forgotPassword.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) forgotPasswordLink.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page.");
            }
        });
        pt.play();
    }

    private void handleGoogleLogin() { /* Logique à implémenter */ }

    private class Particle {
        double x, y, vx, vy, radius, opacity;

        public Particle() {
            // Initialisation différée via Platform.runLater dans setupVisuals
            // garantit que particleCanvas.getWidth() > 0
            if (particleCanvas != null && particleCanvas.getWidth() > 0) {
                this.x = random.nextDouble() * particleCanvas.getWidth();
                this.y = random.nextDouble() * particleCanvas.getHeight();
            } else {
                // Fallback au cas où
                this.x = random.nextDouble() * 800;
                this.y = random.nextDouble() * 600;
            }
            initProperties();
        }

        void initProperties() {
            this.vx = (random.nextDouble() - 0.5) * 0.5; 
            this.vy = (random.nextDouble() - 0.5) * 0.5; 
            this.radius = random.nextDouble() * 3 + 1; 
            this.opacity = random.nextDouble() * 0.5 + 0.2; 
        }

        void update() {
            x += vx;
            y += vy;
            
            if (particleCanvas != null) {
                double w = particleCanvas.getWidth();
                double h = particleCanvas.getHeight();
                if (x < -10) x = w + 10;
                if (x > w + 10) x = -10;
                if (y < -10) y = h + 10;
                if (y > h + 10) y = -10;
            }
        }

        void draw(GraphicsContext gc) {
            gc.setGlobalAlpha(opacity);
            gc.setFill(Color.WHITE);
            gc.fillOval(x, y, radius, radius);
        }
    }
}