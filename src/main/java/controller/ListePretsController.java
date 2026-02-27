package controller;

import entity.Pret;
import entity.StatutPret;
import service.PretService;
import service.MailService;
import org.example.Chatbot;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.stage.FileChooser;
import javafx.stage.Window;
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
    private FilteredList<Pret> filteredData;

    @FXML
    public void initialize() {
        tablePrets.setEditable(true);
        if (comboStatut != null) {
            comboStatut.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "ACCORDE", "REFUSE"));
            comboStatut.getSelectionModel().selectFirst();
        }
        configurerColonnes();
        chargerDonnees();
        ajouterBoutonsActions();
        configurerFiltrageMultiCritere();
    }

    private void configurerFiltrageMultiCritere() {
        filteredData = new FilteredList<>(pretList, p -> true);
        Runnable appliquerFiltres = () -> {
            String textSearch = (txtRecherche.getText() == null) ? "" : txtRecherche.getText().toLowerCase();
            String statutSearch = comboStatut.getSelectionModel().getSelectedItem();
            filteredData.setPredicate(pret -> {
                boolean matchStatut = (statutSearch == null || statutSearch.equals("TOUS")) || pret.getStatut().toString().equals(statutSearch);
                boolean matchText = textSearch.isEmpty() || pret.getMotif().toLowerCase().contains(textSearch);
                return matchStatut && matchText;
            });
        };
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres.run());
        comboStatut.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres.run());
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
            afficherErreur("Erreur", "Chargement impossible.");
        }
    }

    // --- PARTIE MISE À JOUR ICI ---
    private void configurerColonnes() {
        // 1. Colonne MOTIF (String)
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        colMotif.setCellFactory(TextFieldTableCell.forTableColumn());
        colMotif.setOnEditCommit(event -> {
            Pret pret = event.getRowValue();
            pret.setMotif(event.getNewValue());
            mettreAJourPret(pret);
        });

        // 2. Colonne MONTANT (Double)
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montantDemande"));
        colMontant.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMontant.setOnEditCommit(event -> {
            Pret pret = event.getRowValue();
            pret.setMontantDemande(event.getNewValue());
            mettreAJourPret(pret);
        });

        // 3. Colonne DURÉE (Integer)
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeMois"));
        colDuree.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colDuree.setOnEditCommit(event -> {
            Pret pret = event.getRowValue();
            pret.setDureeMois(event.getNewValue());
            mettreAJourPret(pret);
        });

        // 4. Colonne TAUX (Float)
        colTaux.setCellValueFactory(new PropertyValueFactory<>("taux"));
        colTaux.setCellFactory(TextFieldTableCell.forTableColumn(new FloatStringConverter()));
        colTaux.setOnEditCommit(event -> {
            Pret pret = event.getRowValue();
            pret.setTaux(event.getNewValue());
            mettreAJourPret(pret);
        });

        // 5. Colonne STATUT
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    }
    // ------------------------------

    @FXML
    private void ouvrirChatbot(ActionEvent event) {
        try {
            double mtAccorde = pretService.getMontantTotalParStatut(StatutPret.ACCORDE);
            double mtRefuse = pretService.getMontantTotalParStatut(StatutPret.REFUSE);
            double mtAttente = pretService.getMontantTotalParStatut(StatutPret.EN_ATTENTE);

            long nbAccorde = pretService.countPretsParStatut(StatutPret.ACCORDE);
            long nbRefuse = pretService.countPretsParStatut(StatutPret.REFUSE);
            long nbAttente = pretService.countPretsParStatut(StatutPret.EN_ATTENTE);

            StringBuilder synthese = new StringBuilder();
            synthese.append("Tu es l'analyste financier Boussole. Voici l'état actuel des prêts :\n\n");
            synthese.append("1. PRÊTS ACCORDÉS : ").append(nbAccorde).append(" dossiers pour ").append(mtAccorde).append(" DT\n");
            synthese.append("2. PRÊTS REFUSÉS : ").append(nbRefuse).append(" dossiers pour ").append(mtRefuse).append(" DT\n");
            synthese.append("3. PRÊTS EN ATTENTE : ").append(nbAttente).append(" dossiers pour ").append(mtAttente).append(" DT\n\n");
            synthese.append("Réponds aux questions de l'utilisateur avec ces chiffres.");

            Chatbot chatbotApp = new Chatbot(synthese.toString());
            Stage stage = new Stage();
            stage.setScene(chatbotApp.creerSceneChatbot());
            stage.setTitle("Assistant IA Boussole");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void ouvrirFormulaire(ActionEvent event) { naviguerVers("/view/DemandePret.fxml", "Nouvelle Demande"); }

    @FXML
    private void handleExportPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter la liste des prêts en PDF");
        fileChooser.setInitialFileName("Rapport_Prets_Boussole.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"));
        Window stage = tablePrets.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                pretService.genererRapportPDF(filteredData, file.getAbsolutePath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText(null);
                alert.setContentText("Le rapport PDF a été généré avec succès !");
                alert.show();
            } catch (Exception e) {
                afficherErreur("Erreur lors de la génération PDF", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void ouvrirDashboard(ActionEvent event) {
        System.out.println("Navigation vers le dashboard...");
        naviguerVers("/view/DashboardRisque.fxml", "Tableau de Bord");
    }

    private void ajouterBoutonsActions() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("👁");
            private final Button btnCheck = new Button("✔");
            private final Button btnCross = new Button("✖");
            private final Button btnDelete = new Button("🗑");
            private final HBox pane = new HBox(8);

            {
                btnView.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-cursor: hand;");
                btnCheck.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-cursor: hand;");
                btnCross.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-cursor: hand;");

                btnView.setOnAction(e -> ouvrirInterfaceRecouvrement(getTableRow().getItem()));
                btnCheck.setOnAction(e -> gererDecision(getTableRow().getItem(), "ACCORDE"));
                btnCross.setOnAction(e -> gererDecision(getTableRow().getItem(), "REFUSE"));
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

    // MÉTHODE NÉCESSAIRE POUR L'ENREGISTREMENT DE L'ÉDITION
    private void mettreAJourPret(Pret p) {
        try {
            pretService.modifierPret(p);
            System.out.println("✅ Modification enregistrée pour le prêt ID: " + p.getId());
        } catch (Exception e) {
            afficherErreur("Erreur de mise à jour", "Impossible de sauvegarder la modification.");
            chargerDonnees();
        }
    }

    private void confirmerSuppression(Pret p) {
        try {
            pretService.supprimerPret(p.getId());
            chargerDonnees();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void ouvrirInterfaceRecouvrement(Pret p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Mensualites.fxml"));
            Parent root = loader.load();
            ((MensualiteController)loader.getController()).setPretId(p.getId(), p.getMotif());
            ((Stage)tablePrets.getScene().getWindow()).setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void naviguerVers(String fxmlPath, String titre) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("ERREUR : Fichier FXML introuvable -> " + fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) tablePrets.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(titre);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherErreur(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setContentText(m); a.show();
    }
}