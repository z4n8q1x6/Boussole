package controller;

import entity.Mensualite;
import service.PretService;
import service.MailService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class MensualiteController {

    @FXML private TableView<Mensualite> tableMensualites;
    @FXML private TableColumn<Mensualite, String> colDate;
    @FXML private TableColumn<Mensualite, Double> colMontant;
    @FXML private TableColumn<Mensualite, Void> colAction;
    @FXML private Label lblTitre;

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
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEcheance"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        ajouterBoutonPayer();
    }

    private void chargerMensualites() {
        try {
            tableMensualites.setItems(FXCollections.observableArrayList(pretService.getMensualitesByPret(currentPretId)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ajouterBoutonPayer() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPayer = new Button("Enregistrer Paiement");
            {
                btnPayer.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                btnPayer.setOnAction(e -> procederPaiement(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Mensualite m = getTableView().getItems().get(getIndex());
                    if (m.isEstPaye()) {
                        setGraphic(null);
                        setText("✅ Payé");
                    } else {
                        setGraphic(btnPayer);
                        setText(null);
                    }
                }
            }
        });
    }

    private void procederPaiement(Mensualite m) {
        try {
            pretService.marquerMensualiteCommePayee(m);
            mailService.envoyerEmailPaiement(m, currentMotif);
            chargerMensualites();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // LE NOM ICI DOIT ÊTRE IDENTIQUE AU onAction DANS LE FXML
    @FXML
    private void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/ListePrets.fxml"));
            Stage stage = (Stage) tableMensualites.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}