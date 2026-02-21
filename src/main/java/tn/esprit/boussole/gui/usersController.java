package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.boussole.models.user;
import tn.esprit.boussole.service.userService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class usersController {

    @FXML private TableView<user> tableUsers;
    @FXML private TableColumn<user, String> colNom, colPrenom, colEmail, colRole;
    @FXML private TableColumn<user, Void> colActions;
    @FXML private TextField searchField;
    @FXML private Label lblPagination;
    @FXML private Button btnNewUser; // Injecté pour l'animation

    private ObservableList<user> userList = FXCollections.observableArrayList();
    private userService userService = new userService();

    @FXML
    public void initialize() {
        // 1. Rendre la table éditable
        tableUsers.setEditable(true);

        // 2. Configurer les colonnes pour l'édition directe
        setupEditableColumns();

        // 3. Boutons d'actions (uniquement Supprimer)
        setupActionButtons();

        // 4. Animation du bouton ajouter
        setupButtonHover();

        // 5. Charger les données
        loadUsers();
    }

    private void setupEditableColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(TextFieldTableCell.forTableColumn());
        colNom.setOnEditCommit(event -> {
            user u = event.getRowValue();
            u.setNom(event.getNewValue());
            updateUserInDB(u);
        });

        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colPrenom.setCellFactory(TextFieldTableCell.forTableColumn());
        colPrenom.setOnEditCommit(event -> {
            user u = event.getRowValue();
            u.setPrenom(event.getNewValue());
            updateUserInDB(u);
        });

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(TextFieldTableCell.forTableColumn());
        colEmail.setOnEditCommit(event -> {
            user u = event.getRowValue();
            u.setEmail(event.getNewValue());
            updateUserInDB(u);
        });

        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(TextFieldTableCell.forTableColumn());
        colRole.setOnEditCommit(event -> {
            user u = event.getRowValue();
            u.setRole(event.getNewValue());
            updateUserInDB(u);
        });
    }

    private void updateUserInDB(user u) {
        try {
            userService.updateone(u);
            System.out.println("Utilisateur mis à jour : " + u.getEmail());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de sauvegarder.");
            loadUsers();
        }
    }

    // METHODE MANQUANTE QUI CAUSAIT LE CRASH
    @FXML
    private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/adduser.fxml"));
            Parent root = loader.load();
            addUserController controller = loader.getController();
            controller.setOnUserCreated(this::loadUsers);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un membre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier adduser.fxml introuvable.");
        }
    }

    private void setupButtonHover() {
        btnNewUser.setOnMouseEntered(e -> btnNewUser.setStyle("-fx-background-color: #00FFED; -fx-text-fill: #06080F; -fx-font-weight: bold; -fx-background-radius: 10; -fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        btnNewUser.setOnMouseExited(e -> btnNewUser.setStyle("-fx-background-color: #00E5CC; -fx-text-fill: #06080F; -fx-font-weight: bold; -fx-background-radius: 10; -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));
    }

    private void setupActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️");
            private final HBox container = new HBox(deleteBtn);
            {
                container.setAlignment(Pos.CENTER);
                deleteBtn.setStyle("-fx-background-color: rgba(231, 76, 60, 0.15); -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-background-radius: 5;");
                deleteBtn.setOnAction(e -> handleDeleteUser(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadUsers() {
        try {
            userList.setAll(userService.selectAll(null));
            tableUsers.setItems(userList);
            updatePaginationLabel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteUser(user u) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + u.getEmail() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                userService.deleteone(u);
                loadUsers();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression échouée.");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String filter = searchField.getText().toLowerCase();
        tableUsers.setItems(userList.filtered(u ->
                u.getNom().toLowerCase().contains(filter) || u.getEmail().toLowerCase().contains(filter)));
        updatePaginationLabel();
    }

    private void updatePaginationLabel() {
        lblPagination.setText("Total: " + tableUsers.getItems().size() + " utilisateur(s)");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.show();
    }
}