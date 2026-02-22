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
import javafx.scene.control.ButtonType; // Added
import javafx.scene.control.ContextMenu; // Added
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem; // Added
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell; // Added
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter; // Added
import tn.esprit.Boussole.Models.Charge;
import tn.esprit.Boussole.Models.budget_previsionnel;
import tn.esprit.Boussole.Services.ServiceBudgetPrevisionnel;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GestionBudgetsController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnBudgets;
    @FXML private Button btnBilans;
    @FXML private ComboBox<Integer> cbMois;
    @FXML private ComboBox<Integer> cbAnnee;
    @FXML private ComboBox<budget_previsionnel.TypeBudget> cbTypeBudget;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private TextField txtMontant;
    @FXML private CheckBox chkReseau;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnVider;
    @FXML private TableView<budget_previsionnel> tableBudgets;
    //@FXML private TableColumn<budget_previsionnel, Void> colActions; // Supprimé
    @FXML private TableColumn<budget_previsionnel, Integer> colMois;
    @FXML private TableColumn<budget_previsionnel, Integer> colAnnee;
    @FXML private TableColumn<budget_previsionnel, Object> colType;
    @FXML private TableColumn<budget_previsionnel, String> colCategorie;
    @FXML private TableColumn<budget_previsionnel, Double> colMontant;

    private ServiceBudgetPrevisionnel serviceBudget;
    private Integer idBudgetAModifier = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceBudget = new ServiceBudgetPrevisionnel();

        // 1. Configuration des ComboBox
        for (int mois = 1; mois <= 12; mois++) cbMois.getItems().add(mois);
        cbMois.getSelectionModel().selectFirst();

        for (int annee = 2024; annee <= 2030; annee++) cbAnnee.getItems().add(annee);
        cbAnnee.getSelectionModel().selectFirst();

        cbTypeBudget.getItems().addAll(budget_previsionnel.TypeBudget.LIMITE_DEPENSE, budget_previsionnel.TypeBudget.OBJECTIF_REVENU);
        cbTypeBudget.getSelectionModel().selectFirst();
        cbTypeBudget.setOnAction(event -> mettreAJourCategories());
        mettreAJourCategories();

        // 2. Configuration des Colonnes
        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_budget"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantCible"));

        // *** UX MODERNE : TABLE ÉDITABLE ***
        tableBudgets.setEditable(true); // Activer l'édition sur le tableau

        // Configuration pour l'édition du montant
        colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMontant.setOnEditCommit(event -> {
            budget_previsionnel budget = event.getRowValue();
            Double nouveauMontant = event.getNewValue();

            // Validation simple
            if (nouveauMontant == null || nouveauMontant <= 0) {
                afficherMessageErreur("Le montant doit être valide et supérieur à 0.");
                tableBudgets.refresh(); // Annuler visuellement
                return;
            }

            budget.setMontantCible(nouveauMontant);

            try {
                // Mise à jour immédiate en BDD
                serviceBudget.updateOne(budget);
                // System.out.println("DEBUG: Update montant effectué via édition cellule");
            } catch (Exception e) {
                afficherMessageErreur("Erreur lors de la mise à jour : " + e.getMessage());
                tableBudgets.refresh();
            }
        });

        // *** UX MODERNE : MENU CONTEXTUEL POUR SUPPRESSION ***
        // Suppression de l'ancienne colonne Actions
        if (tableBudgets.getColumns().size() > 5) { // Sécurité basique
             // tableBudgets.getColumns().remove(colActions); // Si injecté via FXML, il faut le retirer
             // Note: Si colActions est injecté mais non utilisé, il reste null si je l'enlève du FXML.
             // Ici je vais créer le context menu.
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemSupprimer = new MenuItem("🗑️ Supprimer cette ligne");
        itemSupprimer.setOnAction(e -> {
            budget_previsionnel selected = tableBudgets.getSelectionModel().getSelectedItem();
            if (selected != null) {
                supprimerBudget(selected);
            }
        });
        contextMenu.getItems().add(itemSupprimer);

        // Assigner le menu contextuel à chaque ligne (row)
        tableBudgets.setRowFactory(tv -> {
            javafx.scene.control.TableRow<budget_previsionnel> row = new javafx.scene.control.TableRow<>();
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(contextMenu)
            );
            return row;
        });


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

        // Modification : Le clic simple/double clic remplit toujours le formulaire pour information
        tableBudgets.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) modifierBudget(newSelection); // Remplit le formulaire du haut
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
            Logger.getLogger(GestionBudgetsController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void mettreAJourCategories() {
        cbCategorie.getItems().clear();

        budget_previsionnel.TypeBudget typeBudget = cbTypeBudget.getValue();

        if (typeBudget == budget_previsionnel.TypeBudget.LIMITE_DEPENSE) {
            for (Charge.TypeCharge charge : Charge.TypeCharge.values()) {
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
        if (!validerSaisie()) {
            return;
        }

        try {
            budget_previsionnel budget = new budget_previsionnel();
            budget.setMois(cbMois.getValue());
            budget.setAnnee(cbAnnee.getValue());
            budget.setType_budget(cbTypeBudget.getValue());
            budget.setCategorie(cbCategorie.isDisabled() ? "GLOBAL" : cbCategorie.getValue());
            budget.setMontantCible(Double.parseDouble(txtMontant.getText()));
            budget.setFranchiseId(SessionManager.getInstance().getIdFranchise());

            if (idBudgetAModifier != null) {
                budget.setId(idBudgetAModifier);
                serviceBudget.updateOne(budget);
                afficherMessageSucces("Budget modifié avec succès !");
            } else {
                serviceBudget.add(budget);
                afficherMessageSucces("Budget ajouté avec succès.");
            }

            rafraichirTable();
            nettoyerFormulaire();
        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    // Ajout de la validation de saisie
    private boolean validerSaisie() {
        if (cbMois.getValue() == null || cbAnnee.getValue() == null || cbTypeBudget.getValue() == null) {
            afficherMessageErreur("Veuillez sélectionner le mois, l'année et le type de budget.");
            return false;
        }

        if (cbTypeBudget.getValue() == budget_previsionnel.TypeBudget.LIMITE_DEPENSE && cbCategorie.getValue() == null) {
            afficherMessageErreur("Veuillez sélectionner une catégorie pour le type de budget 'LIMITE_DEPENSE'.");
            return false;
        }

        if (txtMontant.getText().isEmpty()) {
            afficherMessageErreur("Veuillez entrer un montant.");
            return false;
        }

        try {
            double montant = Double.parseDouble(txtMontant.getText());
            if (montant <= 0) {
                afficherMessageErreur("Le montant doit être supérieur à 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            afficherMessageErreur("Le montant doit être un nombre valide.");
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
        cbMois.setValue(b.getMois());
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
     * Configure la colonne Actions (OBSOLÈTE - Laissée vide ou à supprimer du FXML)
     */
    private void configurerColonneActions() {
        // Méthode désactivée pour UX Moderne
    }



    private void nettoyerFormulaire() {
        cbMois.getSelectionModel().selectFirst();
        cbAnnee.getSelectionModel().selectFirst();
        cbTypeBudget.getSelectionModel().selectFirst();
        mettreAJourCategories();
        txtMontant.clear();
        chkReseau.setSelected(false);
        idBudgetAModifier = null;
        btnSauvegarder.setText("Sauvegarder");
    }

    private void viderFormulaire() {
        cbMois.getSelectionModel().clearSelection();
        cbAnnee.getSelectionModel().clearSelection();
        cbTypeBudget.getSelectionModel().clearSelection();
        cbCategorie.getSelectionModel().clearSelection();
        txtMontant.clear();
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

    @FXML
    private void initialize() {
        cbMois.getItems().addAll(IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList()));
        btnVider.setOnAction(event -> viderFormulaire());
    }

    private void changerPage(ActionEvent event, String fxmlPath) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                afficherMessageErreur("Fichier FXML introuvable : " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);

            // Ajout d'une vérification pour éviter le NullPointerException
            URL cssUrl = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css");
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                scene.getStylesheets().add(css);
            } else {
                Logger.getLogger(GestionBudgetsController.class.getName()).log(Level.WARNING, "CSS non chargée");
            }

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            Logger.getLogger(GestionBudgetsController.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
