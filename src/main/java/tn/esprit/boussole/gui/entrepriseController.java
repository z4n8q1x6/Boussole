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
import tn.esprit.boussole.service.userService;
import tn.esprit.boussole.utils.DialogManager;
import tn.esprit.boussole.utils.NotificationManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class entrepriseController {

    @FXML private TableView<franchise> tableEntreprises;
    @FXML private TableColumn<franchise, String> colNom, colEmail, colTelephone, colAdresse;
    @FXML private TableColumn<franchise, Boolean> colActif;
    @FXML private TableColumn<franchise, Double> colSolde;
    @FXML private TableColumn<franchise, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label lblPagination;

    private ObservableList<franchise> entrepriseList = FXCollections.observableArrayList();
    private franchiseService franchiseService = new franchiseService();
    private userService userService = new userService();

    @FXML
    public void initialize() {
        tableEntreprises.setEditable(true);
        setupEditableColumns();
        setupStatusColumn();
        setupSoldeColumn();
        setupActionButtons();

        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs"));
            statusFilter.setValue("Tous");
            statusFilter.setOnAction(e -> handleSearch());
        }

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
                        updateEntrepriseInDB(f);
                        tableEntreprises.refresh();
                    });

                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                    setTooltip(new Tooltip("Cliquez pour changer le statut"));
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
            franchiseService.updateone(f);
            userService.updateFranchiseStatus(f.getId(), f.getActif());
            NotificationManager.show(tableEntreprises.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Mise à jour", "Franchise modifiée avec succès.");
        } catch (Exception e) {
            NotificationManager.show(tableEntreprises.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur", "La mise à jour a échoué.");
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
        Optional<ButtonType> result = DialogManager.showConfirmationDialog(
            tableEntreprises.getScene().getWindow(),
            "Supprimer l'entreprise ?",
            "Voulez-vous vraiment supprimer " + f.getNom() + " ? Cette action supprimera également l'utilisateur associé."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.deleteFranchiseAndUser(f);
                loadEntreprises();
                NotificationManager.show(tableEntreprises.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Suppression", "Entreprise supprimée avec succès.");
            } catch (SQLException e) {
                NotificationManager.show(tableEntreprises.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur", "Suppression impossible.");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String filterText = searchField.getText().toLowerCase();
        String status = statusFilter != null ? statusFilter.getValue() : "Tous";

        ObservableList<franchise> filteredList = entrepriseList.filtered(f -> {
            boolean matchesText = f.getNom().toLowerCase().contains(filterText) || 
                                  f.getEmail().toLowerCase().contains(filterText);

            boolean matchesStatus = true;
            if ("Actifs".equals(status)) {
                matchesStatus = f.getActif();
            } else if ("Inactifs".equals(status)) {
                matchesStatus = !f.getActif();
            }

            return matchesText && matchesStatus;
        });

        tableEntreprises.setItems(filteredList);
        updatePaginationLabel();
    }

    private void updatePaginationLabel() {
        if (lblPagination != null) {
            lblPagination.setText("Total : " + tableEntreprises.getItems().size() + " entreprise(s)");
        }
    }
}
