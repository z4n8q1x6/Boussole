package controller;

import entity.Pret;
import entity.StatutPret;
import service.PretService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.util.List;

public class ListePretsController {

    @FXML private TableView<Pret> tablePrets;
    @FXML private TableColumn<Pret, Integer> colId;
    @FXML private TableColumn<Pret, String> colMotif;
    @FXML private TableColumn<Pret, Double> colMontant;
    @FXML private TableColumn<Pret, Integer> colDuree;
    @FXML private TableColumn<Pret, Float> colTaux;
    @FXML private TableColumn<Pret, StatutPret> colStatut;
    @FXML private TableColumn<Pret, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboStatut;

    private PretService pretService = new PretService();
    private ObservableList<Pret> pretList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Configurer les colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantDemande"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeMois"));
        colTaux.setCellValueFactory(new PropertyValueFactory<>("taux"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // 2. Initialiser le ComboBox
        comboStatut.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "ACCORDE", "REFUSE"));
        comboStatut.getSelectionModel().selectFirst();

        // 3. Boutons d'actions
        ajouterBoutonsActions();

        // 4. Charger les données initiales
        chargerDonnees();

        // 5. Configurer la recherche et les filtres dynamiques
        configurerFiltres();
    }

    @FXML
    public void chargerDonnees() {
        try {
            List<Pret> data = pretService.getAllPrets();
            pretList.setAll(data);
           
        } catch (Exception e) {
            afficherAlerte("Erreur de chargement", "Impossible de récupérer les données : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * GESTION DE LA RECHERCHE (ID / MOTIF / STATUT)
     */
    private void configurerFiltres() {
        // 1. Créer une FilteredList enveloppant notre liste d'origine
        FilteredList<Pret> filteredData = new FilteredList<>(pretList, p -> true);

        // 2. Écouter les changements sur le champ de texte (ID et Motif)
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            appliquerFiltre(filteredData);
        });

        // 3. Écouter les changements sur le ComboBox (Statut)
        comboStatut.valueProperty().addListener((observable, oldValue, newValue) -> {
            appliquerFiltre(filteredData);
        });

        // 4. Lier la liste filtrée à la TableView
        tablePrets.setItems(filteredData);
    }

    private void appliquerFiltre(FilteredList<Pret> filteredData) {
        filteredData.setPredicate(pret -> {
            String input = txtRecherche.getText().toLowerCase().trim();
            String statutFiltre = comboStatut.getValue();

            // --- Logique de recherche par Texte (ID ou Motif) ---
            boolean matchTexte = true;
            if (!input.isEmpty()) {
                String idString = String.valueOf(pret.getId());
                String motifString = pret.getMotif().toLowerCase();

                // Le prêt correspond si l'ID contient la saisie OU si le motif contient la saisie
                matchTexte = idString.contains(input) || motifString.contains(input);
            }

            // --- Logique de filtrage par Statut ---
            boolean matchStatut = statutFiltre == null ||
                    statutFiltre.equals("TOUS") ||
                    pret.getStatut().name().equals(statutFiltre);

            // Le prêt est affiché uniquement s'il valide les deux conditions
            return matchTexte && matchStatut;
        });
    }

    @FXML
    private void ouvrirFormulaire() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/DemandePret.fxml"));
        Stage stage = (Stage) tablePrets.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Boussole - Nouvelle Demande");
    }

    private void ajouterBoutonsActions() {
        Callback<TableColumn<Pret, Void>, TableCell<Pret, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnEdit = new Button("📝");
            private final Button btnDelete = new Button("🗑");
            private final HBox pane = new HBox(btnEdit, btnDelete);

            {
                pane.setSpacing(10);
                btnEdit.getStyleClass().add("btn-edit");
                btnDelete.getStyleClass().add("btn-delete");

                btnEdit.setOnAction(event -> {
                    Pret p = getTableView().getItems().get(getIndex());
                    ouvrirDialogueModification(p);
                });

                btnDelete.setOnAction(event -> {
                    Pret p = getTableView().getItems().get(getIndex());
                    confirmerSuppression(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void ouvrirDialogueModification(Pret p) {
        Dialog<Pret> dialog = new Dialog<>();
        dialog.setTitle("Modifier le prêt #" + p.getId());
        dialog.setHeaderText("Mise à jour des informations du prêt");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField editMotif = new TextField(p.getMotif());
        TextField editMontant = new TextField(String.valueOf(p.getMontantDemande()));
        TextField editDuree = new TextField(String.valueOf(p.getDureeMois()));
        TextField editTaux = new TextField(String.valueOf(p.getTaux()));

        grid.add(new Label("Motif:"), 0, 0); grid.add(editMotif, 1, 0);
        grid.add(new Label("Montant:"), 0, 1); grid.add(editMontant, 1, 1);
        grid.add(new Label("Durée (mois):"), 0, 2); grid.add(editDuree, 1, 2);
        grid.add(new Label("Taux (%):"), 0, 3); grid.add(editTaux, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                p.setMotif(editMotif.getText());
                p.setMontantDemande(Double.parseDouble(editMontant.getText()));
                p.setDureeMois(Integer.parseInt(editDuree.getText()));
                p.setTaux(Float.parseFloat(editTaux.getText()));
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pretModifie -> {
            try {
                pretService.modifierPret(pretModifie);
                chargerDonnees();
                afficherAlerte("Succès", "Le prêt a été mis à jour.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                afficherAlerte("Erreur", "Échec de la modification : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void confirmerSuppression(Pret p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Voulez-vous vraiment supprimer le prêt #" + p.getId() + " ?");
        alert.setContentText("Cela supprimera également toutes les mensualités associées.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                pretService.supprimerPret(p.getId());
                chargerDonnees();
                afficherAlerte("Succès", "Le prêt a été supprimé.", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                afficherAlerte("Erreur SQL", "Erreur lors de la suppression : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void afficherAlerte(String titre, String contenu, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setContentText(contenu);
        alert.showAndWait();
    }
}