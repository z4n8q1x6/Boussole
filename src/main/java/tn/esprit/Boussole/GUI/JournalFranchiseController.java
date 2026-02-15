package tn.esprit.Boussole.GUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.esprit.Boussole.Models.transaction;
import tn.esprit.Boussole.Services.ServiceTransaction;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur fusionné "Journal Complet"
 * Combine les fonctionnalités de JournalTransactions (KPIs, filtres, export CSV)
 * et JournalFranchise (Actions: Modifier / Supprimer avec gestion des droits).
 */
public class JournalFranchiseController implements Initializable {

    // --- FXML Bindings ---
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private ComboBox<String> cbTypeFiltre;
    @FXML private Button btnRechercher;
    @FXML private Button btnReinitialiser;
    @FXML private Button btnExporter;
    @FXML private Button btnDashboard;

    @FXML private TableView<transaction> tableTransactions;
    @FXML private TableColumn<transaction, Date> colDate;
    @FXML private TableColumn<transaction, String> colType;
    @FXML private TableColumn<transaction, String> colDescription;
    @FXML private TableColumn<transaction, Double> colMontant;
    @FXML private TableColumn<transaction, Void> colActions;

    // KPI Labels
    @FXML private Label lblNombreTransactions;
    @FXML private Label lblTotalRecettes;
    @FXML private Label lblTotalCharges;
    @FXML private Label lblSoldeFiltre;

    // --- Data ---
    private ServiceTransaction serviceTransaction;
    private int franchiseId;
    private ObservableList<transaction> toutesLesTransactions = FXCollections.observableArrayList();

    // =========================================================================
    // INITIALISATION
    // =========================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        serviceTransaction = new ServiceTransaction();

        franchiseId = SessionManager.getInstance().getIdFranchise();
        if (franchiseId == 0) franchiseId = 1; // Fallback

        // Configurer le filtre Type
        cbTypeFiltre.setItems(FXCollections.observableArrayList("TOUT", "RECETTE", "DEPENSE"));
        cbTypeFiltre.setValue("TOUT");

        // Configurer les colonnes
        configurerColonnes();

        // Configurer la colonne Actions (Modifier + Supprimer)
        configurerColonneActions();

        // Appliquer le style couleur sur les lignes
        appliquerStyleCouleur();

