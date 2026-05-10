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
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

// Imports pour Gestion des Prêts
import tn.esprit.boussole.models.Pret;
import tn.esprit.boussole.service.PretService;
import tn.esprit.boussole.service.PdfService;
import java.util.List;

public class dashController {

    // Original buttons
    @FXML private Button btnDashboard, btnUsers, btnEntreprises, btnSettings, btnReports, btnLogout;
    @FXML private Button btnReclamations, btnAlertesIA;
    @FXML private Button btnCharges, btnFournisseurs;

    // NEW buttons from second controller
    @FXML private Button btnGestionCatalogue, btnCommandesRecues, btnCarteFranchises;

    // NEW: Gestion des Prêts MenuButton
    @FXML private MenuButton btnGestionPrets;

    @FXML private Label lblUsername;
    @FXML private StackPane contentArea;

    // Services pour Gestion des Prêts
    private final PretService pretService = new PretService();
    private final PdfService pdfService = new PdfService();

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
        btnDashboard.setOnAction(e -> handleMenuClick(btnDashboard, "/DashboardSiege.fxml"));
        btnUsers.setOnAction(e -> handleMenuClick(btnUsers, "/users.fxml"));
        btnEntreprises.setOnAction(e -> handleMenuClick(btnEntreprises, "/entreprise.fxml"));
        btnReports.setOnAction(e -> handleMenuClick(btnReports, "/GestionBilans.fxml"));
        btnSettings.setOnAction(e -> handleMenuClick(btnSettings, "/GestionBudgets.fxml"));

        // 3. Modules de réclamation et alertes
        btnReclamations.setOnAction(e -> handleMenuClick(btnReclamations, "/adminReclamation.fxml"));
        btnAlertesIA.setOnAction(e -> handleMenuClick(btnAlertesIA, "/adminAlerteIA.fxml"));

        // 4. Modules Charges et Fournisseurs
        if (btnCharges != null) {
            btnCharges.setOnAction(e -> handleMenuClick(btnCharges, "/afficherBackCharge.fxml"));
        }
        if (btnFournisseurs != null) {
            btnFournisseurs.setOnAction(e -> handleMenuClick(btnFournisseurs, "/afficherBackFournisseur.fxml"));
        }

        // 5. NOUVEAUX BOUTONS (Projet 1)
        if (btnGestionCatalogue != null) {
            btnGestionCatalogue.setOnAction(e -> handleMenuClick(btnGestionCatalogue, "/GestionCatalogueView.fxml"));
        } else {
            System.err.println("⚠️ btnGestionCatalogue est null - vérifiez le fx:id dans dash.fxml");
        }

        if (btnCommandesRecues != null) {
            btnCommandesRecues.setOnAction(e -> handleMenuClick(btnCommandesRecues, "/CommandesRecuesView.fxml"));
        } else {
            System.err.println("⚠️ btnCommandesRecues est null - vérifiez le fx:id dans dash.fxml");
        }

        if (btnCarteFranchises != null) {
            btnCarteFranchises.setOnAction(e -> handleMenuClick(btnCarteFranchises, "/CarteFranchisesView.fxml"));
        } else {
            System.err.println("⚠️ btnCarteFranchises est null - vérifiez le fx:id dans dash.fxml");
        }

        btnLogout.setOnAction(e -> handleLogout());

        // 6. Effets visuels au survol
        setupHoverEffects();

        // 7. Page par défaut au chargement
        handleMenuClick(btnUsers, "/users.fxml");
    }

    private void handleMenuClick(Button button, String fxmlPath) {
        updateButtonStyle(button);
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
            contentArea.getChildren().setAll(view);

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

        // Hover pour MenuButton Gestion des Prêts
        if (btnGestionPrets != null) {
            btnGestionPrets.setOnMouseEntered(e -> {
                if(!btnGestionPrets.getStyle().contains("0.12")) btnGestionPrets.setOpacity(0.7);
            });
            btnGestionPrets.setOnMouseExited(e -> btnGestionPrets.setOpacity(1.0));
        }
    }

    // --- HANDLERS POUR GESTION DES PRÊTS ---

    @FXML
    private void handleListePrets() {
        try {
            var resource = getClass().getResource("/view/ListePrets.fxml");
            if (resource == null) {
                showAlert("Erreur", "Fichier ListePrets.fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            showAlert("Erreur de chargement", "Impossible d'ouvrir la liste des prêts: " + e.getMessage());
        }
    }

    @FXML
    private void handleNouvelleDemande() {
        try {
            var resource = getClass().getResource("/view/DemandePret.fxml");
            if (resource == null) {
                showAlert("Erreur", "Fichier DemandePret.fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            showAlert("Erreur de chargement", "Impossible d'ouvrir le formulaire de demande: " + e.getMessage());
        }
    }

    @FXML
    private void handleDashboardRisque() {
        try {
            var resource = getClass().getResource("/view/DashboardRisque.fxml");
            if (resource == null) {
                showAlert("Erreur", "Fichier DashboardRisque.fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            showAlert("Erreur de chargement", "Impossible d'ouvrir le dashboard risque: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportPDF() {
        try {
            List<Pret> prets = pretService.selectAll();
            if (prets == null || prets.isEmpty()) {
                showAlert("Info", "Aucun prêt à exporter.");
                return;
            }
            pdfService.genererRapportPrets(prets);
            showAlert("Succès", "PDF généré avec succès sur le bureau !");
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la génération du PDF: " + e.getMessage());
        }
    }
}