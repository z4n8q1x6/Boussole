package tn.esprit.Boussole.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.Boussole.Models.budget_previsionnel;
import tn.esprit.Boussole.Models.TypeCharge;
import tn.esprit.Boussole.Services.ServiceBudgetPrevisionnel;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GestionBudgetsController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnBudgets;
    @FXML private Button btnBilans;
    @FXML private ComboBox<Integer> combMois;
    @FXML private ComboBox<Integer> cbAnnee;
    @FXML private ComboBox<budget_previsionnel.TypeBudget> cbTypeBudget;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private TextField txtMontant;
    @FXML private CheckBox chkReseau;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnVider; // Bouton pour vider le formulaire
    @FXML private TableView<budget_previsionnel> tableBudgets;
    @FXML private TableColumn<budget_previsionnel, Void> colActions;
    @FXML private TableColumn<budget_previsionnel, Integer> colMois;
    @FXML private TableColumn<budget_previsionnel, Integer> colAnnee;
    @FXML private TableColumn<budget_previsionnel, Object> colType;
    @FXML private TableColumn<budget_previsionnel, String> colCategorie;
    @FXML private TableColumn<budget_previsionnel, Double> colMontant;

    private ServiceBudgetPrevisionnel serviceBudget;
    private static final int FRANCHISE_ID = 1;
    private Integer idBudgetAModifier = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceBudget = new ServiceBudgetPrevisionnel();

        // 1. Configuration des ComboBox
        for (int mois = 1; mois <= 12; mois++) combMois.getItems().add(mois);
        combMois.getSelectionModel().selectFirst();

        for (int annee = 2024; annee <= 2030; annee++) cbAnnee.getItems().add(annee);
        cbAnnee.getSelectionModel().selectFirst();

        cbTypeBudget.getItems().addAll(budget_previsionnel.TypeBudget.LIMITE_DEPENSE, budget_previsionnel.TypeBudget.OBJECTIF_REVENU);
        cbTypeBudget.getSelectionModel().selectFirst();
        cbTypeBudget.setOnAction(event -> mettreAJourCategories());
        mettreAJourCategories();

        // 2. Configuration des Colonnes (CORRECTION)
        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_budget"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantCible"));

        // 3. Configuration de la colonne Actions
        configurerColonneActions();

        // 4. Chargement initial des données
        rafraichirTable();

        // 5. Gestion des Événements
        btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"));
        btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBudgets.fxml"));
        btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBilans.fxml"));
        
        btnSauvegarder.setOnAction(event -> sauvegarderBudget());

        if (btnVider != null) {
            btnVider.setOnAction(event -> nettoyerFormulaire());
        }

        tableBudgets.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) modifierBudget(newSelection);
        });
    }

    /**
     * Récupère les données depuis le service et rafraîchit la TableView.
     */
    private void rafraichirTable() {
        try {
            tableBudgets.getItems().clear();
            // Récupère l'ID franchise de la session (ou 1 par défaut si session vide/siège pour test)
            int franchiseId = SessionManager.getInstance().getIdFranchise();
            if (franchiseId == 0) franchiseId = 1; // Fallback pour voir les données en mode Siège/Test

            List<budget_previsionnel> budgets = serviceBudget.getAllByFranchise(franchiseId);
            tableBudgets.getItems().addAll(budgets);
        } catch (Exception e) {
            System.err.println("Erreur lors du rafraîchissement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mettreAJourCategories() {
        cbCategorie.getItems().clear();

        budget_previsionnel.TypeBudget typeBudget = cbTypeBudget.getValue();

        if (typeBudget == budget_previsionnel.TypeBudget.LIMITE_DEPENSE) {
            for (TypeCharge charge : TypeCharge.values()) {
                cbCategorie.getItems().add(charge.name());
            }
            cbCategorie.setDisable(false);
            cbCategorie.getSelectionModel().selectFirst();
        } else if (typeBudget == budget_previsionnel.TypeBudget.OBJECTIF_REVENU) {
            cbCategorie.getItems().add("GLOBAL");
            cbCategorie.setDisable(true);
            cbCategorie.getSelectionModel().selectFirst();
        }
    }

    /**
     * Sauvegarde ou modifie un budget
     * UPDATE si idBudgetAModifier != null, sinon INSERT
     */
    private void sauvegarderBudget() {
        // Valider le formulaire d'abord
        if (!validerFormulaireBudget()) {
            return; // Message d'erreur déjà affiché par la méthode de validation
        }

        try {
            int mois = combMois.getValue();
            int annee = cbAnnee.getValue();
            budget_previsionnel.TypeBudget typeBudget = cbTypeBudget.getValue();
            String categorie = cbCategorie.getValue();
            double montant = Double.parseDouble(txtMontant.getText());

            if (idBudgetAModifier != null) {
                // UPDATE
                budget_previsionnel budget = new budget_previsionnel(
                    idBudgetAModifier, mois, annee, montant, typeBudget, categorie, FRANCHISE_ID
                );
                serviceBudget.updateOne(budget);
                afficherMessageSucces("Budget modifié avec succès !");
                idBudgetAModifier = null;
                btnSauvegarder.setText("Sauvegarder");
            } else {
                // INSERT
                budget_previsionnel budget = new budget_previsionnel(
                    mois, annee, montant, typeBudget, categorie, FRANCHISE_ID
                );
                serviceBudget.add(budget);
                afficherMessageSucces("Budget créé avec succès !");
            }

            rafraichirTable();
            nettoyerFormulaire();

        } catch (Exception e) {
            afficherMessageErreur("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Valide le formulaire de budget
     * Retourne true si valide, false sinon
     */
    private boolean validerFormulaireBudget() {
        // Vérifier Mois
        if (combMois.getValue() == null) {
            afficherMessageErreur("Veuillez sélectionner un mois");
            return false;
        }

        // Vérifier Année
        if (cbAnnee.getValue() == null) {
            afficherMessageErreur("Veuillez sélectionner une année");
            return false;
        }

        // Vérifier Type Budget
        if (cbTypeBudget.getValue() == null) {
            afficherMessageErreur("Veuillez sélectionner un type de budget");
            return false;
        }

        // Vérifier Catégorie (sauf si OBJECTIF_REVENU, car elle est désactivée)
        budget_previsionnel.TypeBudget typeBudget = cbTypeBudget.getValue();
        if (typeBudget == budget_previsionnel.TypeBudget.LIMITE_DEPENSE) {
            if (cbCategorie.getValue() == null) {
                afficherMessageErreur("Veuillez sélectionner une catégorie");
                return false;
            }
        }

        // Vérifier Montant
        String montantText = txtMontant.getText();
        if (montantText == null || montantText.isEmpty()) {
            afficherMessageErreur("Veuillez entrer un montant");
            return false;
        }

        try {
            double montant = Double.parseDouble(montantText);
            if (montant <= 0) {
                afficherMessageErreur("Le montant doit être strictement positif");
                return false;
            }
        } catch (NumberFormatException e) {
            afficherMessageErreur("Montant invalide : doit être un nombre valide");
            return false;
        }

        return true;
    }

    /**
     * Charge le budget pour modification
     */
    private void modifierBudget(budget_previsionnel b) {
        if (b == null) return;

        idBudgetAModifier = b.getId();
        combMois.setValue(b.getMois());
        cbAnnee.setValue(b.getAnnee());
        cbTypeBudget.setValue(b.getType_budget());
        cbCategorie.setValue(b.getCategorie());
        txtMontant.setText(String.valueOf(b.getMontantCible()));
        btnSauvegarder.setText("Modifier");
    }

    /**
     * Supprime un budget après confirmation
     */
    private void supprimerBudget(budget_previsionnel b) {
        if (!confirmerAction("Voulez-vous vraiment supprimer ce budget ?\nCette action est irréversible.")) {
            return; // Utilisateur a cliqué Annuler
        }

        try {
            serviceBudget.deleteOne(b);
            rafraichirTable();
            afficherMessageSucces("Budget supprimé avec succès !");
        } catch (Exception e) {
            afficherMessageErreur("Erreur suppression : " + e.getMessage());
        }
    }

    /**
     * Configure la colonne Actions
     */
    private void configurerColonneActions() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("✏️");
            private final Button btnSupprimer = new Button("🗑️");
            private final HBox hbox = new HBox(5, btnModifier, btnSupprimer);

            {
                btnModifier.getStyleClass().add("button-action-edit");
                btnSupprimer.getStyleClass().add("button-action-delete");
                hbox.setPadding(new Insets(2));

                btnModifier.setOnAction(event -> {
                    budget_previsionnel b = getTableView().getItems().get(getIndex());
                    modifierBudget(b);
                });

                btnSupprimer.setOnAction(event -> {
                    budget_previsionnel b = getTableView().getItems().get(getIndex());
                    supprimerBudget(b);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }



    private void nettoyerFormulaire() {
        combMois.getSelectionModel().selectFirst();
        cbAnnee.getSelectionModel().selectFirst();
        cbTypeBudget.getSelectionModel().selectFirst();
        mettreAJourCategories();
        txtMontant.clear();
        chkReseau.setSelected(false);
        idBudgetAModifier = null;
        btnSauvegarder.setText("Sauvegarder");
    }

    /**
     * Affiche un message de succès (Alert INFORMATION)
     */
    private void afficherMessageSucces(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche un message d'erreur (Alert ERROR)
     */
    private void afficherMessageErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Demande une confirmation à l'utilisateur (Alert CONFIRMATION)
     * Retourne true si OK cliqué, false sinon
     */
    private boolean confirmerAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
            .map(result -> result == javafx.scene.control.ButtonType.OK)
            .orElse(false);
    }

    private void changerPage(ActionEvent event, String fxmlPath) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) return;

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);

            try {
                String css = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("CSS non chargée");
            }

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



