package tn.esprit.boussole.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import tn.esprit.boussole.models.transaction;
import tn.esprit.boussole.services.ServiceTransaction;
import tn.esprit.boussole.services.ServiceExportExcel;
import tn.esprit.boussole.Utilis.SessionManager;

import java.io.File;

import tn.esprit.boussole.Utilis.ThemeManager;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur "Pro" Journal Complet
 */
public class JournalFranchiseController implements Initializable {

    // --- FXML Bindings ---
    @FXML private MenuButton btnPeriode;
    @FXML private MenuItem miCeMois;
    @FXML private MenuItem miMoisDernier;
    @FXML private MenuItem miCetteAnnee;
    @FXML private DatePicker dpPeriodeDu;
    @FXML private DatePicker dpPeriodeAu;
    @FXML private Button btnPeriodeOk;

    @FXML private ComboBox<String> cbTypeFiltre;
    @FXML private TextField txtRechercheGlobal;
    @FXML private Button btnReinitialiser;
    @FXML private Button btnExporter;
    @FXML private Button btnDashboard;

    @FXML private TableView<transaction> tableTransactions;
    @FXML private TableColumn<transaction, Date> colDate;
    @FXML private TableColumn<transaction, String> colType;
    @FXML private TableColumn<transaction, String> colDescription;
    @FXML private TableColumn<transaction, Double> colMontant;

    // KPI Labels
    @FXML private Label lblNombreTransactions;
    @FXML private Label lblTotalRecettes;
    @FXML private Label lblTotalCharges;
    @FXML private Label lblSoldeFiltre;

    // --- Data ---
    private ServiceTransaction serviceTransaction;
    private int franchiseId;

    // Listes pour le filtrage avancé
    private final ObservableList<transaction> masterData = FXCollections.observableArrayList();
    private FilteredList<transaction> filteredData;
    private SortedList<transaction> sortedData;

    // Période de filtrage
    private LocalDate filterDateDebut;
    private LocalDate filterDateFin;

    // =========================================================================
    // INITIALISATION
    // =========================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceTransaction = new ServiceTransaction();

        franchiseId = SessionManager.getInstance().getIdFranchise();
        if (franchiseId == 0) franchiseId = 1; // Fallback

        // 1. Initialiser les filtres UI
        cbTypeFiltre.setItems(FXCollections.observableArrayList("TOUT", "RECETTE", "DEPENSE"));
        cbTypeFiltre.setValue("TOUT");

        // 2. Configuration avancée de la TableView (Sélection multiple, Suppression Clavier/ContextMenu)
        setupTable();

        // 3. Configuration des Colonnes (Tri, Édition)
        configurerColonnes();

        // 4. Chargement initial des données
        chargerTransactions();

        // 5. Configuration du filtre période hybride
        setupDateFilters();

