package tn.esprit.boussole.gui.franchise;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.models.Franchise;
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
import java.util.ResourceBundle;

public class MesCommandesController implements Initializable {

    @FXML private TableView<Commande> commandeTable;
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, Double> colTotal;
    @FXML private TableColumn<Commande, String> colStatut;
    @FXML private TableColumn<Commande, Void> colActions;

    @FXML private Label totalCommandesLabel;
    @FXML private Label totalDepenseLabel;
    @FXML private ComboBox<String> filtreStatutCombo;

    private CommandeService commandeService;
    private LigneCommandeService ligneCommandeService;
    private ProduitService produitService;
    private FranchiseService franchiseService;

    private ObservableList<Commande> commandeList;
    private ObservableList<Commande> filteredList;
    private int franchiseId = 1; // À remplacer par l'ID de la franchise connectée

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private DateTimeFormatter dateCourteFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commandeService = new CommandeService();
        ligneCommandeService = new LigneCommandeService();
        produitService = new ProduitService();
        franchiseService = new FranchiseService();

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
        // Colonne DATE
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate_creation();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(dateFormatter) : ""
            );
        });
        colDate.setPrefWidth(160);
        colDate.setStyle("-fx-alignment: CENTER;");

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
        colTotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

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

                    // Appliquer la couleur selon le statut
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

        // Colonne ACTIONS (voir détails)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button detailsBtn = new Button("🔍 Détails");

            {
                detailsBtn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                detailsBtn.setPrefWidth(80);

                detailsBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    voirDetailsCommande(commande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(detailsBtn);
                }
            }
        });
        colActions.setPrefWidth(100);
        colActions.setStyle("-fx-alignment: CENTER;");
    }

    private void chargerCommandes() {
        try {
            // Récupérer les commandes de la franchise
            commandeList = FXCollections.observableArrayList(
                    commandeService.selectByFranchiseId(franchiseId)
            );

            filteredList = FXCollections.observableArrayList(commandeList);
            mettreAJourStatistiques();
            commandeTable.setItems(filteredList);

            System.out.println("✅ " + commandeList.size() + " commandes chargées pour la franchise " + franchiseId);

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
        int totalCommandes = filteredList.size();
        double totalDepense = 0;

        for (Commande c : filteredList) {
            totalDepense += c.getMontant_total();
        }

        totalCommandesLabel.setText(String.valueOf(totalCommandes));
        totalDepenseLabel.setText(String.format("%.2f DT", totalDepense));
    }

    private void voirDetailsCommande(Commande commande) {
        try {
            List<LigneCommande> lignes = ligneCommandeService.selectByCommandeId(commande.getId());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails de la commande #" + commande.getId());
            dialog.setHeaderText("Commande du " + commande.getDate_creation().format(dateCourteFormatter));

            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-background-color: #1E293B;");

            // Informations générales
            Label franchiseLabel = new Label("Franchise: " + getFranchiseName(commande.getFranchise_id()));
            franchiseLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

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

            Label dateLabel = new Label("Date: " + commande.getDate_creation().format(dateFormatter));
            dateLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");

            // Tableau des produits
            Label produitsTitle = new Label("Produits commandés:");
            produitsTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 5 0;");

            TableView<LigneCommande> produitsTable = new TableView<>();
            produitsTable.setPrefHeight(200);
            produitsTable.setStyle("-fx-background-color: #0B0F1A; -fx-border-color: #334155; -fx-table-cell-border-color: #334155;");

            // Colonne Produit
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

            // Colonne Quantité
            TableColumn<LigneCommande, Integer> colQuantite = new TableColumn<>("Quantité");
            colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
            colQuantite.setPrefWidth(80);
            colQuantite.setStyle("-fx-alignment: CENTER;");

            // Colonne Prix unitaire
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
                    }
                }
            });
            colPrix.setPrefWidth(100);
            colPrix.setStyle("-fx-alignment: CENTER-RIGHT;");

            // Colonne Total ligne
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
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            });
            colTotalLigne.setPrefWidth(100);

            produitsTable.getColumns().addAll(colProdNom, colQuantite, colPrix, colTotalLigne);
            produitsTable.setItems(FXCollections.observableArrayList(lignes));

            // Total général
            double totalGeneral = lignes.stream()
                    .mapToDouble(l -> l.getQuantite() * l.getPrix_unitaire())
                    .sum();

            Label totalGeneralLabel = new Label("Total général: " + String.format("%.2f DT", totalGeneral));
            totalGeneralLabel.setStyle("-fx-text-fill: #0EA5E9; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 0 0 0; -fx-alignment: CENTER-RIGHT;");

            content.getChildren().addAll(franchiseLabel, dateLabel, montantLabel, statutLabel, produitsTitle, produitsTable, totalGeneralLabel);

            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les détails: " + e.getMessage());
        }
    }

    private String getFranchiseName(int franchiseId) {
        try {
            Franchise f = franchiseService.getFranchiseById(franchiseId);
            return f != null ? f.getNom() : "Franchise #" + franchiseId;
        } catch (SQLException e) {
            return "Franchise #" + franchiseId;
        }
    }

    @FXML
    private void handleRefresh() {
        chargerCommandes();
    }

    @FXML
    private void handleVoirToutes() {
        filtreStatutCombo.setValue("Toutes");
        filtrerCommandes();
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}