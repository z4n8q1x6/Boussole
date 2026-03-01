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
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.CommandeService;
import tn.esprit.boussole.service.franchiseService;
import tn.esprit.boussole.service.LigneCommandeService;
import tn.esprit.boussole.service.ProduitService;
import tn.esprit.boussole.utils.EmailService;
import tn.esprit.boussole.utils.NotificationManager;
import tn.esprit.boussole.utils.UserManager;

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
    private franchiseService franchiseService;
    private LigneCommandeService ligneCommandeService;
    private ProduitService produitService;

    private ObservableList<Commande> commandeList;
    private ObservableList<Commande> filteredList;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Vérifier que l'utilisateur est bien SIEGE
        if (!UserManager.isCurrentUserSiege()) {
            NotificationManager.show(
                    commandeTable.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Accès refusé",
                    "Vous n'avez pas les permissions pour accéder à cette page."
            );
            return;
        }

        commandeService = new CommandeService();
        franchiseService = new franchiseService();
        ligneCommandeService = new LigneCommandeService();
        produitService = new ProduitService();

        // Initialiser le filtre
        filtreStatutCombo.getItems().addAll("Toutes", "EN_ATTENTE", "VALIDEE", "REFUSEE");
        filtreStatutCombo.setValue("Toutes");
        filtreStatutCombo.setOnAction(e -> filtrerCommandes());

        // Configurer les colonnes
        configurerTable();

        // Charger les commandes
        chargerCommandes();
    }

    private void configurerTable() {
        // Rendre le tableau non éditable (consultation seulement)
        commandeTable.setEditable(false);

        // Colonne FRANCHISE
        colFranchise.setCellValueFactory(cellData -> {
            try {
                int franchiseId = cellData.getValue().getFranchise_id();
                String nom = getFranchiseName(franchiseId);
                return new javafx.beans.property.SimpleStringProperty(nom);
            } catch (Exception e) {
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
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #00E5CC;");
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
                            setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-background-color: rgba(245,158,11,0.15); -fx-background-radius: 5; -fx-padding: 5; -fx-alignment: CENTER;");
                            break;
                        case "VALIDEE":
                            setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-background-color: rgba(16,185,129,0.15); -fx-background-radius: 5; -fx-padding: 5; -fx-alignment: CENTER;");
                            break;
                        case "REFUSEE":
                            setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-background-color: rgba(239,68,68,0.15); -fx-background-radius: 5; -fx-padding: 5; -fx-alignment: CENTER;");
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
                detailsBtn.setStyle("-fx-background-color: #0C0F1A; -fx-text-fill: #00E5CC; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 8; -fx-background-radius: 3; -fx-border-color: #00E5CC; -fx-border-radius: 3;");
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
                validerBtn.setStyle("-fx-background-color: #0C0F1A; -fx-text-fill: #10B981; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3; -fx-border-color: #10B981; -fx-border-radius: 3;");
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
                refuserBtn.setStyle("-fx-background-color: #0C0F1A; -fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3; -fx-border-color: #EF4444; -fx-border-radius: 3;");
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
            commandeList = FXCollections.observableArrayList(commandeService.selectAll(null));
            filteredList = FXCollections.observableArrayList(commandeList);
            commandeTable.setItems(filteredList);
            mettreAJourStatistiques();
            System.out.println("✅ " + commandeList.size() + " commandes chargées");
        } catch (SQLException e) {
            NotificationManager.show(
                    commandeTable.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur",
                    "Impossible de charger les commandes: " + e.getMessage()
            );
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

    private String getFranchiseName(int franchiseId) {
        try {
            franchise f = franchiseService.getById(franchiseId);
            return f != null ? f.getNom() : "Franchise #" + franchiseId;
        } catch (SQLException e) {
            return "Franchise #" + franchiseId;
        }
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
                NotificationManager.show(
                        commandeTable.getScene().getWindow(),
                        NotificationManager.Type.WARNING,
                        "Stock insuffisant",
                        "Impossible de valider la commande:\n" + messageErreur.toString()
                );
                return;
            }

            // Demander confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Valider la commande");
            confirm.setHeaderText("Confirmer la validation");
            confirm.setContentText("Voulez-vous vraiment valider cette commande ?\nLe stock sera déduit automatiquement.");

            DialogPane dialogPane = confirm.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #0C0F1A;");
            Label contentLabel = (Label) dialogPane.lookup(".content.label");
            if (contentLabel != null) {
                contentLabel.setStyle("-fx-text-fill: white;");
            }

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                // Déduire le stock
                for (LigneCommande ligne : lignes) {
                    Produit produit = produitService.selectById(ligne.getProduit_id());
                    produit.setStock_dispo(produit.getStock_dispo() - ligne.getQuantite());
                    produitService.updateone(produit);
                }

                // Mettre à jour le statut
                commande.setStatut("VALIDEE");
                commandeService.updateone(commande);

                // Envoyer email à la franchise
                franchise franchise = franchiseService.getById(commande.getFranchise_id());
                if (franchise != null && franchise.getEmail() != null && !franchise.getEmail().isEmpty()) {
                    envoyerEmailValidation(commande, franchise, lignes);
                }

                chargerCommandes();
                NotificationManager.show(
                        commandeTable.getScene().getWindow(),
                        NotificationManager.Type.SUCCESS,
                        "Succès",
                        "✅ Commande #" + commande.getId() + " validée avec succès !"
                );
            }

        } catch (SQLException e) {
            NotificationManager.show(
                    commandeTable.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur",
                    "Erreur lors de la validation: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private void envoyerEmailValidation(Commande commande, franchise franchise, List<LigneCommande> lignes) {
        String sujet = "✅ Commande #" + commande.getId() + " validée - Boussole";
        String titre = "Commande validée !";
        String corps = "Votre commande a été validée par le siège.";

        String detailsHTML = genererDetailsCommandeHTML(commande, lignes);

        EmailService.sendHtmlEmail(
                franchise.getEmail(),
                sujet,
                titre,
                corps + "<br><br>" + detailsHTML,
                ""
        );

        System.out.println("📧 Email de validation envoyé à " + franchise.getEmail());
    }

    private String genererDetailsCommandeHTML(Commande commande, List<LigneCommande> lignes) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='margin-top: 20px; padding: 15px; background-color: #f8fafc; border-radius: 8px;'>");
        html.append("<h3 style='color: #10B981; margin-bottom: 15px;'>Détails de la commande</h3>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<thead>");
        html.append("<tr style='background-color: #1E293B; color: white;'>");
        html.append("<th style='padding: 10px; text-align: left;'>Produit</th>");
        html.append("<th style='padding: 10px; text-align: center;'>Quantité</th>");
        html.append("<th style='padding: 10px; text-align: right;'>Prix unitaire</th>");
        html.append("<th style='padding: 10px; text-align: right;'>Total</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        double totalGeneral = 0;
        for (LigneCommande ligne : lignes) {
            try {
                Produit produit = produitService.selectById(ligne.getProduit_id());
                String nomProduit = (produit != null) ? produit.getNom() : "Produit #" + ligne.getProduit_id();
                double sousTotal = ligne.getQuantite() * ligne.getPrix_unitaire();
                totalGeneral += sousTotal;

                html.append("<tr style='border-bottom: 1px solid #e2e8f0;'>");
                html.append("<td style='padding: 10px;'>").append(nomProduit).append("</td>");
                html.append("<td style='padding: 10px; text-align: center;'>").append(ligne.getQuantite()).append("</td>");
                html.append("<td style='padding: 10px; text-align: right;'>").append(String.format("%.2f DT", ligne.getPrix_unitaire())).append("</td>");
                html.append("<td style='padding: 10px; text-align: right; font-weight: bold;'>").append(String.format("%.2f DT", sousTotal)).append("</td>");
                html.append("</tr>");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        html.append("</tbody>");
        html.append("<tfoot>");
        html.append("<tr style='background-color: #f1f5f9; font-weight: bold;'>");
        html.append("<td colspan='3' style='padding: 10px; text-align: right;'>Total général :</td>");
        html.append("<td style='padding: 10px; text-align: right; color: #10B981;'>").append(String.format("%.2f DT", totalGeneral)).append("</td>");
        html.append("</tr>");
        html.append("</tfoot>");
        html.append("</table>");
        html.append("</div>");

        return html.toString();
    }

    private void refuserCommande(Commande commande) {
        // Demander la raison du refus
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Refuser la commande");
        dialog.setHeaderText("Raison du refus");
        dialog.setContentText("Veuillez indiquer la raison du refus:");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #0C0F1A;");
        dialog.getEditor().setStyle("-fx-background-color: #1E293B; -fx-text-fill: white; -fx-border-color: #334155;");

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

            DialogPane confirmPane = confirm.getDialogPane();
            confirmPane.setStyle("-fx-background-color: #0C0F1A;");
            Label contentLabel = (Label) confirmPane.lookup(".content.label");
            if (contentLabel != null) {
                contentLabel.setStyle("-fx-text-fill: white;");
            }

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    commande.setStatut("REFUSEE");
                    commandeService.updateone(commande);

                    // Envoyer email de refus
                    franchise franchise = franchiseService.getById(commande.getFranchise_id());
                    if (franchise != null && franchise.getEmail() != null && !franchise.getEmail().isEmpty()) {
                        envoyerEmailRefus(commande, franchise, raison);
                    }

                    chargerCommandes();
                    NotificationManager.show(
                            commandeTable.getScene().getWindow(),
                            NotificationManager.Type.SUCCESS,
                            "Succès",
                            "❌ Commande #" + commande.getId() + " refusée."
                    );

                } catch (SQLException e) {
                    NotificationManager.show(
                            commandeTable.getScene().getWindow(),
                            NotificationManager.Type.ERROR,
                            "Erreur",
                            "Erreur lors du refus: " + e.getMessage()
                    );
                    e.printStackTrace();
                }
            }
        }
    }

    private void envoyerEmailRefus(Commande commande, franchise franchise, String raison) {
        String sujet = "❌ Commande #" + commande.getId() + " refusée - Boussole";
        String titre = "Commande refusée";
        String corps = "Votre commande a été refusée pour la raison suivante :<br><br>" +
                "<div style='background-color: #FEE2E2; color: #EF4444; padding: 15px; border-radius: 8px; font-weight: bold;'>" +
                raison + "</div>";

        EmailService.sendHtmlEmail(
                franchise.getEmail(),
                sujet,
                titre,
                corps,
                ""
        );

        System.out.println("📧 Email de refus envoyé à " + franchise.getEmail());
    }

    private void voirDetailsCommande(Commande commande) {
        try {
            List<LigneCommande> lignes = ligneCommandeService.selectByCommandeId(commande.getId());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails de la commande #" + commande.getId());

            String franchiseNom = getFranchiseName(commande.getFranchise_id());
            dialog.setHeaderText("Commande de " + franchiseNom + " - " + commande.getDate_creation().format(dateFormatter));

            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: #0C0F1A;");

            // Informations générales
            Label dateLabel = new Label("Date: " + commande.getDate_creation().format(dateFormatter));
            dateLabel.setStyle("-fx-text-fill: #E8EDF5; -fx-font-size: 14px;");

            Label montantLabel = new Label("Montant total: " + String.format("%.2f DT", commande.getMontant_total()));
            montantLabel.setStyle("-fx-text-fill: #00E5CC; -fx-font-size: 16px; -fx-font-weight: bold;");

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
                produitsTable.setStyle("-fx-background-color: transparent; -fx-control-inner-background: #0C0F1A; -fx-border-color: rgba(255,255,255,0.06);");

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
                colQuantite.setStyle("-fx-alignment: CENTER;");

                TableColumn<LigneCommande, Double> colPrix = new TableColumn<>("Prix unitaire");
                colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_unitaire"));
                colPrix.setCellFactory(col -> new TableCell<LigneCommande, Double>() {
                    @Override
                    protected void updateItem(Double prix, boolean empty) {
                        super.updateItem(prix, empty);
                        if (empty || prix == null) {
                            setText(null);
                        } else {
                            setText(String.format("%.2f DT", prix));
                            setStyle("-fx-text-fill: #00E5CC;");
                        }
                    }
                });
                colPrix.setPrefWidth(100);
                colPrix.setStyle("-fx-alignment: CENTER-RIGHT;");

                TableColumn<LigneCommande, Double> colTotalLigne = new TableColumn<>("Total");
                colTotalLigne.setCellValueFactory(cellData -> {
                    double total = cellData.getValue().getQuantite() * cellData.getValue().getPrix_unitaire();
                    return new javafx.beans.property.SimpleDoubleProperty(total).asObject();
                });
                colTotalLigne.setCellFactory(col -> new TableCell<LigneCommande, Double>() {
                    @Override
                    protected void updateItem(Double total, boolean empty) {
                        super.updateItem(total, empty);
                        if (empty || total == null) {
                            setText(null);
                        } else {
                            setText(String.format("%.2f DT", total));
                            setStyle("-fx-text-fill: #00E5CC; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                        }
                    }
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
            NotificationManager.show(
                    commandeTable.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur",
                    "Impossible de charger les détails: " + e.getMessage()
            );
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
}