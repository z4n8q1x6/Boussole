package tn.esprit.Boussole.GUI;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import tn.esprit.Boussole.Models.budget_previsionnel;
import tn.esprit.Boussole.Models.transaction;
import tn.esprit.Boussole.Models.transaction.Type;
import tn.esprit.Boussole.Services.ServiceTransaction;
import tn.esprit.Boussole.Services.ServiceBudgetPrevisionnel;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardFranchiseController implements Initializable {

    // FXML Components
    @FXML
    private Label lblSolde;

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
    private BorderPane contentPane;

    // Budget Table
    @FXML private TableView<budget_previsionnel> tableBudgets;
    @FXML private TableColumn<budget_previsionnel, Integer> colBudgetMois;
    @FXML private TableColumn<budget_previsionnel, Integer> colBudgetAnnee;
    @FXML private TableColumn<budget_previsionnel, String> colBudgetType;
    @FXML private TableColumn<budget_previsionnel, String> colBudgetCategorie;
    @FXML private TableColumn<budget_previsionnel, Double> colBudgetMontant;

    // Budget KPI Labels
    @FXML private Label lblLimiteDepenses;
    @FXML private Label lblObjectifRevenu;
    @FXML private Label lblDepensesMois;

    // Services
    private ServiceTransaction serviceTransaction;
    private ServiceBudgetPrevisionnel serviceBudgetPrevisionnel;

    // Session
    private int franchiseId;
    // CRUD State
    private Integer idTransactionAModifier = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialize services
            try {
                serviceTransaction = new ServiceTransaction();
                serviceBudgetPrevisionnel = new ServiceBudgetPrevisionnel();
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

            // Configure table columns
            if (colDate != null) colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            if (colType != null) colType.setCellValueFactory(new PropertyValueFactory<>("type"));
            if (colDescription != null) colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            if (colMontant != null) colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));

            // Configure budget table columns
            configurerColonnesBudget();

            // Load initial data
            if (franchiseId != 0) {
                chargerSolde();
                chargerTransactions();
                chargerBudgets();
            }

            // Wire button action
            if (btnValider != null) {
                btnValider.setOnAction(event -> validerRecette(event));
            }

            // --- MENU CONTEXTUEL (Clic Droit) ---
            ContextMenu contextMenu = new ContextMenu();
            MenuItem itemModifier = new MenuItem("Modifier");
            MenuItem itemSupprimer = new MenuItem("Supprimer");

            itemModifier.setOnAction(e -> {
                transaction selected = tableMovements.getSelectionModel().getSelectedItem();
                if (selected != null) modifierTransaction(selected);
            });

            itemSupprimer.setOnAction(e -> {
                transaction selected = tableMovements.getSelectionModel().getSelectedItem();
                if (selected != null) supprimerTransaction(selected);
            });

            contextMenu.getItems().addAll(itemModifier, itemSupprimer);
            tableMovements.setContextMenu(contextMenu);

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during Dashboard initialization: " + e.getMessage());
            e.printStackTrace();
            afficherMessageErreur("Erreur inattendue au chargement: " + e.getMessage());
        }
    }

    /**
     * Load and display solde from database
     */
    private void chargerSolde() {
        try {
            double solde = serviceTransaction.calculerSolde(franchiseId);
            lblSolde.setText(String.format("%.2f TND", solde));

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
     * Load all transactions (both Recettes and Dépenses) for this franchise
     */
    private void chargerTransactions() {
        try {
            List<transaction> transactions = serviceTransaction.getAllByFranchise(franchiseId);
            ObservableList<transaction> data = FXCollections.observableArrayList(transactions);
            tableMovements.setItems(data);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des transactions : " + e.getMessage());
        }
    }

    /**
     * Configure budget table columns
     */
    private void configurerColonnesBudget() {
        if (colBudgetMois != null) colBudgetMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        if (colBudgetAnnee != null) colBudgetAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        if (colBudgetType != null) {
            colBudgetType.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                    cellData.getValue().getType_budget() != null
                        ? cellData.getValue().getType_budget().toString().replace("_", " ")
                        : ""
                )
            );
        }
        if (colBudgetCategorie != null) colBudgetCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        if (colBudgetMontant != null) {
            colBudgetMontant.setCellValueFactory(new PropertyValueFactory<>("montantCible"));
            colBudgetMontant.setCellFactory(column -> new TableCell<budget_previsionnel, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f TND", item));
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            });
        }
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
                if (b.getType_budget() == budget_previsionnel.TypeBudget.LIMITE_DEPENSE) {
                    totalLimiteDepenses += b.getMontantCible();
                } else if (b.getType_budget() == budget_previsionnel.TypeBudget.OBJECTIF_REVENU) {
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
                    if (t.getType() == Type.DEPENSE) {
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
            // 1. Validation des champs
            if (dpDate.getValue() == null) {
                afficherMessageErreur("Veuillez sélectionner une date.");
                return;
            }

            if (tfMontant.getText().isEmpty() || tfDescription.getText().isEmpty()) {
                afficherMessageErreur("Veuillez remplir le montant et la description.");
                return;
            }

            double montant;
            try {
                montant = Double.parseDouble(tfMontant.getText());
                if (montant <= 0) {
                    afficherMessageErreur("Le montant doit être strictement positif.");
                    return;
                }
            } catch (NumberFormatException e) {
                afficherMessageErreur("Le montant doit être un nombre valide (ex: 150.50).");
                return;
            }

            // 2. Création de l'objet Transaction
            transaction t = new transaction();
            t.setDate(java.sql.Date.valueOf(dpDate.getValue()));
            t.setMontant(montant);
            t.setDescription(tfDescription.getText());
            t.setType(Type.RECETTE); // Enum RECETTE
            
            // Récupération ID Franchise depuis SessionManager
            int idFranchise = SessionManager.getInstance().getIdFranchise();
            if (idFranchise == 0) {
                 afficherMessageErreur("Erreur session : ID Franchise introuvable.");
                 return;
            }
            t.setFranchiseId(idFranchise);

            // 3. Sauvegarde via le Service (INSERT ou UPDATE)
            if (idTransactionAModifier != null) {
                // Mode UPDATE
                t.setId(idTransactionAModifier);
                serviceTransaction.updateOne(t);
                afficherMessageSucces("Transaction modifiée avec succès !");
            } else {
                // Mode INSERT
                serviceTransaction.insertOne(t);
                afficherMessageSucces("Recette ajoutée avec succès !");
            }

            // 4. Mise à jour de l'UI
            // Vider le formulaire et reset état
            viderFormulaire();

            // Rafraîchir les données
            chargerTransactions(); 
            chargerSolde();

        } catch (Exception e) {
            e.printStackTrace();
            afficherMessageErreur("Erreur lors de l'opération : " + e.getMessage());
        }
    }

    private void modifierTransaction(transaction t) {
        idTransactionAModifier = t.getId();
        tfMontant.setText(String.valueOf(t.getMontant()));
        tfDescription.setText(t.getDescription());
        if (t.getDate() != null) {
            dpDate.setValue(new java.sql.Date(t.getDate().getTime()).toLocalDate());
        }
        btnValider.setText("Modifier");
    }

    private void supprimerTransaction(transaction t) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la transaction ?");
        alert.setContentText("Voulez-vous vraiment supprimer : " + t.getDescription() + " (" + t.getMontant() + " TND) ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                serviceTransaction.deleteOne(t);
                chargerTransactions();
                chargerSolde();
                afficherMessageSucces("Transaction supprimée.");
                viderFormulaire(); // Au cas où on modifiait celle-ci
            }
        });
    }

    private void viderFormulaire() {
        tfMontant.clear();
        tfDescription.clear();
        dpDate.setValue(LocalDate.now());
        idTransactionAModifier = null;
        btnValider.setText("Valider Recette");
    }

    /**
     * Verify if expenses exceed budget for the current month/franchise
     * Called after each expense (when integrated with other modules)
     */
    private void verifierBudget() {
        try {
            // Get current month and year
            LocalDate now = LocalDate.now();
            int mois = now.getMonthValue();
            int annee = now.getYear();

            // Get current spending for this franchise in current month
            List<transaction> transactionsCurrentMonth = serviceTransaction.getAllByFranchise(franchiseId);
            double depensesActuelles = 0.0;
            for (transaction t : transactionsCurrentMonth) {
                if (t.getType() == Type.DEPENSE && t.getDate() != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(new java.util.Date(t.getDate().getTime()));
                    if (cal.get(java.util.Calendar.MONTH) + 1 == mois && cal.get(java.util.Calendar.YEAR) == annee) {
                        depensesActuelles += t.getMontant();
                    }
                }
            }

            // Get budget limit for this franchise
            // Note: getBudgetActuel returns a budget_previsionnel object, not a Map
            tn.esprit.Boussole.Models.budget_previsionnel budgetActuel = serviceBudgetPrevisionnel.getBudgetActuel(franchiseId, mois, annee, "GLOBAL");

            if (budgetActuel != null) {
                double budgetLimit = budgetActuel.getMontantCible();
                if (depensesActuelles > budgetLimit) {
                    afficherMessageAvertissement(
                        "Attention ! Vous avez dépassé votre budget mensuel.\n" +
                        "Dépenses actuelles: " + String.format("%.2f", depensesActuelles) + " TND\n" +
                        "Budget limite: " + String.format("%.2f", budgetLimit) + " TND"
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du budget : " + e.getMessage());
        }
    }

    /**
     * Helper method to show success alert
     */
    private void afficherMessageSucces(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Helper method to show error alert
     */
    private void afficherMessageErreur(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Helper method to show warning alert (for budget warning)
     */
    private void afficherMessageAvertissement(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    @FXML
    private Button btnHistorique;

    @FXML
    void versHistorique(javafx.event.ActionEvent event) {
        changerScene(event, "/tn/esprit/Boussole/GUI/JournalFranchise.fxml", "Journal des Opérations");
    }

    private void changerScene(javafx.event.ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            try {
                 URL cssUrl = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css");
                 if(cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            } catch(Exception ignored){}
            
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            afficherMessageErreur("Impossible de charger la vue : " + fxmlPath + "\n" + e.getMessage());
        }
    }
}
