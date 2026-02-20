package tn.esprit.boussole.gui;

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

public class ProduitController implements Initializable {

    @FXML private TableView<Produit> produitTable;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colReference;
    @FXML private TableColumn<Produit, Double> colPrix;
    @FXML private TableColumn<Produit, Integer> colStock;
    @FXML private TableColumn<Produit, String> colImage;
    @FXML private TableColumn<Produit, Void> colActions;

    // Champs du formulaire
    @FXML private TextField nomField;
    @FXML private TextField referenceField;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private TextField imageField;
    @FXML private ImageView apercuImage;
    @FXML private Button btnParcourir;

    private ProduitService produitService;
    private ObservableList<Produit> produitList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        produitService = new ProduitService();

        // Configuration des colonnes avec édition
        configurerTable();

        // Charger les données
        chargerDonnees();

        // Vider le formulaire au démarrage
        viderFormulaire();

        // Ajouter des listeners pour la validation
        ajouterValidations();
    }

    private void ajouterValidations() {
        // Validation du champ Prix
        prixField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                prixField.setText(oldValue);
            }
        });

        // Validation du champ Stock
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

        // Permettre la sélection de cellules individuelles
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
            Produit produit = event.getRowValue();
            produit.setNom(newValue);
            sauvegarderModification(produit);
        });

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
            Produit produit = event.getRowValue();
            produit.setReference(newValue);
            sauvegarderModification(produit);
        });

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
            Produit produit = event.getRowValue();
            Double newValue = event.getNewValue();
            if (newValue != null && newValue > 0) {
                produit.setPrix_achat(newValue);
                sauvegarderModification(produit);
            } else {
                chargerDonnees();
            }
        });

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
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le stock doit être un nombre entier valide");
                    return null;
                }
            }
        }));
        colStock.setOnEditCommit(event -> {
            Produit produit = event.getRowValue();
            Integer newValue = event.getNewValue();
            if (newValue != null && newValue >= 0) {
                produit.setStock_dispo(newValue);
                sauvegarderModification(produit);
            } else {
                chargerDonnees();
            }
        });

        // Colonne IMAGE (avec affichage réel de l'image)
        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colImage.setCellFactory(param -> new TableCell<Produit, String>() {
            private final ImageView imageView = new ImageView();
            private final Label errorLabel = new Label("❌");

            {
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 20px;");
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);

                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        Image image = null;

                        // Essayer de charger l'image depuis différents endroits
                        if (imagePath.startsWith("http") || imagePath.startsWith("file:")) {
                            image = new Image(imagePath, 50, 50, true, true);
                        } else {
                            File file = new File(imagePath);
                            if (file.exists()) {
                                image = new Image(file.toURI().toString(), 50, 50, true, true);
                            } else {
                                // Image par défaut si non trouvée
                                setGraphic(null);
                                setText("📷");
                                return;
                            }
                        }

                        if (image != null && !image.isError()) {
                            imageView.setImage(image);
                            setGraphic(imageView);
                            setText(null);
                        } else {
                            setGraphic(errorLabel);
                            setText(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                        setText("📷");
                    }
                }
            }
        });

        // Colonne Actions (bouton Supprimer)
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(deleteBtn);

            {
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 3;");

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
    }

    @FXML
    private void handleParcourirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");

        // Filtrer pour n'afficher que les images
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        fileChooser.getExtensionFilters().add(imageFilter);

        // Montrer la boîte de dialogue
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            // Afficher le chemin absolu dans le champ
            imageField.setText(selectedFile.getAbsolutePath());

            // Afficher l'aperçu
            try {
                Image image = new Image(selectedFile.toURI().toString(), 100, 100, true, true);
                apercuImage.setImage(image);
            } catch (Exception e) {
                // Ignorer
            }
        }
    }

    private void sauvegarderModification(Produit produit) {
        try {
            produitService.updateOne(produit);
            System.out.println("Produit modifié: " + produit.getId());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification: " + e.getMessage());
            e.printStackTrace();
            chargerDonnees();
        }
    }

    private void chargerDonnees() {
        try {
            produitList = FXCollections.observableArrayList(produitService.selectAll());
            produitTable.setItems(produitList);
            System.out.println("Produits chargés: " + produitList.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjouter() {
        if (!validerChampsAjout()) return;

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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        chargerDonnees();
    }

    @FXML
    private void handleEffacer() {
        viderFormulaire();
    }

    private void viderFormulaire() {
        nomField.clear();
        referenceField.clear();
        prixField.clear();
        stockField.clear();
        imageField.clear();
        apercuImage.setImage(null);
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
                chargerDonnees();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit supprimé avec succès!");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
            showAlert(Alert.AlertType.WARNING, "Attention", "Le stock doit être un nombre entier valide");
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