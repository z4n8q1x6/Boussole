package tn.esprit.boussole.gui.siege;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.services.ProduitService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class GestionCatalogueController implements Initializable {

    @FXML private TableView<Produit> produitTable;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colReference;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, String> colImage;
    @FXML private TableColumn<Produit, Void> colActions;

    @FXML private TextField nomField;
    @FXML private TextField referenceField;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private TextField imageField;
    @FXML private ImageView apercuImage;
    @FXML private Button btnParcourir;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStockCombo;
    @FXML private Label totalProduitsLabel;
    @FXML private Label enStockLabel;
    @FXML private Label ruptureLabel;
    @FXML private Label valeurStockLabel;

    private ProduitService produitService;
    private ObservableList<Produit> produitList;
    private ObservableList<Produit> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        produitService = new ProduitService();

        // Initialiser les filtres
        filterStockCombo.getItems().addAll("Tous", "En stock", "Rupture de stock", "Stock faible (<5)");
        filterStockCombo.setValue("Tous");
        filterStockCombo.setOnAction(e -> filtrerProduits());

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrerProduits());

        // Configuration du tableau
        configurerTable();

        // Charger les données
        chargerDonnees();

        // Validation en temps réel
        ajouterValidations();
    }

    private void ajouterValidations() {
        // Validation du champ Prix (nombre décimal)
        prixField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                prixField.setText(oldValue);
            }
        });

        // Validation du champ Stock (nombre entier)
        stockField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                stockField.setText(oldValue);
            }
        });

        // Validation du champ Nom (pas de chiffres)
        nomField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches(".*\\d.*")) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le nom ne doit pas contenir de chiffres");
                nomField.setText(oldValue);
            }
        });
    }

    private void configurerTable() {
        // Rendre le tableau éditable
        produitTable.setEditable(true);
        produitTable.getSelectionModel().setCellSelectionEnabled(true);

        // Colonne NOM
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setCellFactory(TextFieldTableCell.forTableColumn());
        colNom.setOnEditCommit(event -> {
            String newValue = event.getNewValue();
            if (newValue.matches(".*\\d.*")) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le nom ne doit pas contenir de chiffres");
                chargerDonnees();
                return;
            }
            Produit p = event.getRowValue();
            p.setNom(newValue);
            sauvegarderModification(p);
        });
        colNom.setPrefWidth(150);

        // Colonne RÉFÉRENCE
        colReference.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colReference.setCellFactory(TextFieldTableCell.forTableColumn());
        colReference.setOnEditCommit(event -> {
            String newValue = event.getNewValue();
            if (newValue.matches("^0+$")) {
                showAlert(Alert.AlertType.WARNING, "Attention", "La référence ne doit pas être que des zéros");
                chargerDonnees();
                return;
            }
            Produit p = event.getRowValue();
            p.setReference(newValue);
            sauvegarderModification(p);
        });
        colReference.setPrefWidth(120);

        // Colonne PRIX
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_achat"));
        colPrix.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter() {
            @Override
            public Double fromString(String value) {
                try {
                    double prix = Double.parseDouble(value);
                    if (prix <= 0) {
                        showAlert(Alert.AlertType.WARNING, "Attention", "Le prix doit être supérieur à 0");
                        return null;
                    }
                    return prix;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le prix doit être un nombre valide");
                    return null;
                }
            }
        }));
        colPrix.setOnEditCommit(event -> {
            Produit p = event.getRowValue();
            Double newValue = event.getNewValue();
            if (newValue != null && newValue > 0) {
                p.setPrix_achat(newValue);
                sauvegarderModification(p);
            }
        });
        colPrix.setPrefWidth(100);

        // Colonne STOCK
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock_dispo"));
        colStock.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                try {
                    int stock = Integer.parseInt(value);
                    if (stock < 0) {
                        showAlert(Alert.AlertType.WARNING, "Attention", "Le stock ne peut pas être négatif");
                        return null;
                    }
                    return stock;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le stock doit être un nombre entier");
                    return null;
                }
            }
        }));
        colStock.setOnEditCommit(event -> {
            Produit p = event.getRowValue();
            Integer newValue = event.getNewValue();
            if (newValue != null && newValue >= 0) {
                p.setStock_dispo(newValue);
                sauvegarderModification(p);
            }
        });
        colStock.setPrefWidth(80);

        // Colonne IMAGE (avec aperçu)
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colImage.setCellFactory(param -> new TableCell<Produit, String>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);

                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        Image image = new Image(imagePath, 40, 40, true, true);
                        imageView.setImage(image);
                        setGraphic(imageView);
                        setText(null);
                    } catch (Exception e) {
                        setGraphic(null);
                        setText("📷");
                    }
                }
            }
        });
        colImage.setPrefWidth(80);

        // Colonne ACTIONS (Modifier/Supprimer)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");

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
        colActions.setPrefWidth(100);
    }

    @FXML
    private void handleParcourirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            String path = selectedFile.toURI().toString();
            imageField.setText(path);

            try {
                Image image = new Image(path, 100, 100, true, true);
                apercuImage.setImage(image);
            } catch (Exception e) {
                // Ignorer
            }
        }
    }

    private void remplirFormulaire(Produit produit) {
        nomField.setText(produit.getNom());
        referenceField.setText(produit.getReference());
        prixField.setText(String.valueOf(produit.getPrix_achat()));
        stockField.setText(String.valueOf(produit.getStock_dispo()));
        imageField.setText(produit.getImage());

        try {
            if (produit.getImage() != null && !produit.getImage().isEmpty()) {
                Image image = new Image(produit.getImage(), 100, 100, true, true);
                apercuImage.setImage(image);
            }
        } catch (Exception e) {
            apercuImage.setImage(null);
        }
    }

    private void sauvegarderModification(Produit p) {
        try {
            produitService.updateOne(p);
            chargerDonnees(); // Recharger pour mettre à jour les stats
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerDonnees() {
        try {
            produitList = FXCollections.observableArrayList(produitService.selectAll());
            filteredList = FXCollections.observableArrayList(produitList);
            produitTable.setItems(filteredList);
            mettreAJourStatistiques();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filtrerProduits() {
        String recherche = searchField.getText().toLowerCase();
        String filtreStock = filterStockCombo.getValue();

        filteredList = FXCollections.observableArrayList();

        for (Produit p : produitList) {
            // Filtre texte
            boolean correspondTexte = recherche.isEmpty() ||
                    p.getNom().toLowerCase().contains(recherche) ||
                    p.getReference().toLowerCase().contains(recherche);

            if (!correspondTexte) continue;

            // Filtre stock
            switch (filtreStock) {
                case "Tous":
                    filteredList.add(p);
                    break;
                case "En stock":
                    if (p.getStock_dispo() > 0) filteredList.add(p);
                    break;
                case "Rupture de stock":
                    if (p.getStock_dispo() == 0) filteredList.add(p);
                    break;
                case "Stock faible (<5)":
                    if (p.getStock_dispo() > 0 && p.getStock_dispo() < 5) filteredList.add(p);
                    break;
            }
        }

        produitTable.setItems(filteredList);
    }

    private void mettreAJourStatistiques() {
        try {
            int total = produitService.countAll();
            int enStock = produitService.countEnStock();
            int rupture = produitService.countRupture();
            double valeur = produitService.getValeurTotaleStock();

            totalProduitsLabel.setText(String.valueOf(total));
            enStockLabel.setText(String.valueOf(enStock));
            ruptureLabel.setText(String.valueOf(rupture));
            valeurStockLabel.setText(String.format("%.2f DT", valeur));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjouter() {
        if (!validerChampsAjout()) return;

        try {
            // Vérifier si la référence existe déjà
            if (produitService.referenceExists(referenceField.getText())) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Cette référence existe déjà !");
                return;
            }

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
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit ajouté avec succès !");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void supprimerProduit(Produit produit) {
        try {
            // Vérifier si le produit est utilisé dans des commandes
            // Cette vérification sera faite plus tard avec LigneCommandeService

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le produit");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer le produit \"" + produit.getNom() + "\" ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                produitService.deleteOne(produit);
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit supprimé !");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleModifier() {
        // La modification se fait directement dans le tableau
        // Cette méthode peut être utilisée pour valider les modifications en cours
        showAlert(Alert.AlertType.INFORMATION, "Info", "Double-cliquez sur une cellule pour modifier");
    }

    @FXML
    private void handleEffacer() {
        viderFormulaire();
    }

    @FXML
    private void handleRefresh() {
        chargerDonnees();
    }

    private void viderFormulaire() {
        nomField.clear();
        referenceField.clear();
        prixField.clear();
        stockField.clear();
        imageField.clear();
        apercuImage.setImage(null);
    }

    private boolean validerChampsAjout() {
        // Validation Nom
        String nom = nomField.getText();
        if (nom.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le nom est obligatoire");
            return false;
        }
        if (nom.matches(".*\\d.*")) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le nom ne doit pas contenir de chiffres");
            return false;
        }

        // Validation Référence
        String ref = referenceField.getText();
        if (ref.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La référence est obligatoire");
            return false;
        }
        if (ref.matches("^0+$")) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La référence ne doit pas être que des zéros");
            return false;
        }

        // Validation Prix
        String prixStr = prixField.getText();
        if (prixStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le prix est obligatoire");
            return false;
        }
        try {
            double prix = Double.parseDouble(prixStr);
            if (prix <= 0) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le prix doit être supérieur à 0");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le prix doit être un nombre valide");
            return false;
        }

        // Validation Stock
        String stockStr = stockField.getText();
        if (stockStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le stock est obligatoire");
            return false;
        }
        try {
            int stock = Integer.parseInt(stockStr);
            if (stock < 0) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Le stock ne peut pas être négatif");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le stock doit être un nombre entier");
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