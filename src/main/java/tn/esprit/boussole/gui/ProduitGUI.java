package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.services.ProduitService;

import java.sql.SQLException;
import java.util.Optional;

public class ProduitGUI {

    private ProduitService produitService;
    private TableView<Produit> table;
    private ObservableList<Produit> produitList;

    // Champs du formulaire
    private TextField nomField;
    private TextField referenceField;
    private TextField prixField;
    private TextField stockField;
    private TextField imageField;

    public ProduitGUI() {
        produitService = new ProduitService();
    }

    public void startInPane(Pane pane) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(0));
        content.setStyle("-fx-background-color: #f5f5f5;");

        // Barre d'outils
        content.getChildren().add(createToolbar());

        // Tableau des produits
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

        Label title = new Label("📦 Gestion des Produits");
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

        // Colonnes
        TableColumn<Produit, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Produit, String> colNom = new TableColumn<>("Nom");
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setPrefWidth(150);

        TableColumn<Produit, String> colReference = new TableColumn<>("Référence");
        colReference.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colReference.setPrefWidth(120);

        TableColumn<Produit, Double> colPrix = new TableColumn<>("Prix (DT)");
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_achat"));
        colPrix.setPrefWidth(100);

        TableColumn<Produit, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock_dispo"));
        colStock.setPrefWidth(80);

        TableColumn<Produit, String> colImage = new TableColumn<>("Image");
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colImage.setPrefWidth(150);

        // Colonne Actions
        TableColumn<Produit, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(150);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 3;");

                editBtn.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    remplirFormulaire(produit);
                });

                deleteBtn.setOnAction(event -> {
                    Produit produit = getTableView().getItems().get(getIndex());
                    supprimerProduit(produit);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(colId, colNom, colReference, colPrix, colStock, colImage, colActions);

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

        Label formTitle = new Label("AJOUTER / MODIFIER UN PRODUIT");
        formTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);

        // Labels et champs
        Label nomLabel = new Label("Nom:");
        nomLabel.setStyle("-fx-font-weight: bold;");
        nomField = new TextField();
        nomField.setPromptText("Nom du produit");
        nomField.setPrefWidth(250);

        Label refLabel = new Label("Référence:");
        refLabel.setStyle("-fx-font-weight: bold;");
        referenceField = new TextField();
        referenceField.setPromptText("Référence");
        referenceField.setPrefWidth(200);

        Label prixLabel = new Label("Prix (DT):");
        prixLabel.setStyle("-fx-font-weight: bold;");
        prixField = new TextField();
        prixField.setPromptText("0.00");
        prixField.setPrefWidth(150);

        Label stockLabel = new Label("Stock:");
        stockLabel.setStyle("-fx-font-weight: bold;");
        stockField = new TextField();
        stockField.setPromptText("0");
        stockField.setPrefWidth(100);

        Label imageLabel = new Label("Image URL:");
        imageLabel.setStyle("-fx-font-weight: bold;");
        imageField = new TextField();
        imageField.setPromptText("URL de l'image");
        imageField.setPrefWidth(400);

        // Ajout à la grille
        grid.add(nomLabel, 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(refLabel, 2, 0);
        grid.add(referenceField, 3, 0);

        grid.add(prixLabel, 0, 1);
        grid.add(prixField, 1, 1);
        grid.add(stockLabel, 2, 1);
        grid.add(stockField, 3, 1);

        grid.add(imageLabel, 0, 2);
        grid.add(imageField, 1, 2, 3, 1);

        // Boutons d'action
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button ajouterBtn = new Button("➕ Ajouter");
        ajouterBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        ajouterBtn.setOnAction(e -> ajouterProduit());

        Button modifierBtn = new Button("✏️ Modifier");
        modifierBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        modifierBtn.setOnAction(e -> modifierProduit());

        Button supprimerBtn = new Button("🗑️ Supprimer");
        supprimerBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        supprimerBtn.setOnAction(e -> {
            Produit selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                supprimerProduit(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un produit à supprimer");
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
            produitList = FXCollections.observableArrayList(produitService.selectAll());
            table.setItems(produitList);

            if (produitList.isEmpty()) {
                System.out.println("Aucun produit trouvé");
            } else {
                System.out.println(produitList.size() + " produits chargés");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private void remplirFormulaire(Produit produit) {
        nomField.setText(produit.getNom());
        referenceField.setText(produit.getReference());
        prixField.setText(String.valueOf(produit.getPrix_achat()));
        stockField.setText(String.valueOf(produit.getStock_dispo()));
        imageField.setText(produit.getImage());
    }

    private void viderFormulaire() {
        nomField.clear();
        referenceField.clear();
        prixField.clear();
        stockField.clear();
        imageField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void ajouterProduit() {
        if (!validerChamps()) return;

        try {
            Produit produit = new Produit(
                    nomField.getText(),
                    referenceField.getText(),
                    Double.parseDouble(prixField.getText()),
                    Integer.parseInt(stockField.getText()),
                    imageField.getText()
            );

            produitService.insertOnePS(produit);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit ajouté avec succès!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    private void modifierProduit() {
        Produit selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un produit à modifier");
            return;
        }

        if (!validerChamps()) return;

        try {
            selected.setNom(nomField.getText());
            selected.setReference(referenceField.getText());
            selected.setPrix_achat(Double.parseDouble(prixField.getText()));
            selected.setStock_dispo(Integer.parseInt(stockField.getText()));
            selected.setImage(imageField.getText());

            produitService.updateOne(selected);
            viderFormulaire();
            chargerDonnees();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit modifié avec succès!");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    private void supprimerProduit(Produit produit) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le produit");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer le produit \"" + produit.getNom() + "\" ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                produitService.deleteOne(produit);
                viderFormulaire();
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit supprimé avec succès!");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private boolean validerChamps() {
        if (nomField.getText().isEmpty() || referenceField.getText().isEmpty() ||
                prixField.getText().isEmpty() || stockField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs obligatoires");
            return false;
        }

        try {
            Double.parseDouble(prixField.getText());
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le prix et le stock doivent être des nombres valides");
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