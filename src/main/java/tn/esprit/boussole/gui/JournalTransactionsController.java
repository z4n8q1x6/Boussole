package tn.esprit.boussole.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import tn.esprit.boussole.models.transaction;
import tn.esprit.boussole.services.ServiceTransaction;
import tn.esprit.boussole.Utilis.SessionManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class JournalTransactionsController implements Initializable {

    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private ComboBox<String> cbTypeFiltre;
    @FXML private Button btnRechercher;
    @FXML private Button btnReinitialiser;
    @FXML private Button btnExporter;
    @FXML private TableView<transaction> tableTransactions;
    @FXML private TableColumn<transaction, Date> colDate;
    @FXML private TableColumn<transaction, String> colType;
    @FXML private TableColumn<transaction, Double> colMontant;
    @FXML private TableColumn<transaction, String> colDescription;
    @FXML private Label lblNombreTransactions;
    @FXML private Label lblTotalRecettes;
    @FXML private Label lblTotalCharges;
    @FXML private Label lblSoldeFiltre;

    private ServiceTransaction serviceTransaction;
    private int franchiseId;
    private ObservableList<transaction> toutesLesTransactions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Initialiser serviceTransaction
        serviceTransaction = new ServiceTransaction();

        // 2. Récupérer franchiseId
        franchiseId = SessionManager.getInstance().getIdFranchise();
        if (franchiseId == 0) franchiseId = 1; // Fallback pour test/Siège

        // 3. Configurer cbTypeFiltre
        cbTypeFiltre.setItems(FXCollections.observableArrayList("TOUT", "RECETTE", "DEPENSE"));
        
        // 4. Définir valeur par défaut
        cbTypeFiltre.setValue("TOUT");

        // 5. Configurer les colonnes
        configurerColonnes();

        // 6. Configurer le style des colonnes et lignes
        appliquerStyleCouleur();

        // 7. Charger les transactions
        chargerTransactions();
    }

    private void configurerColonnes() {
        // 1. colDate
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // 2. colType avec Bindings pour String conversion
        // 2. colType
        colType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getType() != null ? cellData.getValue().getType().toString() : ""
            )
        );


        // 3. colMontant et colDescription
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // 5. Formatter colMontant avec couleurs
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
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (t.getType() == transaction.Type.DEPENSE) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void chargerTransactions() {
        try {
            // 1. Appeler serviceTransaction.getAllByFranchise
            List<transaction> list = serviceTransaction.getAllByFranchise(franchiseId);

            // 2. Stocker dans toutesLesTransactions
            toutesLesTransactions.setAll(list);

            // 3. Afficher dans tableTransactions
            tableTransactions.setItems(toutesLesTransactions);

            // 4. Mettre à jour stats
            mettreAJourStatistiques(toutesLesTransactions);

        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les transactions : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void appliquerFiltres() {
        try {
            // 1. Récupérer critères
            LocalDate dateDebut = dpDateDebut.getValue();
            LocalDate dateFin = dpDateFin.getValue();
            String typeFiltre = cbTypeFiltre.getValue();

            // 4. Filtrer avec stream
            List<transaction> resultat = toutesLesTransactions.stream()
                .filter(t -> {
                    // Conversion Date -> LocalDate
                    if (t.getDate() == null) return false;
                    LocalDate tDate = new java.sql.Date(t.getDate().getTime()).toLocalDate();

                    // Filtre Date Début
                    if (dateDebut != null && tDate.isBefore(dateDebut)) return false;

                    // Filtre Date Fin
                    if (dateFin != null && tDate.isAfter(dateFin)) return false;

                    // Filtre Type
                    if (typeFiltre != null && !"TOUT".equals(typeFiltre)) {
                        if (t.getType() == null || !t.getType().name().equals(typeFiltre)) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

            // 5. Créer ObservableList filtrée
            ObservableList<transaction> listeFiltree = FXCollections.observableArrayList(resultat);

            // 6. Afficher
            tableTransactions.setItems(listeFiltree);

            // 7. Mettre à jour stats
            mettreAJourStatistiques(listeFiltree);

        } catch (Exception e) {
            afficherErreur("Erreur filtre", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void reinitialiserFiltres() {
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        cbTypeFiltre.setValue("TOUT");
        
        tableTransactions.setItems(toutesLesTransactions);
        mettreAJourStatistiques(toutesLesTransactions);
    }

    private void mettreAJourStatistiques(ObservableList<transaction> liste) {
        // 1. Nombre
        lblNombreTransactions.setText(String.valueOf(liste.size()));

        // 2. Total Recettes
        double totalRecettes = liste.stream()
            .filter(t -> t.getType() == transaction.Type.RECETTE)
            .mapToDouble(transaction::getMontant)
            .sum();
        
        // 3. Total Charges
        double totalCharges = liste.stream()
            .filter(t -> t.getType() == transaction.Type.DEPENSE)
            .mapToDouble(transaction::getMontant)
            .sum();

        // 4. Solde
        double solde = totalRecettes - totalCharges;

        // 5. Affichage Labels
        lblTotalRecettes.setText(String.format("%.2f TND", totalRecettes));
        lblTotalCharges.setText(String.format("%.2f TND", totalCharges));
        lblSoldeFiltre.setText(String.format("%.2f TND", solde));

        // 6. Couleur Solde
        if (solde >= 0) {
            lblSoldeFiltre.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            lblSoldeFiltre.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }

    private void appliquerStyleCouleur() {
        tableTransactions.setRowFactory(tv -> new TableRow<transaction>() {
            @Override
            protected void updateItem(transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if (item.getType() == transaction.Type.RECETTE) {
                        setStyle("-fx-background-color: #d5f4e6;"); // Vert clair
                    } else if (item.getType() == transaction.Type.DEPENSE) {
                        setStyle("-fx-background-color: #fadbd8;"); // Rouge clair
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    @FXML
    private void exporterCSV() {
        // 1. Créer FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en CSV");
        fileChooser.setInitialFileName("transactions_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        // 5. Ouvrir dialog
        File file = fileChooser.showSaveDialog(btnExporter.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // En-tête
                writer.write("Date,Type,Montant (TND),Description\n");

                // Contenu (transactions actuellement affichées dans la table)
                for (transaction t : tableTransactions.getItems()) {
                    String dateStr = (t.getDate() != null) ? t.getDate().toString() : "";
                    String typeStr = (t.getType() != null) ? t.getType().toString() : "";
                    String descStr = (t.getDescription() != null) ? t.getDescription().replace(",", ";") : ""; // Eviter conflit CSV
                    
                    writer.write(String.format("%s,%s,%.2f,%s\n", 
                        dateStr, typeStr, t.getMontant(), descStr));
                }

                afficherSucces("Export Réussi", "Fichier enregistré : " + file.getName());

            } catch (IOException e) {
                afficherErreur("Erreur Export", "Impossible d'écrire le fichier : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

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
