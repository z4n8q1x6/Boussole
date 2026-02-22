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

        // Vérifier que tous les champs sont injectés
        verifierChamps();

        // Initialiser les filtres
        if (filterStockCombo != null) {
            filterStockCombo.getItems().addAll("Tous", "En stock", "Rupture de stock", "Stock faible (<5)");
            filterStockCombo.setValue("Tous");
            filterStockCombo.setOnAction(e -> filtrerProduits());
        }

        // Recherche en temps réel
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrerProduits());
        }

        // Configuration du tableau
        configurerTable();

        // Charger les données
        chargerDonnees();

        // Ajouter les validations APRÈS que tous les champs soient initialisés
        ajouterValidations();
    }

    private void verifierChamps() {
        System.out.println("=== Vérification des champs FXML ===");
        System.out.println("nomField: " + (nomField != null ? "OK" : "NULL"));
        System.out.println("referenceField: " + (referenceField != null ? "OK" : "NULL"));
        System.out.println("prixField: " + (prixField != null ? "OK" : "NULL"));
        System.out.println("stockField: " + (stockField != null ? "OK" : "NULL"));
        System.out.println("imageField: " + (imageField != null ? "OK" : "NULL"));
        System.out.println("searchField: " + (searchField != null ? "OK" : "NULL"));
        System.out.println("filterStockCombo: " + (filterStockCombo != null ? "OK" : "NULL"));
        System.out.println("totalProduitsLabel: " + (totalProduitsLabel != null ? "OK" : "NULL"));
        System.out.println("enStockLabel: " + (enStockLabel != null ? "OK" : "NULL"));
        System.out.println("ruptureLabel: " + (ruptureLabel != null ? "OK" : "NULL"));
        System.out.println("valeurStockLabel: " + (valeurStockLabel != null ? "OK" : "NULL"));
        System.out.println("produitTable: " + (produitTable != null ? "OK" : "NULL"));
        System.out.println("================================");
    }

    private void ajouterValidations() {
        // Validation du champ Prix (nombre décimal)
        if (prixField != null) {
            prixField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*\\.?\\d*")) {
                    prixField.setText(oldValue);
                }
            });
        } else {
            System.err.println("⚠️ prixField est null - validation non ajoutée");
        }

        // Validation du champ Stock (nombre entier)
        if (stockField != null) {
            stockField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    stockField.setText(oldValue);
                }
            });
        } else {
            System.err.println("⚠️ stockField est null - validation non ajoutée");
        }

        // Validation du champ Nom (pas de chiffres)
        if (nomField != null) {
            nomField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.matches(".*\\d.*")) {
                    showAlert(Alert.AlertType.WARNING, "Attention", "Le nom ne doit pas contenir de chiffres");
                    nomField.setText(oldValue);
                }
            });
        } else {
            System.err.println("⚠️ nomField est null - validation non ajoutée");
        }
    }

    private void configurerTable() {
        if (produitTable == null) {
            System.err.println("⚠️ produitTable est null !");
            return;
        }

        // Rendre le tableau éditable
        produitTable.setEditable(true);
        produitTable.getSelectionModel().setCellSelectionEnabled(true);

        // Colonne NOM
        if (colNom != null) {
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
        } else {
            System.err.println("⚠️ colNom est null");
        }

        // Colonne RÉFÉRENCE
        if (colReference != null) {
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
        } else {
            System.err.println("⚠️ colReference est null");
        }

        // Colonne PRIX
        if (colPrix != null) {
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
        } else {
            System.err.println("⚠️ colPrix est null");
        }

        // Colonne STOCK
        if (colStock != null) {
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
        } else {
            System.err.println("⚠️ colStock est null");
        }

        // Colonne IMAGE (avec aperçu)
        if (colImage != null) {
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
        } else {
            System.err.println("⚠️ colImage est null");
        }

        // Colonne ACTIONS (Modifier/Supprimer)
        if (colActions != null) {
            colActions.setCellFactory(param -> new TableCell<>() {
                private final Button editBtn = new Button();
                private final Button deleteBtn = new Button();
                private final HBox pane = new HBox(5, editBtn, deleteBtn);

                {
                    // Configurer le bouton Modifier avec image
                    try {
                        ImageView editIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/edit.png")));
                        editIcon.setFitHeight(16);
                        editIcon.setFitWidth(16);
                        editIcon.setPreserveRatio(true);
                        editBtn.setGraphic(editIcon);
                    } catch (Exception e) {
                        editBtn.setText("✏️");
                    }
                    editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                    editBtn.setTooltip(new Tooltip("Modifier"));

                    // Configurer le bouton Supprimer avec image
                    try {
                        ImageView deleteIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/delete.png")));
                        deleteIcon.setFitHeight(16);
                        deleteIcon.setFitWidth(16);
                        deleteIcon.setPreserveRatio(true);
                        deleteBtn.setGraphic(deleteIcon);
                    } catch (Exception e) {
                        deleteBtn.setText("🗑️");
                    }
                    deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");
                    deleteBtn.setTooltip(new Tooltip("Supprimer"));

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
        } else {
            System.err.println("⚠️ colActions est null");
        }
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
        if (nomField != null) nomField.setText(produit.getNom());
        if (referenceField != null) referenceField.setText(produit.getReference());
        if (prixField != null) prixField.setText(String.valueOf(produit.getPrix_achat()));
        if (stockField != null) stockField.setText(String.valueOf(produit.getStock_dispo()));
        if (imageField != null) imageField.setText(produit.getImage());

        try {
            if (produit.getImage() != null && !produit.getImage().isEmpty() && apercuImage != null) {
                Image image = new Image(produit.getImage(), 100, 100, true, true);
                apercuImage.setImage(image);
            }
        } catch (Exception e) {
            if (apercuImage != null) apercuImage.setImage(null);
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
            if (produitTable != null) {
                produitTable.setItems(filteredList);
            }
            mettreAJourStatistiques();
            System.out.println("✅ " + produitList.size() + " produits chargés");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filtrerProduits() {
        if (produitList == null) return;

        String recherche = searchField != null ? searchField.getText().toLowerCase() : "";
        String filtreStock = filterStockCombo != null ? filterStockCombo.getValue() : "Tous";

        filteredList = FXCollections.observableArrayList();

        for (Produit p : produitList) {
            // Filtre texte
            boolean correspondTexte = recherche.isEmpty() ||
                    (p.getNom() != null && p.getNom().toLowerCase().contains(recherche)) ||
                    (p.getReference() != null && p.getReference().toLowerCase().contains(recherche));

            if (!correspondTexte) continue;

            // Filtre stock
            if (filtreStock != null) {
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
                    default:
                        filteredList.add(p);
                }
            } else {
                filteredList.add(p);
            }
        }

        if (produitTable != null) {
            produitTable.setItems(filteredList);
        }
    }

    private void mettreAJourStatistiques() {
        try {
            int total = produitService.countAll();
            int enStock = produitService.countEnStock();
            int rupture = produitService.countRupture();
            double valeur = produitService.getValeurTotaleStock();

            if (totalProduitsLabel != null) totalProduitsLabel.setText(String.valueOf(total));
            if (enStockLabel != null) enStockLabel.setText(String.valueOf(enStock));
            if (ruptureLabel != null) ruptureLabel.setText(String.valueOf(rupture));
            if (valeurStockLabel != null) valeurStockLabel.setText(String.format("%.2f DT", valeur));

            System.out.println("📊 Statistiques - Total: " + total + ", En stock: " + enStock + ", Rupture: " + rupture);

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
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le produit");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer le produit \"" + produit.getNom() + "\" ?");

            // Styliser l'alerte
            DialogPane dialogPane = confirm.getDialogPane();
            dialogPane.setStyle("-fx-background-color: #1E293B;");
            dialogPane.lookup(".content.label").setStyle("-fx-text-fill: white;");

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
    private void handleEffacer() {
        viderFormulaire();
    }

    @FXML
    private void handleRefresh() {
        chargerDonnees();
        if (searchField != null) searchField.clear();
        if (filterStockCombo != null) filterStockCombo.setValue("Tous");
    }

    private void viderFormulaire() {
        if (nomField != null) nomField.clear();
        if (referenceField != null) referenceField.clear();
        if (prixField != null) prixField.clear();
        if (stockField != null) stockField.clear();
        if (imageField != null) imageField.clear();
        if (apercuImage != null) apercuImage.setImage(null);
    }

    private boolean validerChampsAjout() {
        // Validation Nom
        if (nomField == null || nomField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le nom est obligatoire");
            return false;
        }
        String nom = nomField.getText();
        if (nom.matches(".*\\d.*")) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le nom ne doit pas contenir de chiffres");
            return false;
        }

        // Validation Référence
        if (referenceField == null || referenceField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La référence est obligatoire");
            return false;
        }
        String ref = referenceField.getText();
        if (ref.matches("^0+$")) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La référence ne doit pas être que des zéros");
            return false;
        }

        // Validation Prix
        if (prixField == null || prixField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le prix est obligatoire");
            return false;
        }
        String prixStr = prixField.getText();
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
        if (stockField == null || stockField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Le stock est obligatoire");
            return false;
        }
        String stockStr = stockField.getText();
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

        // Styliser l'alerte
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1E293B;");
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white;");
        }

        alert.showAndWait();
    }
}