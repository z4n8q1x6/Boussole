package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.services.LigneCommandeService;
import tn.esprit.boussole.services.ProduitService;
import tn.esprit.boussole.services.CommandeService;

import java.sql.SQLException;
import java.util.Optional;
import java.util.List;

public class LigneCommandeGUI {

    private LigneCommandeService ligneCommandeService;
    private ProduitService produitService;
    private CommandeService commandeService;
    private TableView<LigneCommande> table;
    private ObservableList<LigneCommande> ligneCommandeList;

    // Champs du formulaire
    private ComboBox<Produit> produitCombo;
    private ComboBox<Commande> commandeCombo;
    private TextField quantiteField;
    private TextField prixUnitaireField;

    // Listes pour les combos
    private ObservableList<Produit> produitList;
    private ObservableList<Commande> commandeList;

    public LigneCommandeGUI() {
        ligneCommandeService = new LigneCommandeService();
        produitService = new ProduitService();
        commandeService = new CommandeService();
    }

    public void startInPane(Pane pane) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(0));
        content.setStyle("-fx-background-color: #f5f5f5;");

        // Charger les données pour les combos
        chargerProduits();
        chargerCommandes();

        // Barre d'outils
        content.getChildren().add(createToolbar());

        // Tableau des lignes de commande
        table = new TableView<>();
        configurerTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Formulaire
        VBox formPanel = createFormPanel();

        content.getChildren().addAll(table, formPanel);
        pane.getChildren().add(content);

        // Charger les données
        chargerDonnees();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 0, 10, 0));

        Label title = new Label("📝 Gestion des Lignes de Commande");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Rafraîchir");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        refreshBtn.setOnAction(e -> chargerDonnees());

        toolbar.getChildren().addAll(title, spacer, refreshBtn);
        return toolbar;
    }

    private void configurerTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        // Colonnes
        TableColumn<LigneCommande, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<LigneCommande, Integer> colQuantite = new TableColumn<>("Quantité");
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colQuantite.setPrefWidth(80);

        TableColumn<LigneCommande, Double> colPrix = new TableColumn<>("Prix unitaire");
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_unitaire"));
        colPrix.setPrefWidth(100);

        TableColumn<LigneCommande, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(cellData -> {
            double total = cellData.getValue().getQuantite() * cellData.getValue().getPrix_unitaire();
            return new javafx.beans.property.SimpleDoubleProperty(total).asObject();
        });
        colTotal.setPrefWidth(100);

        TableColumn<LigneCommande, Integer> colCommande = new TableColumn<>("Commande ID");
        colCommande.setCellValueFactory(new PropertyValueFactory<>("commande_id"));
        colCommande.setPrefWidth(100);

        TableColumn<LigneCommande, String> colProduit = new TableColumn<>("Produit");
        colProduit.setCellValueFactory(cellData -> {
            try {
                int produitId = cellData.getValue().getProduit_id();
                for (Produit p : produitList) {
                    if (p.getId() == produitId) {
                        return new javafx.beans.property.SimpleStringProperty(p.getNom());
                    }
                }
            } catch (Exception e) {
                // Ignorer
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        colProduit.setPrefWidth(150);

        // Colonne Actions
        TableColumn<LigneCommande, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(150);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 3;");

                editBtn.setOnAction(event -> {
                    LigneCommande ligne = getTableView().getItems().get(getIndex());
                    remplirFormulaire(ligne);
                });

                deleteBtn.setOnAction(event -> {
                    LigneCommande ligne = getTableView().getItems().get(getIndex());
                    supprimerLigneCommande(ligne);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(colId, colQuantite, colPrix, colTotal, colCommande, colProduit, colActions);

        // Sélection dans la table
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        remplirFormulaire(newSelection);
                    }
                }
        );
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0; -fx-padding: 15;");

        Label formTitle = new Label("AJOUTER / MODIFIER UNE LIGNE DE COMMANDE");
        formTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);

        // Labels et champs
        Label commandeLabel = new Label("Commande:");
        commandeLabel.setStyle("-fx-font-weight: bold;");
        commandeCombo = new ComboBox<>();
        commandeCombo.setItems(commandeList);
        commandeCombo.setPromptText("Sélectionner une commande");
        commandeCombo.setPrefWidth(250);
        commandeCombo.setCellFactory(param -> new ListCell<Commande>() {
            @Override
            protected void updateItem(Commande item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Commande #" + item.getId() + " - " + item.getStatut());
                }
            }
        });
        commandeCombo.setButtonCell(new ListCell<Commande>() {
            @Override
            protected void updateItem(Commande item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Commande #" + item.getId() + " - " + item.getStatut());
                }
            }
        });

        Label produitLabel = new Label("Produit:");
        produitLabel.setStyle("-fx-font-weight: bold;");
        produitCombo = new ComboBox<>();
        produitCombo.setItems(produitList);
        produitCombo.setPromptText("Sélectionner un produit");
        produitCombo.setPrefWidth(250);
        produitCombo.setCellFactory(param -> new ListCell<Produit>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNom() + " - " + item.getPrix_achat() + " DT");
                }
            }
        });
        produitCombo.setButtonCell(new ListCell<Produit>() {
            @Override
            protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNom() + " - " + item.getPrix_achat() + " DT");
                }
            }
        });

        // Quand on sélectionne un produit, le prix unitaire est automatiquement rempli
        produitCombo.setOnAction(e -> {
            Produit selected = produitCombo.getValue();
            if (selected != null) {
                prixUnitaireField.setText(String.valueOf(selected.getPrix_achat()));
            }
        });

        Label quantiteLabel = new Label("Quantité:");
        quantiteLabel.setStyle("-fx-font-weight: bold;");
        quantiteField = new TextField();
        quantiteField.setPromptText("Quantité");
        quantiteField.setPrefWidth(150);

        Label prixLabel = new Label("Prix unitaire:");
        prixLabel.setStyle("-fx-font-weight: bold;");
        prixUnitaireField = new TextField();
        prixUnitaireField.setPromptText("Prix unitaire");
        prixUnitaireField.setPrefWidth(150);
        prixUnitaireField.setEditable(false); // Le prix vient du produit sélectionné
        prixUnitaireField.setStyle("-fx-background-color: #f0f0f0;");

        // Ajout à la grille
        grid.add(commandeLabel, 0, 0);
        grid.add(commandeCombo, 1, 0);
        grid.add(produitLabel, 2, 0);
        grid.add(produitCombo, 3, 0);

        grid.add(quantiteLabel, 0, 1);
        grid.add(quantiteField, 1, 1);
        grid.add(prixLabel, 2, 1);
        grid.add(prixUnitaireField, 3, 1);

        // Boutons d'action
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button ajouterBtn = new Button("➕ Ajouter");
        ajouterBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        ajouterBtn.setOnAction(e -> ajouterLigneCommande());

        Button modifierBtn = new Button("✏️ Modifier");
        modifierBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        modifierBtn.setOnAction(e -> modifierLigneCommande());

        Button supprimerBtn = new Button("🗑️ Supprimer");
        supprimerBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        supprimerBtn.setOnAction(e -> {
            LigneCommande selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                supprimerLigneCommande(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une ligne à supprimer");
            }
        });

        Button effacerBtn = new Button("🗑️ Effacer");
        effacerBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        effacerBtn.setOnAction(e -> viderFormulaire());

        buttonBar.getChildren().addAll(ajouterBtn, modifierBtn, supprimerBtn, effacerBtn);

        panel.getChildren().addAll(formTitle, grid, buttonBar);
        return panel;
    }

    private void chargerProduits() {
        try {
            List<Produit> produits = produitService.selectAll();
            produitList = FXCollections.observableArrayList(produits);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private void chargerCommandes() {
        try {
            List<Commande> commandes = commandeService.selectAll();
            commandeList = FXCollections.observableArrayList(commandes);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes: " + e.getMessage());
        }
    }

    private void chargerDonnees() {
        try {
            ligneCommandeList = FXCollections.observableArrayList(ligneCommandeService.selectAll());
            table.setItems(ligneCommandeList);

            if (ligneCommandeList.isEmpty()) {
                System.out.println("Aucune ligne de commande trouvée");
            } else {
                System.out.println(ligneCommandeList.size() + " lignes chargées");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les lignes de commande: " + e.getMessage());
        }
    }

    private void remplirFormulaire(LigneCommande ligne) {
        // Sélectionner la commande
        for (Commande c : commandeList) {
            if (c.getId() == ligne.getCommande_id()) {
                commandeCombo.setValue(c);
                break;
            }
        }

        // Sélectionner le produit
        for (Produit p : produitList) {
            if (p.getId() == ligne.getProduit_id()) {
                produitCombo.setValue(p);
                break;
            }
        }

        quantiteField.setText(String.valueOf(ligne.getQuantite()));
        prixUnitaireField.setText(String.valueOf(ligne.getPrix_unitaire()));
    }

    private void viderFormulaire() {
        commandeCombo.setValue(null);
        produitCombo.setValue(null);
        quantiteField.clear();
        prixUnitaireField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void ajouterLigneCommande() {
        if (!validerChamps()) return;

        try {
            LigneCommande ligne = new LigneCommande(
                    Integer.parseInt(quantiteField.getText()),
                    Double.parseDouble(prixUnitaireField.getText()),
                    commandeCombo.getValue().getId(),
                    produitCombo.getValue().getId()
            );

            ligneCommandeService.insertOnePS(ligne);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Ligne de commande ajoutée avec succès!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    private void modifierLigneCommande() {
        LigneCommande selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une ligne à modifier");
            return;
        }

        if (!validerChamps()) return;

        try {
            selected.setQuantite(Integer.parseInt(quantiteField.getText()));
            selected.setPrix_unitaire(Double.parseDouble(prixUnitaireField.getText()));
            selected.setCommande_id(commandeCombo.getValue().getId());
            selected.setProduit_id(produitCombo.getValue().getId());

            ligneCommandeService.updateOne(selected);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Ligne de commande modifiée avec succès!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    private void supprimerLigneCommande(LigneCommande ligne) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la ligne de commande");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette ligne ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ligneCommandeService.deleteOne(ligne);
                viderFormulaire();
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Ligne supprimée avec succès!");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private boolean validerChamps() {
        if (commandeCombo.getValue() == null || produitCombo.getValue() == null ||
                quantiteField.getText().isEmpty() || prixUnitaireField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs");
            return false;
        }

        try {
            int quantite = Integer.parseInt(quantiteField.getText());
            if (quantite <= 0) {
                showAlert(Alert.AlertType.WARNING, "Attention", "La quantité doit être positive");
                return false;
            }

            double prix = Double.parseDouble(prixUnitaireField.getText());
            if (prix <= 0) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le prix doit être positif");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La quantité et le prix doivent être des nombres valides");
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}