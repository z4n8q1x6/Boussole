package tn.esprit.boussole.gui.franchise;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.service.ProduitService;
import tn.esprit.boussole.utils.PanierManager;
import tn.esprit.boussole.utils.NotificationManager;
import tn.esprit.boussole.utils.UserManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class CatalogueController implements Initializable {

    @FXML private GridPane catalogueGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label resultCountLabel;

    private ProduitService produitService;
    private ObservableList<Produit> produitList;
    private ObservableList<Produit> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        produitService = new ProduitService();

        // Vérifier que l'utilisateur a une franchise valide (optionnel - le catalogue est public)
        int franchiseId = UserManager.getCurrentUserFranchiseId();
        if (!UserManager.isValidFranchiseId(franchiseId)) {
            System.out.println("⚠️ Utilisateur sans franchise - mode consultation uniquement");
        }

        // Initialiser les options de tri
        sortCombo.getItems().addAll(
                "Prix croissant",
                "Prix décroissant",
                "Nom A-Z",
                "Nom Z-A",
                "Stock disponible"
        );
        sortCombo.setValue("Prix croissant");

        // Ajouter les listeners
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filtrerEtTrier());
        sortCombo.setOnAction(e -> filtrerEtTrier());

        // Charger les produits
        chargerProduits();
    }

    private void chargerProduits() {
        try {
            produitList = FXCollections.observableArrayList(produitService.selectAll(null));
            filteredList = FXCollections.observableArrayList(produitList);
            filtrerEtTrier();
            System.out.println("✅ " + produitList.size() + " produits chargés dans le marketplace");
        } catch (SQLException e) {
            // Utiliser System.err au lieu de NotificationManager car la scène n'est pas encore prête
            System.err.println("Erreur lors du chargement du catalogue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filtrerEtTrier() {
        String recherche = searchField.getText().toLowerCase();
        String tri = sortCombo.getValue();

        // Filtrer
        filteredList = FXCollections.observableArrayList();

        for (Produit p : produitList) {
            boolean correspond = recherche.isEmpty() ||
                    p.getNom().toLowerCase().contains(recherche) ||
                    (p.getReference() != null && p.getReference().toLowerCase().contains(recherche));

            if (correspond) {
                filteredList.add(p);
            }
        }

        // Trier
        if ("Prix croissant".equals(tri)) {
            filteredList.sort((p1, p2) -> Double.compare(p1.getPrix_achat(), p2.getPrix_achat()));
        } else if ("Prix décroissant".equals(tri)) {
            filteredList.sort((p1, p2) -> Double.compare(p2.getPrix_achat(), p1.getPrix_achat()));
        } else if ("Nom A-Z".equals(tri)) {
            filteredList.sort((p1, p2) -> p1.getNom().compareTo(p2.getNom()));
        } else if ("Nom Z-A".equals(tri)) {
            filteredList.sort((p1, p2) -> p2.getNom().compareTo(p1.getNom()));
        } else if ("Stock disponible".equals(tri)) {
            filteredList.sort((p1, p2) -> Integer.compare(p2.getStock_dispo(), p1.getStock_dispo()));
        }

        // Mettre à jour le compteur
        resultCountLabel.setText(filteredList.size() + " produit(s) trouvé(s)");

        // Afficher
        afficherCatalogue();
    }

    private void afficherCatalogue() {
        catalogueGrid.getChildren().clear();

        int colonne = 0;
        int ligne = 0;
        int colonnesMax = 3; // 3 produits par ligne

        for (Produit p : filteredList) {
            VBox card = creerCarteProduit(p);
            catalogueGrid.add(card, colonne, ligne);
            GridPane.setMargin(card, new Insets(10));

            colonne++;
            if (colonne >= colonnesMax) {
                colonne = 0;
                ligne++;
            }
        }
    }

    private VBox creerCarteProduit(Produit p) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #0C0F1A; -fx-background-radius: 12; -fx-padding: 15; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        card.setPrefWidth(280);
        card.setPrefHeight(350);

        // Image du produit
        ImageView imageView = new ImageView();
        imageView.setFitHeight(120);
        imageView.setFitWidth(120);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8;");

        // Charger l'image du produit
        chargerImageProduit(imageView, p);

        // Nom du produit
        Label nomLabel = new Label(p.getNom());
        nomLabel.setStyle("-fx-text-fill: #E8EDF5; -fx-font-size: 16px; -fx-font-weight: bold;");
        nomLabel.setWrapText(true);

        // Référence
        Label refLabel = new Label("Réf: " + p.getReference());
        refLabel.setStyle("-fx-text-fill: #8892A4; -fx-font-size: 12px;");

        // Prix
        Label prixLabel = new Label(String.format("%.2f DT", p.getPrix_achat()));
        prixLabel.setStyle("-fx-text-fill: #00E5CC; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Stock
        Label stockLabel = new Label();
        if (p.getStock_dispo() > 0) {
            if (p.getStock_dispo() < 5) {
                stockLabel.setText("⚠️ Stock faible: " + p.getStock_dispo());
                stockLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
            } else {
                stockLabel.setText("✅ En stock: " + p.getStock_dispo());
                stockLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
            }
        } else {
            stockLabel.setText("❌ Rupture de stock");
            stockLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        }

        // Bouton Ajouter au panier
        Button ajouterBtn = new Button("Ajouter au panier");
        ajouterBtn.setPrefWidth(200);
        ajouterBtn.setPrefHeight(35);

        if (p.getStock_dispo() <= 0) {
            ajouterBtn.setDisable(true);
            ajouterBtn.setText("Rupture de stock");
            ajouterBtn.setStyle("-fx-background-color: #3D1A1A; -fx-text-fill: #F43F5E; -fx-font-weight: bold; -fx-border-color: rgba(244,63,94,0.3); -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: default;");
        } else {
            ajouterBtn.setStyle("-fx-background-color: #00E5CC; -fx-text-fill: #06080F; -fx-font-weight: bold; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
            ajouterBtn.setOnAction(e -> ajouterAuPanier(p));
        }

        card.getChildren().addAll(imageView, nomLabel, refLabel, prixLabel, stockLabel, ajouterBtn);
        return card;
    }

    private void chargerImageProduit(ImageView imageView, Produit p) {
        try {
            if (p.getImage() != null && !p.getImage().isEmpty()) {
                String imagePath = p.getImage();
                Image img = null;

                // Essayer de charger l'image
                if (imagePath.startsWith("http")) {
                    img = new Image(imagePath, 120, 120, true, true);
                } else if (imagePath.startsWith("file:")) {
                    img = new Image(imagePath, 120, 120, true, true);
                } else {
                    java.io.File file = new java.io.File(imagePath);
                    if (file.exists()) {
                        img = new Image(file.toURI().toString(), 120, 120, true, true);
                    }
                }

                if (img != null && !img.isError()) {
                    imageView.setImage(img);
                }
            }
        } catch (Exception e) {
            // Ignorer, l'image restera grise
        }
    }

    private void ajouterAuPanier(Produit p) {
        // Vérifier que l'utilisateur a une franchise
        int franchiseId = UserManager.getCurrentUserFranchiseId();
        if (!UserManager.isValidFranchiseId(franchiseId)) {
            Platform.runLater(() -> NotificationManager.show(
                    catalogueGrid.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur de session",
                    "Vous devez être connecté avec un compte entreprise pour ajouter au panier."
            ));
            return;
        }

        // Demander la quantité
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Ajouter au panier");
        dialog.setHeaderText("Ajouter " + p.getNom() + " au panier");
        dialog.setContentText("Quantité:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int quantite = Integer.parseInt(result.get());

                if (quantite <= 0) {
                    Platform.runLater(() -> NotificationManager.show(
                            catalogueGrid.getScene().getWindow(),
                            NotificationManager.Type.WARNING,
                            "Quantité invalide",
                            "La quantité doit être supérieure à 0."
                    ));
                    return;
                }

                if (quantite > p.getStock_dispo()) {
                    Platform.runLater(() -> NotificationManager.show(
                            catalogueGrid.getScene().getWindow(),
                            NotificationManager.Type.WARNING,
                            "Stock insuffisant",
                            "Stock disponible: " + p.getStock_dispo()
                    ));
                    return;
                }

                PanierManager.getInstance().ajouterProduit(p, quantite);

                Platform.runLater(() -> NotificationManager.show(
                        catalogueGrid.getScene().getWindow(),
                        NotificationManager.Type.SUCCESS,
                        "Ajouté au panier",
                        "✅ " + p.getNom() + " (x" + quantite + ") a été ajouté au panier !"
                ));

            } catch (NumberFormatException e) {
                Platform.runLater(() -> NotificationManager.show(
                        catalogueGrid.getScene().getWindow(),
                        NotificationManager.Type.WARNING,
                        "Quantité invalide",
                        "Veuillez entrer un nombre valide."
                ));
            }
        }
    }

    @FXML
    private void handleRefresh() {
        chargerProduits();
    }
}