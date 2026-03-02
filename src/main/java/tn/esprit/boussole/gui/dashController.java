package tn.esprit.boussole.gui;

import java.io.IOException;
import java.util.prefs.Preferences;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class dashController {

    // Original buttons
    @FXML private Button btnDashboard, btnUsers, btnEntreprises, btnSettings, btnReports, btnLogout;
    @FXML private Button btnReclamations, btnAlertesIA;
    @FXML private Button btnCharges, btnFournisseurs;

    // NEW buttons from second controller
    @FXML private Button btnGestionCatalogue, btnCommandesRecues, btnCarteFranchises;

    @FXML private Label lblUsername;
    @FXML private Label lblPageTitle;
    @FXML private TextField searchField;
    @FXML private StackPane contentArea;

    private Object currentController; // Référence au contrôleur de la vue chargée

    private final String ACTIVE_STYLE =
            "-fx-background-color: rgba(0,229,204,0.12); -fx-text-fill: #00E5CC; -fx-border-color:"
                    + " transparent transparent transparent #00E5CC; -fx-border-width: 0 0 0 3;";
    private final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: #8892A4; -fx-border-width: 0;";

    @FXML
    public void initialize() {
        // 1. Session : Récupération de l'email de l'utilisateur connecté
        Preferences prefs = Preferences.userRoot().node("tn.esprit.boussole.gui.loginController");
        lblUsername.setText(prefs.get("email", "Administrateur"));

        // 2. Actions des boutons principaux
        btnDashboard.setOnAction(e -> handleMenuClick(btnDashboard, "Vue d'ensemble", "/DashboardSiege.fxml"));
        btnUsers.setOnAction(e -> handleMenuClick(btnUsers, "Utilisateurs", "/users.fxml"));
        btnEntreprises.setOnAction(e -> handleMenuClick(btnEntreprises, "Entreprises", "/entreprise.fxml"));
        btnReports.setOnAction(e -> handleMenuClick(btnReports, "Bilans", "/GestionBilans.fxml"));
        btnSettings.setOnAction(e -> handleMenuClick(btnSettings, "Budgets", "/GestionBudgets.fxml"));

        // 3. Modules de réclamation et alertes
        btnReclamations.setOnAction(e -> handleMenuClick(btnReclamations, "Réclamations", "/adminReclamation.fxml"));
        btnAlertesIA.setOnAction(e -> handleMenuClick(btnAlertesIA, "Alertes IA", "/adminAlerteIA.fxml"));

        // 4. Modules Charges et Fournisseurs
        if (btnCharges != null) {
            btnCharges.setOnAction(e -> handleMenuClick(btnCharges, "Charges", "/afficherBackCharge.fxml"));
        }
        if (btnFournisseurs != null) {
            btnFournisseurs.setOnAction(e -> handleMenuClick(btnFournisseurs, "Fournisseurs", "/afficherBackFournisseur.fxml"));
        }

        // 5. NOUVEAUX BOUTONS (Projet 1)
        if (btnGestionCatalogue != null) {
            btnGestionCatalogue.setOnAction(e -> handleMenuClick(btnGestionCatalogue, "Gestion catalogue", "/GestionCatalogueView.fxml"));
        }

        if (btnCommandesRecues != null) {
            btnCommandesRecues.setOnAction(e -> handleMenuClick(btnCommandesRecues, "Commandes reçues", "/CommandesRecuesView.fxml"));
        }

        if (btnCarteFranchises != null) {
            btnCarteFranchises.setOnAction(e -> handleMenuClick(btnCarteFranchises, "Carte franchises", "/CarteFranchisesView.fxml"));
        }

        btnLogout.setOnAction(e -> handleLogout());

        // 6. Recherche globale
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentController instanceof Searchable) {
                    ((Searchable) currentController).onSearch(newVal);
                }
            });
        }

        // 7. Effets visuels au survol
        setupHoverEffects();

        // 8. Page par défaut au chargement
        handleMenuClick(btnDashboard, "Vue d'ensemble", "/DashboardSiege.fxml");
    }

    private void handleMenuClick(Button button, String fxmlPath) {
        handleMenuClick(button, null, fxmlPath);
    }

    private void handleMenuClick(Button button, String title, String fxmlPath) {
        updateButtonStyle(button);
        if (lblPageTitle != null && title != null) {
            lblPageTitle.setText(title);
        }
        if (fxmlPath != null) {
            loadView(fxmlPath);
        } else {
            showPlaceholder(button.getText());
        }
    }

    private void loadView(String fxmlPath) {
        try {
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ Erreur: Fichier " + fxmlPath + " introuvable dans resources.");
                showPlaceholder("Fichier introuvable: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            currentController = loader.getController();
            contentArea.getChildren().setAll(view);

            // Réinitialiser le champ de recherche à chaque changement de page
            if (searchField != null) {
                searchField.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur de chargement", "Impossible d'ouvrir : " + fxmlPath);
        }
    }

    private void updateButtonStyle(Button activeButton) {
        Button[] allButtons = {
                btnDashboard, btnUsers, btnEntreprises, btnReports, btnSettings,
                btnReclamations, btnAlertesIA, btnCharges, btnFournisseurs,
                btnGestionCatalogue, btnCommandesRecues, btnCarteFranchises
        };
        for (Button b : allButtons) {
            if (b != null) {
                b.setStyle(b == activeButton ? ACTIVE_STYLE : INACTIVE_STYLE);
            }
        }
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vous déconnecter ?",
                ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
                    Stage stage = (Stage) btnLogout.getScene().getWindow();
                    stage.setScene(new Scene(root));
                } catch (IOException e) {
                    showAlert("Erreur", "Retour au login impossible.");
                }
            }
        });
    }

    private void showPlaceholder(String text) {
        Label placeholder = new Label("Page : " + text + " (Bientôt disponible)");
        placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 24px;");
        contentArea.getChildren().setAll(placeholder);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    private void setupHoverEffects() {
        Button[] allButtons = {
                btnDashboard, btnUsers, btnEntreprises, btnReports, btnSettings,
                btnReclamations, btnAlertesIA, btnCharges, btnFournisseurs,
                btnGestionCatalogue, btnCommandesRecues, btnCarteFranchises
        };
        for (Button b : allButtons) {
            if (b != null) {
                b.setOnMouseEntered(e -> {
                    if(!b.getStyle().contains("0.12")) b.setOpacity(0.7);
                });
                b.setOnMouseExited(e -> b.setOpacity(1.0));
            }
        }
    }
}