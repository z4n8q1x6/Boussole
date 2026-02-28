package tn.esprit.Boussole.GUI;

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
import javafx.scene.layout.Pane;
import javafx.scene.control.Hyperlink; // Added
import tn.esprit.Boussole.Models.budget_previsionnel;
import tn.esprit.Boussole.Models.budget_previsionnel.TypeBudget;
import tn.esprit.Boussole.Models.transaction;
import tn.esprit.Boussole.Services.ServiceBudgetPrevisionnel;
import tn.esprit.Boussole.Services.ServiceDevise;
import tn.esprit.Boussole.Services.ServiceTransaction;
import tn.esprit.Boussole.Utilis.SessionManager;
import tn.esprit.Boussole.Utilis.NotificationManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
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
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import tn.esprit.Boussole.Utilis.ThemeManager;

public class DashboardFranchiseController implements Initializable {

    // FXML Components
    @FXML
    private Label lblSolde;

    @FXML
    private Label lblContreValeur; // Modification de lblSoldeEuro à lblContreValeur

    @FXML
    private ComboBox<String> cbDevise; // Ajout de la ComboBox

    @FXML
    private Label lblUserInfo;

    @FXML
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
    private TableColumn<transaction, String> colType;

    @FXML
    private TableColumn<transaction, String> colDescription;

    @FXML
    private TableColumn<transaction, Double> colMontant;

    @FXML
    private TableView<budget_previsionnel> tableBudgets;
    @FXML
    private Label lblLimiteDepenses;
    @FXML
    private Label lblObjectifRevenu;
    @FXML
    private Label lblDepensesMois;
    @FXML private Button btnDashboard;
    @FXML private Button btnHistorique;

    // --- Nouveaux éléments pour Notifications ---
    @FXML private StackPane paneNotificationBtn;
    @FXML private Pane badgeNotification;
    @FXML private Label lblNotifCount;

    private List<String> notificationsList = new ArrayList<>();
    private int unreadNotifications = 0;

