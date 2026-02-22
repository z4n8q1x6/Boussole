package service;

import entity.Pret;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class MailService {

    private final String username = "entreprise834@gmail.com";
    private final String password = "vkqy iefq kjzl jcha";

    /**
     * Méthode appelée par le contrôleur lors d'une décision (Accepter/Refuser)
     */
    public void envoyerEmailStatut(Pret p, String message) {
        System.out.println("Préparation de l'envoi d'email pour le prêt : " + p.getMotif());

        // Simulation de l'adresse du destinataire
        // (À remplacer par p.getUser().getEmail() si votre entité Pret est liée à un utilisateur)
        String emailDestinataire = "entreprise834@gmail.com";

        // On réutilise votre logique de notification HTML
        envoyerNotification(
                emailDestinataire,
                p.getMotif(),
                p.getStatut().toString(), // "ACCORDE" ou "REFUSE"
                p.getMontantDemande(),
                p.getDureeMois()
        );
    }

    /**
     * Méthode appelée lors de l'enregistrement d'un paiement (Reçu)
     */
    public void envoyerEmailPaiement(entity.Mensualite m, String motifPret) {
        String emailDestinataire = "entreprise834@gmail.com";
        String sujet = "Confirmation de paiement - Boussole";
        String contenu = "Bonjour, nous confirmons la réception de votre paiement de "
                + String.format("%.2f", m.getMontant()) + " DT pour le prêt : " + motifPret;

        try {
            sendEmail(emailDestinataire, sujet, contenu);
            System.out.println("Reçu de paiement envoyé.");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode générique pour envoyer un mail simple
     */
    public void sendEmail(String to, String subject, String content) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(content);

        Transport.send(message);
    }

    /**
     * Envoi de la notification formatée en HTML (Votre code original)
     */
    public void envoyerNotification(String destinataire, String motif, String decision, double montant, int duree) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("Décision concernant votre demande de prêt - Boussole");

            // Choix de la couleur : Vert pour Accordé, Orange/Rouge pour Refusé
            String couleur = decision.equals("ACCORDE") ? "#27ae60" : "#e67e22";

            String contenu = "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; padding: 20px; border-radius: 10px; max-width: 600px;'>"
                    + "<h2 style='color: #2c3e50; text-align: center;'>BOUSSOLE - Notification de Décision</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Le comité a examiné votre demande pour : <b>" + motif + "</b>.</p>"
                    + "<div style='background-color: #f9f9f9; padding: 20px; border-left: 6px solid " + couleur + "; margin: 20px 0;'>"
                    + "<span style='font-size: 1.2em;'>Statut : <b style='color:" + couleur + ";'>" + decision + "</b></span><br><br>"
                    + (decision.equals("ACCORDE") ?
                    "Montant : <b>" + String.format("%.2f", montant) + " DT</b><br>Durée : <b>" + duree + " mois</b>"
                    : "Malheureusement, votre demande n'a pas été retenue pour le moment.")
                    + "</div>"
                    + "<p>Cordialement,<br><b>L'équipe Boussole</b></p>"
                    + "</div>";

            message.setContent(contenu, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("Email de statut envoyé à : " + destinataire);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}