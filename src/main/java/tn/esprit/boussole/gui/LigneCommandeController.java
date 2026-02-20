package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.DoubleStringConverter;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.services.LigneCommandeService;
import tn.esprit.boussole.services.ProduitService;
import tn.esprit.boussole.services.CommandeService;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class LigneCommandeController implements Initializable {

    @FXML private TableView<LigneCommande> ligneTable;
    @FXML private TableColumn<LigneCommande, Integer> colQuantite;
    @FXML private TableColumn<LigneCommande, Double> colPrix;
    @FXML private TableColumn<LigneCommande, Double> colTotal;
    @FXML private TableColumn<LigneCommande, String> colCommandeDate;
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
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ligneCommandeService = new LigneCommandeService();
        produitService = new ProduitService();
        commandeService = new CommandeService();

        // Charger les données pour les combos
        chargerProduits();
        chargerCommandes();

        // Configuration des colonnes avec édition
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

        // Vider le formulaire au démarrage
        viderFormulaire();
    }

    private void configurerTable() {
        // Rendre le tableau éditable
        ligneTable.setEditable(true);

        // Permettre la sélection de cellules individuelles
        ligneTable.getSelectionModel().setCellSelectionEnabled(true);

        // Colonne QUANTITE (éditable avec validation)
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colQuantite.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                try {
                    int quantite = Integer.parseInt(value);
                    if (quantite <= 0) {
                        showAlert(Alert.AlertType.WARNING, "Attention", "La quantité doit être supérieure à 0");
                        return null;
                    }
                    return quantite;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "La quantité doit être un nombre entier valide");
                    return null;
                }
            }
        }));
        colQuantite.setOnEditCommit(event -> {
            LigneCommande ligne = event.getRowValue();
            Integer newValue = event.getNewValue();
            if (newValue != null && newValue > 0) {
                ligne.setQuantite(newValue);
                sauvegarderModification(ligne);
                rafraichirTotal(ligne);
            } else {
                chargerDonnees();
            }
        });

        // Colonne PRIX (éditable avec validation)
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_unitaire"));
        colPrix.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter() {
            @Override
            public Double fromString(String value) {
                try {
                    double prix = Double.parseDouble(value);
                    if (prix <= 0) {
                        showAlert(Alert.AlertType.WARNING, "Attention", "Le prix doit être supérieur à 0");
                        return null;
                    }
                    return prix;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le prix doit être un nombre valide");
                    return null;
                }
            }
        }));
        colPrix.setOnEditCommit(event -> {
            LigneCommande ligne = event.getRowValue();
            Double newValue = event.getNewValue();
            if (newValue != null && newValue > 0) {
                ligne.setPrix_unitaire(newValue);
                sauvegarderModification(ligne);
                rafraichirTotal(ligne);
            } else {
                chargerDonnees();
            }
        });

        // Colonne TOTAL (calculé)
        colTotal.setCellValueFactory(cellData -> {
            double total = cellData.getValue().getQuantite() * cellData.getValue().getPrix_unitaire();
            return new javafx.beans.property.SimpleDoubleProperty(total).asObject();
        });

        // Colonne DATE COMMANDE (affichage)
        colCommandeDate.setCellValueFactory(cellData -> {
            int commandeId = cellData.getValue().getCommande_id();
            Commande commande = getCommandeById(commandeId);
            if (commande != null && commande.getDate_creation() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        commande.getDate_creation().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Colonne PRODUIT (affichage du nom) - CORRECTION: colProduit au lieu de colProduitNom
        colProduit.setCellValueFactory(cellData -> {
            int produitId = cellData.getValue().getProduit_id();
            Produit produit = getProduitById(produitId);
            if (produit != null) {
                return new javafx.beans.property.SimpleStringProperty(produit.getNom());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Colonne Actions (avec bouton Supprimer)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(deleteBtn);

            {
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");

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

    private void rafraichirTotal(LigneCommande ligne) {
        int index = ligneTable.getItems().indexOf(ligne);
        if (index >= 0) {
            ligneTable.getItems().set(index, ligne);
        }
    }

    private void sauvegarderModification(LigneCommande ligne) {
        try {
            ligneCommandeService.updateOne(ligne);
            System.out.println("Ligne modifiée");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
            chargerDonnees();
        }
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

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Produit getProduitById(int id) {
        for (Produit p : produitList) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    private Commande getCommandeById(int id) {
        for (Commande c : commandeList) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
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

    @FXML
    private void handleAjouter() {
        if (!validerChampsAjout()) return;

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
    private void handleRefresh() {
        chargerDonnees();
        chargerProduits();
        chargerCommandes();
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
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Ligne supprimée avec succès!");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validerChampsAjout() {
        if (commandeCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une commande");
            return false;
        }

        if (produitCombo.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un produit");
            return false;
        }

        // Validation Quantité
        String quantiteStr = quantiteField.getText();
        if (quantiteStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La quantité est obligatoire");
            return false;
        }
        try {
            int quantite = Integer.parseInt(quantiteStr);
            if (quantite <= 0) {
                showAlert(Alert.AlertType.WARNING, "Attention", "La quantité doit être supérieure à 0");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La quantité doit être un nombre entier valide");
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