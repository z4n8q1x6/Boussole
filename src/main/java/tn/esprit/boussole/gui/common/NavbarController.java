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
    @FXML private Label lblPageSubtitle;
    @FXML private Label lblUsername;
    @FXML private Label lblAvatar;
    @FXML private StackPane contentArea;
    @FXML private VBox menuContainer;
    @FXML private Button btnLogout;

    private String userType; // "Franchise" ou "Siege"
    private int userId = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Sera appelé après setUserType
    }

    public void setUserType(String type) {
        this.userType = type;

        // Mettre à jour l'affichage
        if (lblPanelType != null) {
            lblPanelType.setText(type.equals("Siege") ? "SIÈGE" : "FRANCHISE");
        }

        // Avatar avec initiales
        String initiales = type.equals("Siege") ? "A" : "F";
        if (lblAvatar != null) {
            lblAvatar.setText(initiales);
        }

        // Sous-titre dynamique
        if (lblPageSubtitle != null) {
            lblPageSubtitle.setText(type.equals("Siege") ?
                    "Gérez votre catalogue et les commandes" :
                    "Achetez vos produits et suivez vos commandes");
        }

        if (lblUsername != null) {
            lblUsername.setText(type.equals("Siege") ? "Admin Siège" : "Franchise Tunis");
        }

        chargerMenu();
        chargerAccueil();
    }

    private void chargerMenu() {
        menuContainer.getChildren().clear();

        // Titre PRINCIPAL
        Label principalLabel = new Label("PRINCIPAL");
        principalLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 25 0 8 18; -fx-letter-spacing: 1.5;");
        menuContainer.getChildren().add(principalLabel);

        if ("Siege".equals(userType)) {
            // ========== MENU POUR LE SIÈGE ==========
            ajouterBoutonMenu("/images/dashboard.png", "Tableau de bord", "dashboard", true); // désactivé par manque d'image
            ajouterBoutonMenu("/images/products.png", "Gestion Produits", "gestionCatalogue", true);
            ajouterBoutonMenu("/images/orders.png", "Commandes reçues", "commandesRecues", true);

            // Section GESTION
            Label gestionLabel = new Label("GESTION");
            gestionLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 25 0 8 18; -fx-letter-spacing: 1.5;");
            menuContainer.getChildren().add(gestionLabel);

            ajouterBoutonMenu(null, "Franchises", "franchises", false);
            ajouterBoutonMenu(null, "Statistiques", "stats", false);

        } else {
            // ========== MENU POUR LA FRANCHISE ==========
            ajouterBoutonMenu("/images/dashboard.png", "Tableau de bord", "dashboard", true); // désactivé par manque d'image
            ajouterBoutonMenu("/images/marketplace.png", "Marketplace", "catalogue", true);
            ajouterBoutonMenu("/images/cart.png", "Mon panier", "panier", true);
            ajouterBoutonMenu("/images/orders.png", "Mes commandes", "mesCommandes", true);

            // Section ACHATS
            Label achatsLabel = new Label("ACHATS");
            achatsLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 25 0 8 18; -fx-letter-spacing: 1.5;");
            menuContainer.getChildren().add(achatsLabel);

            ajouterBoutonMenu(null, "Produits favoris", "favoris", false);
            ajouterBoutonMenu(null, "Historique", "historique", false);
        }

        // Section FINANCES
        Label financesLabel = new Label("FINANCES");
        financesLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 25 0 8 18; -fx-letter-spacing: 1.5;");
        menuContainer.getChildren().add(financesLabel);

        ajouterBoutonMenu(null, "Bilan", "bilan", false);
        ajouterBoutonMenu(null, "Budget", "budget", false);
    }

    private void ajouterBoutonMenu(String imagePath, String texte, String actionId, boolean actif) {
        Button btn = new Button(texte);
        btn.getStyleClass().add("nav-button");
        btn.setPrefHeight(45);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(javafx.geometry.Pos.BASELINE_LEFT);

        // Ajouter l'image si le chemin est fourni
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
                icon.getStyleClass().add("nav-icon");
                icon.setFitHeight(18);
                icon.setFitWidth(18);
                icon.setPreserveRatio(true);
                btn.setGraphic(icon);
                btn.setGraphicTextGap(12);
            } catch (Exception e) {
                System.err.println("⚠️ Image non trouvée: " + imagePath);
            }
        }

        if (actif) {
            btn.setOnAction(e -> {
                // Enlever la classe selected de tous les boutons
                menuContainer.getChildren().filtered(node -> node instanceof Button)
                        .forEach(b -> ((Button) b).getStyleClass().remove("selected"));
                // Ajouter la classe selected à ce bouton
                btn.getStyleClass().add("selected");
                // Appeler la méthode correspondante
                handleMenuAction(actionId);
            });
        } else {
            btn.setDisable(true);
            btn.setOpacity(0.5);
            btn.setTooltip(new Tooltip("Bientôt disponible"));
        }

        menuContainer.getChildren().add(btn);
    }

    private void handleMenuAction(String actionId) {
        String basePath = "/tn/esprit/boussole/views/";
        String vuePath = "";
        String titre = "";
        String sousTitre = "";

        if ("Siege".equals(userType)) {
            switch (actionId) {
                case "gestionCatalogue":
                    vuePath = "siege/GestionCatalogueView.fxml";
                    titre = "Gestion du catalogue";
                    sousTitre = "Gérez vos produits et stocks";
                    break;
                case "commandesRecues":
                    vuePath = "siege/CommandesRecuesView.fxml";
                    titre = "Commandes reçues";
                    sousTitre = "Validez ou refusez les commandes";
                    break;
                default:
                    return;
            }
        } else {
            switch (actionId) {
                case "catalogue":
                    vuePath = "franchise/CatalogueView.fxml";
                    titre = "Marketplace";
                    sousTitre = "Découvrez tous nos produits";
                    break;
                case "panier":
                    vuePath = "franchise/PanierView.fxml";
                    titre = "Mon panier";
                    sousTitre = "Consultez et validez votre panier";
                    break;
                case "mesCommandes":
                    vuePath = "franchise/MesCommandesView.fxml";
                    titre = "Mes commandes";
                    sousTitre = "Suivez l'état de vos commandes";
                    break;
                default:
                    return;
            }
        }

        chargerVue(basePath + vuePath, titre, sousTitre);
    }

    private void chargerVue(String fxmlPath, String titre, String sousTitre) {
        try {
            Parent vue = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(vue);
            lblPageTitle.setText(titre);
            if (lblPageSubtitle != null) {
                lblPageSubtitle.setText(sousTitre);
            }
            System.out.println("✅ Chargé: " + fxmlPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur chargement: " + fxmlPath);
        }
    }

    private void chargerAccueil() {
        if ("Siege".equals(userType)) {
            chargerVue("/tn/esprit/boussole/views/siege/SiegeMainView.fxml",
                    "Tableau de bord",
                    "Vue d'ensemble de votre activité");
        } else {
            chargerVue("/tn/esprit/boussole/views/franchise/FranchiseMainView.fxml",
                    "Tableau de bord",
                    "Bienvenue sur votre espace");
        }
    }
}