package tn.esprit.boussole.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.prefs.Preferences;

public class dashUserController {

    @FXML private StackPane contentArea;
    @FXML private Label lblUsername;
    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSubtitle;

    // Boutons du menu
    @FXML private Button btnDashboard;
    @FXML private Button btnProduit;
    @FXML private Button btnCommande;
    @FXML private Button btnLigneCommande;
    @FXML private Button btnFournisseur;
    @FXML private Button btnFranchises;
    @FXML private Button btnAlertes;
    @FXML private Button btnReclamations;
    @FXML private Button btnBilan;
    @FXML private Button btnBudget;
    @FXML private Button btnCharge;
    @FXML private Button btnMensualite;
    @FXML private Button btnPret;
    @FXML private Button btnTransaction;
    @FXML private Button btnLogout;

    @FXML
    public void initialize() {
        // Récupérer les infos de session
        Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
        String email = prefs.get("email", "Utilisateur");
        if (lblUsername != null) {
            lblUsername.setText(email);
        }

        // Configuration des actions des boutons
        setupMenuActions();
        
        // Vue par défaut
        setActiveButton(btnDashboard);
        updateHeader("Tableau de bord", "Vue d'ensemble de votre activité");
        // loadView("/dashboardUser.fxml"); // À créer plus tard
    }

    private void setupMenuActions() {
        btnDashboard.setOnAction(e -> handleMenuClick(btnDashboard, "Tableau de bord", "Vue d'ensemble", null));
        btnProduit.setOnAction(e -> handleMenuClick(btnProduit, "Gestion des Produits", "Gérer votre catalogue", null));
        btnCommande.setOnAction(e -> handleMenuClick(btnCommande, "Commandes", "Suivi des commandes", null));
        btnLigneCommande.setOnAction(e -> handleMenuClick(btnLigneCommande, "Lignes de Commande", "Détails des commandes", null));
        
        btnFournisseur.setOnAction(e -> handleMenuClick(btnFournisseur, "Fournisseurs", "Gestion des partenaires", null));
        btnFranchises.setOnAction(e -> handleMenuClick(btnFranchises, "Franchises", "Réseau de franchises", null));
        btnAlertes.setOnAction(e -> handleMenuClick(btnAlertes, "Alertes", "Notifications importantes", null));
        btnReclamations.setOnAction(e -> handleMenuClick(btnReclamations, "Réclamations", "Suivi des incidents", null));
        
        btnBilan.setOnAction(e -> handleMenuClick(btnBilan, "Bilan Financier", "État des lieux", null));
        btnBudget.setOnAction(e -> handleMenuClick(btnBudget, "Budget Prévisionnel", "Planification", null));
        btnCharge.setOnAction(e -> handleMenuClick(btnCharge, "Charges", "Dépenses courantes", null));
        btnMensualite.setOnAction(e -> handleMenuClick(btnMensualite, "Mensualités", "Échéances à venir", null));
        btnPret.setOnAction(e -> handleMenuClick(btnPret, "Prêts", "Gestion des emprunts", null));
        btnTransaction.setOnAction(e -> handleMenuClick(btnTransaction, "Transactions", "Historique des mouvements", null));

        btnLogout.setOnAction(e -> handleLogout());
    }

    private void handleMenuClick(Button button, String title, String subtitle, String fxmlPath) {
        setActiveButton(button);
        updateHeader(title, subtitle);
        if (fxmlPath != null) {
            loadView(fxmlPath);
        } else {
            // Placeholder pour les vues non implémentées
            contentArea.getChildren().clear();
            Label placeholder = new Label("Page " + title + " en construction");
            placeholder.setStyle("-fx-font-size: 24px; -fx-text-fill: #7F8C8D;");
            contentArea.getChildren().add(placeholder);
        }
    }

    private void updateHeader(String title, String subtitle) {
        lblPageTitle.setText(title);
        lblPageSubtitle.setText(subtitle);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la vue : " + fxmlPath);
        }
    }

    private void setActiveButton(Button activeButton) {
        // Réinitialiser le style de tous les boutons dans le ScrollPane -> VBox
        if (activeButton.getParent() instanceof VBox) {
            VBox menuBox = (VBox) activeButton.getParent();
            for (Node node : menuBox.getChildren()) {
                if (node instanceof Button) {
                    node.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-border-width: 0;");
                }
            }
        }
        
        // Appliquer le style actif
        activeButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: white; -fx-border-width: 0 0 0 5;");
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous vraiment vous déconnecter ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
            prefs.remove("jwt");
            prefs.remove("email");
            prefs.remove("role");

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) btnLogout.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