        // Charger toutes les transactions
        chargerTransactions();
    }

    // =========================================================================
    // CONFIGURATION DES COLONNES
    // =========================================================================
    private void configurerColonnes() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        colType.setCellValueFactory(cellData ->
            new SimpleStringProperty(
                cellData.getValue().getType() != null ? cellData.getValue().getType().toString() : ""
            )
        );

        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));

        // Formatter Montant avec couleurs
        colMontant.setCellFactory(column -> new TableCell<transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f TND", item));
                    transaction t = getTableView().getItems().get(getIndex());
                    if (t.getType() == transaction.Type.RECETTE) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    } else if (t.getType() == transaction.Type.DEPENSE) {
                        setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    // =========================================================================
    // COLONNE ACTIONS (MODIFIER + SUPPRIMER) - DROITS RECETTE SEULEMENT
    // =========================================================================
    private void configurerColonneActions() {
        Callback<TableColumn<transaction, Void>, TableCell<transaction, Void>> cellFactory =
            new Callback<TableColumn<transaction, Void>, TableCell<transaction, Void>>() {
                @Override
                public TableCell<transaction, Void> call(final TableColumn<transaction, Void> param) {
                    return new TableCell<transaction, Void>() {

                        private final Button btnEdit = new Button("✏");
                        private final Button btnDelete = new Button("🗑");
                        private final HBox pane = new HBox(5, btnEdit, btnDelete);

                        {
                            // Style Modifier (Orange)
                            btnEdit.setStyle(
                                "-fx-background-color: #f39c12; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4 8;"
                            );
                            // Style Supprimer (Rouge)
                            btnDelete.setStyle(
                                "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                "-fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4 8;"
                            );
                            pane.setAlignment(Pos.CENTER);

                            btnEdit.setOnAction(event -> {
                                transaction t = getTableView().getItems().get(getIndex());
                                modifierTransaction(t);
                            });

                            btnDelete.setOnAction(event -> {
                                transaction t = getTableView().getItems().get(getIndex());
                                confirmerEtSupprimer(t);
                            });
                        }

                        @Override
                        public void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                transaction t = getTableView().getItems().get(getIndex());
                                // *** LOGIQUE MÉTIER CRITIQUE ***
                                // Modifier + Supprimer UNIQUEMENT pour RECETTE
                                // DEPENSE = facture du siège => pas de modification possible
                                if (t.getType() == transaction.Type.RECETTE) {
                                    btnEdit.setVisible(true);
                                    btnDelete.setVisible(true);
                                    setGraphic(pane);
                                } else {
                                    // DEPENSE : cacher les boutons
                                    setGraphic(null);
                                }
                            }
                        }
                    };
                }
            };

        colActions.setCellFactory(cellFactory);
    }

    // =========================================================================
    // CHARGEMENT DES DONNÉES
    // =========================================================================
    private void chargerTransactions() {
        try {
            List<transaction> list = serviceTransaction.getAllByFranchise(franchiseId);
            toutesLesTransactions.setAll(list);
            tableTransactions.setItems(toutesLesTransactions);
            calculerTotaux(toutesLesTransactions);
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les transactions : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // FILTRES
    // =========================================================================
    @FXML
    private void appliquerFiltres() {
        LocalDate dateDebut = dpDateDebut.getValue();
        LocalDate dateFin = dpDateFin.getValue();
        String typeFiltre = cbTypeFiltre.getValue();

        List<transaction> resultat = toutesLesTransactions.stream()
            .filter(t -> {
                if (t.getDate() == null) return false;
                LocalDate tDate = new java.sql.Date(t.getDate().getTime()).toLocalDate();

                if (dateDebut != null && tDate.isBefore(dateDebut)) return false;
                if (dateFin != null && tDate.isAfter(dateFin)) return false;

                if (typeFiltre != null && !"TOUT".equals(typeFiltre)) {
                    if (t.getType() == null || !t.getType().name().equals(typeFiltre)) return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        ObservableList<transaction> listeFiltree = FXCollections.observableArrayList(resultat);
        tableTransactions.setItems(listeFiltree);
        calculerTotaux(listeFiltree);
    }

    @FXML
    private void reinitialiserFiltres() {
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        cbTypeFiltre.setValue("TOUT");
        tableTransactions.setItems(toutesLesTransactions);
        calculerTotaux(toutesLesTransactions);
    }

    // =========================================================================
    // CALCUL DES KPI (appelé après filtre, suppression, modification)
    // =========================================================================
    private void calculerTotaux(ObservableList<transaction> liste) {
        // Nombre total
        lblNombreTransactions.setText(String.valueOf(liste.size()));

        // Total Recettes
        double totalRecettes = liste.stream()
            .filter(t -> t.getType() == transaction.Type.RECETTE)
            .mapToDouble(transaction::getMontant)
            .sum();

        // Total Charges
        double totalCharges = liste.stream()
            .filter(t -> t.getType() == transaction.Type.DEPENSE)
            .mapToDouble(transaction::getMontant)
            .sum();

        // Solde
        double solde = totalRecettes - totalCharges;

        lblTotalRecettes.setText(String.format("%.2f TND", totalRecettes));
        lblTotalCharges.setText(String.format("%.2f TND", totalCharges));
        lblSoldeFiltre.setText(String.format("%.2f TND", solde));

        // Couleur : bleu profond adapté à l'interface
        lblSoldeFiltre.setStyle("-fx-text-fill: #0b3d91; -fx-font-size:22px; -fx-font-weight:bold;");
    }

    // =========================================================================
    // MODIFIER TRANSACTION (Dialog)
    // =========================================================================
    private void modifierTransaction(transaction t) {
        Dialog<transaction> dialog = new Dialog<>();
        dialog.setTitle("Modifier Transaction");
        dialog.setHeaderText("Modifier les détails de la recette");

        ButtonType btnValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValider, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        DatePicker datePick = new DatePicker();
        if (t.getDate() != null) {
            datePick.setValue(new java.sql.Date(t.getDate().getTime()).toLocalDate());
        }
        TextField tfMontant = new TextField(String.valueOf(t.getMontant()));
        TextField tfDescription = new TextField(t.getDescription());

        grid.add(new Label("Date :"), 0, 0);
        grid.add(datePick, 1, 0);
        grid.add(new Label("Montant (TND) :"), 0, 1);
        grid.add(tfMontant, 1, 1);
        grid.add(new Label("Description :"), 0, 2);
        grid.add(tfDescription, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnValider) {
                try {
                    t.setMontant(Double.parseDouble(tfMontant.getText()));
                    t.setDescription(tfDescription.getText());
                    if (datePick.getValue() != null) {
                        t.setDate(java.sql.Date.valueOf(datePick.getValue()));
                    }
                    return t;
                } catch (NumberFormatException e) {
                    afficherErreur("Erreur", "Montant invalide.");
                    return null;
                }
            }
            return null;
        });

        Optional<transaction> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            serviceTransaction.updateOne(updated);
            chargerTransactions(); // Recharge tout + recalcule KPIs
            afficherSucces("Succès", "Transaction modifiée avec succès !");
        });
    }

    // =========================================================================
    // SUPPRIMER TRANSACTION (Confirmation)
    // =========================================================================
    private void confirmerEtSupprimer(transaction t) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer cette recette ?");
        alert.setContentText("Description : " + t.getDescription() + "\nMontant : " + String.format("%.2f", t.getMontant()) + " TND");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceTransaction.deleteOne(t);
            chargerTransactions(); // Recharge tout + recalcule KPIs
            afficherSucces("Supprimé", "Recette supprimée avec succès.");
        }
    }

    // =========================================================================
    // EXPORT CSV
    // =========================================================================
    @FXML
    private void exporterCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en CSV");
        fileChooser.setInitialFileName("journal_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(btnExporter.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Date,Type,Montant (TND),Description\n");
                for (transaction t : tableTransactions.getItems()) {
                    String dateStr = (t.getDate() != null) ? t.getDate().toString() : "";
                    String typeStr = (t.getType() != null) ? t.getType().toString() : "";
                    String descStr = (t.getDescription() != null) ? t.getDescription().replace(",", ";") : "";
                    writer.write(String.format("%s,%s,%.2f,%s\n", dateStr, typeStr, t.getMontant(), descStr));
                }
                afficherSucces("Export Réussi", "Fichier enregistré : " + file.getName());
            } catch (IOException e) {
                afficherErreur("Erreur Export", "Impossible d'écrire le fichier : " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // STYLE COULEUR DES LIGNES
    // =========================================================================
    private void appliquerStyleCouleur() {
        tableTransactions.setRowFactory(tv -> new TableRow<transaction>() {
            @Override
            protected void updateItem(transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if (item.getType() == transaction.Type.RECETTE) {
                        setStyle("-fx-background-color: #e8f5e9;"); // Vert très clair
                    } else if (item.getType() == transaction.Type.DEPENSE) {
                        setStyle("-fx-background-color: #ffebee;"); // Rouge très clair
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================
    @FXML
    void versDashboard(ActionEvent event) {
        chargerScene(event, "/tn/esprit/Boussole/GUI/DashboardFranchise.fxml", "Tableau de Bord - Franchise");
    }

    private void chargerScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            try {
                URL cssUrl = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css");
                if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            afficherErreur("Erreur Navigation", "Impossible de charger : " + fxmlPath);
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
