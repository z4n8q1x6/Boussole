package tn.esprit.chargesdepenses.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import tn.esprit.chargesdepenses.models.Fournisseur;
import tn.esprit.chargesdepenses.services.FournisseurService;
import java.io.IOException;
import java.sql.SQLException;

public class afficherBackFournisseurController {
    @FXML private TableView<Fournisseur> tableFournisseurs;
    @FXML private TableColumn<Fournisseur, String> colNom;
    @FXML private TableColumn<Fournisseur, String> colMatricule;
    @FXML private TableColumn<Fournisseur, String> colTelephone;
    @FXML private TableColumn<Fournisseur, String> colFranchiseId;
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
        tableFournisseurs.setEditable(true);

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(TextFieldTableCell.forTableColumn());
        colNom.setOnEditCommit(event -> {
            Fournisseur f = event.getRowValue();
            f.setNom(event.getNewValue());
            updateFournisseurInDB(f);
        });

        colMatricule.setCellValueFactory(new PropertyValueFactory<>("matriculeFiscal"));
        colMatricule.setCellFactory(TextFieldTableCell.forTableColumn());
        colMatricule.setOnEditCommit(event -> {
            Fournisseur f = event.getRowValue();
            f.setMatriculeFiscal(event.getNewValue());
            updateFournisseurInDB(f);
        });

        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colTelephone.setCellFactory(TextFieldTableCell.forTableColumn());
        colTelephone.setOnEditCommit(event -> {
            Fournisseur f = event.getRowValue();
            f.setTelephone(event.getNewValue());
            updateFournisseurInDB(f);
        });

        // Affichage du NOM de la franchise
        colFranchiseId.setCellValueFactory(new PropertyValueFactory<>("franchiseName"));

        addModifierButtonToTable();
        addSupprimerButtonToTable();

        filteredData = new FilteredList<>(fournisseursList, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableFournisseurs.comparatorProperty());
        tableFournisseurs.setItems(sortedData);

        txtRecherche.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(f -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return f.getNom().toLowerCase().contains(newValue.toLowerCase());
            });
            calculerTotal();
        });

        comboTri.setItems(FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
        comboTri.setOnAction(e -> trierFournisseurs());

        loadFournisseurs();
        btnAjouter.setOnAction(e -> openAjoutForm());
        if (btnFront != null) btnFront.setOnAction(e -> openFrontOffice());
    }

    private void updateFournisseurInDB(Fournisseur f) {
        try {
            fournisseurService.updateOne(f);
        } catch (SQLException e) {
            showAlert("Erreur", "MAJ impossible : " + e.getMessage());
            loadFournisseurs();
        }
    }

    private void loadFournisseurs() {
        fournisseursList.clear();
        try {
            fournisseursList.addAll(fournisseurService.selectAll());
            calculerTotal();
        } catch (SQLException e) {
            showAlert("Erreur", "Chargement impossible : " + e.getMessage());
        }
    }

    private void calculerTotal() {
        lblTotal.setText("Total : " + tableFournisseurs.getItems().size());
    }

    private void trierFournisseurs() {
        String selection = comboTri.getValue();
        if ("Matricule Croissant".equals(selection)) {
            colMatricule.setSortType(TableColumn.SortType.ASCENDING);
        } else {
            colMatricule.setSortType(TableColumn.SortType.DESCENDING);
        }
        tableFournisseurs.getSortOrder().setAll(colMatricule);
    }

    private void addModifierButtonToTable() {
        colModifier.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("✎");
            {
                btn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> openModifierForm(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addSupprimerButtonToTable() {
        colSupprimer.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("🗑");
            {
                btn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> supprimerFournisseur(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void supprimerFournisseur(Fournisseur f) {
        try {
            fournisseurService.deleteOne(f);
            fournisseursList.remove(f);
            calculerTotal();
        } catch (SQLException e) {
            showAlert("Erreur", "Suppression impossible : " + e.getMessage());
        }
    }

    private void openAjoutForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait();
            loadFournisseurs();
        } catch (IOException e) {
            showAlert("Erreur", "Ouverture impossible : " + e.getMessage());
        }
    }

    private void openModifierForm(Fournisseur f) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierFournisseur.fxml"));
            Parent root = loader.load();
            modifierFournisseurController controller = loader.getController();
            controller.setFournisseurActuel(f);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadFournisseurs();
        } catch (IOException e) {
            showAlert("Erreur", "Ouverture impossible : " + e.getMessage());
        }
    }

    private void openFrontOffice() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/afficherFrontFournisseur.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Front Office inaccessible : " + e.getMessage());
        }
    }

    private void showAlert(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
