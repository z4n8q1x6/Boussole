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

    // Boutons du menu existants
    @FXML private Button btnDashboard;

    @FXML private Button btnFournisseur;

    @FXML private Button btnAlertes;
    @FXML private Button btnReclamations;

    @FXML private Button btnCharge;
    @FXML private Button btnMensualite;
    @FXML private Button btnPret;
    @FXML private Button btnTransaction;
    @FXML private Button btnLogout;
    @FXML private Button btnTheme;

    // NOUVEAUX BOUTONS POUR LES FONCTIONNALITÉS MARKETPLACE
    @FXML private Button btnCatalogue;
    @FXML private Button btnPanier;
    @FXML private Button btnMesCommandes;

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
                if (node instanceof Button && node != btnLogout && node != btnTheme) {
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

        // Vue par défaut : On charge le Dashboard réel
        handleMenuClick(btnDashboard, "Tableau de bord", "/DashboardFranchise.fxml");
    }

    private void setupMenuActions() {
        // Actions des boutons existants avec leurs chemins FXML
        btnDashboard.setOnAction(e -> handleMenuClick(btnDashboard, "Tableau de bord", "/DashboardFranchise.fxml"));
        // Modules Front-office (Charges, Fournisseurs, IA)
        btnFournisseur.setOnAction(e -> handleMenuClick(btnFournisseur, "Fournisseurs", "/afficherFrontFournisseur.fxml"));

        btnAlertes.setOnAction(e -> handleMenuClick(btnAlertes, "Alertes", "/alerteIA.fxml"));
        btnReclamations.setOnAction(e -> handleMenuClick(btnReclamations, "Réclamations", "/reclamation.fxml"));
        btnCharge.setOnAction(e -> handleMenuClick(btnCharge, "Charges", "/afficherFrontCharge.fxml"));
        btnMensualite.setOnAction(e -> handleMenuClick(btnMensualite, "Mensualités", null));
        btnPret.setOnAction(e -> handleMenuClick(btnPret, "Prêts", null));

        // Historique des transactions
        btnTransaction.setOnAction(e -> handleMenuClick(btnTransaction, "Historique des transactions", "/JournalFranchise.fxml"));

        // ACTIONS POUR LES NOUVEAUX BOUTONS MARKETPLACE
        if (btnCatalogue != null) {
            btnCatalogue.setOnAction(e -> handleMenuClick(btnCatalogue, "Marketplace", "/CatalogueView.fxml"));
        } else {
            System.err.println("⚠️ btnCatalogue est null - vérifiez le fx:id dans dashUser.fxml");
        }

        if (btnPanier != null) {
            btnPanier.setOnAction(e -> handleMenuClick(btnPanier, "Mon panier", "/PanierView.fxml"));
        } else {
            System.err.println("⚠️ btnPanier est null - vérifiez le fx:id dans dashUser.fxml");
        }

        if (btnMesCommandes != null) {
            btnMesCommandes.setOnAction(e -> handleMenuClick(btnMesCommandes, "Mes commandes", "/MesCommandesView.fxml"));
        } else {
            System.err.println("⚠️ btnMesCommandes est null - vérifiez le fx:id dans dashUser.fxml");
        }

        btnLogout.setOnAction(e -> handleLogout());
    }

    @FXML
    private void handleThemeToggle() {
        if (btnTheme.getScene() != null) {
            ThemeManager.toggleTheme(btnTheme.getScene());
            updateThemeButtonIcon();
        }
    }

    private void updateThemeButtonIcon() {
        if (btnTheme != null) {
            if (ThemeManager.isDarkMode()) {
                btnTheme.setText("☀️"); // Icône pour passer en mode clair
            } else {
                btnTheme.setText("🌙"); // Icône pour passer en mode sombre
            }
        }
    }

    private void handleMenuClick(Button button, String title, String fxmlPath) {
        setActiveButton(button);
        if (lblPageTitle != null) {
            lblPageTitle.setText(title);
        }

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
        if (menuButtons == null || menuButtons.isEmpty()) return;

        for (Button btn : menuButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("menu-button-active");
                if (!btn.getStyleClass().contains("menu-button")) {
                    btn.getStyleClass().add("menu-button");
                }
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().add("menu-button-active");
        }
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