        // 6. Mise en place du Filtrage "Smart Filter"
        setupFilters();
    }

    // =========================================================================
    // OBJECTIF 1 : SETUP TABLE (Sélection & Suppression Pro)
    // =========================================================================
    private void setupTable() {
        // Mode sélection multiple activé
        tableTransactions.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Configuration basic
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setCellFactory(column -> new TableCell<transaction, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(new java.text.SimpleDateFormat("dd/MM/yyyy").format(item));
                    setStyle("-fx-text-fill: white; -fx-alignment: CENTER;");
                }
            }
        });

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDescription.setCellFactory(TextFieldTableCell.forTableColumn()); // Editable text field styling handled in CSS now

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(column -> new TableCell<transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    if ("RECETTE".equals(item)) {
                        setText(" RECETTE");
                        Label icon = new Label("✓");
                        icon.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                        setGraphic(icon);
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    } else {
                        setText(" DÉPENSE");
                        Label icon = new Label("⚠");
                        icon.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                        setGraphic(icon);
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    }
                }
            }
        });

        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colMontant.setCellFactory(column -> new TableCell<transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f TND", item));
                    // Récupérer le type pour la couleur
                    transaction tx = getTableView().getItems().get(getIndex());
                    if (tx != null && tx.getType() != null && "RECETTE".equals(tx.getType().name())) {
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    } else {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            }
        });

        // Édition : Sauvegarde auto lors de la modification
        tableTransactions.setEditable(true);
        colDescription.setOnEditCommit(event -> {
            transaction t = event.getRowValue();
            if (t.getType() == transaction.Type.RECETTE) {
                t.setDescription(event.getNewValue());
                serviceTransaction.updateone(t);
            } else {
                afficherErreur("Interdit", "Modification interdite sur les Dépenses.");
                tableTransactions.refresh();
            }
        });

        colMontant.setOnEditCommit(event -> {
            transaction t = event.getRowValue();
            if (t.getType() == transaction.Type.RECETTE) {
                if (event.getNewValue() != null && event.getNewValue() > 0) {
                    t.setMontant(event.getNewValue());
                    serviceTransaction.updateone(t);
                    updatePredicate();
                }
            } else {
                afficherErreur("Interdit", "Modification interdite sur les Dépenses.");
                tableTransactions.refresh();
            }
        });
    }

    // =========================================================================
    // OBJECTIF 1 : LOGIQUE DE SUPPRESSION DE MASSE
    // =========================================================================
    private void supprimerSelection() {
        List<transaction> selectedItems = tableTransactions.getSelectionModel().getSelectedItems();

        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

        boolean containsDepense = selectedItems.stream()
            .anyMatch(t -> t.getType() == transaction.Type.DEPENSE);

        if (containsDepense) {
            afficherErreur("Action Interdite", "Impossible de supprimer une DEPENSE validée par le siège.\nVeuillez désélectionner les dépenses.");
            return;
        }

        int count = selectedItems.size();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous vraiment supprimer ces " + count + " ligne(s) sélectionnée(s) ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                for (transaction t : selectedItems) {
                    serviceTransaction.deleteone(t);
                }

                masterData.removeAll(selectedItems);
                calculerTotaux(tableTransactions.getItems());
                afficherSucces("Suppression réussie", count + " transaction(s) supprimée(s).");

            } catch (Exception e) {
                afficherErreur("Erreur", "Problème lors de la suppression : " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // FILTRE DATE HYBRIDE (MenuButton)
    // =========================================================================
    private void setupDateFilters() {
        // Valeur par défaut : Ce Mois
        appliquerCeMois();

        if (miCeMois != null) {
            miCeMois.setOnAction(e -> {
                appliquerCeMois();
                updatePredicate();
            });
        }
        if (miMoisDernier != null) {
            miMoisDernier.setOnAction(e -> {
                appliquerMoisDernier();
                updatePredicate();
            });
        }
        if (miCetteAnnee != null) {
            miCetteAnnee.setOnAction(e -> {
                appliquerCetteAnnee();
                updatePredicate();
            });
        }

        // Plage personnalisée
        if (btnPeriodeOk != null) {
            btnPeriodeOk.setOnAction(e -> {
                LocalDate du = dpPeriodeDu != null ? dpPeriodeDu.getValue() : null;
                LocalDate au = dpPeriodeAu != null ? dpPeriodeAu.getValue() : null;

                if (du == null || au == null) {
                    afficherErreur("Période invalide", "Veuillez choisir une date de début et une date de fin.");
                    return;
                }
                if (au.isBefore(du)) {
                    afficherErreur("Période invalide", "La date de fin doit être après la date de début.");
                    return;
                }

                filterDateDebut = du;
                filterDateFin = au;

                if (btnPeriode != null) {
                    btnPeriode.setText("📅 Période : " + du + " → " + au);
                }

                updatePredicate();

                // UX : fermer le menu après validation
                if (btnPeriode != null) {
                    btnPeriode.hide();
                }
            });
        }

        // Bonus UX : si l'utilisateur choisit du / au et appuie Enter dans un DatePicker, on peut valider pareil
        if (dpPeriodeDu != null) {
            dpPeriodeDu.setOnAction(e -> { /* ne filtre pas encore, attente OK */ });
        }
        if (dpPeriodeAu != null) {
            dpPeriodeAu.setOnAction(e -> { /* ne filtre pas encore, attente OK */ });
        }
    }

    private void appliquerCeMois() {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        filterDateDebut = ym.atDay(1);
        filterDateFin = today;
        if (btnPeriode != null) btnPeriode.setText("📅 Période : Ce Mois");

        // Synchroniser le custom range (optionnel)
        if (dpPeriodeDu != null) dpPeriodeDu.setValue(filterDateDebut);
        if (dpPeriodeAu != null) dpPeriodeAu.setValue(filterDateFin);
    }

    private void appliquerMoisDernier() {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today).minusMonths(1);
        filterDateDebut = ym.atDay(1);
        filterDateFin = ym.atEndOfMonth();
        if (btnPeriode != null) btnPeriode.setText("📅 Période : Mois Dernier");

        if (dpPeriodeDu != null) dpPeriodeDu.setValue(filterDateDebut);
        if (dpPeriodeAu != null) dpPeriodeAu.setValue(filterDateFin);
    }

    private void appliquerCetteAnnee() {
        LocalDate today = LocalDate.now();
        filterDateDebut = LocalDate.of(today.getYear(), 1, 1);
        filterDateFin = LocalDate.of(today.getYear(), 12, 31);
        if (btnPeriode != null) btnPeriode.setText("📅 Période : Cette Année");

        if (dpPeriodeDu != null) dpPeriodeDu.setValue(filterDateDebut);
        if (dpPeriodeAu != null) dpPeriodeAu.setValue(filterDateFin);
    }

    private void appliquerTout() {
        filterDateDebut = null;
        filterDateFin = null;
        if (btnPeriode != null) btnPeriode.setText("📅 Période : Tout");
        if (dpPeriodeDu != null) dpPeriodeDu.setValue(null);
        if (dpPeriodeAu != null) dpPeriodeAu.setValue(null);
    }

    // =========================================================================
    // OBJECTIF 2 : FILTRAGE AVANCÉ & INSTANTANÉ
    // =========================================================================
    private void setupFilters() {
        filteredData = new FilteredList<>(masterData, p -> true);

        if (txtRechercheGlobal != null) {
            txtRechercheGlobal.textProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        }
        if (cbTypeFiltre != null) {
            cbTypeFiltre.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        }

        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableTransactions.comparatorProperty());
        tableTransactions.setItems(sortedData);

        updatePredicate();
    }

    private void updatePredicate() {
        if (filteredData == null) return;

        filteredData.setPredicate(t -> {
            // 1. Filtre Global (Texte)
            String searchText = (txtRechercheGlobal != null && txtRechercheGlobal.getText() != null)
                ? txtRechercheGlobal.getText().toLowerCase() : "";

            if (!searchText.isEmpty()) {
                boolean matchDesc = t.getDescription() != null &&
                    t.getDescription().toLowerCase().contains(searchText);
                if (!matchDesc) return false;
            }

            // 2. Filtre Type
            String typeSelect = cbTypeFiltre != null ? cbTypeFiltre.getValue() : null;
            if (typeSelect != null && !"TOUT".equals(typeSelect)) {
                if (t.getType() == null || !t.getType().toString().equals(typeSelect)) {
                    return false;
                }
            }

            // 3. Filtre Dates (période)
            if (filterDateDebut != null && filterDateFin != null && t.getDate() != null) {
                LocalDate tDate = new java.sql.Date(t.getDate().getTime()).toLocalDate();
                if (tDate.isBefore(filterDateDebut) || tDate.isAfter(filterDateFin)) return false;
            }

            return true;
        });

        calculerTotaux(sortedData);
    }

    // =========================================================================
    // CONFIGURATION DES COLONNES (OBJ 3: TRI)
    // =========================================================================
    private void configurerColonnes() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        colDate.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.StringConverter<Date>() {
            @Override public String toString(Date object) { return object != null ? object.toString() : ""; }
            @Override public Date fromString(String string) {
                try { return Date.valueOf(string); } catch (Exception e) { return null; }
            }
        }));

        colDate.setOnEditCommit(event -> {
            transaction t = event.getRowValue();
            if (t.getType() == transaction.Type.RECETTE) {
                if (event.getNewValue() != null) {
                    t.setDate(event.getNewValue());
                    serviceTransaction.updateone(t);
                }
            } else {
                afficherErreur("Interdit", "Modification interdite sur les Dépenses.");
                tableTransactions.refresh();
            }
        });

        colType.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getType() != null ? cellData.getValue().getType().toString() : ""
        ));

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));


        tableTransactions.setEditable(true);

        colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescription.setOnEditCommit(event -> {
            transaction t = event.getRowValue();
            if (t.getType() == transaction.Type.RECETTE) {
                t.setDescription(event.getNewValue());
                serviceTransaction.updateone(t);
            } else {
                afficherErreur("Interdit", "Modification interdite sur les Dépenses.");
                tableTransactions.refresh();
            }
        });

        colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMontant.setOnEditCommit(event -> {
            transaction t = event.getRowValue();
            if (t.getType() == transaction.Type.RECETTE) {
                if (event.getNewValue() != null && event.getNewValue() > 0) {
                    t.setMontant(event.getNewValue());
                    serviceTransaction.updateone(t);
                    updatePredicate();
                }
            } else {
                afficherErreur("Interdit", "Modification interdite sur les Dépenses.");
                tableTransactions.refresh();
            }
        });
    }

    // =========================================================================
    // CHARGEMENT DES DONNÉES
    // =========================================================================
    private void chargerTransactions() {
        try {
            List<transaction> list = serviceTransaction.getAllByFranchise(franchiseId);
            masterData.setAll(list);

            if (sortedData != null) {
                calculerTotaux(sortedData);
            }
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les transactions : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void reinitialiserFiltres() {
        appliquerTout();
        if (cbTypeFiltre != null) cbTypeFiltre.setValue("TOUT");
        if (txtRechercheGlobal != null) txtRechercheGlobal.clear();
        updatePredicate();
    }

    @FXML
    private void appliquerFiltres() {
        updatePredicate();
    }

    // =========================================================================
    // CALCUL DES KPI
    // =========================================================================
    private void calculerTotaux(List<transaction> liste) {
        if (liste == null) return;

        lblNombreTransactions.setText(String.valueOf(liste.size()));

        double totalRecettes = liste.stream()
            .filter(t -> t.getType() == transaction.Type.RECETTE)
            .mapToDouble(transaction::getMontant)
            .sum();

        double totalCharges = liste.stream()
            .filter(t -> t.getType() == transaction.Type.DEPENSE)
            .mapToDouble(transaction::getMontant)
            .sum();

        lblTotalRecettes.setText(String.format("%.2f TND", totalRecettes));
        lblTotalCharges.setText(String.format("%.2f TND", totalCharges));
        lblSoldeFiltre.setText(String.format("%.2f TND", totalRecettes - totalCharges));
    }

    // =========================================================================
    // NAVIGATION & EXPORT
    // =========================================================================
    @FXML
    void versDashboard(ActionEvent event) {
        chargerScene(event, "/tn/esprit/boussole/gui/DashboardFranchise.fxml", "Tableau de Bord - Franchise");
    }

    private void chargerScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            try {
                URL cssUrl = getClass().getResource("/tn/esprit/boussole/gui/styles.css");
                if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            } catch (Exception ignored) {
            }
            stage.setScene(scene);
            ThemeManager.getInstance().applyCurrentTheme(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            afficherErreur("Erreur Navigation", "Impossible de charger : " + fxmlPath);
        }
    }

    @FXML
    private void exporter() {
        // Selection Logic
        List<transaction> transactionsToExport = new java.util.ArrayList<>(tableTransactions.getSelectionModel().getSelectedItems());
        if (transactionsToExport.isEmpty()) {
            // No selection -> export all filtered rows
            transactionsToExport = new java.util.ArrayList<>(tableTransactions.getItems());
        }

        if (transactionsToExport.isEmpty()) {
            afficherErreur("Export vide", "Aucune donnée à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en Excel (Professionnel)");
        fileChooser.setInitialFileName("journal_export.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
        File file = fileChooser.showSaveDialog(btnExporter.getScene().getWindow());

        if (file != null) {
            try {
                ServiceExportExcel exporter = new ServiceExportExcel();
                exporter.exporterTransactions(transactionsToExport, file.getAbsolutePath());
                afficherSucces("Export Excel réussi", "Fichier sauvegardé avec succès :\n" + file.getName());
            } catch (IOException e) {
                afficherErreur("Erreur d'export", "Impossible de créer le fichier Excel : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // =========================================================================
    // UTILITAIRES ALERTES
    // =========================================================================
    private void afficherSucces(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
