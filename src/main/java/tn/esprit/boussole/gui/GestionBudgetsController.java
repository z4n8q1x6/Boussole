package tn.esprit.boussole.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.boussole.Utilis.MyBdConnexion;
import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.models.budget_previsionnel;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.services.ServiceBudgetPrevisionnel;
import tn.esprit.boussole.Utilis.NotificationManager;
import tn.esprit.boussole.Utilis.SessionManager;

import tn.esprit.boussole.Utilis.ThemeManager;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.util.Callback;

public class GestionBudgetsController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnBudgets;
    @FXML private Button btnBilans;
    @FXML private ComboBox<Integer> cbMois;
    @FXML private ComboBox<Integer> cbAnnee;
    @FXML private ComboBox<budget_previsionnel.TypeBudget> cbTypeBudget;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private TextField txtMontant;
    @FXML private ListView<franchise> listFranchises; // Changed from CheckBox to ListView
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

        listFranchises.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Configuration de l'affichage (nom seulement)
        listFranchises.setCellFactory(new Callback<ListView<franchise>, ListCell<franchise>>() {
            @Override
            public ListCell<franchise> call(ListView<franchise> param) {
                return new ListCell<franchise>() {
                    @Override
                    protected void updateItem(franchise item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getNom());
                        }
                    }
                };
            }
        });
        
        chargerFranchises();

        // 1. Configuration des ComboBox de formulaire
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

        tableBudgets.setEditable(true);

        // Style pour colType
        colType.setCellFactory(column -> new TableCell<budget_previsionnel, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    String typeStr = item.toString();
                    if ("OBJECTIF_REVENU".equals(typeStr)) {
                        setText(" Objectif Revenu");
                        Label icon = new Label("🎯");
                        icon.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                        setGraphic(icon);
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    } else {
                        setText(" Limite Dépense");
                        Label icon = new Label("🛑");
                        icon.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                        setGraphic(icon);
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    }
                }
            }
        });

        Integer[] moisArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        colMois.setCellFactory(ComboBoxTableCell.forTableColumn(moisArray));
        colMois.setOnEditCommit(event -> {
            budget_previsionnel budget = event.getRowValue();
            Integer nvMois = event.getNewValue();
            if (nvMois != null) {
                budget.setMois(nvMois);
                try {
                    serviceBudget.updateone(budget);
                    afficherMessageSucces("Mois modifié !");
                } catch (Exception e) {
                    afficherMessageErreur("Erreur : " + e.getMessage());
                    tableBudgets.refresh();
                }
            }
        });

        Integer[] anneeArray = {2024, 2025, 2026, 2027, 2028, 2029, 2030};
        colAnnee.setCellFactory(ComboBoxTableCell.forTableColumn(anneeArray));
        colAnnee.setOnEditCommit(event -> {
            budget_previsionnel budget = event.getRowValue();
            Integer nvAnnee = event.getNewValue();
            if (nvAnnee != null) {
                budget.setAnnee(nvAnnee);
                try {
                    serviceBudget.updateone(budget);
                    afficherMessageSucces("Année modifiée !");
                } catch (Exception e) {
                    afficherMessageErreur("Erreur : " + e.getMessage());
                    tableBudgets.refresh();
                }
            }
        });

        // Configuration correcte de la colonne CATEGORIE en ComboBox éditable
        java.util.List<String> categoriesList = new java.util.ArrayList<>();
        categoriesList.add("GLOBAL");
        java.util.Arrays.stream(Charge.TypeCharge.values())
                .map(Enum::name)
                .forEach(categoriesList::add);
        String[] categories = categoriesList.toArray(new String[0]);

        colCategorie.setCellFactory(ComboBoxTableCell.forTableColumn(categories));

        colCategorie.setOnEditCommit(event -> {
            budget_previsionnel budget = event.getRowValue();
            String nouvelleCategorie = event.getNewValue();
            if (nouvelleCategorie == null || nouvelleCategorie.isEmpty()) return;
            budget.setCategorie(nouvelleCategorie);
            try {
                serviceBudget.updateone(budget);
            } catch (Exception e) {
                afficherMessageErreur("Erreur lors de la mise à jour de la catégorie : " + e.getMessage());
                tableBudgets.refresh();
            }
        });


        // Montant éditable
        colMontant.setCellFactory(column -> new TableCell<budget_previsionnel, Double>() {
             @Override
             protected void updateItem(Double item, boolean empty) {
                 super.updateItem(item, empty);
                 if (empty || item == null) {
                     setText(null);
                     setStyle("");
                 } else {
                     setText(String.format("%.2f TND", item));
                     // Couleur dynamique selon le type de la ligne
                     budget_previsionnel b = getTableView().getItems().get(getIndex());
                     if (b != null && b.getType_budget() == budget_previsionnel.TypeBudget.OBJECTIF_REVENU) {
                         setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                     } else {
                         setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                     }
                 }
             }
        });
        /*
        // Suppression de l'ancienne factory editable pour privilégier le look.
        // Si on veut éditer le montant, on peut remettre TextFieldTableCell mais il faut le styliser lourdement.
        // Ici on privilégie l'harmonie visuelle demandée.
        // colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        */

        // On remet la logique double click pour modifier via le formulaire (déjà existante)
        tableBudgets.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableBudgets != null) {
                 // Logic handled by existing selection listener or can be added here
                 budget_previsionnel selected = tableBudgets.getSelectionModel().getSelectedItem();
                 if (selected != null) {
                     modifierBudget(selected);
                 }
            }
        });

        // Menu contextuel pour suppression (on garde)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemSupprimer = new MenuItem("🗑️ Supprimer cette ligne");
        itemSupprimer.setOnAction(e -> {
            budget_previsionnel selected = tableBudgets.getSelectionModel().getSelectedItem();
            if (selected != null) {
                supprimerBudget(selected);
            }
        });
        contextMenu.getItems().add(itemSupprimer);

        // Simplification du RowFactory pour éliminer les anciennes classes CSS "row-revenu/depense"
        // et s'aligner sur le style "Historique Bilans" (noir profond par défaut via styles.css)
        tableBudgets.setRowFactory(tv -> {
            TableRow<budget_previsionnel> row = new TableRow<>();
            // On garde uniquement le context menu
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(contextMenu)
            );
            return row;
        });

        // Chargement initial des données
        rafraichirTable();

        // Navigation & autres listeners existants
        btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/boussole/gui/DashboardSiege.fxml"));
        btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/boussole/gui/GestionBudgets.fxml"));
        btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/boussole/gui/GestionBilans.fxml"));
        
        btnSauvegarder.setOnAction(event -> sauvegarderBudget());

        if (btnVider != null) {
            btnVider.setOnAction(event -> nettoyerFormulaire());
        }

        // Modification : la modification se fait en ligne et non depuis le formulaire.
        // Listener supprimé.
    }

    /**
     * Récupère les données depuis le service et rafraîchit la TableView.
     */
    private void rafraichirTable() {
        try {
            tableBudgets.getItems().clear();
            
            String role = SessionManager.getInstance().getRole();
            if ("SIEGE".equals(role) || role == null) {
                // Le siège voit tous les budgets
                List<budget_previsionnel> budgets = serviceBudget.selectAll();
                tableBudgets.getItems().addAll(budgets);
            } else {
                // La franchise ne voit que ses propres budgets
                int franchiseId = SessionManager.getInstance().getIdFranchise();
                List<budget_previsionnel> budgets = serviceBudget.getAllByFranchise(franchiseId);
                tableBudgets.getItems().addAll(budgets);
            }
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
     * Charge la liste des franchises pour la ListView
     */
    private void chargerFranchises() {
        System.out.println("--- DÉBUT chargerFranchises() ---");
        listFranchises.getItems().clear();
        
        // Pseudo-franchise pour "Tous le réseau"
        franchise tous = new franchise();
        tous.setId(0);
        tous.setNom("Tous le réseau");
        listFranchises.getItems().add(tous);
        
        String sql = "SELECT id, nom FROM franchises";
        int count = 0;
        
        java.sql.Connection cnx = MyBdConnexion.getinstance().getCnx();
        try (java.sql.PreparedStatement ps = cnx.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                franchise f = new franchise();
                f.setId(rs.getInt("id"));
                f.setNom(rs.getString("nom"));
                listFranchises.getItems().add(f);
                count++;
            }
            System.out.println("Franchises trouvées et ajoutées à la liste : " + count);
        } catch (java.sql.SQLException e) {
            System.out.println("Erreur SQL chargement franchises: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur INCONNUE chargement franchises: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("--- FIN chargerFranchises() ---");
    }

    /**
     * Sauvegarde ou modifie un budget
     * UPDATE si idBudgetAModifier != null, sinon INSERT par franchise sélectionnée
     */
    private void sauvegarderBudget() {
        if (!validerSaisie()) {
            return;
        }

        List<franchise> selection = listFranchises.getSelectionModel().getSelectedItems();
        if (selection == null || selection.isEmpty()) {
            afficherMessageErreur("Veuillez sélectionner au moins une franchise cible.");
            return;
        }

        try {
            boolean isModification = (idBudgetAModifier != null);
            
            // Vérifier si "Tous le réseau" est sélectionné
            boolean applyToAll = selection.stream().anyMatch(f -> f.getId() != null && f.getId() == 0);
            
            List<franchise> finalTargets = new java.util.ArrayList<>();
            if (applyToAll) {
                // Ajouter toutes les franchises réelles (id != 0)
                listFranchises.getItems().stream()
                        .filter(f -> f.getId() != null && f.getId() != 0)
                        .forEach(finalTargets::add);
            } else {
                finalTargets.addAll(selection);
            }

            for (franchise f : finalTargets) {
                int fId = f.getId();
                
                budget_previsionnel budget = new budget_previsionnel();
                budget.setMois(cbMois.getValue());
                budget.setAnnee(cbAnnee.getValue());
                budget.setType_budget(cbTypeBudget.getValue());
                budget.setCategorie(cbCategorie.isDisabled() ? "GLOBAL" : cbCategorie.getValue());
                budget.setMontantCible(Double.parseDouble(txtMontant.getText()));
                budget.setFranchiseId(fId);

                if (isModification) {
                    budget.setId(idBudgetAModifier);
                    serviceBudget.updateone(budget);
                } else {
                    serviceBudget.add(budget);
                }
            }
            
            if (isModification) {
                afficherMessageSucces("Budget(s) modifié(s) avec succès !");
            } else {
                afficherMessageSucces("Budget(s) ajouté(s) avec succès !");
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
        
        // Sélectionner la franchise correspondante dans la ListView
        listFranchises.getSelectionModel().clearSelection();
        for (franchise item : listFranchises.getItems()) {
            if (item.getId() != null && item.getId() == b.getFranchiseId()) {
                listFranchises.getSelectionModel().select(item);
                break;
            }
        }
        
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
            serviceBudget.deleteone(b);
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
        listFranchises.getSelectionModel().clearSelection();
        idBudgetAModifier = null;
        btnSauvegarder.setText("Sauvegarder");
    }



    private void afficherMessageSucces(String message) {
        NotificationManager.showSuccess("Succès", message);
    }

    private void afficherMessageErreur(String message) {
        NotificationManager.showError("Erreur", message);
    }

    private boolean confirmerAction(String message) {
        // Pour l'instant on garde une simple confirmation blocante via Alert,
        // tu pourras plus tard la transformer en dialog personnalisé si besoin.
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().map(result -> result == ButtonType.OK).orElse(false);
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
            URL cssUrl = getClass().getResource("/tn/esprit/boussole/gui/styles.css");
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                scene.getStylesheets().add(css);
            } else {
                Logger.getLogger(GestionBudgetsController.class.getName()).log(Level.WARNING, "CSS non chargée");
            }

            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            ThemeManager.getInstance().applyCurrentTheme(scene);
            stage.show();

        } catch (IOException e) {
            Logger.getLogger(GestionBudgetsController.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
