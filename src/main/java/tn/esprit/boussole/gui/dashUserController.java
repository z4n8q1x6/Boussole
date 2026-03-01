package tn.esprit.boussole.gui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.application.Platform;
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
import tn.esprit.boussole.utils.ThemeManager;

public class dashUserController {

    @FXML private StackPane contentArea;
    @FXML private Label lblUsername;
    @FXML private Label lblPageTitle;

    // Boutons du menu
    @FXML private Button btnDashboard, btnProduit, btnCommande, btnLigneCommande;
    @FXML private Button btnFournisseur, btnFranchises, btnAlertes, btnReclamations;
    @FXML private Button btnBilan, btnBudget, btnCharge, btnMensualite, btnPret;
    @FXML private Button btnTransaction, btnLogout, btnTheme;

    private List<Button> menuButtons;

    @FXML
    public void initialize() {
        // Appliquer le thème au démarrage
        Platform.runLater(() -> {
            if (btnTheme != null && btnTheme.getScene() != null) {
                ThemeManager.applyTheme(btnTheme.getScene());
                updateThemeButtonIcon();
            }
        });

        // Initialiser la liste des boutons de menu pour le style "Active"
        menuButtons = new ArrayList<>();
        if (btnDashboard != null && btnDashboard.getParent() instanceof VBox) {
            VBox menuBox = (VBox) btnDashboard.getParent();
            for (Node node : menuBox.getChildrenUnmodifiable()) {
                if (node instanceof Button && node != btnLogout) {
                    menuButtons.add((Button) node);
                }
            }
        }

        // Récupérer les infos de session
        Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
        String email = prefs.get("email", "Utilisateur");
        if (lblUsername != null) {
            lblUsername.setText(email);
        }

        // Configuration des actions des boutons
        setupMenuActions();

        // Vue par défaut
        handleMenuClick(btnDashboard, "Tableau de bord", null);
    }

    private void setupMenuActions() {
<<<<<<< HEAD
        btnDashboard.setOnAction(e -> handleMenuClick(btnDashboard, "Tableau de bord", "/DashboardFranchise.fxml"));
=======
        btnDashboard.setOnAction(e -> handleMenuClick(btnDashboard, "Tableau de bord", null));
>>>>>>> 2118e9cc01de212c47c7cbfda8004c4fa0bea0f9
        btnProduit.setOnAction(e -> handleMenuClick(btnProduit, "Gestion des Produits", null));
        btnCommande.setOnAction(e -> handleMenuClick(btnCommande, "Commandes", null));
        btnLigneCommande.setOnAction(e -> handleMenuClick(btnLigneCommande, "Lignes de Commande", null));

        // Chemins mis à jour avec tes fichiers
        btnFournisseur.setOnAction(e -> handleMenuClick(btnFournisseur, "Fournisseurs", "/afficherFrontFournisseur.fxml"));
        btnFranchises.setOnAction(e -> handleMenuClick(btnFranchises, "Franchises", null));
        btnAlertes.setOnAction(e -> handleMenuClick(btnAlertes, "Alertes", "/alerteIA.fxml"));
        btnReclamations.setOnAction(e -> handleMenuClick(btnReclamations, "Réclamations", "/reclamation.fxml"));

        btnBilan.setOnAction(e -> handleMenuClick(btnBilan, "Bilan Financier", null));
        btnBudget.setOnAction(e -> handleMenuClick(btnBudget, "Budget Prévisionnel", null));

        // Chemin mis à jour pour les charges
        btnCharge.setOnAction(e -> handleMenuClick(btnCharge, "Charges", "/afficherFrontCharge.fxml"));

        btnMensualite.setOnAction(e -> handleMenuClick(btnMensualite, "Mensualités", null));
        btnPret.setOnAction(e -> handleMenuClick(btnPret, "Prêts", null));
<<<<<<< HEAD
        btnTransaction.setOnAction(e -> handleMenuClick(btnTransaction, "Historique des transactions", "/JournalFranchise.fxml"));
=======
        btnTransaction.setOnAction(e -> handleMenuClick(btnTransaction, "Transactions", null));
>>>>>>> 2118e9cc01de212c47c7cbfda8004c4fa0bea0f9

        btnLogout.setOnAction(e -> handleLogout());
    }

    @FXML
    private void handleThemeToggle() {
        ThemeManager.toggleTheme(btnTheme.getScene());
        updateThemeButtonIcon();
    }

    private void updateThemeButtonIcon() {
        if (ThemeManager.isDarkMode()) {
            btnTheme.setText("☀️");
        } else {
            btnTheme.setText("🌙");
        }
    }

    private void handleMenuClick(Button button, String title, String fxmlPath) {
        setActiveButton(button);
        if (lblPageTitle != null) lblPageTitle.setText(title);

        if (fxmlPath != null) {
            loadView(fxmlPath);
        } else {
            contentArea.getChildren().clear();
            Label placeholder = new Label("Page '" + title + "' en construction");
            placeholder.setStyle("-fx-font-size: 24px; -fx-text-fill: #94A3B8;");
            contentArea.getChildren().add(placeholder);
        }
    }

    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier FXML introuvable : " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la vue : " + fxmlPath);
        }
    }

    private void setActiveButton(Button activeButton) {
        if (menuButtons == null) return;
        for (Button btn : menuButtons) {
            btn.getStyleClass().remove("menu-button-active");
            if (!btn.getStyleClass().contains("menu-button")) {
                btn.getStyleClass().add("menu-button");
            }
        }
        activeButton.getStyleClass().add("menu-button-active");
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment vous déconnecter ?", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Déconnexion");
        alert.setHeaderText(null);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
            prefs.remove("jwt");
            prefs.remove("email");
            prefs.remove("role");

            try {
                Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
                Stage stage = (Stage) btnLogout.getScene().getWindow();
                stage.setScene(new Scene(root));
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