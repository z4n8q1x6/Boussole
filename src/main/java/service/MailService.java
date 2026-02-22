package service;

import entity.Mensualite;
import entity.Pret;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class MailService {

    // Vos identifiants configurés
    private final String username = "entreprise834@gmail.com";
    private final String password = "vkqy iefq kjzl jcha";

    /**
     * Méthode privée pour centraliser la configuration SMTP et la session
     */
    private Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /**
     * Méthode appelée lors d'une décision du comité (Accepter/Refuser)
     */
    public void envoyerEmailStatut(Pret p, String message) {
        String emailDestinataire = "entreprise834@gmail.com"; // À remplacer par p.getUser().getEmail() en production

        try {
            Message msg = new MimeMessage(getSession());
            msg.setFrom(new InternetAddress(username, "Boussole Finance"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestinataire));
            msg.setSubject("Décision concernant votre demande de prêt - Boussole");

            String decision = p.getStatut().toString();
            String couleur = decision.equals("ACCORDE") ? "#27ae60" : "#e67e22";

            String contenu = "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; padding: 20px; border-radius: 10px; max-width: 600px;'>"
                    + "<h2 style='color: #2c3e50; text-align: center;'>BOUSSOLE - Notification de Décision</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Le comité a examiné votre demande pour : <b>" + p.getMotif() + "</b>.</p>"
                    + "<div style='background-color: #f9f9f9; padding: 20px; border-left: 6px solid " + couleur + "; margin: 20px 0;'>"
                    + "<span style='font-size: 1.2em;'>Statut : <b style='color:" + couleur + ";'>" + decision + "</b></span><br><br>"
                    + (decision.equals("ACCORDE") ?
                    "Montant : <b>" + String.format("%.2f", p.getMontantDemande()) + " DT</b><br>Durée : <b>" + p.getDureeMois() + " mois</b>"
                    : "Malheureusement, votre demande n'a pas été retenue pour le moment.")
                    + "</div>"
                    + "<p>Cordialement,<br><b>L'équipe Boussole</b></p>"
                    + "</div>";

            msg.setContent(contenu, "text/html; charset=utf-8");
            Transport.send(msg);
            System.out.println("Email de statut envoyé avec succès.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode appelée lors de l'enregistrement d'un paiement (STRUCTURE HTML DEMANDÉE)
     */
    public void envoyerEmailPaiement(Mensualite m, String motifPret) {
        String emailDestinataire = "entreprise834@gmail.com"; // Simulation destinataire

        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(username, "Boussole Recouvrement"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestinataire));
            message.setSubject("Confirmation de paiement - Mensualité " + motifPret);

            // --- STRUCTURE HTML FORMATÉE ---
            String htmlContent = "<div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; border: 1px solid #eee; padding: 20px;'>"
                    + "<h2 style='color: #27ae60;'>Confirmation de réception</h2>"
                    + "<p>Bonjour,</p>"
                    + "<p>Nous vous confirmons la réception du paiement pour votre mensualité.</p>"
                    + "<p><strong>Détails de la transaction :</strong></p>"
                    + "<ul style='list-style-type: none; padding-left: 0;'>"
                    + "  <li style='margin-bottom: 8px;'><strong>Projet :</strong> " + motifPret + "</li>"
                    + "  <li style='margin-bottom: 8px;'><strong>Date d'échéance :</strong> " + m.getDateEcheance() + "</li>"
                    + "  <li style='margin-bottom: 8px;'><strong>Montant réglé :</strong> <span style='color: #27ae60; font-weight: bold;'>" + String.format("%.2f", m.getMontant()) + " DT</span></li>"
                    + "</ul>"
                    + "<p style='margin-top: 20px;'>Votre situation a été mise à jour dans votre espace <strong>BOUSSOLE</strong>.</p>"
                    + "<p>Cordialement,<br>L'équipe BOUSSOLE.</p>"
                    + "</div>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("E-mail de confirmation de paiement envoyé.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode générique pour envoyer un mail en texte brut si nécessaire
     */
    public void sendEmailSimple(String to, String subject, String content) throws MessagingException {
        Message message = new MimeMessage(getSession());
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(content);

        Transport.send(message);
    }
}