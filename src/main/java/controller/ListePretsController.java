package controller;

import entity.Pret;
import entity.StatutPret;
import service.PretService;
import service.MailService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;
import java.io.File;

public class ListePretsController {

    @FXML private TableView<Pret> tablePrets;
    @FXML private TableColumn<Pret, String> colMotif;
    @FXML private TableColumn<Pret, Double> colMontant;
    @FXML private TableColumn<Pret, Integer> colDuree;
    @FXML private TableColumn<Pret, Float> colTaux;
    @FXML private TableColumn<Pret, StatutPret> colStatut;
    @FXML private TableColumn<Pret, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboStatut;

    private PretService pretService = new PretService();
    private MailService mailService = new MailService();
    private ObservableList<Pret> pretList = FXCollections.observableArrayList();

    // On déclare la FilteredList en variable de classe pour y accéder facilement
    private FilteredList<Pret> filteredData;

    @FXML
    public void initialize() {
        // --- ACTIVATION DE L'ÉDITION ---
        tablePrets.setEditable(true);

        // Initialisation de la ComboBox
        if (comboStatut != null) {
            comboStatut.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "ACCORDE", "REFUSE"));
            comboStatut.getSelectionModel().selectFirst();
        }

        configurerColonnes();
        chargerDonnees();
        ajouterBoutonsActions();
        configurerFiltrageMultiCritere();
    }

    /**
     * Logique de filtrage combinée : Recherche textuelle + ComboBox Statut
     */
    private void configurerFiltrageMultiCritere() {
        // 1. Créer la FilteredList
        filteredData = new FilteredList<>(pretList, p -> true);

        // 2. Créer une méthode interne pour appliquer les filtres
        Runnable appliquerFiltres = () -> {
            String textSearch = (txtRecherche.getText() == null) ? "" : txtRecherche.getText().toLowerCase();
            String statutSearch = comboStatut.getSelectionModel().getSelectedItem();

            filteredData.setPredicate(pret -> {
                // --- FILTRE 1 : Statut ---
                boolean matchStatut = true;
                if (statutSearch != null && !statutSearch.equals("TOUS")) {
                    matchStatut = pret.getStatut().toString().equals(statutSearch);
                }

                // --- FILTRE 2 : Recherche (Motif ou Mois) ---
                boolean matchText = true;
                if (!textSearch.isEmpty()) {
                    boolean matchMotif = pret.getMotif().toLowerCase().contains(textSearch);
                    boolean matchMois = String.valueOf(pret.getDureeMois()).contains(textSearch);
                    matchText = (matchMotif || matchMois);
                }

                // Le prêt est affiché seulement s'il valide les DEUX conditions
                return matchStatut && matchText;
            });
        };

        // 3. Écouter les changements sur le TextField
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres.run());

        // 4. Écouter les changements sur la ComboBox
        comboStatut.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres.run());

        // 5. Lier au tableau avec tri conservé
        SortedList<Pret> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePrets.comparatorProperty());
        tablePrets.setItems(sortedData);
    }

    @FXML
    private void chargerDonnees() {
        try {
            pretList.setAll(pretService.getAllPrets());
            tablePrets.refresh();
        } catch (Exception e) {
            afficherErreur("Erreur de chargement", "Impossible de récupérer les prêts : " + e.getMessage());
        }
    }

    private void configurerColonnes() {
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        colMotif.setCellFactory(TextFieldTableCell.forTableColumn());
        colMotif.setOnEditCommit(event -> {
            Pret p = event.getRowValue();
            p.setMotif(event.getNewValue());
            updatePretInDatabase(p);
        });

        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantDemande"));
        colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMontant.setOnEditCommit(event -> {
            Pret p = event.getRowValue();
            p.setMontantDemande(event.getNewValue());
            updatePretInDatabase(p);
        });

        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeMois"));
        colDuree.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colDuree.setOnEditCommit(event -> {
            Pret p = event.getRowValue();
            p.setDureeMois(event.getNewValue());
            updatePretInDatabase(p);
        });

        colTaux.setCellValueFactory(new PropertyValueFactory<>("taux"));
        colTaux.setCellFactory(TextFieldTableCell.forTableColumn(new FloatStringConverter()));
        colTaux.setOnEditCommit(event -> {
            Pret p = event.getRowValue();
            p.setTaux(event.getNewValue());
            updatePretInDatabase(p);
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    }

    private void updatePretInDatabase(Pret p) {
        try {
            pretService.modifierPret(p);
            System.out.println("Prêt mis à jour : " + p.getId());
        } catch (Exception e) {
            afficherErreur("Erreur de mise à jour", e.getMessage());
            chargerDonnees();
        }
    }

    // --- NAVIGATION ---

    @FXML
    private void ouvrirFormulaire() {
        naviguerVers("/view/DemandePret.fxml", "Nouvelle Demande");
    }

    @FXML
    private void ouvrirDashboard() {
        naviguerVers("/view/DashboardRisque.fxml", "Dashboard Risque");
    }

    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter PDF");
        fileChooser.setInitialFileName("Rapport_Prets.pdf");
        File file = fileChooser.showSaveDialog(tablePrets.getScene().getWindow());

        if (file != null) {
            try {
                // On exporte les données actuellement visibles (filtrées)
                pretService.genererRapportPDF(filteredData, file.getAbsolutePath());
                new Alert(Alert.AlertType.INFORMATION, "PDF généré avec succès !").show();
            } catch (Exception e) {
                afficherErreur("Erreur PDF", e.getMessage());
            }
        }
    }

    private void naviguerVers(String fxmlPath, String titre) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) tablePrets.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titre);
        } catch (Exception e) {
            afficherErreur("Erreur de navigation", "Impossible d'ouvrir : " + fxmlPath);
        }
    }

    // --- GESTION DES ACTIONS ---

    private void ajouterBoutonsActions() {
        Callback<TableColumn<Pret, Void>, TableCell<Pret, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnView = new Button("👁");
            private final Button btnCheck = new Button("✔");
            private final Button btnCross = new Button("✖");
            private final Button btnDelete = new Button("🗑");
            private final HBox pane = new HBox(5);

            {
                btnView.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btnCheck.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                btnCross.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: white; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                btnView.setOnAction(e -> ouvrirInterfaceRecouvrement(getTableRow().getItem()));
                btnCheck.setOnAction(e -> confirmerDecision(getTableRow().getItem(), "ACCORDE"));
                btnCross.setOnAction(e -> confirmerDecision(getTableRow().getItem(), "REFUSE"));
                btnDelete.setOnAction(e -> confirmerSuppression(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Pret p = getTableRow().getItem();
                    pane.getChildren().clear();
                    pane.getChildren().add(btnView);
                    if (p.getStatut() == StatutPret.EN_ATTENTE) {
                        pane.getChildren().addAll(btnCheck, btnCross);
                    }
                    pane.getChildren().add(btnDelete);
                    setGraphic(pane);
                }
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void confirmerDecision(Pret p, String decision) {
        String msg = decision.equals("ACCORDE") ? "accepter" : "refuser";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous " + msg + " ce prêt ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                gererDecision(p, decision);
            }
        });
    }

    private void gererDecision(Pret p, String decision) {
        try {
            if (decision.equals("ACCORDE")) {
                p.setStatut(StatutPret.ACCORDE);
                pretService.modifierPret(p);
                pretService.genererMensualites(p);
                mailService.envoyerEmailStatut(p, "Accordé");
            } else {
                p.setStatut(StatutPret.REFUSE);
                pretService.modifierPret(p);
                mailService.envoyerEmailStatut(p, "Refusé");
            }
            chargerDonnees();
        } catch (Exception e) {
            afficherErreur("Erreur", e.getMessage());
        }
    }

    private void confirmerSuppression(Pret p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce prêt ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    pretService.supprimerPret(p.getId());
                    chargerDonnees();
                } catch (Exception e) {
                    afficherErreur("Erreur", "Suppression impossible.");
                }
            }
        });
    }

    private void ouvrirInterfaceRecouvrement(Pret p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Mensualites.fxml"));
            Parent root = loader.load();
            MensualiteController controller = loader.getController();
            controller.setPretId(p.getId(), p.getMotif());
            Stage stage = (Stage) tablePrets.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherErreur(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t);
        a.setContentText(m);
        a.show();
    }
}