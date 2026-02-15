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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.Boussole.Models.bilan;
import tn.esprit.Boussole.Services.ServiceBilan;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GestionBilansController implements Initializable {

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnBudgets;

    @FXML
    private Button btnBilans;

    @FXML
    private Button btnGenererBilan;

    @FXML
    private Button btnExporterPDF;

    @FXML
    private TableView<bilan> tableBilans;

    @FXML
    private TableColumn<bilan, Integer> colMois;

    @FXML
    private TableColumn<bilan, Integer> colAnnee;

    @FXML
    private TableColumn<bilan, Double> colRecettes;

    @FXML
    private TableColumn<bilan, Double> colCharges;

    @FXML
    private TableColumn<bilan, Double> colResultat;

    @FXML
    private TableColumn<bilan, Void> colActions;

    private ServiceBilan serviceBilan;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Vérification Session
        SessionManager session = SessionManager.getInstance();
        String role = session.getRole();
        boolean accesAutorise = session.isSessionActive() || "SIEGE".equals(role) || "ROLE_SIEGE".equals(role);

        if (!accesAutorise) {
            afficherMessageErreur("Session perdue ou accès refusé. Veuillez vous reconnecter.");
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.close();
            return;
        }

        serviceBilan = new ServiceBilan();

        // 2. Configuration des Colonnes (CORRECTION)
        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colRecettes.setCellValueFactory(new PropertyValueFactory<>("totalRecettes"));
        colCharges.setCellValueFactory(new PropertyValueFactory<>("totalCharges"));
        colResultat.setCellValueFactory(new PropertyValueFactory<>("resultatNet"));

        // 3. Configuration de la colonne Actions
        configurerColonneActions();

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
     * Configure la colonne Actions avec deux boutons (Modifier et Supprimer)
     * en utilisant une cellFactory.
     */
    private void configurerColonneActions() {
        colActions.setCellFactory(param -> new TableCell<bilan, Void>() {
            private final Button btnModifier = new Button("✏️");
            private final Button btnSupprimer = new Button("🗑️");
            private final HBox hbox = new HBox(5, btnModifier, btnSupprimer);

            {
                btnModifier.getStyleClass().add("button-action-edit");
                btnSupprimer.getStyleClass().add("button-action-delete");
                hbox.setPadding(new Insets(2));

                btnModifier.setOnAction(event -> {
                    bilan b = getTableView().getItems().get(getIndex());
                    afficherDialogueModification(b);
                });

                btnSupprimer.setOnAction(event -> {
                    bilan b = getTableView().getItems().get(getIndex());
                    supprimerBilan(b);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
    }

    /**
     * Affiche un Dialog pour modifier les valeurs total_recettes et total_charges d'un bilan.
     */
    private void afficherDialogueModification(bilan b) {
        Dialog<bilan> dialog = new Dialog<>();
        dialog.setTitle("Modifier le Bilan");
        dialog.setHeaderText("Modifier les totaux du bilan pour " + b.getMois() + "/" + b.getAnnee());

        // Créer les champs de saisie
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        Label lblRecettes = new Label("Total Recettes (TND):");
        TextField txtRecettes = new TextField(String.valueOf(b.getTotalRecettes()));

        Label lblCharges = new Label("Total Charges (TND):");
        TextField txtCharges = new TextField(String.valueOf(b.getTotalCharges()));

        content.getChildren().addAll(
                lblRecettes, txtRecettes,
                lblCharges, txtCharges
        );

        dialog.getDialogPane().setContent(content);

        // Ajouter les boutons OK et Annuler
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
        );

        // Gérer le résultat
        dialog.setResultConverter(buttonType -> {
            if (buttonType == javafx.scene.control.ButtonType.OK) {
                try {
                    double recettes = Double.parseDouble(txtRecettes.getText());
                    double charges = Double.parseDouble(txtCharges.getText());
                    double resultat = recettes - charges;

                    b.setTotalRecettes(recettes);
                    b.setTotalCharges(charges);
                    b.setResultatNet(resultat);

                    return b;
                } catch (NumberFormatException ex) {
                    System.out.println("Erreur : valeurs numériques invalides. " + ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(bilanModifie -> {
            // Mettre à jour la base de données
            try {
                serviceBilan.updateOne(bilanModifie);
                System.out.println("Bilan modifié avec succès : " + bilanModifie.getId());
                // Rafraîchir la table
                rafraichirTable();
            } catch (Exception e) {
                System.out.println("Erreur lors de la modification du bilan: " + e.getMessage());
            }
        });
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
