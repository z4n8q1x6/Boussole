package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Charge;
import tn.esprit.boussole.models.budget_previsionnel;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.ServiceBudgetPrevisionnel;
import tn.esprit.boussole.service.franchiseService;
import java.util.prefs.Preferences;
import tn.esprit.boussole.utils.MyBdConnexion;
import tn.esprit.boussole.utils.NotificationManager;
import tn.esprit.boussole.utils.ThemeManagerS;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class GestionBudgetsController implements Initializable {

    // Éléments de navigation (Sécurisés)
    @FXML private Button btnDashboard;
    @FXML private Button btnBudgets;
    @FXML private Button btnBilans;

    // Formulaire
    @FXML private ComboBox<Integer> cbMois;
    @FXML private ComboBox<Integer> cbAnnee;
    @FXML private ComboBox<budget_previsionnel.TypeBudget> cbTypeBudget;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private TextField txtMontant;
    @FXML private ListView<franchise> listFranchises;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnVider;

    // Table
    @FXML private TableView<budget_previsionnel> tableBudgets;
    @FXML private TableColumn<budget_previsionnel, Integer> colFranchise;
    @FXML private TableColumn<budget_previsionnel, Integer> colMois;
    @FXML private TableColumn<budget_previsionnel, Integer> colAnnee;
    @FXML private TableColumn<budget_previsionnel, Object> colType;
    @FXML private TableColumn<budget_previsionnel, String> colCategorie;
    @FXML private TableColumn<budget_previsionnel, Double> colMontant;

    private ServiceBudgetPrevisionnel serviceBudget;
    private franchiseService franchiseService;
    private final ObservableList<franchise> franchisesDisponibles = FXCollections.observableArrayList();
    private franchise optionTousReseau;
    private Integer idBudgetAModifier = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceBudget = new ServiceBudgetPrevisionnel();
        franchiseService = new franchiseService();

        // 1. Initialisation de la Navigation (Avec vérification de nullité)
        configurerNavigation();

        // 2. Configuration du Formulaire et de la ListView
        configurerFormulaire();
        // Applique un filtre numérique simple pour le montant
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            return text.matches("[0-9]*\\.?[0-9]*") ? change : null;
        };
        txtMontant.setTextFormatter(new TextFormatter<>(filter));
        chargerFranchises();

        // 3. Configuration de la Table (Édition et visuel)
        configurerTable();

        // Chargement initial
        rafraichirTable();
    }

    private void configurerNavigation() {
        if (btnDashboard != null) btnDashboard.setOnAction(event -> changerPage(event, "/DashboardSiege.fxml"));
        if (btnBudgets != null) btnBudgets.setOnAction(event -> changerPage(event, "/GestionBudgets.fxml"));
        if (btnBilans != null) btnBilans.setOnAction(event -> changerPage(event, "/GestionBilans.fxml"));

        if (btnSauvegarder != null) btnSauvegarder.setOnAction(event -> sauvegarderBudget());
        if (btnVider != null) btnVider.setOnAction(event -> nettoyerFormulaire());
    }

    private void configurerFormulaire() {
        if (listFranchises != null) {
            listFranchises.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listFranchises.setCellFactory(param -> new ListCell<franchise>() {
                @Override
                protected void updateItem(franchise item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? null : item.getNom());
                }
            });
        }

        // Remplissage ComboBox
        if (cbMois != null) {
            for (int m = 1; m <= 12; m++) cbMois.getItems().add(m);
            cbMois.getSelectionModel().selectFirst();
        }
        if (cbAnnee != null) {
            for (int a = 2024; a <= 2030; a++) cbAnnee.getItems().add(a);
            cbAnnee.getSelectionModel().selectFirst();
        }
        if (cbTypeBudget != null) {
            cbTypeBudget.getItems().addAll(budget_previsionnel.TypeBudget.values());
            cbTypeBudget.getSelectionModel().selectFirst();
            cbTypeBudget.setOnAction(e -> mettreAJourCategories());
            mettreAJourCategories();
        }
    }

    private void configurerTable() {
        if (tableBudgets == null) return;

        // Activer la sélection multiple
        tableBudgets.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Colonne Franchise (affiche le nom au lieu de l'ID)
        colFranchise.setCellValueFactory(new PropertyValueFactory<>("franchiseId"));
        colFranchise.setCellFactory(column -> new TableCell<budget_previsionnel, Integer>() {
            @Override
            protected void updateItem(Integer franchiseId, boolean empty) {
                super.updateItem(franchiseId, empty);
                if (empty || franchiseId == null) {
                    setText(null);
                } else {
                    String nom = null;
                    for (franchise f : franchisesDisponibles) {
                        if (f.getId() != null && f.getId().equals(franchiseId)) {
                            nom = f.getNom();
                            break;
                        }
                    }
                    setText(nom != null ? nom : "ID:" + franchiseId);
                }
            }
        });

        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_budget"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantCible"));

        tableBudgets.setEditable(true);

        // Rendu visuel Type Budget
        colType.setCellFactory(column -> new TableCell<budget_previsionnel, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    boolean isRevenu = item.toString().equals("OBJECTIF_REVENU");
                    setText(isRevenu ? " 🎯 Objectif" : " 🛑 Limite");
                    setStyle(isRevenu ? "-fx-text-fill: #10B981; -fx-font-weight: bold;" : "-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            }
        });

        // Menu Contextuel
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemSupprimer = new MenuItem("🗑 Supprimer");
        itemSupprimer.setOnAction(e -> {
            budget_previsionnel b = tableBudgets.getSelectionModel().getSelectedItem();
            if (b != null) supprimerBudget(b);
        });
        contextMenu.getItems().add(itemSupprimer);
        tableBudgets.setContextMenu(contextMenu);

        // Double clic pour modifier
        tableBudgets.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                modifierBudget(tableBudgets.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void sauvegarderBudget() {
        if (!validerSaisie()) return;

        Double montantValue;
        try {
            montantValue = Double.parseDouble(txtMontant.getText());
            if (montantValue <= 0) {
                afficherMessageErreur("Le montant doit être un nombre positif.");
                return;
            }
        } catch (NumberFormatException ex) {
            afficherMessageErreur("Le montant doit être numérique.");
            return;
        }

        List<franchise> selection = new ArrayList<>(listFranchises.getSelectionModel().getSelectedItems());
        if (optionTousReseau != null && selection.contains(optionTousReseau)) {
            selection.remove(optionTousReseau);
            selection.addAll(franchisesDisponibles);
        }
        selection.removeIf(f -> f == null || f.getId() == null);
        if (selection.isEmpty()) {
            afficherMessageErreur("Sélectionnez au moins une franchise.");
            return;
        }

        try {
            double montant = montantValue;
            for (franchise f : selection) {
                // Si "Tous le réseau" (ID 0) est sélectionné, on pourrait boucler sur tous,
                // mais ici on suit la sélection de la liste.
                budget_previsionnel b = new budget_previsionnel();
                b.setMois(cbMois.getValue());
                b.setAnnee(cbAnnee.getValue());
                b.setType_budget(cbTypeBudget.getValue());
                b.setCategorie(cbCategorie.isDisabled() ? "GLOBAL" : cbCategorie.getValue());
                b.setMontantCible(montant);
                b.setFranchiseId(f.getId());

                if (idBudgetAModifier != null) {
                    b.setId(idBudgetAModifier);
                    serviceBudget.updateone(b);
                } else {
                    serviceBudget.add(b);
                }
            }
            afficherMessageSucces("Opération réussie");
            rafraichirTable();
            nettoyerFormulaire();
        } catch (Exception e) {
            afficherMessageErreur("Erreur : " + e.getMessage());
        }
    }

    private void rafraichirTable() {
        if (tableBudgets == null) return;
        try {
            tableBudgets.getItems().clear();
            Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
            String role = prefs.get("role", "");

            if ("SIEGE".equals(role) || role.isEmpty()) {
                tableBudgets.getItems().addAll(serviceBudget.getTousBudgets());
            } else {
                tableBudgets.getItems().addAll(serviceBudget.getAllByFranchise(fetchFranchiseId(prefs.get("email", ""))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mettreAJourCategories() {
        if (cbCategorie == null) return;
        cbCategorie.getItems().clear();
        if (cbTypeBudget.getValue() == budget_previsionnel.TypeBudget.LIMITE_DEPENSE) {
            for (Charge.TypeCharge c : Charge.TypeCharge.values()) cbCategorie.getItems().add(c.name());
            cbCategorie.setDisable(false);
        } else {
            cbCategorie.getItems().add("GLOBAL");
            cbCategorie.setDisable(true);
        }
        cbCategorie.getSelectionModel().selectFirst();
    }

    private void chargerFranchises() {
        if (listFranchises == null) return;
        optionTousReseau = new franchise(0, "Tous le réseau", null, null, null, null, true, 0.0);

        ObservableList<franchise> items = FXCollections.observableArrayList();
        items.add(optionTousReseau);
        try {
            franchisesDisponibles.setAll(franchiseService.selectAll(null));
            items.addAll(franchisesDisponibles);
        } catch (SQLException e) {
            afficherMessageErreur("Impossible de charger les franchises : " + e.getMessage());
        }
        listFranchises.setItems(items);
        listFranchises.getSelectionModel().clearSelection();
        listFranchises.getSelectionModel().select(optionTousReseau);
    }

    private void modifierBudget(budget_previsionnel b) {
        if (b == null || btnSauvegarder == null) return;
        idBudgetAModifier = b.getId();
        cbMois.setValue(b.getMois());
        cbAnnee.setValue(b.getAnnee());
        cbTypeBudget.setValue(b.getType_budget());
        cbCategorie.setValue(b.getCategorie());
        txtMontant.setText(String.valueOf(b.getMontantCible()));
        btnSauvegarder.setText("Modifier");
    }

    private void supprimerBudget(budget_previsionnel b) {
        if (confirmerAction("Supprimer ce budget ?")) {
            try {
                serviceBudget.deleteone(b);
                rafraichirTable();
                NotificationManager.showSuccess("Supprimé", "Budget retiré.");
            } catch (Exception e) { afficherMessageErreur(e.getMessage()); }
        }
    }

    private void nettoyerFormulaire() {
        idBudgetAModifier = null;
        if (txtMontant != null) txtMontant.clear();
        if (btnSauvegarder != null) btnSauvegarder.setText("Sauvegarder");
        if (listFranchises != null) {
            listFranchises.getSelectionModel().clearSelection();
            if (optionTousReseau != null) listFranchises.getSelectionModel().select(optionTousReseau);
        }
    }

    private boolean validerSaisie() {
        if (cbMois == null || cbMois.getValue() == null) {
            afficherMessageErreur("Choisissez un mois.");
            return false;
        }
        if (cbAnnee == null || cbAnnee.getValue() == null) {
            afficherMessageErreur("Choisissez une année.");
            return false;
        }
        if (cbTypeBudget == null || cbTypeBudget.getValue() == null) {
            afficherMessageErreur("Choisissez un type de budget.");
            return false;
        }
        if ((cbCategorie != null && !cbCategorie.isDisabled() && cbCategorie.getValue() == null)) {
            afficherMessageErreur("Choisissez une catégorie.");
            return false;
        }
        if (txtMontant == null || txtMontant.getText().isBlank()) {
            afficherMessageErreur("Montant requis.");
            return false;
        }
        return true;
    }

    private void changerPage(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            ThemeManagerS.getInstance().applyCurrentTheme(scene);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void afficherMessageSucces(String msg) { NotificationManager.showSuccess("Succès", msg); }
    private void afficherMessageErreur(String msg) { NotificationManager.showError("Erreur", msg); }
    private boolean confirmerAction(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private int fetchFranchiseId(String email) {
        try (java.sql.Connection conn = MyBdConnexion.getinstance().getCnx();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id_franchise FROM utilisateur WHERE email = ?")) {
            ps.setString(1, email);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_franchise");
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}

