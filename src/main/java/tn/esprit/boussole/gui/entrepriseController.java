package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.util.converter.DoubleStringConverter;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.franchiseService;
import tn.esprit.boussole.service.userService; // IMPORTANT : Ajout du service utilisateur

import java.sql.SQLException;
import java.util.List;

public class entrepriseController {

    @FXML private TableView<franchise> tableEntreprises;
    @FXML private TableColumn<franchise, String> colNom, colEmail, colTelephone, colAdresse;
    @FXML private TableColumn<franchise, Boolean> colActif;
    @FXML private TableColumn<franchise, Double> colSolde;
    @FXML private TableColumn<franchise, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Label lblPagination;

    private ObservableList<franchise> entrepriseList = FXCollections.observableArrayList();
    private franchiseService franchiseService = new franchiseService();
    private userService userService = new userService(); // IMPORTANT : Pour la synchronisation

    @FXML
    public void initialize() {
        // 1. Activer l'édition sur la table
        tableEntreprises.setEditable(true);

        // 2. Configurer les colonnes texte
        setupEditableColumns();

        // 3. Configurer le statut (Synchronisé avec les utilisateurs)
        setupStatusColumn();

        // 4. Configurer le solde
        setupSoldeColumn();

        // 5. Boutons d'actions
        setupActionButtons();

        // 6. Charger les données
        loadEntreprises();
    }

    private void setupEditableColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(TextFieldTableCell.forTableColumn());
        colNom.setOnEditCommit(event -> {
            franchise f = event.getRowValue();
            f.setNom(event.getNewValue());
            updateEntrepriseInDB(f);
        });

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(event -> {
            franchise f = event.getRowValue();
            f.setEmail(event.getNewValue());
            updateEntrepriseInDB(f);
        });

        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colTelephone.setCellFactory(TextFieldTableCell.forTableColumn());
        colTelephone.setOnEditCommit(event -> {
            franchise f = event.getRowValue();
            f.setTelephone(event.getNewValue());
            updateEntrepriseInDB(f);
        });

        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse"));
        colAdresse.setCellFactory(TextFieldTableCell.forTableColumn());
        colAdresse.setOnEditCommit(event -> {
            franchise f = event.getRowValue();
            f.setAdresse(event.getNewValue());
            updateEntrepriseInDB(f);
        });
    }

    private void setupStatusColumn() {
        colActif.setCellValueFactory(new PropertyValueFactory<>("actif"));
        colActif.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(active ? "ACTIF" : "INACTIF");
                    badge.setPadding(new javafx.geometry.Insets(2, 10, 2, 10));

                    String baseStyle = "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;";
                    badge.setStyle(active ?
                            baseStyle + "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10B981;" :
                            baseStyle + "-fx-background-color: rgba(244, 63, 94, 0.2); -fx-text-fill: #F43F5E;");

                    badge.setOnMouseClicked(e -> {
                        franchise f = getTableView().getItems().get(getIndex());
                        f.setActif(!f.getActif());
                        updateEntrepriseInDB(f); // Ici on déclenche la double mise à jour
                        tableEntreprises.refresh();
                    });

                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                    setTooltip(new Tooltip("Cliquez pour changer le statut (bloque aussi les accès utilisateurs)"));
                }
            }
        });
    }

    private void setupSoldeColumn() {
        colSolde.setCellValueFactory(new PropertyValueFactory<>("soldeActuel"));
        colSolde.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colSolde.setOnEditCommit(event -> {
            franchise f = event.getRowValue();
            f.setSoldeActuel(event.getNewValue());
            updateEntrepriseInDB(f);
        });
    }

    private void setupActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");
            private final HBox container = new HBox(deleteBtn);
            {
                container.setAlignment(Pos.CENTER);
                deleteBtn.setStyle("-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #E74C3C; -fx-cursor: hand; -fx-background-radius: 5;");
                deleteBtn.setOnAction(e -> handleDeleteEntreprise(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void updateEntrepriseInDB(franchise f) {
        try {
            // 1. Mise à jour de la franchise elle-même
            franchiseService.updateone(f);

            // 2. SYNCHRONISATION : Bloquer/Débloquer les utilisateurs rattachés
            userService.updateFranchiseStatus(f.getId(), f.getActif());

            System.out.println("✅ Franchise " + f.getNom() + " et utilisateurs synchronisés.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "La mise à jour ou la synchronisation a échoué.");
            loadEntreprises();
        }
    }

    private void loadEntreprises() {
        try {
            List<franchise> entreprises = franchiseService.selectAll(new franchise());
            entrepriseList.setAll(entreprises);
            tableEntreprises.setItems(entrepriseList);
            updatePaginationLabel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteEntreprise(franchise f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'entreprise " + f.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                franchiseService.deleteone(f);
                loadEntreprises();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible.");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String filter = searchField.getText().toLowerCase();
        tableEntreprises.setItems(entrepriseList.filtered(f ->
                f.getNom().toLowerCase().contains(filter) || f.getEmail().toLowerCase().contains(filter)));
        updatePaginationLabel();
    }

    private void updatePaginationLabel() {
        if (lblPagination != null) {
            lblPagination.setText("Total : " + tableEntreprises.getItems().size() + " entreprise(s)");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.show();
    }
}