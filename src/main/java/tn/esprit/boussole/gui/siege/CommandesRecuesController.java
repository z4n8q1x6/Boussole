package tn.esprit.boussole.gui.siege;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.services.CommandeService;
import tn.esprit.boussole.services.FranchiseService;
import tn.esprit.boussole.services.LigneCommandeService;
import tn.esprit.boussole.services.ProduitService;

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

    private ObservableList<Commande> commandeList;
    private ObservableList<Commande> filteredList;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commandeService = new CommandeService();
        franchiseService = new FranchiseService();
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
        // Colonne FRANCHISE
        colFranchise.setCellValueFactory(cellData -> {
            try {
                int franchiseId = cellData.getValue().getFranchise_id();
                String nom = franchiseService.getFranchiseById(franchiseId).getNom();
                return new javafx.beans.property.SimpleStringProperty(nom);
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleStringProperty("Inconnue");
            }
        });
        colFranchise.setPrefWidth(180);

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
                            setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-background-color: rgba(245, 158, 11, 0.1); -fx-background-radius: 5; -fx-padding: 5;");
                            break;
                        case "VALIDEE":
                            setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 5; -fx-padding: 5;");
                            break;
                        case "REFUSEE":
                            setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-background-color: rgba(239, 68, 68, 0.1); -fx-background-radius: 5; -fx-padding: 5;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        colStatut.setPrefWidth(120);

        // Colonne ACTIONS (Valider/Refuser)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button validerBtn = new Button("✓ Valider");
            private final Button refuserBtn = new Button("✗ Refuser");
            private final Button detailsBtn = new Button("🔍");
            private final HBox pane = new HBox(5, detailsBtn, validerBtn, refuserBtn);

            {
                validerBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                refuserBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                detailsBtn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 8; -fx-background-radius: 3;");

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
                alert.showAndWait();
                return;
            }

            // Demander confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Valider la commande");
            confirm.setHeaderText("Confirmer la validation");
            confirm.setContentText("Voulez-vous vraiment valider cette commande ?\nLe stock sera déduit automatiquement.");

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

                chargerCommandes();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande validée avec succès !");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la validation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refuserCommande(Commande commande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Refuser la commande");
        confirm.setHeaderText("Confirmer le refus");
        confirm.setContentText("Voulez-vous vraiment refuser cette commande ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commande.setStatut("REFUSEE");
                commandeService.updateOne(commande);
                chargerCommandes();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande refusée.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du refus: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void voirDetailsCommande(Commande commande) {
        try {
            List<LigneCommande> lignes = ligneCommandeService.selectByCommandeId(commande.getId());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails de la commande");

            // Titre avec infos franchise
            String franchiseNom = franchiseService.getFranchiseById(commande.getFranchise_id()).getNom();
            dialog.setHeaderText("Commande #" + commande.getId() + " - " + franchiseNom);

            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));

            // Informations générales
            Label dateLabel = new Label("Date: " + commande.getDate_creation().format(dateFormatter));
            dateLabel.setStyle("-fx-font-weight: bold;");

            Label statutLabel = new Label("Statut: " + commande.getStatut());
            switch (commande.getStatut()) {
                case "EN_ATTENTE":
                    statutLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                    break;
                case "VALIDEE":
                    statutLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    break;
                case "REFUSEE":
                    statutLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    break;
            }

            Label totalLabel = new Label("Total: " + String.format("%.2f DT", commande.getMontant_total()));
            totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0EA5E9;");

            // Tableau des produits
            TableView<LigneCommande> produitsTable = new TableView<>();
            produitsTable.setPrefHeight(200);

            TableColumn<LigneCommande, String> colProdNom = new TableColumn<>("Produit");
            colProdNom.setCellValueFactory(cellData -> {
                try {
                    int produitId = cellData.getValue().getProduit_id();
                    Produit p = produitService.selectById(produitId);
                    return new javafx.beans.property.SimpleStringProperty(p.getNom());
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
            colPrix.setPrefWidth(120);

            TableColumn<LigneCommande, Double> colTotalLigne = new TableColumn<>("Total");
            colTotalLigne.setCellValueFactory(cellData -> {
                double total = cellData.getValue().getQuantite() * cellData.getValue().getPrix_unitaire();
                return new javafx.beans.property.SimpleDoubleProperty(total).asObject();
            });
            colTotalLigne.setPrefWidth(120);

            produitsTable.getColumns().addAll(colProdNom, colQuantite, colPrix, colTotalLigne);
            produitsTable.setItems(FXCollections.observableArrayList(lignes));

            content.getChildren().addAll(dateLabel, statutLabel, totalLabel, new Label("Produits:"), produitsTable);

            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}