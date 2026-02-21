package tn.esprit.boussole.gui;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.boussole.models.user;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.userService;
import org.mindrot.jbcrypt.BCrypt;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Random;

public class addUserController {

    @FXML private AnchorPane mainAnchorPane;
    @FXML private TextField txtNom, txtPrenom, txtEmail, txtTelephone, txtSolde, txtNomEntreprise, txtAdresseEntreprise;
    @FXML private CheckBox checkActif;
    @FXML private Button btnCreate;

    private Runnable onUserCreated;
    Dotenv dotenv = Dotenv.load();
    // CONFIGURATION GMAIL
    private final String MON_EMAIL = dotenv.get("EMAIL_USER");
    private final String MA_CLE_GOOGLE = dotenv.get("EMAIL_PASSWORD");

    @FXML
    public void initialize() {
        // Animation d'entrée
        mainAnchorPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), mainAnchorPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Validation numérique téléphone (8 chiffres)
        txtTelephone.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*") || newVal.length() > 8) {
                txtTelephone.setText(old);
            }
        });
    }

    @FXML
    private void handleCreate() {
        if (!validateFields()) return;

        try {
            userService service = new userService();

            // 1. GÉNÉRATION DU MOT DE PASSE EN CLAIR
            String motDePasseClair = genererMotDePasse(8);

            // 2. HACHAGE POUR LA BDD
            String motDePasseHache = BCrypt.hashpw(motDePasseClair, BCrypt.gensalt());

            // Objet User
            user u = new user();
            u.setNom(txtNom.getText().trim());
            u.setPrenom(txtPrenom.getText().trim());
            u.setEmail(txtEmail.getText().trim());
            u.setMotDePasse(motDePasseHache);
            u.setRole("ENTREPRISE");
            u.setActif(checkActif.isSelected());
            u.setDateCreation(LocalDateTime.now());

            // Objet Franchise
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

            // 3. SAUVEGARDE DB
            service.insertUserWithFranchise(u, f);

            // 4. ENVOI DE L'EMAIL EN ARRIÈRE-PLAN
            envoyerEmailBienvenue(u.getEmail(), u.getPrenom(), motDePasseClair);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Compte créé ! Un email avec les identifiants a été envoyé.");

            if (onUserCreated != null) onUserCreated.run();
            closeWindow();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Détails : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String genererMotDePasse(int longueur) {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < longueur; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    private void envoyerEmailBienvenue(String destinataire, String prenom, String mdp) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(MON_EMAIL, MA_CLE_GOOGLE);
            }
        });

        // Utilisation d'un Thread pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(MON_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
                message.setSubject("Bienvenue sur Boussole - Vos identifiants");

                String corps = "Bonjour " + prenom + ",\n\n"
                        + "Votre compte entreprise a été créé avec succès.\n"
                        + "Voici vos identifiants pour vous connecter :\n\n"
                        + "Email : " + destinataire + "\n"
                        + "Mot de passe : " + mdp + "\n\n"
                        + "L'équipe Boussole.";

                message.setText(corps);
                Transport.send(message);
                System.out.println("✅ Email envoyé avec succès à " + destinataire);
            } catch (MessagingException e) {
                System.err.println("❌ Erreur Email : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private boolean validateFields() {
        if (txtEmail.getText().isEmpty() || !txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Email invalide.");
            return false;
        }
        if (txtNomEntreprise.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Le nom de l'entreprise est obligatoire.");
            return false;
        }
        if (txtTelephone.getText().length() != 8) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Le téléphone doit contenir 8 chiffres.");
            return false;
        }
        return true;
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) btnCreate.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
            a.showAndWait();
        });
    }

    public void setOnUserCreated(Runnable callback) { this.onUserCreated = callback; }
}