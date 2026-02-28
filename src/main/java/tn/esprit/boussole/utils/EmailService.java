package tn.esprit.boussole.utils;

import io.github.cdimascio.dotenv.Dotenv;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String USERNAME = dotenv.get("EMAIL_USER");
    private static final String PASSWORD = dotenv.get("EMAIL_PASSWORD");

    public static void sendHtmlEmail(String recipient, String subject, String title, String bodyContent, String codeOrPassword) {
        // Vérifier si les identifiants ont été chargés
        if (USERNAME == null || PASSWORD == null || USERNAME.isEmpty() || PASSWORD.isEmpty()) {
            System.err.println("Erreur: Les variables d'environnement EMAIL_USER et/ou EMAIL_PASSWORD ne sont pas définies dans le fichier .env.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("support@boussole.tn", "Boussole Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);

            // Template HTML Moderne
            String htmlContent = buildHtmlTemplate(title, bodyContent, codeOrPassword);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Email HTML envoyé avec succès à " + recipient);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
        }
    }

    private static String buildHtmlTemplate(String title, String body, String highlightBox) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #F3F4F6; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%); padding: 30px; text-align: center; }" +
                ".logo { font-size: 40px; margin-bottom: 10px; }" +
                ".brand { color: #ffffff; font-size: 24px; font-weight: bold; letter-spacing: 1px; }" +
                ".brand span { color: #00E5CC; }" +
                ".content { padding: 40px 30px; color: #334155; line-height: 1.6; }" +
                ".title { font-size: 22px; font-weight: bold; color: #0F172A; margin-bottom: 20px; }" +
                ".highlight-box { background-color: #F0FDFA; border: 1px solid #CCFBF1; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                ".code { font-family: 'Courier New', monospace; font-size: 24px; font-weight: bold; color: #0F766E; letter-spacing: 2px; }" +
                ".footer { background-color: #F8FAFC; padding: 20px; text-align: center; font-size: 12px; color: #94A3B8; border-top: 1px solid #E2E8F0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "  <div class='header'>" +
                "    <div class='logo'>🧭</div>" +
                "    <div class='brand'>Boussole<span>APP</span></div>" +
                "  </div>" +
                "  <div class='content'>" +
                "    <div class='title'>" + title + "</div>" +
                "    <p>" + body + "</p>" +
                "    <div class='highlight-box'>" +
                "      <div class='code'>" + highlightBox + "</div>" +
                "    </div>" +
                "    <p>Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.</p>" +
                "  </div>" +
                "  <div class='footer'>" +
                "    &copy; 2024 Boussole Management. Tous droits réservés.<br>" +
                "    Ceci est un message automatique, merci de ne pas y répondre." +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}