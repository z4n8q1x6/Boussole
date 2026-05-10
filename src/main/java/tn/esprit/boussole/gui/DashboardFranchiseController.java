package tn.esprit.boussole.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.geometry.Pos;
import tn.esprit.boussole.models.budget_previsionnel;
import tn.esprit.boussole.models.budget_previsionnel.TypeBudget;
import tn.esprit.boussole.models.transaction;
import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.service.ServiceBudgetPrevisionnel;
import tn.esprit.boussole.service.ServiceDevise;
import tn.esprit.boussole.service.ServiceTransaction;
import tn.esprit.boussole.service.ChargeService;


import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors; // Added
import javafx.util.Callback;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.boussole.utils.ThemeManagerS;
import tn.esprit.boussole.utils.NotificationManager;
import java.util.prefs.Preferences;

public class DashboardFranchiseController implements Initializable, Searchable {

    // FXML Components
    @FXML
    private Label lblSolde;

    @FXML
    private Label lblContreValeur; // Modification de lblSoldeEuro à lblContreValeur

    @FXML
    private ComboBox<String> cbDevise;

    // Non présents dans le FXML actuel - sans @FXML
    private Label lblUserInfo;
    private Pane paneKpi;

    @FXML
    private DatePicker dpDate;

    @FXML
    private TextField tfMontant;

    @FXML
    private TextField tfDescription;

    @FXML
    private Button btnValider;

    @FXML
    private TableView<transaction> tableMovements;

    @FXML
    private TableColumn<transaction, Date> colDate;

    @FXML
    private TableColumn<transaction, transaction.Type> colType;

    @FXML
    private TableColumn<transaction, String> colDescription;

    @FXML
    private TableColumn<transaction, Double> colMontant;

    // Non présents dans le FXML actuel - sans @FXML
    private TableView<budget_previsionnel> tableBudgets;
    @FXML
    private Label lblLimiteDepenses;
    @FXML
    private Label lblObjectifRevenu;
    private Label lblDepensesMois;
    private Button btnDashboard;
    private Button btnHistorique;

    // --- Notifications (non présents dans le FXML actuel) ---
    private StackPane paneNotificationBtn;
    private StackPane badgeNotification;
    private Label lblNotifCount;

    private List<String> notificationsList = new ArrayList<>();
    private int unreadNotifications = 0;

    // services
    private ServiceTransaction serviceTransaction;
    private ServiceBudgetPrevisionnel serviceBudgetPrevisionnel;
    private ServiceDevise serviceDevise; // Added ServiceDevise
    private ChargeService serviceCharge; // Pour les charges (dépenses)

    // Session
    private int franchiseId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialize services
            try {
                serviceTransaction = new ServiceTransaction();
                serviceBudgetPrevisionnel = new ServiceBudgetPrevisionnel();
                serviceDevise = new ServiceDevise(); // Initialize ServiceDevise
                serviceCharge = new ChargeService(); // Initialize ChargeService
            } catch (Exception e) {
                System.err.println("Error initializing services: " + e.getMessage());
                afficherMessageErreur("Erreur de connexion à la base de données: " + e.getMessage());
                return;
            }

            // Get franchise ID from session
            Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
            franchiseId = fetchFranchiseId(prefs.get("email", ""));
            // Fallback : si non trouvé en BDD, on utilise 1 par défaut
            if (franchiseId <= 0) {
                franchiseId = 1;
                System.out.println("⚠️ franchiseId non trouvé → fallback à 1");
            }

            // Update user info label
            if (lblUserInfo != null) {
                lblUserInfo.setText("Franchise ID: " + franchiseId);
            }

            // Set today's date in DatePicker
            if (dpDate != null) {
                dpDate.setValue(LocalDate.now());
            }

            // Configure table columns (LECTURE SEULE)
            if (colDate != null) {
                colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
                // Format date yyyy-MM-dd, couleur #CBD5E1
                colDate.setCellFactory(col -> new TableCell<transaction, Date>() {
                    private final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    @Override
                    protected void updateItem(Date d, boolean empty) {
                        super.updateItem(d, empty);
                        if (empty || d == null) { setText(null); setStyle(""); }
                        else {
                            setText(fmt.format(d));
                            setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-padding: 0 0 0 20; -fx-alignment: CENTER-LEFT;");
                        }
                    }
                });
            }

