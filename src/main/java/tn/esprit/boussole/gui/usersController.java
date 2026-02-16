package tn.esprit.boussole.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.boussole.models.user;
import tn.esprit.boussole.service.userService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class usersController {

    @FXML private TableView<user> tableUsers;
    @FXML private TableColumn<user, Integer> colId;
    @FXML private TableColumn<user, String> colNom;
    @FXML private TableColumn<user, String> colPrenom;
    @FXML private TableColumn<user, String> colEmail;
    @FXML private TableColumn<user, String> colRole;
    @FXML private TableColumn<user, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Label lblPagination;
    @FXML private Button btnNewUser;

    private ObservableList<user> userList = FXCollections.observableArrayList();
    private userService userService;

    @FXML
    public void initialize() {
        userService = new userService();

        // Remplacer l'ID de la base par un numéro séquentiel (1, 2, 3...)
        colId.setText("N°"); // Changer le titre de la colonne
        colId.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tableUsers.getItems().indexOf(column.getValue()) + 1));
        colId.setSortable(false); // Désactiver le tri sur cette colonne car elle dépend de l'ordre d'affichage

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        addActionButtons();
        loadUsers();
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");

                editBtn.setOnAction(event -> {
                    user selectedUser = getTableView().getItems().get(getIndex());
                    if (selectedUser != null) {
                        handleEditUser(selectedUser);
                    } else {
                        System.err.println("Erreur : Utilisateur sélectionné est null");
                    }
                });

                deleteBtn.setOnAction(event -> {
                    user selectedUser = getTableView().getItems().get(getIndex());
                    if (selectedUser != null) {
                        handleDeleteUser(selectedUser);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadUsers() {
        userList.clear();
        try {
            List<user> users = userService.selectAll(null);
            userList.addAll(users);
            tableUsers.setItems(userList);
            updatePaginationLabel();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les utilisateurs.");
        }
    }

    @FXML
    private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/adduser.fxml"));
            Parent root = loader.load();
            addUserController controller = loader.getController();
            controller.setOnUserCreated(this::loadUsers);

            Stage stage = new Stage();
            stage.setTitle("Ajouter un utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void handleEditUser(user selectedUser) {
        try {
            System.out.println("Tentative d'ouverture de updateUser.fxml pour : " + selectedUser.getEmail());
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/updateUser.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("Fichier FXML introuvable : /updateUser.fxml");
            }
            
            Parent root = loader.load();
            System.out.println("FXML chargé avec succès");

            updateUserController controller = loader.getController();
            if (controller == null) {
                throw new IllegalStateException("Le contrôleur updateUserController est null !");
            }
            
            controller.initData(selectedUser);
            controller.setOnUserUpdated(this::loadUsers);

            Stage stage = new Stage();
            stage.setTitle("Modifier Utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace(); // Affiche la trace complète dans la console
            showAlert(Alert.AlertType.ERROR, "Erreur Critique", 
                      "Impossible d'ouvrir le formulaire de modification.\n\n" +
                      "Cause : " + e.getClass().getSimpleName() + "\n" +
                      "Message : " + e.getMessage());
        }
    }

    private void handleDeleteUser(user selectedUser) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'utilisateur");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedUser.getPrenom() + " " + selectedUser.getNom() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.deleteone(selectedUser);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur supprimé avec succès !");
                loadUsers();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'utilisateur.");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            tableUsers.setItems(userList);
        } else {
            ObservableList<user> filteredList = FXCollections.observableArrayList();
            for (user u : userList) {
                if (u.getNom().toLowerCase().contains(searchText) ||
                        u.getPrenom().toLowerCase().contains(searchText) ||
                        u.getEmail().toLowerCase().contains(searchText)) {
                    filteredList.add(u);
                }
            }
            tableUsers.setItems(filteredList);
        }
        updatePaginationLabel();
    }

    private void updatePaginationLabel() {
        int total = tableUsers.getItems().size();
        lblPagination.setText("Affichage de 1 à " + total + " sur " + total + " utilisateurs");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
