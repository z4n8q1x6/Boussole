package tn.esprit.chargesdepenses.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailFournisseurService {

    // Identifiants de l'expéditeur
    private final String username = "waellpbt@gmail.com";
    private final String password = "fgcc qiiy qqdr lyxz"; // Ton code de 16 lettres généré par Google

    public void envoyerFicheFournisseur(String destinataire, String sujet, String corps) throws MessagingException {
        // Configuration du serveur SMTP de Google
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Création de la session authentifiée
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Construction du message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
        message.setSubject(sujet);
        message.setText(corps);

        // Envoi effectif
        Transport.send(message);
    }
}