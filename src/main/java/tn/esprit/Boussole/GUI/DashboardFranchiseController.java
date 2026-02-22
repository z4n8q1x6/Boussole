package tn.esprit.Boussole.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import tn.esprit.Boussole.Models.budget_previsionnel;
import tn.esprit.Boussole.Models.budget_previsionnel.TypeBudget;
import tn.esprit.Boussole.Models.transaction;
import tn.esprit.Boussole.Services.ServiceBudgetPrevisionnel;
import tn.esprit.Boussole.Services.ServiceDevise;
import tn.esprit.Boussole.Services.ServiceTransaction;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.cell.PropertyValueFactory;

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

            // Configure table columns
            if (colDate != null) colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            if (colType != null) colType.setCellValueFactory(new PropertyValueFactory<>("type"));
            if (colDescription != null) colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            if (colMontant != null) colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));

            // Load initial data
            if (franchiseId != 0) {
                // Initialisation de la combo box devises
                if (cbDevise != null) {
                    cbDevise.getItems().addAll("EUR", "USD", "GBP", "CAD");
                    cbDevise.setValue("EUR"); // Valeur par défaut
                    cbDevise.setOnAction(e -> chargerSolde());
                }

                chargerSolde();
                chargerTransactions();
                chargerBudgets();
            }

            // Wire button action
            if (btnValider != null) {
                btnValider.setOnAction(this::validerRecette);
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

            // Enable professional sorting for the TableView
            tableMovements.getSortOrder().add(colDate); // Set default sorting by Date

            // Corrected comparator for the Montant column
            colMontant.setComparator(Comparator.comparingDouble(Double::doubleValue));

            // Allow multi-column sorting with simple logic to avoid type complexity issues
            tableMovements.setSortPolicy(table -> {
                FXCollections.sort(table.getItems(), (t1, t2) -> {
                    if (t1.getDate() == null || t2.getDate() == null) return 0;
                    return t2.getDate().compareTo(t1.getDate()); // Default sort: desc date
                });
                return true;
            });

            // Correction du tri professionnel
            configurerTriTable(); // Removed to avoid complexity issues for now

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during Dashboard initialization: " + e.getMessage());
            Logger.getLogger(DashboardFranchiseController.class.getName()).log(Level.SEVERE, null, e);
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

    private void modifierTransaction(transaction t) {
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
        btnValider.setText("Valider Recette");
    }

    /**
     * Change scene to historique view
     */
    @FXML
    void versHistorique(javafx.event.ActionEvent event) {
        changerScene(event, "/tn/esprit/Boussole/GUI/JournalFranchise.fxml", "Journal des Opérations");
    }

    // Correction des paramètres inutilisés dans changerScene (valeurs constantes)
    // Je vais modifier la méthode pour utiliser les arguments
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


    // Correction de la méthode refreshTable
    private void refreshTable() {
        try {
            List<transaction> transactions = serviceTransaction.getAllByFranchise(franchiseId);
            ObservableList<transaction> data = FXCollections.observableArrayList(transactions);
            tableMovements.setItems(data);
        } catch (Exception e) {
            afficherMessageErreur("Erreur lors du rafraîchissement de la table : " + e.getMessage());
        }
    }

    // Ajout des méthodes manquantes pour gérer les messages d'erreur et de succès
    private void afficherMessageErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherMessageSucces(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Correction de la méthode configurerTriTable avec un tri professionnel et sans warnings
    private void configurerTriTable() {
        tableMovements.setSortPolicy(table -> {
            ObservableList<transaction> items = table.getItems();
            if (items == null || items.isEmpty()) return true;

            Comparator<transaction> comparator = (o1, o2) -> 0;

            for (TableColumn<transaction, ?> col : table.getSortOrder()) {
                Comparator<transaction> colComparator = (t1, t2) -> {
                    Object v1 = col.getCellData(t1);
                    Object v2 = col.getCellData(t2);
                    if (v1 == null && v2 == null) return 0;
                    if (v1 == null) return 1;
                    if (v2 == null) return -1;
                    if (v1 instanceof Comparable && v2 instanceof Comparable) {
                        try {
                            return ((Comparable) v1).compareTo(v2);
                        } catch (Exception e) {
                            return 0;
                        }
                    }
                    return 0;
                };

                if (col.getSortType() == TableColumn.SortType.DESCENDING) {
                    colComparator = colComparator.reversed();
                }

                comparator = comparator.thenComparing(colComparator);
            }

            FXCollections.sort(items, comparator);
            return true;
        });
    }
}
