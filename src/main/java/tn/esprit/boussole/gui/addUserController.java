package tn.esprit.boussole.gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio; // Import nécessaire
import tn.esprit.boussole.models.user;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.FacePlusPlusService;
import tn.esprit.boussole.service.userService;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.boussole.utils.EmailService;
import tn.esprit.boussole.utils.NotificationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class addUserController {

    static {
        try { nu.pattern.OpenCV.loadLocally(); } catch (Exception e) {}
    }

    @FXML private AnchorPane mainAnchorPane;
    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTelephone, txtSolde, txtNomEntreprise, txtAdresseEntreprise;
    @FXML private CheckBox checkActif;
    @FXML private Button btnCreate;
    @FXML private Button btnPhoto;

    private Runnable onUserCreated;
    private String capturedFaceToken = null;
    private final FacePlusPlusService faceService = new FacePlusPlusService();

    // Webcam
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private Stage cameraStage;

    @FXML
    public void initialize() {
        mainAnchorPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), mainAnchorPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        txtTelephone.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*") || newVal.length() > 8) txtTelephone.setText(old);
        });
    }

    @FXML
    private void handleCapturePhoto() {
        cameraStage = new Stage();
        cameraStage.setTitle("Enregistrement Visage");
        
        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(640);
        cameraView.setFitHeight(480);
        
        Button btnCapture = new Button("📷 Capturer");
        btnCapture.setStyle("-fx-background-color: #00E5CC; -fx-text-fill: #06080F; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 20;");
        btnCapture.setOnAction(e -> processFaceCapture());

        VBox layout = new VBox(10, cameraView, btnCapture);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #06080F; -fx-padding: 20;");
        
        Scene scene = new Scene(layout);
        cameraStage.setScene(scene);
        cameraStage.setOnCloseRequest(e -> stopCamera());
        cameraStage.show();

        startCamera(cameraView);
    }

    private void startCamera(ImageView view) {
        // Utilisation de CAP_DSHOW pour éviter les erreurs MSMF
        capture = new VideoCapture(0, Videoio.CAP_DSHOW);
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
            NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur Caméra", "Impossible d'ouvrir la webcam.");
            cameraStage.close();
        }
    }

    private void stopCamera() {
        if (timer != null && !timer.isShutdown()) {
            timer.shutdown();
            try { timer.awaitTermination(33, TimeUnit.MILLISECONDS); } catch (InterruptedException e) {}
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
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

                NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.INFO, "Traitement", "Analyse du visage en cours...");

                new Thread(() -> {
                    try {
                        String token = faceService.detectFace(imageBytes);
                        
                        if (token != null) {
                            faceService.addFaceToSet(token);
                            capturedFaceToken = token;
                            
                            Platform.runLater(() -> {
                                NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Succès", "Visage enregistré !");
                                btnPhoto.setText("✅ Visage enregistré");
                                btnPhoto.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10B981; -fx-border-color: #10B981; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-weight: bold;");
                            });
                        } else {
                            Platform.runLater(() -> NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.WARNING, "Échec", "Aucun visage détecté. Réessayez."));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Platform.runLater(() -> NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur API", "Problème avec Face++."));
                    }
                }).start();
            }
        }
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    @FXML
    private void handleCreate() {
        if (!validateFields()) return;

        try {
            userService service = new userService();

            String motDePasseClair = genererMotDePasse(8);
            String motDePasseHache = BCrypt.hashpw(motDePasseClair, BCrypt.gensalt());

            user u = new user();
            u.setNom(txtNom.getText().trim());
            u.setPrenom(txtPrenom.getText().trim());
            u.setEmail(txtEmail.getText().trim());
            u.setMotDePasse(motDePasseHache);
            u.setRole("ENTREPRISE");
            u.setActif(checkActif.isSelected());
            u.setDateCreation(LocalDateTime.now());
            
            if (capturedFaceToken != null) {
                u.setFaceToken(capturedFaceToken);
            }

            franchise f = new franchise();
            f.setNom(txtNomEntreprise.getText().trim());
            f.setAdresse(txtAdresseEntreprise.getText().trim());
            f.setEmail(txtEmail.getText().trim());
            f.setTelephone(txtTelephone.getText().trim());
            f.setActif(checkActif.isSelected());
            try {
                f.setSoldeActuel(Double.parseDouble(txtSolde.getText()));
            } catch (NumberFormatException e) {
                f.setSoldeActuel(0.0);
            }
            f.setDateCreation(LocalDateTime.now());

            service.insertUserWithFranchise(u, f);

            String subject = "Bienvenue sur Boussole - Vos identifiants";
            String title = "Bienvenue, " + u.getPrenom() + " !";
            String body = "Votre compte entreprise a été créé avec succès. Voici votre mot de passe temporaire.";
            EmailService.sendHtmlEmail(u.getEmail(), subject, title, body, motDePasseClair);

            NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Succès", "Compte créé et email envoyé.");

            if (onUserCreated != null) onUserCreated.run();
            closeWindow();

        } catch (Exception e) {
            NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur", "Détails : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private String genererMotDePasse(int longueur) {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < longueur; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    private boolean validateFields() {
        if (txtEmail.getText().isEmpty() || !txtEmail.getText().contains("@")) {
            NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.WARNING, "Erreur de saisie", "Email invalide.");
            return false;
        }
        if (txtNomEntreprise.getText().isEmpty()) {
            NotificationManager.show(mainAnchorPane.getScene().getWindow(), NotificationManager.Type.WARNING, "Erreur de saisie", "Le nom de l'entreprise est obligatoire.");
            return false;
        }
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCreate.getScene().getWindow();
        stage.close();
    }

    public void setOnUserCreated(Runnable callback) { this.onUserCreated = callback; }
}