package tn.esprit.boussole.gui.common;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
            // Menu pour le SIEGE
            ajouterBoutonMenu("📊  Tableau de bord", "dashboard", true);
            ajouterBoutonMenu("📦  Gestion Produits", "gestionCatalogue", true);
            ajouterBoutonMenu("📋  Commandes reçues", "commandesRecues", true);

            // Section GESTION
            Label gestionLabel = new Label("📌 GESTION");
            gestionLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
            gestionLabel.setPadding(new Insets(15, 0, 5, 10));
            menuContainer.getChildren().add(gestionLabel);

            ajouterBoutonMenu("🏢  Franchises", "franchises", false);
            ajouterBoutonMenu("📊  Statistiques", "stats", false);

        } else {
            // Menu pour la FRANCHISE
            ajouterBoutonMenu("📊  Tableau de bord", "dashboard", true);
            ajouterBoutonMenu("🛒  Marketplace", "catalogue", true);
            ajouterBoutonMenu("🛍️  Mon panier", "panier", true);
            ajouterBoutonMenu("📦  Mes commandes", "mesCommandes", true);

            // Section ACHATS
            Label achatsLabel = new Label("📌 ACHATS");
            achatsLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
            achatsLabel.setPadding(new Insets(15, 0, 5, 10));
            menuContainer.getChildren().add(achatsLabel);

            ajouterBoutonMenu("⭐  Produits favoris", "favoris", false);
            ajouterBoutonMenu("📋  Historique", "historique", false);
        }

        // Section FINANCES (commune)
        Label financesLabel = new Label("📌 FINANCES");
        financesLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
        financesLabel.setPadding(new Insets(15, 0, 5, 10));
        menuContainer.getChildren().add(financesLabel);

        ajouterBoutonMenu("📈  Bilan", "bilan", false);
        ajouterBoutonMenu("💰  Budget", "budget", false);
    }

    private void ajouterBoutonMenu(String texte, String actionId, boolean actif) {
        Button btn = new Button(texte);
        btn.getStyleClass().add("menu-button");
        btn.setPrefHeight(45);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(javafx.geometry.Pos.BASELINE_LEFT);

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
                default:
                    return;
            }
        } else {
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
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur chargement: " + fxmlPath);
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