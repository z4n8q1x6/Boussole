package tn.esprit.boussole.api.clients;

import tn.esprit.boussole.api.models.EmailRequest;
import tn.esprit.boussole.api.models.EmailResponse;
import io.github.cdimascio.dotenv.Dotenv;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailClient {

    private static EmailClient instance;
    private final Properties properties;
    private final String username;
    private final String password;
    private final boolean debug;

    private EmailClient() {
        // Load from .env file (Project 2 style)
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.username = dotenv.get("EMAIL_USER");
        this.password = dotenv.get("EMAIL_PASSWORD");
        if (this.username == null || this.username.isEmpty() || this.password == null || this.password.isEmpty()) {
            System.err.println("⚠️ EmailClient: EMAIL_USER ou EMAIL_PASSWORD introuvable dans .env. Le service email ne fonctionnera pas correctement.");
        }
        this.debug = false;

        this.properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "5000");
        properties.put("mail.smtp.writetimeout", "5000");
        properties.put("mail.debug", String.valueOf(debug));
    }

    public static synchronized EmailClient getInstance() {
        if (instance == null) {
            instance = new EmailClient();
        }
        return instance;
    }

    public EmailResponse envoyerEmail(EmailRequest request) {
        EmailResponse response = new EmailResponse();

        try {
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getDestinataire()));
            message.setSubject(request.getSujet());
            message.setContent(request.getContenuHTML(), "text/html; charset=utf-8");

            Transport.send(message);

            response.setSucces(true);
            response.setMessage("Email envoyé avec succès à " + request.getDestinataire());
            response.setDestinataire(request.getDestinataire());

            System.out.println("✅ Email envoyé à " + request.getDestinataire());

        } catch (MessagingException e) {
            response.setSucces(false);
            response.setMessage("Erreur lors de l'envoi: " + e.getMessage());
            response.setErreur(e.getMessage());

            System.err.println("❌ Erreur envoi email: " + e.getMessage());
        }

        return response;
    }

    public EmailResponse envoyerEmailSimple(String destinataire, String sujet, String contenuHTML) {
        EmailRequest request = new EmailRequest(destinataire, sujet, contenuHTML);
        return envoyerEmail(request);
    }

    public boolean testerConnexion() {
        try {
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();

            System.out.println("✅ Connexion SMTP réussie");
            return true;

        } catch (MessagingException e) {
            System.err.println("❌ Échec connexion SMTP: " + e.getMessage());
            return false;
        }
    }

    public static EmailClient getTestInstance() {
        return new EmailClient() {
            @Override
            public EmailResponse envoyerEmail(EmailRequest request) {
                System.out.println("📧 [TEST MODE] Email envoyé à " + request.getDestinataire());
                System.out.println("   Sujet: " + request.getSujet());
                System.out.println("   Contenu: " + request.getContenuHTML().substring(0, Math.min(100, request.getContenuHTML().length())) + "...");

                EmailResponse response = new EmailResponse();
                response.setSucces(true);
                response.setMessage("[TEST] Email simulé avec succès");
                response.setDestinataire(request.getDestinataire());
                return response;
            }

            @Override
            public boolean testerConnexion() {
                System.out.println("📧 [TEST MODE] Test de connexion simulé");
                return true;
            }
        };
    }
}