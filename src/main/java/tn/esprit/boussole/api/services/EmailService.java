package tn.esprit.boussole.api.services;

import tn.esprit.boussole.api.clients.EmailClient;
import tn.esprit.boussole.api.models.EmailRequest;
import tn.esprit.boussole.api.models.EmailResponse;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.models.Franchise;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.services.LigneCommandeService;
import tn.esprit.boussole.services.ProduitService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service métier pour l'envoi d'emails liés aux commandes
 */
public class EmailService {

    private final EmailClient emailClient;
    private final LigneCommandeService ligneCommandeService;
    private final ProduitService produitService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Mode test (true = pas d'envoi réel)
    private boolean testMode = false;

    public EmailService() {
        this.emailClient = EmailClient.getInstance();
        this.ligneCommandeService = new LigneCommandeService();
        this.produitService = new ProduitService();
    }

    /**
     * Constructeur pour les tests
     */
    public EmailService(boolean testMode) {
        this.testMode = testMode;
        if (testMode) {
            this.emailClient = EmailClient.getTestInstance();
        } else {
            this.emailClient = EmailClient.getInstance();
        }
        this.ligneCommandeService = new LigneCommandeService();
        this.produitService = new ProduitService();
    }

    // ==================== EMAILS DE COMMANDE ====================

    /**
     * Envoyer un email de confirmation quand une commande est validée
     */
    public EmailResponse envoyerEmailValidationCommande(Commande commande, Franchise franchise) {
        String sujet = "✅ Votre commande #" + commande.getId() + " a été validée !";
        String contenu = genererContenuValidationCommande(commande, franchise);

        EmailRequest request = new EmailRequest(franchise.getEmail(), sujet, contenu);
        return emailClient.envoyerEmail(request);
    }

    /**
     * Envoyer un email quand une commande est refusée
     */
    public EmailResponse envoyerEmailRefusCommande(Commande commande, Franchise franchise, String raison) {
        String sujet = "❌ Votre commande #" + commande.getId() + " a été refusée";
        String contenu = genererContenuRefusCommande(commande, franchise, raison);

        EmailRequest request = new EmailRequest(franchise.getEmail(), sujet, contenu);
        return emailClient.envoyerEmail(request);
    }

    /**
     * Envoyer un email quand une commande est créée (en attente)
     */
    public EmailResponse envoyerEmailCommandeCreee(Commande commande, Franchise franchise) {
        String sujet = "🛒 Commande #" + commande.getId() + " créée avec succès";
        String contenu = genererContenuCommandeCreee(commande, franchise);

        EmailRequest request = new EmailRequest(franchise.getEmail(), sujet, contenu);
        return emailClient.envoyerEmail(request);
    }

    /**
     * Envoyer un email de notification de stock faible
     */
    public EmailResponse envoyerEmailStockFaible(String emailAdmin, List<ProduitStockFaible> produits) {
        String sujet = "⚠️ Alerte: Stock faible détecté";
        String contenu = genererContenuStockFaible(produits);

        EmailRequest request = new EmailRequest(emailAdmin, sujet, contenu);
        return emailClient.envoyerEmail(request);
    }

    // ==================== GÉNÉRATION DE CONTENU HTML ====================

    private String genererContenuValidationCommande(Commande commande, Franchise franchise) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }");
        html.append(".container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; }");
        html.append(".header { background-color: #10B981; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }");
        html.append(".content { padding: 30px; }");
        html.append(".commande-info { background-color: #f8fafc; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #10B981; }");
        html.append(".footer { text-align: center; color: #64748B; font-size: 12px; margin-top: 30px; }");
        html.append(".btn { background-color: #10B981; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; display: inline-block; }");
        html.append(".details { width: 100%; border-collapse: collapse; margin-top: 15px; }");
        html.append(".details th { background-color: #1E293B; color: white; padding: 10px; text-align: left; }");
        html.append(".details td { padding: 10px; border-bottom: 1px solid #e2e8f0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>✅ Commande validée</h1>");
        html.append("</div>");

        // Content
        html.append("<div class='content'>");
        html.append("<h2>Bonjour ").append(franchise.getNom()).append(",</h2>");
        html.append("<p>Votre commande <strong>#").append(commande.getId()).append("</strong> a été validée par le siège.</p>");

        // Informations commande
        html.append("<div class='commande-info'>");
        html.append("<p><strong>Date :</strong> ").append(commande.getDate_creation().format(dateFormatter)).append("</p>");
        html.append("<p><strong>Montant total :</strong> ").append(String.format("%.2f DT", commande.getMontant_total())).append("</p>");
        html.append("<p><strong>Statut :</strong> <span style='color: #10B981;'>VALIDÉE</span></p>");
        html.append("</div>");

        // Détails des produits
        try {
            List<LigneCommande> lignes = ligneCommandeService.selectByCommandeId(commande.getId());
            if (!lignes.isEmpty()) {
                html.append("<h3>Détails de la commande :</h3>");
                html.append("<table class='details'>");
                html.append("<tr><th>Produit</th><th>Quantité</th><th>Prix unitaire</th><th>Total</th></tr>");

                for (LigneCommande ligne : lignes) {
                    String nomProduit = produitService.selectById(ligne.getProduit_id()).getNom();
                    html.append("<tr>");
                    html.append("<td>").append(nomProduit).append("</td>");
                    html.append("<td>").append(ligne.getQuantite()).append("</td>");
                    html.append("<td>").append(String.format("%.2f DT", ligne.getPrix_unitaire())).append("</td>");
                    html.append("<td>").append(String.format("%.2f DT", ligne.getQuantite() * ligne.getPrix_unitaire())).append("</td>");
                    html.append("</tr>");
                }

                html.append("</table>");
            }
        } catch (SQLException e) {
            html.append("<p>Erreur lors du chargement des détails</p>");
        }

