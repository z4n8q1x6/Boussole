package tn.esprit.boussole.service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class ServiceEmail {

    // Configurer ici avec une adresse Gmail et un mot de passe d'application.
    // Exemple : xyz@gmail.com / "aaaa bbbb cccc dddd"
    private final String username = "siwar.raouafi1@gmail.com";
    private final String password = "dvqr ejql mppp qasa";

    public void envoyerEmailHTML(String destinataire, String sujet, String contenuHTML) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); // TLS

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        message.setSubject(sujet);

        // Envoyer le contenu en tant que HTML
        message.setContent(contenuHTML, "text/html; charset=utf-8");

        Transport.send(message);
    }
}