            // TYPE : flèche cyan + texte cyan bold
            if (colType != null) {
                colType.setCellValueFactory(new PropertyValueFactory<>("type"));
                colType.setCellFactory(col -> new TableCell<transaction, transaction.Type>() {
                    @Override
                    protected void updateItem(transaction.Type item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setGraphic(null); setStyle(""); return; }
                        boolean isRecette = transaction.Type.RECETTE.equals(item);
                        String color  = isRecette ? "#22C55E" : "#EF4444";
                        String arrow  = isRecette ? "↗" : "↘";
                        String label  = isRecette ? "RECETTE" : "DÉPENSE";

                        HBox box = new HBox(6);
                        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        Label iconLbl = new Label(arrow);
                        iconLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-weight:bold;");
                        Label txtLbl = new Label(label);
                        txtLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px; -fx-font-weight:bold;");
                        box.getChildren().addAll(iconLbl, txtLbl);
                        setText(null);
                        setGraphic(box);
                        setStyle("-fx-padding: 0 0 0 20;");
                    }
                });
            }

            if (colDescription != null) {
                colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
                colDescription.setCellFactory(col -> new TableCell<transaction, String>() {
                    @Override
                    protected void updateItem(String s, boolean empty) {
                        super.updateItem(s, empty);
                        if (empty || s == null) { setText(null); setStyle(""); }
                        else {
                            setText(s);
                            setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-padding: 0 0 0 20; -fx-alignment: CENTER-LEFT;");
                        }
                    }
                });
            }

            if (colMontant != null) {
                colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
                // Montant aligné droite, cyan pour RECETTE, rouge pour DÉPENSE
                colMontant.setCellFactory(col -> new TableCell<transaction, Double>() {
                    @Override
                    protected void updateItem(Double val, boolean empty) {
                        super.updateItem(val, empty);
                        if (empty || val == null) { setText(null); setStyle(""); return; }
                        transaction tx = getTableView().getItems().get(getIndex());
                        boolean isRecette = tx != null && transaction.Type.RECETTE.equals(tx.getType());
                        String color = isRecette ? "#22C55E" : "#EF4444";
                        setText(String.format("%.2f TND", val));
                        setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold; -fx-font-size:13px;"
                                + "-fx-alignment:CENTER-RIGHT; -fx-padding: 0 20 0 0;");                    }
                });
            }

            // IMPORTANT: Désactivation de l'édition et du menu contextuel pour le Dashboard
            tableMovements.setEditable(false);
            tableMovements.setContextMenu(null);

            // Load initial data
            // Initialisation de la combo box devises
            if (cbDevise != null) {
                cbDevise.getItems().addAll("EUR", "USD", "GBP", "CAD");
                cbDevise.setValue("EUR");
                cbDevise.setOnAction(e -> chargerSolde());
            }

            ajouterNotification("Bienvenue sur votre Dashboard de Franchise.");
            chargerDonneesDashboard();

            // Wire button action
            if (btnValider != null) {
                btnValider.setOnAction(this::validerRecette);
            }

            // ─── AUTO-REFRESH : recharger dès que la fenêtre reprend le focus ───
            // Cela permet d'afficher les charges ajoutées depuis un autre écran (ex: Charges)
            tableMovements.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                        if (newWin != null) {
                            newWin.focusedProperty().addListener((obs3, wasFocused, isFocused) -> {
                                if (isFocused) {
                                    chargerDonneesDashboard();
                                }
                            });
                        }
                    });
                }
            });


            // Cleaned up unused sorting/context menu code...

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during Dashboard initialization: " + e.getMessage());
            Logger.getLogger(DashboardFranchiseController.class.getName()).log(Level.SEVERE, null, e);
            afficherMessageErreur("Erreur inattendue au chargement: " + e.getMessage());
        }
    }

    // --- Logique Notifications ---
    public void ajouterNotification(String message) {
        notificationsList.add(0, message); // Ajout au début
        unreadNotifications++;
        mettreAJourBadge();
        NotificationManager.showInfo("Nouvelle Alerte", message);
    }

    private void mettreAJourBadge() {
        if (badgeNotification == null || lblNotifCount == null) return;
        if (unreadNotifications > 0) {
            badgeNotification.setVisible(true);
            lblNotifCount.setText(String.valueOf(unreadNotifications));
        } else {
            badgeNotification.setVisible(false);
        }
    }

    @FXML
    private void afficherNotifications() {
        VBox contenu = new VBox(10);
        contenu.setStyle("-fx-padding: 15; -fx-background-color: #0D1117; -fx-background-radius: 12; " +
                "-fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 12;");
        contenu.setPrefWidth(300);

        Label titre = new Label("Centre de Notifications");
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        contenu.getChildren().add(titre);

        if (notificationsList.isEmpty()) {
            Label emptyLbl = new Label("Aucune notification.");
            emptyLbl.setStyle("-fx-text-fill: #64748B;");
            contenu.getChildren().add(emptyLbl);
        } else {
            for (String notif : notificationsList) {
                Label lbl = new Label("• " + notif);
                lbl.setStyle("-fx-wrap-text: true; -fx-text-fill: #E2E8F0;");
                lbl.setMaxWidth(280);
                contenu.getChildren().add(lbl);
            }
        }

        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.getContent().add(contenu);
        popup.setAutoHide(true);

        javafx.geometry.Bounds bounds = paneNotificationBtn.localToScreen(paneNotificationBtn.getBoundsInLocal());
        if (bounds != null) {
            popup.show(paneNotificationBtn.getScene().getWindow(),
                    bounds.getMaxX() - 300, bounds.getMaxY() + 5);
        }

        // Réinitialiser le compteur une fois ouvert
        unreadNotifications = 0;
        mettreAJourBadge();
    }
    // ----------------------------

    private void chargerDonneesDashboard() {
        chargerSolde();
        chargerDerniersMouvements();
        chargerBudgets();
    }


    /**
     * Load and display solde directly from the franchises table in the database.
     * Utilise la colonne solde_actuel de la table franchises comme source de vérité.
     */
    private void chargerSolde() {
        try {
            double solde = 0.0;

            // Lire directement depuis la table franchises
            String sql = "SELECT solde_actuel FROM franchises WHERE id = ?";
            try (java.sql.Connection conn = tn.esprit.boussole.utils.MyBdConnexion.getinstance().getCnx();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, franchiseId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        solde = rs.getDouble("solde_actuel");
                    }
                }
            }

            System.out.println("💰 Solde franchise " + franchiseId + " (depuis franchises.solde_actuel) = " + solde);
            lblSolde.setText(String.format("%.2f TND", solde));

            // Conversion dynamique
            try {
                String devise = "EUR";
                if(cbDevise != null && cbDevise.getValue() != null) {
                    devise = cbDevise.getValue();
                }

                double taux = serviceDevise.convertir(1.0, devise);

                if (taux > 0) {
                    double soldeConverti = solde * taux;
                    String symbole = "";
                    switch(devise) {
                        case "EUR": symbole = "€"; break;
                        case "USD": symbole = "$"; break;
                        case "GBP": symbole = "£"; break;
                        case "CAD": symbole = "$C"; break;
                        default: symbole = devise;
                    }
                    if (lblContreValeur != null) {
                        lblContreValeur.setText(String.format("(soit ≈ %.2f %s)", soldeConverti, symbole));
                    }
                } else if (lblContreValeur != null) {
                    lblContreValeur.setText("ID Franchise utilisé: " + franchiseId);
                }
            } catch (Exception ex) {
                System.err.println("Erreur conversion devise: " + ex.getMessage());
                if (lblContreValeur != null) lblContreValeur.setText("(-)");
            }

            // Change color based on solde
            if (solde >= 0) {
                lblSolde.setStyle("-fx-text-fill: #22C55E; -fx-font-size: 34px; -fx-font-weight: 900;");
            } else {
                lblSolde.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 34px; -fx-font-weight: 900;");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du solde : " + e.getMessage());
            lblSolde.setText("—");
        }
    }

    /**
     * Load LAST 5 mouvements (transactions + charges converties en DÉPENSE) for this franchise
     */
    private void chargerDerniersMouvements() {
        try {
            // 1. Récupérer les transactions de la franchise
            List<transaction> transactions = serviceTransaction.getAllByFranchise(franchiseId);

            // 2. Récupérer les charges et les convertir en pseudo-transactions DÉPENSE
            List<Charge> charges = serviceCharge.getChargesByFranchise(franchiseId);
            for (Charge charge : charges) {
                transaction t = new transaction();
                // Convertir LocalDate -> java.util.Date
                t.setDate(java.sql.Date.valueOf(charge.getDateCharge()));
                t.setMontant(charge.getMontant());
                t.setType(transaction.Type.DEPENSE);
                t.setDescription(charge.getTitre() != null ? charge.getTitre() : "Charge");
                t.setFranchiseId(charge.getFranchiseId());
                transactions.add(t);
            }

            // 3. Trier par date décroissante
            transactions.sort((a, b) -> {
                if (a.getDate() == null && b.getDate() == null) return 0;
                if (a.getDate() == null) return 1;
                if (b.getDate() == null) return -1;
                return b.getDate().compareTo(a.getDate());
            });

            // 4. Limiter aux 5 derniers
            List<transaction> dernieres = transactions.stream()
                .limit(5)
                .collect(Collectors.toList());

            ObservableList<transaction> data = FXCollections.observableArrayList(dernieres);
            tableMovements.setItems(data);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des mouvements : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // chargerTransactions replaced by chargerDerniersMouvements, keeping helper method name consistent with FXML actions if any
    private void chargerTransactions() {
        chargerDerniersMouvements();
    }

    /**
     * Load budgets declared by Siege for this franchise and update KPIs.
     * Récupère les budgets de toute l'année en cours pour la franchise connectée
     * ET les budgets globaux (franchise_id IS NULL).
     */
    private void chargerBudgets() {
        try {
            int currentYear = LocalDate.now().getYear();

            double totalLimiteDepenses = 0;
            double totalObjectifRevenu = 0;

            // Requête SQL : uniquement les budgets de cette franchise
            String sql = "SELECT " +
                         "COALESCE(SUM(CASE WHEN type_budget='LIMITE_DEPENSE' THEN montant_cible ELSE 0 END), 0) as limite_totale, " +
                         "COALESCE(SUM(CASE WHEN type_budget='OBJECTIF_REVENU' THEN montant_cible ELSE 0 END), 0) as objectif_total " +
                         "FROM budget_previsionnel " +
                         "WHERE franchise_id = ? AND annee = ?";

            try (java.sql.Connection conn = tn.esprit.boussole.utils.MyBdConnexion.getinstance().getCnx();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, franchiseId);
                ps.setInt(2, currentYear);
                
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalLimiteDepenses = rs.getDouble("limite_totale");
                        totalObjectifRevenu = rs.getDouble("objectif_total");
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur SQL Budgets: " + e.getMessage());
            }

            System.out.println("📊 Budget franchise " + franchiseId + " (année " + currentYear + ") → Limite: " + totalLimiteDepenses + " | Objectif: " + totalObjectifRevenu);

            // Mettre à jour les labels
            if (lblLimiteDepenses != null) {
                lblLimiteDepenses.setText(String.format("%.2f TND", totalLimiteDepenses));
            }
            if (lblObjectifRevenu != null) {
                lblObjectifRevenu.setText(String.format("%.2f TND", totalObjectifRevenu));
            }

            if (lblDepensesMois != null) {
                // Total des dépenses réelles (transactions + charges)
                List<transaction> allTransactions = serviceTransaction.getAllByFranchise(franchiseId);
                double totalDepenses = 0;
                for (transaction t : allTransactions) {
                    if (t.getType() == transaction.Type.DEPENSE) {
                        totalDepenses += t.getMontant();
                    }
                }
                double totalCharges = serviceCharge.getTotalChargesByFranchise(franchiseId);
                totalDepenses += totalCharges;
                lblDepensesMois.setText(String.format("%.2f TND", totalDepenses));

                if (totalLimiteDepenses > 0 && totalDepenses > totalLimiteDepenses) {
                    lblDepensesMois.setStyle("-fx-text-fill: #c62828; -fx-font-size:20px; -fx-font-weight:bold;");
                } else {
                    lblDepensesMois.setStyle("-fx-text-fill: #e65100; -fx-font-size:20px; -fx-font-weight:bold;");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des budgets : " + e.getMessage());
        }
    }


    /**
     * Handle 'Valider Recette' button action
     */
    @FXML
    void validerRecette(javafx.event.ActionEvent event) {
        // 1. Champs vides
        if (tfMontant == null || tfMontant.getText().trim().isEmpty()) {
            afficherMessageErreur("Le montant est obligatoire.");
            return;
        }
        if (tfDescription == null || tfDescription.getText().trim().isEmpty()) {
            afficherMessageErreur("La description est obligatoire.");
            return;
        }
        // 2. Montant numérique > 0
        double montant;
        try {
            montant = Double.parseDouble(tfMontant.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            afficherMessageErreur("Montant invalide (ex: 150.00).");
            return;
        }
        if (montant <= 0) {
            afficherMessageErreur("Le montant doit être > 0.");
            return;
        }
        // 3. Date
        if (dpDate == null || dpDate.getValue() == null) {
            afficherMessageErreur("Veuillez sélectionner une date.");
            return;
        }

        // 4. Build transaction
        int fid = (franchiseId > 0) ? franchiseId : 1;
        transaction t = new transaction();
        t.setDate(java.sql.Date.valueOf(dpDate.getValue()));
        t.setMontant(montant);
        t.setDescription(tfDescription.getText().trim());
        t.setType(transaction.Type.RECETTE);
        t.setFranchiseId(fid);

        System.out.println("🚀 INSERT transaction : montant=" + montant
                + ", desc='" + t.getDescription()
                + "', date=" + t.getDate()
                + ", franchiseId=" + fid);

        // 5. Insertion
        try {
            serviceTransaction.insertone(t);
            System.out.println("✅ Insertion réussie, id=" + t.getId());
        } catch (java.sql.SQLException sqle) {
            System.err.println("❌ SQL : " + sqle.getMessage());
            sqle.printStackTrace();
            afficherMessageErreur("Erreur SQL : " + sqle.getMessage());
            return;
        } catch (Exception ex) {
            System.err.println("❌ Exception : " + ex.getMessage());
            ex.printStackTrace();
            afficherMessageErreur("Erreur : " + ex.getMessage());
            return;
        }

        // 6. Reset champs
        tfMontant.clear();
        tfDescription.clear();
        dpDate.setValue(LocalDate.now());

        // 7. Refresh
        chargerDerniersMouvements();
        chargerSolde();
        afficherMessageSucces("Recette ajoutée avec succès !");
    }

    // Ajout des méthodes manquantes pour gérer les messages d'erreur et de succès
    private void afficherMessageErreur(String message) {
        NotificationManager.showError("Erreur", message);
    }

    private void afficherMessageSucces(String message) {
        NotificationManager.showSuccess("Succès", message);
    }

    private void afficherMessageInfo(String message) {
        NotificationManager.showInfo("Information", message);
    }

    /**
     * Change scene to historique view
     */
    @FXML
    void versHistorique(javafx.event.ActionEvent event) {
        changerScene(event, "/JournalFranchise.fxml", "Journal des Opérations");
    }

    private void changerScene(javafx.event.ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            ThemeManagerS.getInstance().applyCurrentTheme(stage.getScene());
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            Logger.getLogger(DashboardFranchiseController.class.getName()).log(Level.SEVERE, null, e);
            afficherMessageErreur("Impossible de charger la vue : " + fxmlPath + "\n" + e.getMessage());
        }
    }

    // --- Theme Toggle ---
    private ToggleButton btnTheme;

    @FXML
    private void toggleTheme() {
        ThemeManagerS.getInstance().toggleTheme(btnTheme.getScene());
        if (btnTheme != null) {
            btnTheme.setText(ThemeManagerS.getInstance().isDark() ? "🌞 Mode Clair" : "🌙 Mode Sombre");
        }
    }

    // Helper to fetch true franchiseID using the email stored in preferences
    private int fetchFranchiseId(String email) {
        if (email == null || email.isEmpty()) return 0;
        String sql = "SELECT id_franchise FROM utilisateur WHERE email = ? LIMIT 1";
        try (java.sql.Connection conn = tn.esprit.boussole.utils.MyBdConnexion.getinstance().getCnx();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_franchise");
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // --- Implémentation Searchable ---
    @Override
    public void onSearch(String keyword) {
        if (tableMovements == null) return;
        if (keyword == null || keyword.trim().isEmpty()) {
            chargerDerniersMouvements();
            return;
        }
        String lower = keyword.toLowerCase();
        try {
            List<transaction> transactions = serviceTransaction.getAllByFranchise(franchiseId);
            List<Charge> charges = serviceCharge.getChargesByFranchise(franchiseId);
            for (Charge charge : charges) {
                transaction t = new transaction();
                t.setDate(java.sql.Date.valueOf(charge.getDateCharge()));
                t.setMontant(charge.getMontant());
                t.setType(transaction.Type.DEPENSE);
                t.setDescription(charge.getTitre() != null ? charge.getTitre() : "Charge");
                t.setFranchiseId(charge.getFranchiseId());
                transactions.add(t);
            }
            List<transaction> filtered = transactions.stream()
                .filter(t -> {
                    if (t.getDescription() != null && t.getDescription().toLowerCase().contains(lower)) return true;
                    if (t.getType() != null && t.getType().name().toLowerCase().contains(lower)) return true;
                    if (String.valueOf(t.getMontant()).contains(lower)) return true;
                    return false;
                })
                .sorted((a, b) -> {
                    if (a.getDate() == null && b.getDate() == null) return 0;
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    return b.getDate().compareTo(a.getDate());
                })
                .limit(20)
                .collect(Collectors.toList());
            tableMovements.setItems(FXCollections.observableArrayList(filtered));
        } catch (Exception e) {
            System.err.println("Erreur recherche: " + e.getMessage());
        }
    }
}
