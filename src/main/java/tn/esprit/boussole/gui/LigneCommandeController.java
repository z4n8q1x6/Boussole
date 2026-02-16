package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.services.LigneCommandeService;
import tn.esprit.boussole.services.ProduitService;
import tn.esprit.boussole.services.CommandeService;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class LigneCommandeController implements Initializable {

    @FXML private TableView<LigneCommande> ligneTable;
    @FXML private TableColumn<LigneCommande, Integer> colId;
    @FXML private TableColumn<LigneCommande, Integer> colQuantite;
    @FXML private TableColumn<LigneCommande, Double> colPrix;
    @FXML private TableColumn<LigneCommande, Double> colTotal;
    @FXML private TableColumn<LigneCommande, Integer> colCommandeId;
    @FXML private TableColumn<LigneCommande, String> colProduit;
    @FXML private TableColumn<LigneCommande, Void> colActions;

    @FXML private ComboBox<Commande> commandeCombo;
    @FXML private ComboBox<Produit> produitCombo;
    @FXML private TextField quantiteField;
    @FXML private TextField prixUnitaireField;

    private LigneCommandeService ligneCommandeService;
    private ProduitService produitService;
    private CommandeService commandeService;

    private ObservableList<LigneCommande> ligneCommandeList;
    private ObservableList<Produit> produitList;
    private ObservableList<Commande> commandeList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ligneCommandeService = new LigneCommandeService();
        produitService = new ProduitService();
        commandeService = new CommandeService();

        // Charger les données pour les combos
        chargerProduits();
        chargerCommandes();

        // Configuration des colonnes
        configurerTable();

        // Charger les données
        chargerDonnees();

        // Listener pour le prix automatique
        produitCombo.setOnAction(e -> {
            Produit selected = produitCombo.getValue();
            if (selected != null) {
                prixUnitaireField.setText(String.valueOf(selected.getPrix_achat()));
            }
        });

        // Listener pour la sélection
        ligneTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        remplirFormulaire(newSelection);
                    }
                }
        );
    }

    private void configurerTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_unitaire"));
        colCommandeId.setCellValueFactory(new PropertyValueFactory<>("commande_id"));

        // Calcul du total
        colTotal.setCellValueFactory(cellData -> {
            double total = cellData.getValue().getQuantite() * cellData.getValue().getPrix_unitaire();
            return new javafx.beans.property.SimpleDoubleProperty(total).asObject();
        });

        // Affichage du nom du produit
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

        // Colonne Actions avec boutons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");

                editBtn.setOnAction(event -> {
                    LigneCommande ligne = getTableView().getItems().get(getIndex());
                    remplirFormulaire(ligne);
                    ligneTable.getSelectionModel().select(ligne);
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
    }

    private void chargerProduits() {
        try {
            produitList = FXCollections.observableArrayList(produitService.selectAll());
            produitCombo.setItems(produitList);

            // Personnaliser l'affichage du produit dans la combo
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

            System.out.println("Produits chargés pour combo: " + produitList.size());

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerCommandes() {
        try {
            commandeList = FXCollections.observableArrayList(commandeService.selectAll());
            commandeCombo.setItems(commandeList);

            // Personnaliser l'affichage de la commande
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

            System.out.println("Commandes chargées pour combo: " + commandeList.size());

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerDonnees() {
        try {
            ligneCommandeList = FXCollections.observableArrayList(ligneCommandeService.selectAll());
            ligneTable.setItems(ligneCommandeList);
            System.out.println("Lignes de commande chargées: " + ligneCommandeList.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les lignes de commande: " + e.getMessage());
            e.printStackTrace();
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

    @FXML
    private void handleRefresh() {
        chargerDonnees();
        viderFormulaire();
    }

    @FXML
    private void handleAjouter() {
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleModifier() {
        LigneCommande selected = ligneTable.getSelectionModel().getSelectedItem();
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSupprimer() {
        LigneCommande selected = ligneTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une ligne à supprimer");
            return;
        }
        supprimerLigneCommande(selected);
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
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleEffacer() {
        viderFormulaire();
    }

    private void viderFormulaire() {
        commandeCombo.setValue(null);
        produitCombo.setValue(null);
        quantiteField.clear();
        prixUnitaireField.clear();
        ligneTable.getSelectionModel().clearSelection();
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