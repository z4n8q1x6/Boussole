package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.services.CommandeService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CommandeGUI {

    private CommandeService commandeService;
    private TableView<Commande> table;
    private ObservableList<Commande> commandeList;

    // Champs du formulaire
    private TextField montantField;
    private ComboBox<String> statutCombo;
    private TextField franchiseIdField;
    private DatePicker datePicker;

    public CommandeGUI() {
        commandeService = new CommandeService();
    }

    public void startInPane(Pane pane) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(0));
        content.setStyle("-fx-background-color: #f5f5f5;");

        // Barre d'outils
        content.getChildren().add(createToolbar());

        // Tableau des commandes
        table = new TableView<>();
        configurerTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Formulaire
        VBox formPanel = createFormPanel();

        content.getChildren().addAll(table, formPanel);
        pane.getChildren().add(content);

        // Charger les données
        chargerDonnees();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 0, 10, 0));

        Label title = new Label("📋 Gestion des Commandes");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Rafraîchir");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        refreshBtn.setOnAction(e -> chargerDonnees());

        toolbar.getChildren().addAll(title, spacer, refreshBtn);
        return toolbar;
    }

    private void configurerTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Colonnes
        TableColumn<Commande, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Commande, LocalDateTime> colDate = new TableColumn<>("Date création");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_creation"));
        colDate.setCellFactory(column -> new TableCell<Commande, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(formatter));
                }
            }
        });
        colDate.setPrefWidth(150);

        TableColumn<Commande, Double> colMontant = new TableColumn<>("Montant total");
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant_total"));
        colMontant.setPrefWidth(120);

        TableColumn<Commande, String> colStatut = new TableColumn<>("Statut");
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setPrefWidth(100);

        TableColumn<Commande, Integer> colFranchise = new TableColumn<>("Franchise ID");
        colFranchise.setCellValueFactory(new PropertyValueFactory<>("franchise_id"));
        colFranchise.setPrefWidth(100);

        // Colonne Actions
        TableColumn<Commande, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(150);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 3;");

                editBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    remplirFormulaire(commande);
                });

                deleteBtn.setOnAction(event -> {
                    Commande commande = getTableView().getItems().get(getIndex());
                    supprimerCommande(commande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(colId, colDate, colMontant, colStatut, colFranchise, colActions);

        // Sélection dans la table
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        remplirFormulaire(newSelection);
                    }
                }
        );
    }

    private VBox createFormPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0; -fx-padding: 15;");

        Label formTitle = new Label("AJOUTER / MODIFIER UNE COMMANDE");
        formTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);

        // Labels et champs
        Label montantLabel = new Label("Montant total:");
        montantLabel.setStyle("-fx-font-weight: bold;");
        montantField = new TextField();
        montantField.setPromptText("0.00");
        montantField.setPrefWidth(200);

        Label statutLabel = new Label("Statut:");
        statutLabel.setStyle("-fx-font-weight: bold;");
        statutCombo = new ComboBox<>();
        statutCombo.getItems().addAll("En attente", "Confirmée", "Expédiée", "Livrée", "Annulée");
        statutCombo.setPromptText("Sélectionner un statut");
        statutCombo.setPrefWidth(200);

        Label franchiseLabel = new Label("Franchise ID:");
        franchiseLabel.setStyle("-fx-font-weight: bold;");
        franchiseIdField = new TextField();
        franchiseIdField.setPromptText("ID de la franchise");
        franchiseIdField.setPrefWidth(150);

        // Ajout à la grille
        grid.add(montantLabel, 0, 0);
        grid.add(montantField, 1, 0);
        grid.add(statutLabel, 2, 0);
        grid.add(statutCombo, 3, 0);

        grid.add(franchiseLabel, 0, 1);
        grid.add(franchiseIdField, 1, 1);

        // Boutons d'action
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button ajouterBtn = new Button("➕ Ajouter");
        ajouterBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        ajouterBtn.setOnAction(e -> ajouterCommande());

        Button modifierBtn = new Button("✏️ Modifier");
        modifierBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        modifierBtn.setOnAction(e -> modifierCommande());

        Button supprimerBtn = new Button("🗑️ Supprimer");
        supprimerBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        supprimerBtn.setOnAction(e -> {
            Commande selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                supprimerCommande(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une commande à supprimer");
            }
        });

        Button effacerBtn = new Button("🗑️ Effacer");
        effacerBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        effacerBtn.setOnAction(e -> viderFormulaire());

        buttonBar.getChildren().addAll(ajouterBtn, modifierBtn, supprimerBtn, effacerBtn);

        panel.getChildren().addAll(formTitle, grid, buttonBar);
        return panel;
    }

    private void chargerDonnees() {
        try {
            commandeList = FXCollections.observableArrayList(commandeService.selectAll());
            table.setItems(commandeList);

            if (commandeList.isEmpty()) {
                System.out.println("Aucune commande trouvée");
            } else {
                System.out.println(commandeList.size() + " commandes chargées");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les commandes: " + e.getMessage());
        }
    }

    private void remplirFormulaire(Commande commande) {
        montantField.setText(String.valueOf(commande.getMontant_total()));
        statutCombo.setValue(commande.getStatut());
        franchiseIdField.setText(String.valueOf(commande.getFranchise_id()));
    }

    private void viderFormulaire() {
        montantField.clear();
        statutCombo.setValue(null);
        franchiseIdField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void ajouterCommande() {
        if (!validerChamps()) return;

        try {
            Commande commande = new Commande(
                    LocalDateTime.now(),
                    Double.parseDouble(montantField.getText()),
                    statutCombo.getValue(),
                    Integer.parseInt(franchiseIdField.getText())
            );

            commandeService.insertOnePS(commande);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande ajoutée avec succès!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    private void modifierCommande() {
        Commande selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une commande à modifier");
            return;
        }

        if (!validerChamps()) return;

        try {
            selected.setMontant_total(Double.parseDouble(montantField.getText()));
            selected.setStatut(statutCombo.getValue());
            selected.setFranchise_id(Integer.parseInt(franchiseIdField.getText()));

            commandeService.updateOne(selected);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande modifiée avec succès!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    private void supprimerCommande(Commande commande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la commande");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer la commande #" + commande.getId() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commandeService.deleteOne(commande);
                viderFormulaire();
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Commande supprimée avec succès!");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private boolean validerChamps() {
        if (montantField.getText().isEmpty() || statutCombo.getValue() == null ||
                franchiseIdField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs");
            return false;
        }

        try {
            Double.parseDouble(montantField.getText());
            Integer.parseInt(franchiseIdField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le montant et l'ID franchise doivent être des nombres valides");
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