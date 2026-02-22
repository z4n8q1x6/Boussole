package controller;

import entity.Mensualite;
import service.PretService;
import service.MailService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

public class MensualiteController {

    @FXML private TableView<Mensualite> tableMensualites;
    @FXML private TableColumn<Mensualite, String> colDate;
    @FXML private TableColumn<Mensualite, Double> colMontant;
    @FXML private TableColumn<Mensualite, String> colStatus; // AJOUTÉ
    @FXML private TableColumn<Mensualite, Void> colAction;

    @FXML private Label lblTitre;
    @FXML private Label lblStatusPret; // AJOUTÉ
    @FXML private Label lblResteAPayer; // AJOUTÉ

    private PretService pretService = new PretService();
    private MailService mailService = new MailService();
    private int currentPretId;
    private String currentMotif;

    public void setPretId(int id, String motif) {
        this.currentPretId = id;
        this.currentMotif = motif;
        if (lblTitre != null) {
            lblTitre.setText("Recouvrement : " + motif);
        }
        chargerMensualites();
    }

    @FXML
    public void initialize() {
        // Configuration des colonnes de base
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEcheance"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));

        // Configuration de la colonne ÉTAT (Visuel)
        configurerColonneEtat();

        // Boutons d'action
        ajouterBoutonPayer();
    }

    private void chargerMensualites() {
        try {
            List<Mensualite> liste = pretService.getMensualitesByPret(currentPretId);
            tableMensualites.setItems(FXCollections.observableArrayList(liste));

            // Mise à jour des labels de résumé
            mettreAJourResume(liste);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mettreAJourResume(List<Mensualite> liste) {
        double reste = liste.stream()
                .filter(m -> !m.isEstPaye())
                .mapToDouble(Mensualite::getMontant)
                .sum();

        lblResteAPayer.setText(String.format("%.2f DT", reste));

        if (reste == 0 && !liste.isEmpty()) {
            lblStatusPret.setText("TERMINÉ");
            lblStatusPret.setStyle("-fx-text-fill: #10B981; -fx-font-weight: 900;");
        } else {
            lblStatusPret.setText("EN COURS");
            lblStatusPret.setStyle("-fx-text-fill: #00E5CC; -fx-font-weight: 900;");
        }
    }

    private void configurerColonneEtat() {
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Mensualite m = getTableRow().getItem();
                    if (m.isEstPaye()) {
                        setText("✅ PAYÉ");
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    } else {
                        setText("⏳ EN ATTENTE");
                        setStyle("-fx-text-fill: #F87171; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void ajouterBoutonPayer() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPayer = new Button("Enregistrer Paiement");
            {
                // Style néon pour le bouton
                btnPayer.setStyle("-fx-background-color: #00E5CC; -fx-text-fill: #06080F; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                btnPayer.setOnAction(e -> procederPaiement(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Mensualite m = getTableView().getItems().get(getIndex());
                    if (m.isEstPaye()) {
                        setGraphic(null); // Pas de bouton si déjà payé
                    } else {
                        setGraphic(btnPayer);
                    }
                }
            }
        });
    }

    private void procederPaiement(Mensualite m) {
        // Confirmation simple avant paiement
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer l'encaissement de " + m.getMontant() + " DT ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    pretService.marquerMensualiteCommePayee(m);
                    mailService.envoyerEmailPaiement(m, currentMotif);
                    chargerMensualites(); // Rafraîchit tout (tableau + labels)
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/ListePrets.fxml"));
            Stage stage = (Stage) tableMensualites.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Liste des Prêts");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}