        html.append("<p style='text-align: center; margin-top: 30px;'>");
        html.append("<a href='http://localhost:8080/mes-commandes' class='btn'>Voir ma commande</a>");
        html.append("</p>");

        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Cet email a été envoyé automatiquement par Boussole.</p>");
        html.append("<p>© 2025 Boussole - Gestion Commerciale</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String genererContenuRefusCommande(Commande commande, Franchise franchise, String raison) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }" +
                ".container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; }" +
                ".header { background-color: #EF4444; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { padding: 30px; }" +
                ".commande-info { background-color: #f8fafc; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #EF4444; }" +
                ".footer { text-align: center; color: #64748B; font-size: 12px; margin-top: 30px; }" +
                ".raison { background-color: #FEE2E2; color: #EF4444; padding: 15px; border-radius: 5px; margin: 15px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>❌ Commande refusée</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Bonjour " + franchise.getNom() + ",</h2>" +
                "<p>Votre commande <strong>#" + commande.getId() + "</strong> a été refusée par le siège.</p>" +
                "<div class='raison'>" +
                "<p><strong>Raison du refus :</strong> " + raison + "</p>" +
                "</div>" +
                "<div class='commande-info'>" +
                "<p><strong>Date :</strong> " + commande.getDate_creation().format(dateFormatter) + "</p>" +
                "<p><strong>Montant total :</strong> " + String.format("%.2f DT", commande.getMontant_total()) + "</p>" +
                "<p><strong>Statut :</strong> <span style='color: #EF4444;'>REFUSÉE</span></p>" +
                "</div>" +
                "<p>Vous pouvez passer une nouvelle commande ou contacter le support pour plus d'informations.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Cet email a été envoyé automatiquement par Boussole.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String genererContenuCommandeCreee(Commande commande, Franchise franchise) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }" +
                ".container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; }" +
                ".header { background-color: #0EA5E9; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { padding: 30px; }" +
                ".commande-info { background-color: #f8fafc; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #0EA5E9; }" +
                ".footer { text-align: center; color: #64748B; font-size: 12px; margin-top: 30px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🛒 Commande créée</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Bonjour " + franchise.getNom() + ",</h2>" +
                "<p>Votre commande <strong>#" + commande.getId() + "</strong> a été créée avec succès.</p>" +
                "<div class='commande-info'>" +
                "<p><strong>Date :</strong> " + commande.getDate_creation().format(dateFormatter) + "</p>" +
                "<p><strong>Montant total :</strong> " + String.format("%.2f DT", commande.getMontant_total()) + "</p>" +
                "<p><strong>Statut :</strong> <span style='color: #F59E0B;'>EN ATTENTE</span></p>" +
                "</div>" +
                "<p>Vous serez notifié dès que votre commande sera validée par le siège.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Cet email a été envoyé automatiquement par Boussole.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String genererContenuStockFaible(List<ProduitStockFaible> produits) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }");
        html.append(".container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; }");
        html.append(".header { background-color: #F59E0B; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }");
        html.append(".content { padding: 30px; }");
        html.append(".produit { background-color: #f8fafc; padding: 15px; border-radius: 5px; margin: 10px 0; border-left: 4px solid #F59E0B; }");
        html.append(".footer { text-align: center; color: #64748B; font-size: 12px; margin-top: 30px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>⚠️ Alerte Stock Faible</h1>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<p>Les produits suivants ont un stock faible :</p>");

        for (ProduitStockFaible p : produits) {
            html.append("<div class='produit'>");
            html.append("<p><strong>").append(p.getNom()).append("</strong></p>");
            html.append("<p>Référence: ").append(p.getReference()).append("</p>");
            html.append("<p>Stock actuel: <strong style='color: #EF4444;'>").append(p.getStock()).append("</strong></p>");
            html.append("<p>Seuil d'alerte: ").append(p.getSeuil()).append("</p>");
            html.append("</div>");
        }

        html.append("<p style='margin-top: 20px;'>Pensez à réapprovisionner ces produits.</p>");
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>Email automatique - Boussole</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    // ==================== CLASSE INTERNE ====================

    /**
     * Classe pour représenter un produit avec stock faible
     */
    public static class ProduitStockFaible {
        private String nom;
        private String reference;
        private int stock;
        private int seuil;

        public ProduitStockFaible(String nom, String reference, int stock, int seuil) {
            this.nom = nom;
            this.reference = reference;
            this.stock = stock;
            this.seuil = seuil;
        }

        public String getNom() { return nom; }
        public String getReference() { return reference; }
        public int getStock() { return stock; }
        public int getSeuil() { return seuil; }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Activer/désactiver le mode test
     */
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * Tester la connexion SMTP
     */
    public boolean testerConnexion() {
        return emailClient.testerConnexion();
    }
}