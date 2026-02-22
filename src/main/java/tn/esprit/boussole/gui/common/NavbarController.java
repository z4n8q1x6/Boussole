package tn.esprit.boussole.gui.common;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class NavbarController implements Initializable {

    @FXML private Label lblPanelType;
    @FXML private Label lblPageTitle;
    @FXML private Label lblUsername;
    @FXML private StackPane contentArea;
    @FXML private VBox menuContainer;
    @FXML private Button btnLogout;

    private String userType; // "Franchise" ou "Siege"
    private int userId = 1; // À remplacer par l'ID de session

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Sera appelé après setUserType
    }

    public void setUserType(String type) {
        this.userType = type;
        lblPanelType.setText(type.equals("Siege") ? "PANEL SIÈGE" : "PANEL FRANCHISE");
        lblUsername.setText(type.equals("Siege") ? "Admin Siège" : "Franchise Tunis");
        chargerMenu();
        chargerAccueil();
    }

    private void chargerMenu() {
        menuContainer.getChildren().clear();

        // Titre PRINCIPAL
        Label principalLabel = new Label("PRINCIPAL");
        principalLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
        principalLabel.setPadding(new Insets(5, 0, 5, 10));
        menuContainer.getChildren().add(principalLabel);

        if ("Siege".equals(userType)) {
            // ========== MENU POUR LE SIÈGE ==========
            ajouterBoutonMenu("/images/dashboard.png", "Tableau de bord", "dashboard", true);
            ajouterBoutonMenu("/images/products.png", "Gestion Produits", "gestionCatalogue", true);
            ajouterBoutonMenu("/images/orders.png", "Commandes reçues", "commandesRecues", true);
            ajouterBoutonMenu("/images/map.png", "Carte franchises", "carteFranchises", true); // NOUVEAU BOUTON

            // Section GESTION
            Label gestionLabel = new Label("📌 GESTION");
            gestionLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
            gestionLabel.setPadding(new Insets(15, 0, 5, 10));
            menuContainer.getChildren().add(gestionLabel);

            ajouterBoutonMenu(null, "Franchises", "franchises", false);
            ajouterBoutonMenu(null, "Statistiques", "stats", false);

        } else {
            // ========== MENU POUR LA FRANCHISE ==========
            ajouterBoutonMenu("/images/dashboard.png", "Tableau de bord", "dashboard", true);
            ajouterBoutonMenu("/images/marketplace.png", "Marketplace", "catalogue", true);
            ajouterBoutonMenu("/images/cart.png", "Mon panier", "panier", true);
            ajouterBoutonMenu("/images/orders.png", "Mes commandes", "mesCommandes", true);

            // Section ACHATS
            Label achatsLabel = new Label("📌 ACHATS");
            achatsLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
            achatsLabel.setPadding(new Insets(15, 0, 5, 10));
            menuContainer.getChildren().add(achatsLabel);

            ajouterBoutonMenu(null, "Produits favoris", "favoris", false);
            ajouterBoutonMenu(null, "Historique", "historique", false);
        }

        // Section FINANCES (commune)
        Label financesLabel = new Label("📌 FINANCES");
        financesLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
        financesLabel.setPadding(new Insets(15, 0, 5, 10));
        menuContainer.getChildren().add(financesLabel);

        ajouterBoutonMenu(null, "Bilan", "bilan", false);
        ajouterBoutonMenu(null, "Budget", "budget", false);
    }

    private void ajouterBoutonMenu(String imagePath, String texte, String actionId, boolean actif) {
        Button btn = new Button(texte);
        btn.getStyleClass().add("menu-button");
        btn.setPrefHeight(45);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(javafx.geometry.Pos.BASELINE_LEFT);

        // Ajouter l'image si le chemin est fourni
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
                icon.setFitHeight(20);
                icon.setFitWidth(20);
                icon.setPreserveRatio(true);
                btn.setGraphic(icon);
                btn.setGraphicTextGap(15);
            } catch (Exception e) {
                System.err.println("⚠️ Image non trouvée: " + imagePath);
                // Ne pas planter, continuer sans image
            }
        }

        if (actif) {
            btn.setOnAction(e -> {
                // Enlever la classe active de tous les boutons
                menuContainer.getChildren().filtered(node -> node instanceof Button)
                        .forEach(b -> ((Button) b).getStyleClass().remove("active"));
                // Ajouter la classe active à ce bouton
                btn.getStyleClass().add("active");
                // Appeler la méthode correspondante
                handleMenuAction(actionId);
            });
        } else {
            btn.setDisable(true);
            btn.setStyle("-fx-opacity: 0.5;");
            btn.setTooltip(new Tooltip("Bientôt disponible"));
        }

        menuContainer.getChildren().add(btn);
    }

    private void handleMenuAction(String actionId) {
        String basePath = "/tn/esprit/boussole/views/";
        String vuePath = "";
        String titre = "";

        if ("Siege".equals(userType)) {
            // ========== VUES POUR LE SIÈGE ==========
            switch (actionId) {
                case "dashboard":
                    vuePath = "siege/SiegeMainView.fxml";
                    titre = "Tableau de bord";
                    break;
                case "gestionCatalogue":
                    vuePath = "siege/GestionCatalogueView.fxml";
                    titre = "Gestion du catalogue";
                    break;
                case "commandesRecues":
                    vuePath = "siege/CommandesRecuesView.fxml";
                    titre = "Commandes reçues";
                    break;
                case "carteFranchises":
                    // Pour la carte, on ouvre une nouvelle fenêtre au lieu de charger dans contentArea
                    ouvrirCarteFranchises();
                    return; // Ne pas continuer avec chargerVue
                default:
                    return;
            }
        } else {
            // ========== VUES POUR LA FRANCHISE ==========
            switch (actionId) {
                case "dashboard":
                    vuePath = "franchise/FranchiseMainView.fxml";
                    titre = "Tableau de bord";
                    break;
                case "catalogue":
                    vuePath = "franchise/CatalogueView.fxml";
                    titre = "Marketplace";
                    break;
                case "panier":
                    vuePath = "franchise/PanierView.fxml";
                    titre = "Mon panier";
                    break;
                case "mesCommandes":
                    vuePath = "franchise/MesCommandesView.fxml";
                    titre = "Mes commandes";
                    break;
                default:
                    return;
            }
        }

        chargerVue(basePath + vuePath, titre);
    }

    private void chargerVue(String fxmlPath, String titre) {
        try {
            Parent vue = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(vue);
            lblPageTitle.setText(titre);
            System.out.println("✅ Chargé: " + fxmlPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur chargement: " + fxmlPath);
        }
    }

    private void ouvrirCarteFranchises() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/siege/CarteFranchisesView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("🗺️ Carte des franchises - Boussole");
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();

            System.out.println("✅ Carte des franchises ouverte dans une nouvelle fenêtre");

        } catch (IOException e) {
            System.err.println("❌ Erreur ouverture carte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chargerAccueil() {
        if ("Siege".equals(userType)) {
            chargerVue("/tn/esprit/boussole/views/siege/SiegeMainView.fxml", "Tableau de bord");
        } else {
            chargerVue("/tn/esprit/boussole/views/franchise/FranchiseMainView.fxml", "Tableau de bord");
        }
    }
}