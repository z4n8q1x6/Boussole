package tn.esprit.boussole.gui;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.boussole.utils.MyBdConnexion;
import tn.esprit.boussole.service.AuthService;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

public class loginController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImage;
    @FXML private Canvas particleCanvas;

    @FXML private VBox brandingVBox;
    @FXML private VBox loginFormVBox;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button loginButton;
    @FXML private Button googleLoginButton;
    @FXML private Button faceAuthButton;
    @FXML private Hyperlink forgotPasswordLink;

    private final Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private final AuthService authService = new AuthService();

    private GraphicsContext gc;
    private List<Particle> particles;
    private final Random random = new Random();

    // Configuration Google OAuth2
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    @FXML
    public void initialize() {
        // Animation d'entrée
        if (brandingVBox != null) brandingVBox.setTranslateX(-500);
        if (loginFormVBox != null) loginFormVBox.setTranslateX(500);

        Platform.runLater(() -> {
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

        setupVisuals();

        String remembered = prefs.get("rememberedEmail", "");
        if (!remembered.isEmpty()) {
            emailField.setText(remembered);
        }

        loginButton.setOnAction(e -> handleLogin());
        faceAuthButton.setOnAction(e -> handleFaceAuth());
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
                    "Bonjour " + info.prenom + ",\n\nVotre compte est actuellement désactivé.");
            passwordField.clear();
            return;
        }

        proceedToLogin(info);
    }

    @FXML
    private void handleFaceAuth() {
        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            showAlert(Alert.AlertType.WARNING, "Email manquant", "Veuillez entrer votre email avant de lancer la reconnaissance faciale.");
            return;
        }

        AuthInfo info = fetchAuthInfo(email);
        if (info == null) {
            showAlert(Alert.AlertType.ERROR, "Utilisateur inconnu", "Aucun compte n'est associé à cet email.");
            return;
        }

        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION, "Lancement de la reconnaissance faciale...");
        infoAlert.setTitle("Reconnaissance faciale");
        infoAlert.setHeaderText("Veuillez regarder la caméra.");
        infoAlert.show();

        new Thread(() -> {
            try {
                String pythonPath = "python";
                String scriptPath = new File("python_scripts/face_auth.py").getAbsolutePath();

                ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, email);
                pb.redirectErrorStream(true);

                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Python script: " + line);
                        output.append(line);
                    }
                }

                process.waitFor();
                String finalOutput = output.toString();

                Platform.runLater(() -> {
                    infoAlert.close();
                    if (finalOutput.contains("AUTH_SUCCESS")) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Visage reconnu ! Connexion en cours...");
                        proceedToLogin(info);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Échec", "La reconnaissance faciale a échoué.");
                    }
                });

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    infoAlert.close();
                    showAlert(Alert.AlertType.ERROR, "Erreur d'exécution", "Impossible de lancer le script.");
                });
            }
        }).start();
    }

    @FXML
    private void handleGoogleLogin() {
        new Thread(() -> {
            try {
                InputStream in = loginController.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
                if (in == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Config", "Fichier credentials.json introuvable."));
                    return;
                }

                GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), new InputStreamReader(in));

                GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), clientSecrets, SCOPES)
                        .setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                        .setAccessType("offline")
                        .build();

                LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
                Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

                fetchGoogleUserInfo(credential);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Google", "Échec de la connexion Google : " + e.getMessage()));
            }
        }).start();
    }

    private void fetchGoogleUserInfo(Credential credential) {
        try {
            Oauth2 oauth2 = new Oauth2.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("Boussole")
                    .build();

            Userinfo userInfo = oauth2.userinfo().get().execute();
            String email = userInfo.getEmail();
            String nom = userInfo.getFamilyName();
            String prenom = userInfo.getGivenName();

            System.out.println("Connexion Google réussie : " + email);

            Platform.runLater(() -> handleUserDatabaseLogin(email, nom, prenom));

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur API", "Impossible de récupérer les infos Google."));
        }
    }

    private void handleUserDatabaseLogin(String email, String nom, String prenom) {
        AuthInfo info = fetchAuthInfo(email);

        if (info != null) {
            if (!info.isActif) {
                showAlert(Alert.AlertType.ERROR, "Compte Verrouillé", "Votre compte est désactivé.");
                return;
            }
            proceedToLogin(info);
        } else {
            createGoogleUser(email, nom, prenom);
            info = fetchAuthInfo(email);
            if (info != null) {
                proceedToLogin(info);
            }
        }
    }

    private void createGoogleUser(String email, String nom, String prenom) {
        String sql = "INSERT INTO utilisateur (email, nom, prenom, role, actif, mot_de_passe, date_creation) VALUES (?, ?, ?, 'USER', 1, 'GOOGLE_AUTH', NOW())";
        Connection conn = MyBdConnexion.getinstance().getCnx();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, nom != null ? nom : "GoogleUser");
            ps.setString(3, prenom != null ? prenom : "");
            ps.executeUpdate();
            System.out.println("Nouvel utilisateur Google créé : " + email);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur DB", "Impossible de créer le compte Google.");
        }
    }

    private void proceedToLogin(AuthInfo info) {
        String token = authService.generateToken(info.email, info.role == null ? "" : info.role);

        Preferences session = Preferences.userRoot().node(loginController.class.getName());
        session.put("jwt", token);
        session.put("email", info.email);
        session.put("role", info.role == null ? "" : info.role);
        session.put("prenom", info.prenom == null ? "" : info.prenom);

        if (rememberMeCheckbox.isSelected()) prefs.put("rememberedEmail", info.email);
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
                            email,
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
        final String email, storedPassword, role, prenom;
        final boolean isActif;

        AuthInfo(String email, String storedPassword, String role, String prenom, boolean isActif) {
            this.email = email;
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
            
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage != null) {
                // 1. Configurer la scène
                Scene scene = new Scene(root);
                stage.setScene(scene);

                // 2. Forcer la taille de l'écran (Workaround pour le bug de redimensionnement)
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                stage.setX(screenBounds.getMinX());
                stage.setY(screenBounds.getMinY());
                stage.setWidth(screenBounds.getWidth());
                stage.setHeight(screenBounds.getHeight());

                // 3. Appliquer Maximized avec une "secousse"
                stage.setMaximized(false);
                stage.setMaximized(true);

                stage.show();
            } else {
                System.err.println("Erreur critique : Impossible de récupérer la fenêtre principale.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean verifyPassword(String plain, String stored) {
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            try {
                Class<?> bc = Class.forName("org.mindrot.jbcrypt.BCrypt");
                java.lang.reflect.Method checkpw = bc.getMethod("checkpw", String.class, String.class);
                return (Boolean) checkpw.invoke(null, plain, stored);
            } catch (Exception ex) { return false; }
        }
        return plain.equals(stored);
    }

    private void setupVisuals() {
        if (backgroundImage != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
            ScaleTransition st = new ScaleTransition(Duration.seconds(20), backgroundImage);
            st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.1); st.setToY(1.1);
            st.setCycleCount(Animation.INDEFINITE); st.setAutoReverse(true); st.play();
        }

        if (particleCanvas != null) {
            particleCanvas.widthProperty().bind(rootPane.widthProperty());
            particleCanvas.heightProperty().bind(rootPane.heightProperty());
            particleCanvas.setMouseTransparent(true);
            particleCanvas.toFront();

            gc = particleCanvas.getGraphicsContext2D();
            particles = new ArrayList<>();

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
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }

    private void handleForgotPassword() {
        ParallelTransition pt = new ParallelTransition();
        if (brandingVBox != null) {
            TranslateTransition slideLeft = new TranslateTransition(Duration.seconds(0.8), brandingVBox);
            slideLeft.setToX(-brandingVBox.getWidth() - 200);
            pt.getChildren().add(slideLeft);
        }
        if (loginFormVBox != null) {
            TranslateTransition slideRight = new TranslateTransition(Duration.seconds(0.8), loginFormVBox);
            slideRight.setToX(loginFormVBox.getWidth() + 200);
            pt.getChildren().add(slideRight);
        }
        if (pt.getChildren().isEmpty()) {
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

    private class Particle {
        double x, y, vx, vy, radius, opacity;

        public Particle() {
            if (particleCanvas != null && particleCanvas.getWidth() > 0) {
                this.x = random.nextDouble() * particleCanvas.getWidth();
                this.y = random.nextDouble() * particleCanvas.getHeight();
            } else {
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