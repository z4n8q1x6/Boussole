package tn.esprit.boussole.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainGUI extends Application {

    private BorderPane root;
    private VBox navigationPanel;
    private StackPane contentArea;

    // Les différentes interfaces (3 fonctionnelles pour l'instant)
    private ProduitGUI produitGUI;
    private CommandeGUI commandeGUI;
    private LigneCommandeGUI ligneCommandeGUI;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Boussole - Gestion Commerciale");
        stage.setMaximized(true);

        // Initialiser les interfaces
        produitGUI = new ProduitGUI();
        commandeGUI = new CommandeGUI();
        ligneCommandeGUI = new LigneCommandeGUI();

        // Layout principal
        root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f2f5;");

        // Créer la barre de navigation
        createNavigationBar();

        // Zone de contenu
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f0f2f5;");
        root.setCenter(contentArea);

        // Afficher l'accueil par défaut
        showHome();

        Scene scene = new Scene(root, 1400, 800);

        // CSS supplémentaire
        scene.getStylesheets().add("data:text/css," +
                ".nav-button {" +
                "    -fx-background-color: transparent;" +
                "    -fx-text-fill: #ecf0f1;" +
                "    -fx-font-size: 14px;" +
                "    -fx-alignment: CENTER_LEFT;" +
                "    -fx-padding: 12 15 12 20;" +
                "    -fx-cursor: hand;" +
                "    -fx-border-width: 0;" +
                "    -fx-background-radius: 0;" +
                "}" +
                ".nav-button:hover {" +
                "    -fx-background-color: #34495e;" +
                "}" +
                ".nav-button:selected {" +
                "    -fx-background-color: #3498db;" +
                "    -fx-text-fill: white;" +
                "}" +
                ".nav-header {" +
                "    -fx-text-fill: #95a5a6;" +
                "    -fx-font-size: 12px;" +
                "    -fx-font-weight: bold;" +
                "    -fx-padding: 20 20 5 20;" +
                "}" +
                ".separator {" +
                "    -fx-background-color: #34495e;" +
                "    -fx-padding: 0 0 0 0;" +
                "    -fx-opacity: 0.3;" +
                "}" +
                ".welcome-label {" +
                "    -fx-font-size: 28px;" +
                "    -fx-font-weight: bold;" +
                "    -fx-text-fill: #2c3e50;" +
                "}" +
                ".subtitle-label {" +
                "    -fx-font-size: 16px;" +
                "    -fx-text-fill: #7f8c8d;" +
                "}"
        );

        stage.setScene(scene);
        stage.show();
    }

    private void createNavigationBar() {
        navigationPanel = new VBox();
        navigationPanel.setPrefWidth(280);
        navigationPanel.setStyle("-fx-background-color: #2c3e50;");
        navigationPanel.setPadding(new Insets(20, 0, 20, 0));

        // Logo / Titre
        Label logo = new Label("BOUSSOLE");
        logo.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold; -fx-padding: 0 0 20 20;");
        logo.setAlignment(Pos.CENTER_LEFT);
        navigationPanel.getChildren().add(logo);

        // Séparateur
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #34495e;");
        navigationPanel.getChildren().add(sep1);

        // Section principale
        Label mainSection = new Label("PRINCIPAL");
        mainSection.getStyleClass().add("nav-header");
        navigationPanel.getChildren().add(mainSection);

        // Boutons fonctionnels (3 premiers)
        NavButton btnDashboard = createNavButton("🏠", "Tableau de bord", true);
        NavButton btnProduit = createNavButton("📦", "Produit", true);
        NavButton btnCommande = createNavButton("📋", "Commande", true);
        NavButton btnLigneCommande = createNavButton("📝", "Ligne commande", true);

        // Tous les autres boutons (non fonctionnels pour l'instant)
        NavButton btnAlerteias = createNavButton("⚠️", "Alerteias", false);
        NavButton btnBilan = createNavButton("📊", "Bilan", false);
        NavButton btnBudget = createNavButton("💰", "Budget prévisionnel", false);
        NavButton btnCharge = createNavButton("💸", "Charge", false);
        NavButton btnFournisseur = createNavButton("🚚", "Fournisseur", false);
        NavButton btnFranchises = createNavButton("🏢", "Franchises", false);
        NavButton btnMensualite = createNavButton("📅", "Mensualité", false);
        NavButton btnPret = createNavButton("🏦", "Prêt", false);
        NavButton btnReclamations = createNavButton("📢", "Réclamations", false);
        NavButton btnTransaction = createNavButton("💳", "Transaction", false);
        NavButton btnUtilisateur = createNavButton("👤", "Utilisateur", false);

        // Ajouter les boutons fonctionnels
        navigationPanel.getChildren().addAll(
                btnDashboard, btnProduit, btnCommande, btnLigneCommande
        );

        // Section Gestion
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #34495e;");
        navigationPanel.getChildren().add(sep2);

        Label gestionSection = new Label("GESTION");
        gestionSection.getStyleClass().add("nav-header");
        navigationPanel.getChildren().add(gestionSection);

        navigationPanel.getChildren().addAll(
                btnFournisseur, btnFranchises, btnAlerteias, btnReclamations
        );

        // Section Finances
        Separator sep3 = new Separator();
        sep3.setStyle("-fx-background-color: #34495e;");
        navigationPanel.getChildren().add(sep3);

        Label financeSection = new Label("FINANCES");
        financeSection.getStyleClass().add("nav-header");
        navigationPanel.getChildren().add(financeSection);

        navigationPanel.getChildren().addAll(
                btnBilan, btnBudget, btnCharge, btnMensualite, btnPret, btnTransaction
        );

        // Section Utilisateurs
        Separator sep4 = new Separator();
        sep4.setStyle("-fx-background-color: #34495e;");
        navigationPanel.getChildren().add(sep4);

        Label adminSection = new Label("ADMINISTRATION");
        adminSection.getStyleClass().add("nav-header");
        navigationPanel.getChildren().add(adminSection);

        navigationPanel.getChildren().addAll(
                btnUtilisateur
        );

        // Espace flexible en bas
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        navigationPanel.getChildren().add(spacer);

        // Version info
        Label version = new Label("Version 1.0.0");
        version.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px; -fx-padding: 10 0 10 20;");
        navigationPanel.getChildren().add(version);

        root.setLeft(navigationPanel);
    }

    private NavButton createNavButton(String icon, String text, boolean isEnabled) {
        NavButton button = new NavButton(icon + "  " + text);
        button.getStyleClass().add("nav-button");

        if (!isEnabled) {
            button.setDisable(true);
            button.setStyle(button.getStyle() + "-fx-opacity: 0.5;");
            button.setTooltip(new Tooltip("Bientôt disponible"));
        } else {
            button.setUserData(text.toLowerCase());

            // Ajouter les actions pour les boutons fonctionnels
            switch (text) {
                case "Produit":
                    button.setOnAction(e -> showProduits());
                    break;
                case "Commande":
                    button.setOnAction(e -> showCommandes());
                    break;
                case "Ligne commande":
                    button.setOnAction(e -> showLignesCommande());
                    break;
                case "Tableau de bord":
                    button.setOnAction(e -> showHome());
                    break;
            }
        }

        return button;
    }

    private void showHome() {
        VBox homeContent = new VBox(30);
        homeContent.setAlignment(Pos.CENTER);
        homeContent.setPadding(new Insets(50));

        Label welcome = new Label("👋 Bienvenue sur Boussole");
        welcome.getStyleClass().add("welcome-label");

        Label subtitle = new Label("Système de Gestion Commerciale");
        subtitle.getStyleClass().add("subtitle-label");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setAlignment(Pos.CENTER);

        // Cartes de statistiques (exemple)
        statsGrid.add(createStatCard("📦", "Produits", "0", "#3498db"), 0, 0);
        statsGrid.add(createStatCard("📋", "Commandes", "0", "#e74c3c"), 1, 0);
        statsGrid.add(createStatCard("📝", "Lignes", "0", "#f39c12"), 2, 0);
        statsGrid.add(createStatCard("💰", "Budget", "0 DT", "#2ecc71"), 0, 1);
        statsGrid.add(createStatCard("🏢", "Franchises", "0", "#9b59b6"), 1, 1);
        statsGrid.add(createStatCard("💳", "Transactions", "0", "#1abc9c"), 2, 1);

        homeContent.getChildren().addAll(welcome, subtitle, statsGrid);

        contentArea.getChildren().clear();
        contentArea.getChildren().add(homeContent);
    }

    private VBox createStatCard(String icon, String title, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0); -fx-min-width: 180;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 40px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(iconLabel, titleLabel, valueLabel);
        return card;
    }

    private void showProduits() {
        contentArea.getChildren().clear();

        BorderPane container = new BorderPane();
        container.setPadding(new Insets(20));

        Label title = new Label("📦 Gestion des Produits");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 20 0;");
        container.setTop(title);

        VBox produitContent = new VBox();
        produitGUI.startInPane(produitContent);
        container.setCenter(produitContent);

        contentArea.getChildren().add(container);
    }

    private void showCommandes() {
        contentArea.getChildren().clear();

        BorderPane container = new BorderPane();
        container.setPadding(new Insets(20));

        Label title = new Label("📋 Gestion des Commandes");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 20 0;");
        container.setTop(title);

        VBox commandeContent = new VBox();
        commandeGUI.startInPane(commandeContent);
        container.setCenter(commandeContent);

        contentArea.getChildren().add(container);
    }

    private void showLignesCommande() {
        contentArea.getChildren().clear();

        BorderPane container = new BorderPane();
        container.setPadding(new Insets(20));

        Label title = new Label("📝 Gestion des Lignes de Commande");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 20 0;");
        container.setTop(title);

        VBox ligneContent = new VBox();
        ligneCommandeGUI.startInPane(ligneContent);
        container.setCenter(ligneContent);

        contentArea.getChildren().add(container);
    }

    // Classe interne pour les boutons de navigation
    private class NavButton extends Button {
        public NavButton(String text) {
            super(text);
            setMaxWidth(Double.MAX_VALUE);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}