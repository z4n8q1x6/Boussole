package tn.esprit.boussole.service;

// Les imports ont été changés de jakarta.* vers javax.*
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailFournisseurService {

    // Identifiants de l'expéditeur
    private final String username = "waellpbt@gmail.com";
    private final String password = "fgcc qiiy qqdr lyxz";

    public void envoyerFicheFournisseur(String destinataire, String sujet, String corps) throws MessagingException {
        // Configuration du serveur SMTP de Google
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Utilisation explicite de javax.mail.Authenticator pour éviter tout conflit
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Construction du message avec javax.mail
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        message.setSubject(sujet);
        message.setText(corps);

        // Envoi effectif via la bibliothèque JavaX
        Transport.send(message);
    }
}