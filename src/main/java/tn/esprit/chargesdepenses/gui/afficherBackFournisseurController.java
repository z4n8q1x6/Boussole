package tn.esprit.chargesdepenses.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;

public class afficherBackFournisseurController {

    @FXML private TableView<Fournisseur> tableFournisseurs;
    @FXML private TableColumn<Fournisseur, String> colNom;
    @FXML private TableColumn<Fournisseur, String> colMatricule;
    @FXML private TableColumn<Fournisseur, String> colTelephone;
    @FXML private TableColumn<Fournisseur, String> colFranchiseId; // Changé en String
    @FXML private TableColumn<Fournisseur, Void> colModifier;
    @FXML private TableColumn<Fournisseur, Void> colSupprimer;
    
    @FXML private Button btnAjouter;
    @FXML private Button btnFront;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label lblTotal;
    @FXML private TextField txtRecherche;

    private final FournisseurService fournisseurService = new FournisseurService();
    private final ObservableList<Fournisseur> fournisseursList = FXCollections.observableArrayList();
    private FilteredList<Fournisseur> filteredData;
    private SortedList<Fournisseur> sortedData;

    @FXML
    public void initialize() {
        // Rendre la table éditable
        tableFournisseurs.setEditable(true);

        // Configuration des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(TextFieldTableCell.forTableColumn());
        colNom.setOnEditCommit(event -> {
            Fournisseur fournisseur = event.getRowValue();
            if (!event.getNewValue().matches("^\\d+$")) {
                fournisseur.setNom(event.getNewValue());
                updateFournisseurInDB(fournisseur);
            } else {
                showAlert("Erreur", "Le nom ne peut pas contenir uniquement des chiffres.");
                tableFournisseurs.refresh();
            }
        });

        colMatricule.setCellValueFactory(new PropertyValueFactory<>("matriculeFiscal"));
        colMatricule.setCellFactory(TextFieldTableCell.forTableColumn());
        colMatricule.setOnEditCommit(event -> {
            Fournisseur fournisseur = event.getRowValue();
            if (!event.getNewValue().matches("^\\d+$")) {
                fournisseur.setMatriculeFiscal(event.getNewValue());
                updateFournisseurInDB(fournisseur);
            } else {
                showAlert("Erreur", "Le matricule ne peut pas contenir uniquement des chiffres.");
                tableFournisseurs.refresh();
            }
        });

        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colTelephone.setCellFactory(TextFieldTableCell.forTableColumn());
        colTelephone.setOnEditCommit(event -> {
            Fournisseur fournisseur = event.getRowValue();
            fournisseur.setTelephone(event.getNewValue());
            updateFournisseurInDB(fournisseur);
        });
        
        // Afficher le nom de la franchise (non éditable inline)
        colFranchiseId.setCellValueFactory(new PropertyValueFactory<>("franchiseName"));

        // Ajout des boutons d'action dans le tableau
        addModifierButtonToTable();
        addSupprimerButtonToTable();

        // Initialisation de la liste filtrée et triée
        filteredData = new FilteredList<>(fournisseursList, p -> true);
        sortedData = new SortedList<>(filteredData);
        
        // Lier le comparateur de la SortedList au TableView
        sortedData.comparatorProperty().bind(tableFournisseurs.comparatorProperty());
        
        tableFournisseurs.setItems(sortedData);

        // Listener pour la recherche par NOM
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(fournisseur -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return fournisseur.getNom().toLowerCase().contains(lowerCaseFilter);
            });
            calculerTotal(); // Recalculer le total basé sur les éléments filtrés
        });

        // Initialisation du ComboBox de tri par MATRICULE
        comboTri.setItems(FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
        comboTri.setOnAction(e -> trierFournisseurs());

        // Chargement initial des données
        loadFournisseurs();
        
        // Actions des boutons principaux
        btnAjouter.setOnAction(e -> openAjoutForm());
        btnFront.setOnAction(e -> openFrontOffice());
    }

    private void updateFournisseurInDB(Fournisseur fournisseur) {
        try {
            fournisseurService.updateOne(fournisseur);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de mettre à jour le fournisseur : " + e.getMessage());
            loadFournisseurs(); // Recharger pour annuler les changements visuels
        }
    }

    private void trierFournisseurs() {
        String selection = comboTri.getValue();
        if (selection != null) {
            if (selection.equals("Matricule Croissant")) {
                tableFournisseurs.getSortOrder().clear();
                colMatricule.setSortType(TableColumn.SortType.ASCENDING);
                tableFournisseurs.getSortOrder().add(colMatricule);
                tableFournisseurs.sort();
            } else if (selection.equals("Matricule Décroissant")) {
                tableFournisseurs.getSortOrder().clear();
                colMatricule.setSortType(TableColumn.SortType.DESCENDING);
                tableFournisseurs.getSortOrder().add(colMatricule);
                tableFournisseurs.sort();
            }
        }
    }

    private void loadFournisseurs() {
        fournisseursList.clear();
        try {
            fournisseursList.addAll(fournisseurService.selectAll());
            calculerTotal();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les fournisseurs: " + e.getMessage());
        }
    }

    private void calculerTotal() {
        // Compter le nombre d'éléments visibles
        int total = tableFournisseurs.getItems().size();
        lblTotal.setText(String.valueOf(total));
    }

    private void addModifierButtonToTable() {
        colModifier.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.control.Button btn = new javafx.scene.control.Button("✎");
            {
                btn.setStyle("-fx-background-color: #4593cb; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                btn.setOnAction(event -> {
                    Fournisseur fournisseur = getTableView().getItems().get(getIndex());
                    openModifierForm(fournisseur);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addSupprimerButtonToTable() {
        colSupprimer.setCellFactory(param -> new TableCell<>() {
            private final javafx.scene.control.Button btn = new javafx.scene.control.Button("🗑");
            {
                btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
                btn.setOnAction(event -> {
                    Fournisseur fournisseur = getTableView().getItems().get(getIndex());
                    supprimerFournisseur(fournisseur);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openModifierForm(Fournisseur fournisseur) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierFournisseur.fxml"));
            Parent root = loader.load();
            
            modifierFournisseurController controller = loader.getController();
            controller.setFournisseurActuel(fournisseur); 
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier le Fournisseur");
            stage.showAndWait();
            loadFournisseurs(); // Rafraîchir après modification
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter un Fournisseur");
            stage.showAndWait(); // Attendre la fermeture pour rafraîchir
            loadFournisseurs(); 
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage());
        }
    }
    
    private void openFrontOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherFrontFournisseur.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Fournisseurs - Front Office");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le Front Office: " + e.getMessage());
        }
    }

    private void supprimerFournisseur(Fournisseur fournisseur) {
        // Idéalement, demander confirmation avant suppression
        try {
            fournisseurService.deleteOne(fournisseur);
            fournisseursList.remove(fournisseur);
            calculerTotal(); 
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de supprimer le fournisseur: " + e.getMessage());
        }
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
