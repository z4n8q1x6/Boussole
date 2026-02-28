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
import tn.esprit.boussole.utils.DialogManager;
import tn.esprit.boussole.utils.NotificationManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class usersController {

    @FXML private TableView<user> tableUsers;
    @FXML private TableColumn<user, String> colNom, colPrenom, colEmail, colRole;
    @FXML private TableColumn<user, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label lblPagination;
    @FXML private Button btnNewUser;

    @FXML private Label lblTotalUsers;
    @FXML private Label lblActiveUsers;
    @FXML private Label lblInactiveUsers;

    private ObservableList<user> userList = FXCollections.observableArrayList();
    private userService userService = new userService();

    @FXML
    public void initialize() {
        tableUsers.setEditable(true);
        setupEditableColumns();
        setupActionButtons();
        setupButtonHover();

        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs"));
            statusFilter.setValue("Tous");
            statusFilter.setOnAction(e -> handleSearch());
        }

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
            updateStatistics();
            NotificationManager.show(tableUsers.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Mise à jour", "Utilisateur modifié avec succès.");
        } catch (Exception e) {
            NotificationManager.show(tableUsers.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur", "Impossible de sauvegarder les modifications.");
            loadUsers();
        }
    }

    @FXML
    private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/adduser.fxml"));
            Parent root = loader.load();
            addUserController controller = loader.getController();
            
            controller.setOnUserCreated(() -> {
                this.loadUsers();
                NotificationManager.show(tableUsers.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Succès", "Nouvel utilisateur ajouté avec succès.");
            });

            Stage stage = new Stage();
            stage.setTitle("Ajouter un membre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            NotificationManager.show(tableUsers.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur", "Fichier adduser.fxml introuvable.");
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
            updateStatistics();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatistics() {
        if (userList == null) return;

        long total = userList.size();
        long active = userList.stream().filter(user::getActif).count();
        long inactive = total - active;

        if (lblTotalUsers != null) lblTotalUsers.setText(String.valueOf(total));
        if (lblActiveUsers != null) lblActiveUsers.setText(String.valueOf(active));
        if (lblInactiveUsers != null) lblInactiveUsers.setText(String.valueOf(inactive));
    }

    private void handleDeleteUser(user u) {
        Optional<ButtonType> result = DialogManager.showConfirmationDialog(
            tableUsers.getScene().getWindow(),
            "Supprimer l'utilisateur ?",
            "Voulez-vous vraiment supprimer " + u.getEmail() + " ? Cette action supprimera également la franchise associée."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.deleteUserAndFranchise(u);
                loadUsers();
                NotificationManager.show(tableUsers.getScene().getWindow(), NotificationManager.Type.SUCCESS, "Suppression", "Utilisateur supprimé avec succès.");
            } catch (SQLException e) {
                NotificationManager.show(tableUsers.getScene().getWindow(), NotificationManager.Type.ERROR, "Erreur", "La suppression a échoué.");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String filterText = searchField.getText().toLowerCase();
        String status = statusFilter != null ? statusFilter.getValue() : "Tous";

        ObservableList<user> filteredList = userList.filtered(u -> {
            boolean matchesText = u.getNom().toLowerCase().contains(filterText) || 
                                  u.getEmail().toLowerCase().contains(filterText);

            boolean matchesStatus = true;
            if ("Actifs".equals(status)) {
                matchesStatus = u.getActif();
            } else if ("Inactifs".equals(status)) {
                matchesStatus = !u.getActif();
            }

            return matchesText && matchesStatus;
        });

        tableUsers.setItems(filteredList);
        updatePaginationLabel();
    }

    private void updatePaginationLabel() {
        lblPagination.setText("Total: " + tableUsers.getItems().size() + " utilisateur(s)");
    }
}
