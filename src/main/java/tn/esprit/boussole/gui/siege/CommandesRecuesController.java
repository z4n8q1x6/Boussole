package tn.esprit.boussole.gui.siege;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.boussole.api.clients.EmailClient;
import tn.esprit.boussole.api.models.EmailResponse;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.models.Franchise;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.services.CommandeService;
import tn.esprit.boussole.services.FranchiseService;
import tn.esprit.boussole.services.LigneCommandeService;
import tn.esprit.boussole.services.ProduitService;
import tn.esprit.boussole.utils.EmailConfig;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CommandesRecuesController implements Initializable {

    @FXML private TableView<Commande> commandeTable;
    @FXML private TableColumn<Commande, String> colFranchise;
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, Double> colTotal;
    @FXML private TableColumn<Commande, String> colStatut;
    @FXML private TableColumn<Commande, Void> colActions;

    @FXML private Label totalCommandesLabel;
    @FXML private Label totalAttenteLabel;
    @FXML private Label totalValideesLabel;
    @FXML private Label totalRefuseesLabel;
    @FXML private Label chiffreAffairesLabel;
    @FXML private ComboBox<String> filtreStatutCombo;

    private CommandeService commandeService;
    private FranchiseService franchiseService;
    private LigneCommandeService ligneCommandeService;
    private ProduitService produitService;
    private EmailClient emailClient;

    private ObservableList<Commande> commandeList;
    private ObservableList<Commande> filteredList;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ===== CONFIGURATION EMAIL =====
    // Mettre à false pour les envois réels, true pour les tests
    private static final boolean MODE_TEST = false;  // ← METTRE À FALSE POUR ENVOIS RÉELS

    // Vos identifiants Gmail (à remplacer)
    private static final String VOTRE_EMAIL = "azizjlassi235@gmail.com";      // ← REMPLACEZ ICI
    private static final String MOT_DE_PASSE_APP = "zckamzqldkxsicyg"; // ← REMPLACEZ ICI

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commandeService = new CommandeService();
        franchiseService = new FranchiseService();
        ligneCommandeService = new LigneCommandeService();
        produitService = new ProduitService();

        // Initialiser le client email
        initialiserEmail();

        // Initialiser le filtre
        filtreStatutCombo.getItems().addAll("Toutes", "EN_ATTENTE", "VALIDEE", "REFUSEE");
        filtreStatutCombo.setValue("Toutes");
        filtreStatutCombo.setOnAction(e -> filtrerCommandes());

        // Configurer les colonnes
        configurerTable();

        // Charger les commandes
        chargerCommandes();
    }

    /**
     * Initialisation du service email
     */
    private void initialiserEmail() {
        try {
            if (MODE_TEST) {
                // Mode test - pas d'envoi réel
                emailClient = EmailClient.getTestInstance();
                System.out.println("📧 Mode TEST email activé - Les emails ne sont pas réellement envoyés");
                return;
            }

            // Mode réel - avec Gmail
            System.out.println("📧 Configuration email réel en cours...");
            System.out.println("   Compte: " + VOTRE_EMAIL);

            // Créer la configuration Gmail
            EmailConfig config = EmailConfig.getCustomConfig(
                    "smtp.gmail.com",      // Serveur Gmail
                    "587",                   // Port TLS
                    VOTRE_EMAIL,             // Votre email
                    MOT_DE_PASSE_APP,        // Mot de passe d'application
                    false                    // Debug désactivé
            );

            // Créer le client email
            emailClient = EmailClient.getInstance(config);

            // Tester la connexion
            boolean connexionOK = emailClient.testerConnexion();

            if (connexionOK) {
                System.out.println("✅ Serveur SMTP Gmail connecté avec succès !");
                System.out.println("📧 Les emails seront envoyés réellement depuis: " + VOTRE_EMAIL);
            } else {
                System.out.println("❌ Échec de connexion SMTP - Vérifiez vos identifiants");
                System.out.println("   Passage en mode test...");
                emailClient = EmailClient.getTestInstance();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation email: " + e.getMessage());
            System.out.println("   Passage en mode test...");
            emailClient = EmailClient.getTestInstance();
        }
    }

    private void configurerTable() {
        // Rendre le tableau non éditable (consultation seulement)
        commandeTable.setEditable(false);

        // Colonne FRANCHISE
        colFranchise.setCellValueFactory(cellData -> {
            try {
                int franchiseId = cellData.getValue().getFranchise_id();
                Franchise franchise = franchiseService.getFranchiseById(franchiseId);
                String nom = (franchise != null) ? franchise.getNom() : "Inconnue";
                return new javafx.beans.property.SimpleStringProperty(nom);
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Inconnue");
            }
        });
        colFranchise.setPrefWidth(200);

        // Colonne DATE
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate_creation();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(dateFormatter) : ""
            );
        });
        colDate.setPrefWidth(160);

        // Colonne TOTAL
        colTotal.setCellValueFactory(new PropertyValueFactory<>("montant_total"));
        colTotal.setCellFactory(col -> new TableCell<Commande, Double>() {
            @Override
            protected void updateItem(Double montant, boolean empty) {
                super.updateItem(montant, empty);
                if (empty || montant == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DT", montant));
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
                }
            }
        });
        colTotal.setPrefWidth(120);

        // Colonne STATUT avec code couleur
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<Commande, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);

                    switch (statut) {
                        case "EN_ATTENTE":
                            setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-background-color: rgba(245, 158, 11, 0.1); -fx-background-radius: 5; -fx-padding: 5; -fx-alignment: CENTER;");
                            break;
                        case "VALIDEE":
                            setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 5; -fx-padding: 5; -fx-alignment: CENTER;");
                            break;
                        case "REFUSEE":
                            setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-background-color: rgba(239, 68, 68, 0.1); -fx-background-radius: 5; -fx-padding: 5; -fx-alignment: CENTER;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        colStatut.setPrefWidth(120);

        // Colonne ACTIONS (avec boutons)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button detailsBtn = new Button();
            private final Button validerBtn = new Button();
            private final Button refuserBtn = new Button();
            private final HBox pane = new HBox(5, detailsBtn, validerBtn, refuserBtn);

            {
                // Configurer le bouton Détails
                try {
                    ImageView detailsIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/search.png")));
                    detailsIcon.setFitHeight(16);
                    detailsIcon.setFitWidth(16);
                    detailsIcon.setPreserveRatio(true);
                    detailsBtn.setGraphic(detailsIcon);
                } catch (Exception e) {
                    detailsBtn.setText("🔍");
                }
                detailsBtn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 8; -fx-background-radius: 3;");
                detailsBtn.setTooltip(new Tooltip("Voir les détails"));

                // Configurer le bouton Valider
                try {
                    ImageView validerIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/validate.png")));
                    validerIcon.setFitHeight(16);
                    validerIcon.setFitWidth(16);
                    validerIcon.setPreserveRatio(true);
                    validerBtn.setGraphic(validerIcon);
                } catch (Exception e) {
                    validerBtn.setText("✓");
                }
                validerBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                validerBtn.setTooltip(new Tooltip("Valider la commande"));

                // Configurer le bouton Refuser
                try {
                    ImageView refuserIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/reject.png")));
                    refuserIcon.setFitHeight(16);
                    refuserIcon.setFitWidth(16);
                    refuserIcon.setPreserveRatio(true);
                    refuserBtn.setGraphic(refuserIcon);
                } catch (Exception e) {
                    refuserBtn.setText("✗");
                }
                refuserBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                refuserBtn.setTooltip(new Tooltip("Refuser la commande"));

                detailsBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    voirDetailsCommande(commande);
                });

                validerBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    validerCommande(commande);
                });

                refuserBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    refuserCommande(commande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Commande commande = getTableView().getItems().get(getIndex());
                    if ("EN_ATTENTE".equals(commande.getStatut())) {
                        setGraphic(pane);
                    } else {
                        // Pour les commandes déjà traitées, afficher seulement les détails
                        setGraphic(detailsBtn);
                    }
                }
            }
        });
        colActions.setPrefWidth(200);
    }

    private void chargerCommandes() {
        try {
            commandeList = FXCollections.observableArrayList(commandeService.selectAll());
            filteredList = FXCollections.observableArrayList(commandeList);
            commandeTable.setItems(filteredList);
            mettreAJourStatistiques();
            System.out.println("✅ " + commandeList.size() + " commandes chargées");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filtrerCommandes() {
        String filtre = filtreStatutCombo.getValue();

        if ("Toutes".equals(filtre)) {
            filteredList = FXCollections.observableArrayList(commandeList);
        } else {
            filteredList = FXCollections.observableArrayList();
            for (Commande c : commandeList) {
                if (filtre.equals(c.getStatut())) {
                    filteredList.add(c);
                }
            }
        }

        commandeTable.setItems(filteredList);
        mettreAJourStatistiques();
    }

    private void mettreAJourStatistiques() {
        int total = commandeList.size();
        int enAttente = 0;
        int validees = 0;
        int refusees = 0;
        double ca = 0;

        for (Commande c : commandeList) {
            switch (c.getStatut()) {
                case "EN_ATTENTE":
                    enAttente++;
                    break;
                case "VALIDEE":
                    validees++;
                    ca += c.getMontant_total();
                    break;
                case "REFUSEE":
                    refusees++;
                    break;
            }
        }

        totalCommandesLabel.setText(String.valueOf(total));
        totalAttenteLabel.setText(String.valueOf(enAttente));
        totalValideesLabel.setText(String.valueOf(validees));
        totalRefuseesLabel.setText(String.valueOf(refusees));
        chiffreAffairesLabel.setText(String.format("%.2f DT", ca));
    }

    private void validerCommande(Commande commande) {
        try {
            // Vérifier le stock pour chaque produit
            List<LigneCommande> lignes = ligneCommandeService.selectByCommandeId(commande.getId());
            boolean stockSuffisant = true;
            StringBuilder messageErreur = new StringBuilder();

            for (LigneCommande ligne : lignes) {
                Produit produit = produitService.selectById(ligne.getProduit_id());
                if (produit.getStock_dispo() < ligne.getQuantite()) {
                    stockSuffisant = false;
                    messageErreur.append("- ").append(produit.getNom())
                            .append(": stock insuffisant (")
                            .append(produit.getStock_dispo())
                            .append("/").append(ligne.getQuantite())
                            .append(")\n");
                }
            }

            if (!stockSuffisant) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Stock insuffisant");
                alert.setHeaderText("Impossible de valider la commande");
                alert.setContentText("Les produits suivants n'ont pas assez de stock:\n" + messageErreur.toString());

                styliserAlert(alert);
                alert.showAndWait();
                return;
            }

            // Demander confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Valider la commande");
            confirm.setHeaderText("Confirmer la validation");
            confirm.setContentText("Voulez-vous vraiment valider cette commande ?\nLe stock sera déduit automatiquement.");

            styliserAlert(confirm);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                // Déduire le stock
                for (LigneCommande ligne : lignes) {
                    Produit produit = produitService.selectById(ligne.getProduit_id());
                    produit.setStock_dispo(produit.getStock_dispo() - ligne.getQuantite());
                    produitService.updateOne(produit);
                }

                // Mettre à jour le statut
                commande.setStatut("VALIDEE");
                commandeService.updateOne(commande);

                // === ENVOI D'EMAIL À LA FRANCHISE ===
                Franchise franchise = franchiseService.getFranchiseById(commande.getFranchise_id());

                if (franchise != null && franchise.getEmail() != null && !franchise.getEmail().isEmpty()) {
                    envoyerEmailValidation(commande, franchise, lignes);
                } else {
                    System.out.println("⚠️ Franchise sans email - pas de notification envoyée");
                }

                chargerCommandes();
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "✅ Commande #" + commande.getId() + " validée avec succès !");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la validation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoyer un email de validation
     */
    private void envoyerEmailValidation(Commande commande, Franchise franchise, List<LigneCommande> lignes) {
        try {
            String sujet = "✅ Commande #" + commande.getId() + " validée - Boussole";
            String contenu = genererEmailValidation(commande, franchise, lignes);

            System.out.println("📧 Envoi d'email à " + franchise.getEmail() + "...");

            EmailResponse response = emailClient.envoyerEmailSimple(
                    franchise.getEmail(),
                    sujet,
                    contenu
            );

            if (response.isSucces()) {
                System.out.println("✅ Email envoyé avec succès à " + franchise.getEmail());

                if (!MODE_TEST) {
                    showAlert(Alert.AlertType.INFORMATION, "Email envoyé",
                            "Un email de confirmation a été envoyé à " + franchise.getEmail());
                }
            } else {
                System.err.println("❌ Échec envoi email: " + response.getMessage());

                if (!MODE_TEST) {
                    showAlert(Alert.AlertType.WARNING, "Attention",
                            "La commande a été validée mais l'email n'a pas pu être envoyé.\n" +
                                    "Erreur: " + response.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Exception lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    private String genererEmailValidation(Commande commande, Franchise franchise, List<LigneCommande> lignes) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }");
        html.append(".container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; }");
        html.append(".header { background-color: #10B981; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }");
        html.append(".content { padding: 30px; }");
        html.append(".commande-info { background-color: #f8fafc; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #10B981; }");
        html.append(".footer { text-align: center; color: #64748B; font-size: 12px; margin-top: 30px; }");
        html.append(".details { width: 100%; border-collapse: collapse; margin-top: 15px; }");
        html.append(".details th { background-color: #1E293B; color: white; padding: 10px; text-align: left; }");
        html.append(".details td { padding: 10px; border-bottom: 1px solid #e2e8f0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'><h1>✅ Commande validée</h1></div>");
        html.append("<div class='content'>");
        html.append("<h2>Bonjour ").append(franchise.getNom()).append(",</h2>");
        html.append("<p>Votre commande <strong>#").append(commande.getId()).append("</strong> a été validée par le siège.</p>");

        html.append("<div class='commande-info'>");
        html.append("<p><strong>Date :</strong> ").append(commande.getDate_creation().format(dateFormatter)).append("</p>");
        html.append("<p><strong>Montant total :</strong> ").append(String.format("%.2f DT", commande.getMontant_total())).append("</p>");
        html.append("<p><strong>Statut :</strong> <span style='color: #10B981;'>VALIDÉE</span></p>");
        html.append("</div>");

        if (!lignes.isEmpty()) {
            html.append("<h3>Détails de la commande :</h3>");
            html.append("<table class='details'>");
            html.append("<tr><th>Produit</th><th>Quantité</th><th>Prix unitaire</th><th>Total</th></tr>");

            double total = 0;
            for (LigneCommande ligne : lignes) {
                try {
                    Produit produit = produitService.selectById(ligne.getProduit_id());
                    String nomProduit = (produit != null) ? produit.getNom() : "Produit #" + ligne.getProduit_id();
                    double sousTotal = ligne.getQuantite() * ligne.getPrix_unitaire();
                    total += sousTotal;

                    html.append("<tr>");
                    html.append("<td>").append(nomProduit).append("</td>");
                    html.append("<td>").append(ligne.getQuantite()).append("</td>");
                    html.append("<td>").append(String.format("%.2f DT", ligne.getPrix_unitaire())).append("</td>");
                    html.append("<td>").append(String.format("%.2f DT", sousTotal)).append("</td>");
                    html.append("</tr>");
                } catch (SQLException e) {
                    html.append("<tr><td colspan='4'>Erreur chargement produit</td></tr>");
                }
            }

            html.append("<tr style='font-weight: bold; background-color: #f1f5f9;'><td colspan='3' style='text-align: right;'>Total général :</td><td>").append(String.format("%.2f DT", total)).append("</td></tr>");
            html.append("</table>");
        }

        html.append("<p style='text-align: center; margin-top: 30px;'>");
        html.append("<span style='background-color: #10B981; color: white; padding: 12px 25px; border-radius: 5px; display: inline-block;'>Merci pour votre confiance</span>");
        html.append("</p>");
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>Cet email a été envoyé automatiquement par Boussole.</p>");
        html.append("<p>© 2025 Boussole - Gestion Commerciale</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private void refuserCommande(Commande commande) {
        // Demander la raison du refus
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refuser la commande");
        dialog.setHeaderText("Raison du refus");
        dialog.setContentText("Veuillez indiquer la raison du refus:");

        styliserDialog(dialog);

        Optional<String> raisonResult = dialog.showAndWait();

        if (raisonResult.isPresent()) {
            String raison = raisonResult.get();
            if (raison.isEmpty()) {
                raison = "Non spécifiée";
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmer le refus");
            confirm.setHeaderText("Refuser la commande #" + commande.getId());
            confirm.setContentText("Voulez-vous vraiment refuser cette commande ?\nRaison: " + raison);

            styliserAlert(confirm);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    commande.setStatut("REFUSEE");
                    commandeService.updateOne(commande);

                    // === ENVOI D'EMAIL DE REFUS ===
                    Franchise franchise = franchiseService.getFranchiseById(commande.getFranchise_id());

                    if (franchise != null && franchise.getEmail() != null && !franchise.getEmail().isEmpty()) {
                        envoyerEmailRefus(commande, franchise, raison);
                    }

                    chargerCommandes();
                    showAlert(Alert.AlertType.INFORMATION, "Succès",
                            "❌ Commande #" + commande.getId() + " refusée.");

                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du refus: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Envoyer un email de refus
     */
    private void envoyerEmailRefus(Commande commande, Franchise franchise, String raison) {
        try {
            String sujet = "❌ Commande #" + commande.getId() + " refusée - Boussole";
            String contenu = genererEmailRefus(commande, franchise, raison);

            System.out.println("📧 Envoi d'email de refus à " + franchise.getEmail() + "...");

            EmailResponse response = emailClient.envoyerEmailSimple(
                    franchise.getEmail(),
                    sujet,
                    contenu
            );

            if (response.isSucces()) {
                System.out.println("✅ Email de refus envoyé à " + franchise.getEmail());
            } else {
                System.err.println("❌ Échec envoi email de refus: " + response.getMessage());
            }

        } catch (Exception e) {
            System.err.println("❌ Exception lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    private String genererEmailRefus(Commande commande, Franchise franchise, String raison) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }" +
                ".container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; }" +
                ".header { background-color: #EF4444; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { padding: 30px; }" +
                ".commande-info { background-color: #f8fafc; padding: 20px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #EF4444; }" +
                ".raison { background-color: #FEE2E2; color: #EF4444; padding: 15px; border-radius: 5px; margin: 15px 0; font-weight: bold; }" +
                ".footer { text-align: center; color: #64748B; font-size: 12px; margin-top: 30px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'><h1>❌ Commande refusée</h1></div>" +
                "<div class='content'>" +
                "<h2>Bonjour " + franchise.getNom() + ",</h2>" +
                "<p>Votre commande <strong>#" + commande.getId() + "</strong> a été refusée par le siège.</p>" +
                "<div class='raison'>Raison : " + raison + "</div>" +
                "<div class='commande-info'>" +
                "<p><strong>Date :</strong> " + commande.getDate_creation().format(dateFormatter) + "</p>" +
                "<p><strong>Montant total :</strong> " + String.format("%.2f DT", commande.getMontant_total()) + "</p>" +
                "<p><strong>Statut :</strong> <span style='color: #EF4444;'>REFUSÉE</span></p>" +
                "</div>" +
                "<p>Vous pouvez passer une nouvelle commande ou contacter le support pour plus d'informations.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Email automatique - Boussole</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void voirDetailsCommande(Commande commande) {
        try {
            List<LigneCommande> lignes = ligneCommandeService.selectByCommandeId(commande.getId());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails de la commande #" + commande.getId());

            Franchise franchise = franchiseService.getFranchiseById(commande.getFranchise_id());
            String franchiseNom = (franchise != null) ? franchise.getNom() : "Inconnue";
            dialog.setHeaderText("Commande de " + franchiseNom + " - " + commande.getDate_creation().format(dateFormatter));

            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: #1E293B;");

            // Informations générales
            Label dateLabel = new Label("Date: " + commande.getDate_creation().format(dateFormatter));
            dateLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            Label montantLabel = new Label("Montant total: " + String.format("%.2f DT", commande.getMontant_total()));
            montantLabel.setStyle("-fx-text-fill: #0EA5E9; -fx-font-size: 16px; -fx-font-weight: bold;");

            Label statutLabel = new Label("Statut: " + commande.getStatut());
            switch (commande.getStatut()) {
                case "EN_ATTENTE":
                    statutLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 14px;");
                    break;
                case "VALIDEE":
                    statutLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 14px;");
                    break;
                case "REFUSEE":
                    statutLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");
                    break;
            }

            content.getChildren().addAll(dateLabel, montantLabel, statutLabel, new Label("Produits:"));

            if (!lignes.isEmpty()) {
                TableView<LigneCommande> produitsTable = new TableView<>();
                produitsTable.setPrefHeight(200);
                produitsTable.setStyle("-fx-background-color: #0B0F1A; -fx-border-color: #334155;");

                TableColumn<LigneCommande, String> colProdNom = new TableColumn<>("Produit");
                colProdNom.setCellValueFactory(cellData -> {
                    try {
                        int produitId = cellData.getValue().getProduit_id();
                        Produit p = produitService.selectById(produitId);
                        return new javafx.beans.property.SimpleStringProperty(p != null ? p.getNom() : "N/A");
                    } catch (SQLException e) {
                        return new javafx.beans.property.SimpleStringProperty("N/A");
                    }
                });
                colProdNom.setPrefWidth(200);

                TableColumn<LigneCommande, Integer> colQuantite = new TableColumn<>("Quantité");
                colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
                colQuantite.setPrefWidth(80);

                TableColumn<LigneCommande, Double> colPrix = new TableColumn<>("Prix unitaire");
                colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_unitaire"));
                colPrix.setPrefWidth(100);

                TableColumn<LigneCommande, Double> colTotalLigne = new TableColumn<>("Total");
                colTotalLigne.setCellValueFactory(cellData -> {
                    double total = cellData.getValue().getQuantite() * cellData.getValue().getPrix_unitaire();
                    return new javafx.beans.property.SimpleDoubleProperty(total).asObject();
                });
                colTotalLigne.setPrefWidth(100);

                produitsTable.getColumns().addAll(colProdNom, colQuantite, colPrix, colTotalLigne);
                produitsTable.setItems(FXCollections.observableArrayList(lignes));

                content.getChildren().add(produitsTable);
            }

            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les détails: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        chargerCommandes();
    }

    @FXML
    private void handleEnAttente() {
        filtreStatutCombo.setValue("EN_ATTENTE");
        filtrerCommandes();
    }

    @FXML
    private void handleValidees() {
        filtreStatutCombo.setValue("VALIDEE");
        filtrerCommandes();
    }

    @FXML
    private void handleRefusees() {
        filtreStatutCombo.setValue("REFUSEE");
        filtrerCommandes();
    }

    @FXML
    private void handleToutes() {
        filtreStatutCombo.setValue("Toutes");
        filtrerCommandes();
    }

    private void styliserAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1E293B;");
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white;");
        }
    }

    private void styliserDialog(TextInputDialog dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1E293B;");
        dialogPane.lookup(".content.label").setStyle("-fx-text-fill: white;");
        dialog.getEditor().setStyle("-fx-background-color: #0B0F1A; -fx-text-fill: white; -fx-border-color: #334155;");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styliserAlert(alert);
        alert.showAndWait();
    }
}