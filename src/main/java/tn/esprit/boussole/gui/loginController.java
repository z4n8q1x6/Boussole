package tn.esprit.boussole.gui;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import tn.esprit.boussole.utils.MyBdConnexion;
import tn.esprit.boussole.service.AuthService;
import tn.esprit.boussole.service.FacePlusPlusService;
import tn.esprit.boussole.utils.NotificationManager;

import java.io.*;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class loginController {

    static {
        try {
            OpenCV.loadLocally();
        } catch (Exception e) {
            System.err.println("Erreur chargement OpenCV : " + e.getMessage());
        }
    }

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImage;
    @FXML private Canvas particleCanvas;
    @FXML private VBox brandingVBox, loginFormVBox;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button loginButton, googleLoginButton, faceAuthButton;
    @FXML private Hyperlink forgotPasswordLink;

    private final Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    private final AuthService authService = new AuthService();
    private final FacePlusPlusService faceService = new FacePlusPlusService();

    private GraphicsContext gc;
    private List<Particle> particles;
    private final Random random = new Random();

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private Stage cameraStage;

    @FXML
    public void initialize() {
        // Animation d'entrée des panneaux
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
        if (!remembered.isEmpty()) emailField.setText(remembered);

        loginButton.setOnAction(e -> handleLogin());
        faceAuthButton.setOnAction(e -> handleFaceAuth());
        forgotPasswordLink.setOnAction(e -> handleForgotPassword());
        if (googleLoginButton != null) googleLoginButton.setOnAction(e -> handleGoogleLogin());

        setupButtonHoverEffects();
    }

    // --- AUTHENTIFICATION CLASSIQUE ---
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.WARNING, "Champs vides", "Veuillez remplir tous les champs.");
            return;
        }

        AuthInfo info = fetchAuthInfo(email);
        if (info == null) {
            NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Email inconnu", "Aucun compte trouvé pour cet email.");
            passwordField.clear();
            return;
        }

        if (!verifyPassword(password, info.storedPassword)) {
            NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Mot de passe incorrect", "Le mot de passe que vous avez saisi est incorrect.");
            passwordField.clear();
            return;
        }

        if (!info.isActif) {
            NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Compte Verrouillé", "Bonjour " + info.prenom + ", votre compte est désactivé.");
            return;
        }

        proceedToLogin(info);
    }

    // --- RECONNAISSANCE FACIALE ---
    @FXML
    private void handleFaceAuth() {
        cameraStage = new Stage();
        cameraStage.setTitle("Reconnaissance Faciale");

        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(640);
        cameraView.setFitHeight(480);

        Button btnCapture = new Button("📷 Scanner mon visage");
        btnCapture.setStyle("-fx-background-color: #00E5CC; -fx-text-fill: #06080F; -fx-font-weight: bold; -fx-background-radius: 20;");
        btnCapture.setOnAction(e -> processFaceCapture());

        VBox layout = new VBox(10, cameraView, btnCapture);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #06080F; -fx-padding: 20;");

        cameraStage.setScene(new Scene(layout));
        cameraStage.setOnCloseRequest(e -> stopCamera());
        cameraStage.show();

        startCamera(cameraView);
    }

    private void startCamera(ImageView view) {
        capture = new VideoCapture(0);
        if (capture.isOpened()) {
            timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(() -> {
                Mat frame = new Mat();
                if (capture.read(frame)) {
                    Image imageToShow = mat2Image(frame);
                    Platform.runLater(() -> view.setImage(imageToShow));
                }
            }, 0, 33, TimeUnit.MILLISECONDS);
        } else {
            NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur Caméra", "Impossible d'ouvrir la webcam.");
            cameraStage.close();
        }
    }

    private void stopCamera() {
        if (timer != null && !timer.isShutdown()) timer.shutdown();
        if (capture != null && capture.isOpened()) capture.release();
    }

    private void processFaceCapture() {
        if (capture != null && capture.isOpened()) {
            Mat frame = new Mat();
            if (capture.read(frame)) {
                MatOfByte buffer = new MatOfByte();
                Imgcodecs.imencode(".png", frame, buffer);
                byte[] imageBytes = buffer.toArray();
                stopCamera();
                cameraStage.close();

                NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.INFO, "Analyse", "Vérification du visage...");

                new Thread(() -> {
                    try {
                        String faceToken = faceService.searchFace(imageBytes);
                        Platform.runLater(() -> {
                            if (faceToken != null) {
                                AuthInfo info = fetchAuthInfoByFaceToken(faceToken);
                                if (info != null && info.isActif) {
                                    NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Succès", "Visage reconnu !");
                                    proceedToLogin(info);
                                } else {
                                    NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Échec", "Utilisateur non trouvé ou inactif.");
                                }
                            } else {
                                NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Échec", "Visage non reconnu.");
                            }
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur API", "Connexion Face++ impossible."));
                    }
                }).start();
            }
        }
    }

    // --- GOOGLE OAUTH ---
    @FXML
    private void handleGoogleLogin() {
        new Thread(() -> {
            try {
                InputStream in = loginController.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
                if (in == null) return;

                GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), new InputStreamReader(in));
                GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), clientSecrets, SCOPES)
                        .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                        .setAccessType("offline")
                        .build();

                LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
                Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
                fetchGoogleUserInfo(credential);
            } catch (Exception e) {
                Platform.runLater(() -> NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur Google", "Échec de connexion."));
            }
        }).start();
    }

    private void fetchGoogleUserInfo(Credential credential) throws Exception {
        Oauth2 oauth2 = new Oauth2.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Boussole").build();
        Userinfo userInfo = oauth2.userinfo().get().execute();
        Platform.runLater(() -> handleUserDatabaseLogin(userInfo.getEmail(), userInfo.getFamilyName(), userInfo.getGivenName()));
    }

    // --- LOGIQUE COMMUNE ---
    private void proceedToLogin(AuthInfo info) {
        String token = authService.generateToken(info.email, info.role);
        Preferences session = Preferences.userRoot().node(loginController.class.getName());
        session.put("jwt", token);
        session.put("email", info.email);
        session.put("role", info.role);

        if (rememberMeCheckbox.isSelected()) prefs.put("rememberedEmail", info.email);
        else prefs.remove("rememberedEmail");

        NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Bienvenue", "Connexion réussie !");

        PauseTransition delay = new PauseTransition(Duration.seconds(1.2));
        delay.setOnFinished(e -> navigateToRoleInterface(info.role));
        delay.play();
    }

    private void navigateToRoleInterface(String role) {
        try {
            String fxml = "SIEGE".equalsIgnoreCase(role) ? "/dash.fxml" : "/dashUser.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private boolean verifyPassword(String plain, String stored) {
        if (stored == null) return false;
        
        // Symfony/PHP uses $2y$ by default, but jbcrypt only understands $2a$.
        // Since they are algorithmically identical, we can safely replace the prefix.
        if (stored.startsWith("$2y$")) {
            stored = "$2a$" + stored.substring(4);
        }
        
        if (stored.startsWith("$2")) {
            try {
                Class<?> bc = Class.forName("org.mindrot.jbcrypt.BCrypt");
                Method checkpw = bc.getMethod("checkpw", String.class, String.class);
                return (Boolean) checkpw.invoke(null, plain, stored);
            } catch (Exception ex) { 
                ex.printStackTrace();
                return false; 
            }
        }
        return plain.equals(stored);
    }

    private AuthInfo fetchAuthInfo(String email) {
        String sql = "SELECT mot_de_passe, role, prenom, actif FROM utilisateur WHERE email = ?";
        try (PreparedStatement ps = MyBdConnexion.getinstance().getCnx().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new AuthInfo(email, rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4));
        } catch (SQLException e) {
            e.printStackTrace();
            if (rootPane != null && rootPane.getScene() != null) {
                NotificationManager.show(rootPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur base de données", "Impossible de vérifier l'email/mot de passe. Vérifiez la connexion à la base de données.");
            }
        }
        return null;
    }

    private AuthInfo fetchAuthInfoByFaceToken(String token) {
        String sql = "SELECT email, mot_de_passe, role, prenom, actif FROM utilisateur WHERE face_token = ?";
        try (PreparedStatement ps = MyBdConnexion.getinstance().getCnx().prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new AuthInfo(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5));
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private void handleUserDatabaseLogin(String email, String nom, String prenom) {
        AuthInfo info = fetchAuthInfo(email);
        if (info != null) proceedToLogin(info);
        else {
            // Création automatique si inexistant
            String sql = "INSERT INTO utilisateur (email, nom, prenom, role, actif, mot_de_passe, date_creation) VALUES (?, ?, ?, 'USER', 1, 'GOOGLE_AUTH', NOW())";
            try (PreparedStatement ps = MyBdConnexion.getinstance().getCnx().prepareStatement(sql)) {
                ps.setString(1, email); ps.setString(2, nom); ps.setString(3, prenom);
                ps.executeUpdate();
                proceedToLogin(fetchAuthInfo(email));
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private static class AuthInfo {
        final String email, storedPassword, role, prenom;
        final boolean isActif;
        AuthInfo(String e, String s, String r, String p, boolean a) {
            this.email = e; this.storedPassword = s; this.role = r; this.prenom = p; this.isActif = a;
        }
    }

    // --- VISUELS & PARTICULES ---
    private void setupVisuals() {
        if (backgroundImage != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        if (particleCanvas != null) {
            particleCanvas.widthProperty().bind(rootPane.widthProperty());
            particleCanvas.heightProperty().bind(rootPane.heightProperty());
            gc = particleCanvas.getGraphicsContext2D();
            particles = new ArrayList<>();
            for (int i = 0; i < 80; i++) particles.add(new Particle());
            new AnimationTimer() { @Override public void handle(long now) { updateParticles(); } }.start();
        }
    }

    private void updateParticles() {
        gc.clearRect(0, 0, particleCanvas.getWidth(), particleCanvas.getHeight());
        for (Particle p : particles) { p.update(); p.draw(gc); }
    }

    private void setupButtonHoverEffects() {
        loginButton.setOnMouseEntered(e -> loginButton.setOpacity(0.9));
        loginButton.setOnMouseExited(e -> loginButton.setOpacity(1.0));
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    private void handleForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/forgotPassword.fxml"));
            rootPane.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private class Particle {
        double x, y, vx, vy, radius, opacity;
        public Particle() { x = random.nextDouble() * 1200; y = random.nextDouble() * 800; vx = (random.nextDouble()-0.5)*0.5; vy = (random.nextDouble()-0.5)*0.5; radius = random.nextDouble()*3+1; opacity = random.nextDouble()*0.5+0.2; }
        void update() { x += vx; y += vy; if (x < 0 || x > 1920) vx *= -1; if (y < 0 || y > 1080) vy *= -1; }
        void draw(GraphicsContext gc) { gc.setGlobalAlpha(opacity); gc.setFill(Color.WHITE); gc.fillOval(x, y, radius, radius); }
    }
}