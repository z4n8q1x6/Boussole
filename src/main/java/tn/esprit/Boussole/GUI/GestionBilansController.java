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
import javafx.scene.control.ContextMenu; // Added
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem; // Added
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell; // Added
import javafx.stage.FileChooser;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter; // Added
import tn.esprit.Boussole.Models.bilan;
import tn.esprit.Boussole.Services.ServiceBilan;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GestionBilansController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnBudgets;
    @FXML private Button btnBilans;
    @FXML private Button btnGenererBilan;
    @FXML private Button btnExporterPDF;
    @FXML private TableView<bilan> tableBilans;
    @FXML private TableColumn<bilan, Integer> colMois;
    @FXML private TableColumn<bilan, Integer> colAnnee;
    @FXML private TableColumn<bilan, Double> colRecettes;
    @FXML private TableColumn<bilan, Double> colCharges;
    @FXML private TableColumn<bilan, Double> colResultat;
    //@FXML private TableColumn<bilan, Void> colActions; // Supprimé

    private ServiceBilan serviceBilan;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Vérification Session
        SessionManager session = SessionManager.getInstance();
        String role = session.getRole();
        boolean accesAutorise = session.isSessionActive() || "SIEGE".equals(role) || "ROLE_SIEGE".equals(role);

        if (!accesAutorise) {
            afficherMessageErreur("Session perdue ou accès refusé. Veuillez vous reconnecter.");
            // Attention: si l'UI n'est pas encore affichée, getWindow peut renvoyer null
            // Ici initialize est appelé après le chargement, donc ça devrait aller
            if (btnDashboard.getScene() != null) {
                ((Stage) btnDashboard.getScene().getWindow()).close();
            }
            return;
        }

        serviceBilan = new ServiceBilan();

        // 2. Configuration des Colonnes (CORRECTION)
        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colRecettes.setCellValueFactory(new PropertyValueFactory<>("totalRecettes"));
        colCharges.setCellValueFactory(new PropertyValueFactory<>("totalCharges"));
        colResultat.setCellValueFactory(new PropertyValueFactory<>("resultatNet"));

        // *** UX MODERNE : TABLE ÉDITABLE ***
        tableBilans.setEditable(true);

        // Édition Recettes
        colRecettes.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colRecettes.setOnEditCommit(event -> {
            bilan b = event.getRowValue();
            Double newVal = event.getNewValue();
            if (newVal == null || newVal < 0) {
                afficherMessageErreur("Valeur invalide.");
                tableBilans.refresh();
                return;
            }
            b.setTotalRecettes(newVal);
            b.setResultatNet(newVal - b.getTotalCharges()); // Recalcul auto
            try {
                serviceBilan.updateOne(b);
                tableBilans.refresh(); // Pour mettre à jour la colonne ColResultat visuellement
            } catch (Exception e) {
                afficherMessageErreur("Erreur mise à jour : " + e.getMessage());
            }
        });

        // Édition Charges
        colCharges.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colCharges.setOnEditCommit(event -> {
            bilan b = event.getRowValue();
            Double newVal = event.getNewValue();
            if (newVal == null || newVal < 0) {
                afficherMessageErreur("Valeur invalide.");
                tableBilans.refresh();
                return;
            }
            b.setTotalCharges(newVal);
            b.setResultatNet(b.getTotalRecettes() - newVal); // Recalcul auto
            try {
                serviceBilan.updateOne(b);
                tableBilans.refresh(); // Pour mettre à jour la colonne ColResultat visuellement
            } catch (Exception e) {
                afficherMessageErreur("Erreur mise à jour : " + e.getMessage());
            }
        });

        // *** UX MODERNE : MENU CONTEXTUEL POUR SUPPRESSION ***
        // Suppression de l'ancienne colonne Actions si elle existe dans FXML
        if (tableBilans.getColumns().size() > 5) {
             // Logic to ignore or hide colActions if still present in FXML
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemSupprimer = new MenuItem("🗑️ Supprimer cette ligne");
        itemSupprimer.setOnAction(e -> {
            bilan selected = tableBilans.getSelectionModel().getSelectedItem();
            if (selected != null) {
                supprimerBilan(selected);
            }
        });
        contextMenu.getItems().add(itemSupprimer);

        // Assigner le menu contextuel à chaque ligne (row)
        tableBilans.setRowFactory(tv -> {
            javafx.scene.control.TableRow<bilan> row = new javafx.scene.control.TableRow<>();
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(contextMenu)
            );
            return row;
        });

        // 4. Chargement initial des données
        rafraichirTable();

        // 5. Navigation & Actions
        btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"));
        btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBudgets.fxml"));
        btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBilans.fxml"));
        
        btnGenererBilan.setOnAction(event -> genererBilan());
        btnExporterPDF.setOnAction(event -> exporterPDF());
    }

    /**
     * Récupère l'historique depuis le service et rafraîchit la TableView.
     */
    private void rafraichirTable() {
        try {
            tableBilans.getItems().clear();
            // Récupère l'ID franchise (ou 1 si Siège pour visualisation)
            int franchiseId = SessionManager.getInstance().getIdFranchise();
            if (franchiseId == 0) franchiseId = 1; 

            List<bilan> bilans = serviceBilan.getHistorique(franchiseId);
            tableBilans.getItems().addAll(bilans);
        } catch (Exception e) {
            System.out.println("Erreur lors du rafraîchissement des bilans: " + e.getMessage());
        }
    }

    /**
     * Supprime un bilan après confirmation
     */
    private void supprimerBilan(bilan b) {
        if (!confirmerAction("Voulez-vous vraiment supprimer ce bilan ?\nCette action est irréversible.")) {
            return; // Utilisateur a cliqué Annuler
        }

        try {
            serviceBilan.deleteOne(b);
            rafraichirTable();
            afficherMessageSucces("Bilan supprimé avec succès !");
        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Génère un nouveau bilan avec validation
     */
    private void genererBilan() {
        try {
            // Valeurs actuelles (à remplacer par ComboBox si besoin)
            int mois = 2;   // TODO : à lier à une ComboBox
            int annee = 2026; // TODO : à lier à une ComboBox
            int franchiseId = SessionManager.getInstance().getIdFranchise();

            // Valider les valeurs
            if (mois < 1 || mois > 12) {
                afficherMessageErreur("Mois invalide : doit être entre 1 et 12");
                return;
            }

            if (annee < 2020 || annee > 2030) {
                afficherMessageErreur("Année invalide : doit être entre 2020 et 2030");
                return;
            }

            // Générer le bilan
            serviceBilan.genererBilan(mois, annee, franchiseId);
            rafraichirTable();
            afficherMessageSucces("Bilan généré avec succès pour " + mois + "/" + annee);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la génération : " + e.getMessage());
        }
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

    /**
     * Méthode utilitaire pour changer de page (navigation entre écrans FXML).
     * À réutiliser dans les autres contrôleurs (DashboardSiegeController, GestionBudgetsController).
     */
    private void changerPage(ActionEvent event, String fxmlPath) {
        try {
            // Charger le nouveau FXML
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Erreur : fichier FXML non trouvé : " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);

            // Charger la feuille CSS
            try {
                String css = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("Attention : CSS non chargée (" + e.getMessage() + ")");
            }

            // Obtenir la stage actuelle depuis le bouton source et changer la scène
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Boussole - " + fxmlPath);
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du FXML : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors du changement de page : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Exporte un bilan sélectionné au format PDF
     * Ouvre un FileChooser pour demander à l'utilisateur où enregistrer le fichier
     */
    @FXML
    private void exporterPDF() {
        // Vérifier qu'une ligne est sélectionnée dans la TableView
        int selectedIndex = tableBilans.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            afficherMessageErreur("Veuillez sélectionner un bilan à exporter");
            return;
        }

        // Récupérer le bilan sélectionné
        bilan bilanSelectionne = tableBilans.getSelectionModel().getSelectedItem();

        // Ouvrir le FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le bilan en PDF");

        // Définir le répertoire initial (Documents utilisateur)
        String userDocumentsPath = System.getProperty("user.home") + File.separator + "Documents";
        fileChooser.setInitialDirectory(new File(userDocumentsPath));

        // Proposer un nom de fichier par défaut
        fileChooser.setInitialFileName("bilan_" + bilanSelectionne.getMois() + "_" + bilanSelectionne.getAnnee() + ".pdf");

        // Ajouter un filtre pour les fichiers PDF
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(pdfFilter);

        // Afficher le dialog et récupérer le fichier choisi
        File fichierChoisi = fileChooser.showSaveDialog((Stage) btnExporterPDF.getScene().getWindow());

        // Si l'utilisateur a choisi un fichier
        if (fichierChoisi != null) {
            try {
                // Appeler le service pour exporter le bilan en PDF
                serviceBilan.exporterBilanPDF(bilanSelectionne, fichierChoisi.getAbsolutePath());

                // Afficher un message de succès
                afficherMessageSucces("Bilan exporté avec succès !\nFichier : " + fichierChoisi.getAbsolutePath());

            } catch (Exception e) {
                afficherMessageErreur("Erreur lors de l'export : " + e.getMessage());
                System.err.println("Exception : " + e.getMessage());
                e.printStackTrace();
            }
        }
        // Si l'utilisateur annule, ne rien faire
    }
}