    // Services
    private ServiceTransaction serviceTransaction;
    private ServiceBudgetPrevisionnel serviceBudgetPrevisionnel;
    private ServiceDevise serviceDevise; // Added ServiceDevise

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
            } catch (Exception e) {
                System.err.println("Error initializing services: " + e.getMessage());
                afficherMessageErreur("Erreur de connexion à la base de données: " + e.getMessage());
                return;
            }

            // Get franchise ID from session
            try {
                if (SessionManager.getInstance() != null) {
                    franchiseId = SessionManager.getInstance().getIdFranchise();
                } else {
                    System.err.println("SessionManager instance is null");
                }
            } catch (Exception e) {
                System.err.println("Error accessing SessionManager: " + e.getMessage());
            }

            if (franchiseId == 0) {
                afficherMessageErreur("Session invalide : identifiant franchise manquant.");
                // Continue execution but data loading might fail or show empty
                // return; // Optional: stop here if critical
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
            if (colDate != null) colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            if (colType != null) colType.setCellValueFactory(new PropertyValueFactory<>("type"));
            if (colDescription != null) colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            if (colMontant != null) colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));

            // IMPORTANT: Désactivation de l'édition et du menu contextuel pour le Dashboard
            tableMovements.setEditable(false);
            tableMovements.setContextMenu(null);

            // Load initial data
            if (franchiseId != 0) {
                // Initialisation de la combo box devises
                if (cbDevise != null) {
                    cbDevise.getItems().addAll("EUR", "USD", "GBP", "CAD");
                    cbDevise.setValue("EUR"); // Valeur par défaut
                    cbDevise.setOnAction(e -> chargerSolde());
                }

                // Simulation d'une première notification au démarrage
                ajouterNotification("Bienvenue sur votre Dashboard de Franchise.");
                
                chargerDonneesDashboard();
            }

            // Wire button action
            if (btnValider != null) {
                btnValider.setOnAction(this::validerRecette);
            }

            appliquerCellFactoryMontant();

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
        contenu.setStyle("-fx-padding: 15;");
        contenu.getStyleClass().add("kpi-card");
        contenu.setPrefWidth(300);
        contenu.setPrefHeight(250);

        Label titre = new Label("Centre de Notifications");
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titre.getStyleClass().add("page-title");
        contenu.getChildren().add(titre);

        if (notificationsList.isEmpty()) {
            Label emptyLbl = new Label("Aucune notification.");
            emptyLbl.getStyleClass().add("page-subtitle");
            contenu.getChildren().add(emptyLbl);
        } else {
            for (String notif : notificationsList) {
                Label lbl = new Label("- " + notif);
                lbl.setStyle("-fx-wrap-text: true;");
                lbl.setMaxWidth(280);
                contenu.getChildren().add(lbl);
            }
        }

        PopOver popOver = new PopOver(contenu);
        popOver.setArrowLocation(PopOver.ArrowLocation.TOP_RIGHT);
        popOver.show(paneNotificationBtn);

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

    private void appliquerCellFactoryMontant() {
        colMontant.setCellFactory(new Callback<TableColumn<transaction, Double>, TableCell<transaction, Double>>() {
            @Override
            public TableCell<transaction, Double> call(TableColumn<transaction, Double> column) {
                return new TableCell<transaction, Double>() {
                    @Override
                    protected void updateItem(Double montant, boolean empty) {
                        super.updateItem(montant, empty);
                        if (empty || montant == null) {
                            setText(null);
                            setStyle("");
                            return;
                        }
                        setText(String.format("%.2f", montant));
                        transaction tx = getTableView().getItems().get(getIndex());
                        if (tx != null && tx.getType() != null) {
                            String type = tx.getType().name();
                            if ("RECETTE".equalsIgnoreCase(type)) {
                                setStyle("-fx-background-color: rgba(46,125,50,0.15);");
                            } else if ("DEPENSE".equalsIgnoreCase(type)) {
                                setStyle("-fx-background-color: rgba(198,40,40,0.15);");
                            } else {
                                setStyle("");
                            }
                        } else {
                            setStyle("");
                        }
                    }
                };
            }
        });
    }

    /**
     * Load and display solde from database
     */
    private void chargerSolde() {
        try {
            double solde = serviceTransaction.calculerSolde(franchiseId);
            lblSolde.setText(String.format("%.2f TND", solde));

            // Conversion dynamique
            try {
                String devise = "EUR";
                if(cbDevise != null && cbDevise.getValue() != null) {
                    devise = cbDevise.getValue();
                }

                double taux = serviceDevise.convertir(1.0, devise); // On récupère le taux pour 1 TND

                if (taux > 0) {
                    double soldeConverti = solde * taux;
                    // Symboles de devises
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
                     lblContreValeur.setText("(Service indisponible)");
                }
            } catch (Exception ex) {
                System.err.println("Erreur conversion devise: " + ex.getMessage());
                if (lblContreValeur != null) lblContreValeur.setText("(-)");
            }

            // Change color based on solde (green if positive, red if negative)
            if (solde >= 0) {
                lblSolde.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 48px; -fx-font-weight: bold;");
                paneKpi.setStyle("-fx-background-color: #e8f5e9; -fx-border-radius: 10; -fx-background-radius: 10;");
            } else {
                lblSolde.setStyle("-fx-text-fill: #c62828; -fx-font-size: 48px; -fx-font-weight: bold;");
                paneKpi.setStyle("-fx-background-color: #ffebee; -fx-border-radius: 10; -fx-background-radius: 10;");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du solde : " + e.getMessage());
            lblSolde.setText("—");
        }
    }

    /**
     * Load LAST 5 transactions for this franchise
     */
    private void chargerDerniersMouvements() {
        try {
            List<transaction> transactions = serviceTransaction.getAllByFranchise(franchiseId);

            // Limit to 5 most recent
            List<transaction> denieresTransactions = transactions.stream()
                .limit(5)
                .collect(Collectors.toList());

            ObservableList<transaction> data = FXCollections.observableArrayList(denieresTransactions);
            tableMovements.setItems(data);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des transactions : " + e.getMessage());
        }
    }

    // chargerTransactions replaced by chargerDerniersMouvements, keeping helper method name consistent with FXML actions if any
    private void chargerTransactions() {
        chargerDerniersMouvements();
    }

    /**
     * Load budgets declared by Siege for this franchise and update KPIs
     */
    private void chargerBudgets() {
        try {
            List<budget_previsionnel> budgets = serviceBudgetPrevisionnel.getAllByFranchise(franchiseId);
            if (tableBudgets != null) {
                tableBudgets.setItems(FXCollections.observableArrayList(budgets));
            }

            // Calculer les TOTAUX de tous les budgets déclarés par le siège
            double totalLimiteDepenses = 0;
            double totalObjectifRevenu = 0;

            for (budget_previsionnel b : budgets) {
                if (b.getType_budget() == TypeBudget.LIMITE_DEPENSE) {
                    totalLimiteDepenses += b.getMontantCible();
                } else if (b.getType_budget() == TypeBudget.OBJECTIF_REVENU) {
                    totalObjectifRevenu += b.getMontantCible();
                }
            }

            // Mise à jour des KPI Labels
            if (lblLimiteDepenses != null) {
                lblLimiteDepenses.setText(totalLimiteDepenses > 0
                    ? String.format("%.2f TND", totalLimiteDepenses)
                    : "— TND");
            }
            if (lblObjectifRevenu != null) {
                lblObjectifRevenu.setText(totalObjectifRevenu > 0
                    ? String.format("%.2f TND", totalObjectifRevenu)
                    : "— TND");
            }

            // Calculer les dépenses totales réelles
            if (lblDepensesMois != null) {
                List<transaction> allTransactions = serviceTransaction.getAllByFranchise(franchiseId);
                double totalDepenses = 0;
                for (transaction t : allTransactions) {
                    if (t.getType() == transaction.Type.DEPENSE) {
                        totalDepenses += t.getMontant();
                    }
                }
                lblDepensesMois.setText(String.format("%.2f TND", totalDepenses));

                // Rouge si dépassement du budget, orange sinon
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
        try {
            // Validation des champs
            if (tfMontant.getText().isEmpty() || tfDescription.getText().isEmpty()) {
                afficherMessageErreur("Tous les champs doivent être remplis.");
                return;
            }

            double montant = Double.parseDouble(tfMontant.getText());
            if (montant <= 0) {
                afficherMessageErreur("Le montant doit être supérieur à 0.");
                return;
            }

            if (!tfDescription.getText().matches("[a-zA-Z\\s]+")) {
                afficherMessageErreur("La description ne doit contenir que des lettres.");
                return;
            }

            // Création de la transaction
            transaction t = new transaction();
            t.setDate(Date.valueOf(dpDate.getValue()));
            t.setMontant(montant);
            t.setDescription(tfDescription.getText());
            t.setType(transaction.Type.RECETTE);
            t.setFranchiseId(SessionManager.getInstance().getIdFranchise());

            serviceTransaction.insertOne(t);
            afficherMessageSucces("Recette ajoutée avec succès.");

            // Rafraîchir la table et le solde
            refreshTable();
            lblSolde.setText(String.format("%.2f TND", serviceTransaction.calculerSolde(SessionManager.getInstance().getIdFranchise())));
        } catch (NumberFormatException e) {
            afficherMessageErreur("Le montant doit être un nombre valide.");
        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de l'ajout de la recette : " + e.getMessage());
        }
    }

    // Correction de la méthode refreshTable
    private void refreshTable() {
        chargerDerniersMouvements();
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
        changerScene(event, "/tn/esprit/Boussole/GUI/JournalFranchise.fxml", "Journal des Opérations");
    }

    private void changerScene(javafx.event.ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            Logger.getLogger(DashboardFranchiseController.class.getName()).log(Level.SEVERE, null, e);
            afficherMessageErreur("Impossible de charger la vue : " + fxmlPath + "\n" + e.getMessage());
        }
    }

    // --- Theme Toggle ---
    @FXML private ToggleButton btnTheme;

    @FXML
    private void toggleTheme() {
        ThemeManager.getInstance().toggleTheme();
        if (btnTheme != null) {
            btnTheme.setText(ThemeManager.getInstance().isDark() ? "🌞 Mode Clair" : "🌙 Mode Sombre");
        }
    }
}
