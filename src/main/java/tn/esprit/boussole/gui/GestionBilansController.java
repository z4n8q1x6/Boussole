package tn.esprit.boussole.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu; // Added
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem; // Added
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell; // Added
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.boussole.Utilis.MyBdConnexion;
import tn.esprit.boussole.models.bilan;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.models.transaction;
import tn.esprit.boussole.services.ServiceBilan;
import tn.esprit.boussole.services.ServiceEmail;
import tn.esprit.boussole.services.ServiceQuickChart;
import tn.esprit.boussole.Utilis.SessionManager;

import java.io.File;
import tn.esprit.boussole.Utilis.ThemeManager;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class GestionBilansController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnBudgets;
    @FXML private Button btnBilans;
    @FXML private Button btnGenererBilan;
    @FXML private Button btnExporterPDF;
    @FXML private Button btnEnvoyerEmail;
    @FXML private TableView<bilan> tableBilans;
    @FXML private TableColumn<bilan, Integer> colFranchise;
    @FXML private TableColumn<bilan, Integer> colMois;
    @FXML private TableColumn<bilan, Integer> colAnnee;
    @FXML private TableColumn<bilan, Double> colRecettes;
    @FXML private TableColumn<bilan, Double> colCharges;
    @FXML private TableColumn<bilan, Double> colResultat;
    @FXML private TableColumn<bilan, Double> colStatut;
    @FXML private ComboBox<String> comboMois;
    @FXML private ComboBox<Integer> comboAnnee;
    @FXML private ComboBox<franchise> cbFranchiseCible;

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

        // Initialisation de cbFranchiseCible
        try {
            Connection cnx = MyBdConnexion.getinstance().getCnx();
            String sql = "SELECT id, nom FROM franchises";
            cbFranchiseCible.getItems().add(new franchise(0, "TOUT LE RÉSEAU", "", "", "", null, true, 0.0));
            try (PreparedStatement ps = cnx.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    franchise f = new franchise();
                    f.setId(rs.getInt("id"));
                    f.setNom(rs.getString("nom"));
                    cbFranchiseCible.getItems().add(f);
                }
            }
            // Cell Factory to display nom
            javafx.util.StringConverter<franchise> cbConverter = new javafx.util.StringConverter<franchise>() {
                @Override public String toString(franchise f) { return f == null ? "" : f.getNom(); }
                @Override public franchise fromString(String string) { return null; }
            };
            cbFranchiseCible.setConverter(cbConverter);

            if ("SIEGE".equals(role) || "ROLE_SIEGE".equals(role)) {
                cbFranchiseCible.getSelectionModel().selectFirst();
            } else {
                int fId = session.getIdFranchise();
                cbFranchiseCible.getItems().stream().filter(f -> f.getId() != null && f.getId() == fId).findFirst().ifPresent(cbFranchiseCible.getSelectionModel()::select);
                cbFranchiseCible.setDisable(true);
            }
            cbFranchiseCible.setOnAction(e -> rafraichirTable());
        } catch (SQLException e) { System.err.println("Erreur chargement franchises : " + e.getMessage()); }

        // Formatting monétaire
        javafx.util.StringConverter<Double> currencyConverter = new javafx.util.StringConverter<Double>() {
            java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
            {
                format.setMinimumFractionDigits(2);
                format.setMaximumFractionDigits(2);
            }
            @Override
            public String toString(Double object) {
                if (object == null) return "0,00 TND";
                return format.format(object) + " TND";
            }
            @Override
            public Double fromString(String string) {
                try {
                    if (string == null || string.trim().isEmpty()) return 0.0;
                    return format.parse(string.replace(" TND", "").replace(" ", "").trim()).doubleValue();
                } catch (Exception e) {
                    return 0.0;
                }
            }
        };

        // 2. Configuration des Colonnes (CORRECTION)
        colFranchise.setCellValueFactory(new PropertyValueFactory<>("franchiseId"));
        colFranchise.setCellFactory(column -> new TableCell<bilan, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                } else if (id == 0) {
                    setText("TOUT LE RÉSEAU");
                } else {
                    String nom = "Inconnu";
                    for (franchise f : cbFranchiseCible.getItems()) {
                        if (f.getId() != null && f.getId().equals(id)) {
                            nom = f.getNom(); break;
                        }
                    }
                    setText(nom);
                }
            }
        });

        colMois.setCellValueFactory(new PropertyValueFactory<>("mois"));
        colAnnee.setCellValueFactory(new PropertyValueFactory<>("annee"));
        colRecettes.setCellValueFactory(new PropertyValueFactory<>("totalRecettes"));
        colCharges.setCellValueFactory(new PropertyValueFactory<>("totalCharges"));
        colResultat.setCellValueFactory(new PropertyValueFactory<>("resultatNet"));

        // Format Resultat (non éditable)
        colResultat.setCellFactory(column -> new TableCell<bilan, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyConverter.toString(item));
                }
            }
        });

        // Colonne Statut / Rentabilité
        colStatut.setCellValueFactory(new PropertyValueFactory<>("resultatNet"));
        colStatut.setCellFactory(column -> new TableCell<bilan, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    if (item > 0) {
                        setText(" Bénéficiaire");
                        // Icone check
                        Label icon = new Label("✓");
                        icon.setStyle("-fx-text-fill: #10B981; -fx-font-size: 14px; -fx-font-weight: bold;");
                        setGraphic(icon);
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    } else if (item < 0) {
                        setText(" Déficitaire");
                        // Icone warning
                        Label icon = new Label("⚠");
                        icon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-font-weight: bold;");
                        setGraphic(icon);
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    } else {
                        setText(" Équilibré");
                        Label icon = new Label("—");
                        icon.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: bold;");
                        setGraphic(icon);
                        setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            }
        });

        // Initialisation de la Barre d'outils
        comboMois.getItems().addAll(
            "1 - Janvier", "2 - Février", "3 - Mars", "4 - Avril",
            "5 - Mai", "6 - Juin", "7 - Juillet", "8 - Août",
            "9 - Septembre", "10 - Octobre", "11 - Novembre", "12 - Décembre"
        );
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        comboMois.getSelectionModel().select(currentMonth - 1);

        for (int yr = 2024; yr <= 2030; yr++) {
            comboAnnee.getItems().add(yr);
        }
        comboAnnee.getSelectionModel().select(Integer.valueOf(java.time.LocalDate.now().getYear()));

        // *** UX MODERNE : TABLE ÉDITABLE ***
        tableBilans.setEditable(true);

        // Édition Recettes
        colRecettes.setCellFactory(TextFieldTableCell.forTableColumn(currencyConverter));
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
                serviceBilan.updateone(b);
                tableBilans.refresh(); // Pour mettre à jour la colonne ColResultat visuellement
            } catch (Exception e) {
                afficherMessageErreur("Erreur mise à jour : " + e.getMessage());
            }
        });

        // Édition Charges
        colCharges.setCellFactory(TextFieldTableCell.forTableColumn(currencyConverter));
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
                serviceBilan.updateone(b);
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

        // 4.1 Double-clic pour Drill-Down Transactions
        tableBilans.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableBilans.getSelectionModel().getSelectedItem() != null) {
                bilan selected = tableBilans.getSelectionModel().getSelectedItem();
                afficherTransactionsDetails(selected);
            }
        });

        // 5. Navigation & Actions
        btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/boussole/gui/DashboardSiege.fxml"));
        btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/boussole/gui/GestionBudgets.fxml"));
        btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/boussole/gui/GestionBilans.fxml"));
        
        btnGenererBilan.setOnAction(event -> genererBilan());
        btnExporterPDF.setOnAction(event -> exporterPDF());
        if (btnEnvoyerEmail != null) btnEnvoyerEmail.setOnAction(event -> envoyerBilanEmail());
    }

    /**
     * Récupère l'historique depuis le service et rafraîchit la TableView.
     */
    private void rafraichirTable() {
        try {
            tableBilans.getItems().clear();
            franchise f = cbFranchiseCible.getSelectionModel().getSelectedItem();
            int franchiseId = (f != null && f.getId() != null) ? f.getId() : 0;

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
            serviceBilan.deleteone(b);
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
            if (comboMois.getSelectionModel().getSelectedIndex() < 0 || comboAnnee.getSelectionModel().getSelectedItem() == null) {
                afficherMessageErreur("Veuillez sélectionner un mois et une année.");
                return;
            }
            int mois = comboMois.getSelectionModel().getSelectedIndex() + 1;
            int annee = comboAnnee.getSelectionModel().getSelectedItem();
            
            franchise f = cbFranchiseCible.getSelectionModel().getSelectedItem();
            if (f == null) {
                afficherMessageErreur("Veuillez sélectionner une cible.");
                return;
            }
            int targetFranchiseId = (f.getId() != null) ? f.getId() : 0;

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
            serviceBilan.genererBilan(mois, annee, targetFranchiseId);
            rafraichirTable();
            afficherMessageSucces("Bilan généré avec succès pour " + mois + "/" + annee);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la génération : " + e.getMessage());
        }
    }

    /**
     * Affiche la liste des transactions pour un bilan (Drill-Down)
     */
    private void afficherTransactionsDetails(bilan b) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du Bilan : " + b.getMois() + " / " + b.getAnnee());
        dialog.setHeaderText("Liste des transactions de la période");

        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        TableView<transaction> table = new TableView<>();
        table.setPrefWidth(600);
        table.setPrefHeight(400);

        TableColumn<transaction, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setPrefWidth(120);

        TableColumn<transaction, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(100);

        TableColumn<transaction, Double> colMontant = new TableColumn<>("Montant");
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colMontant.setPrefWidth(120);

        TableColumn<transaction, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDesc.setPrefWidth(240);

        table.getColumns().addAll(colDate, colType, colMontant, colDesc);

        // Récupérer les données
        try {
            Connection cnx = MyBdConnexion.getinstance().getCnx();
            String sql = "SELECT * FROM transaction WHERE MONTH(date) = ? AND YEAR(date) = ?";
            if (b.getFranchiseId() != 0) {
                sql += " AND franchise_id = ?";
            }
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, b.getMois());
            ps.setInt(2, b.getAnnee());
            if (b.getFranchiseId() != 0) {
                ps.setInt(3, b.getFranchiseId());
            }
            
            ResultSet rs = ps.executeQuery();

            javafx.collections.ObservableList<transaction> list = javafx.collections.FXCollections.observableArrayList();
            while (rs.next()) {
                transaction t = new transaction();
                t.setId(rs.getInt("id"));
                t.setDate(rs.getDate("date"));
                t.setMontant(rs.getDouble("montant"));
                t.setType(transaction.Type.valueOf(rs.getString("type").toUpperCase()));
                t.setDescription(rs.getString("description"));
                t.setFranchiseId(rs.getInt("franchise_id"));
                list.add(t);
            }
            table.setItems(list);
        } catch (SQLException e) {
            System.err.println("Erreur chargement transactions : " + e.getMessage());
        }

        dialog.getDialogPane().setContent(table);
        dialog.showAndWait();
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
                String css = getClass().getResource("/tn/esprit/boussole/gui/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                System.out.println("Attention : CSS non chargée (" + e.getMessage() + ")");
            }

            // Obtenir la stage actuelle depuis le bouton source et changer la scène
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            ThemeManager.getInstance().applyCurrentTheme(scene);
            stage.setTitle("boussole - " + fxmlPath);
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

    /**
     * Envoie le bilan sélectionné par email au gérant de la franchise avec graphique QuickChart intégré.
     */
    @FXML
    private void envoyerBilanEmail() {
        // Vérifier qu'une ligne est sélectionnée dans la TableView
        int selectedIndex = tableBilans.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            afficherMessageErreur("Veuillez sélectionner un bilan à envoyer par email.");
            return;
        }

        bilan bilanSel = tableBilans.getSelectionModel().getSelectedItem();
        
        // Récupérer la franchise pour obtenir l'email
        String emailDestinataire = "";
        String nomFranchise = "Tout le Réseau";
        
        if (bilanSel.getFranchiseId() == 0) {
            // C'est un bilan consolidé ("TOUT LE RÉSEAU")
            // On demandera l'email ci-dessous car il n'y a pas de gérant spécifique dans la table franchises
        } else {
            try {
                Connection cnx = MyBdConnexion.getinstance().getCnx();
                String sql = "SELECT nom, email FROM franchises WHERE id = ?";
                PreparedStatement ps = cnx.prepareStatement(sql);
                ps.setInt(1, bilanSel.getFranchiseId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    nomFranchise = rs.getString("nom");
                    emailDestinataire = rs.getString("email");
                }
            } catch (SQLException e) {
                afficherMessageErreur("Erreur de base de données : impossible de récupérer l'email du gérant.");
                return;
            }
        }

        // Si pas d'email en base (ou bilan global), on demande à l'utilisateur de le saisir
        if (emailDestinataire == null || emailDestinataire.trim().isEmpty()) {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Email manquant");
            dialog.setHeaderText("Aucune adresse email trouvée pour : " + nomFranchise);
            dialog.setContentText("Veuillez saisir l'adresse email de destination :");

            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                emailDestinataire = result.get().trim();
            } else {
                afficherMessageErreur("Envoi annulé : L'adresse email est obligatoire.");
                return;
            }
        }

        // Configuration pour ServiceQuickChart et ServiceEmail
        ServiceQuickChart serviceChart = new ServiceQuickChart();
        ServiceEmail serviceEmail = new ServiceEmail();

        String urlGraphique = serviceChart.genererUrlGraphique(bilanSel.getTotalRecettes(), bilanSel.getTotalCharges());
        
        // Construire le contenu HTML du message
        String sujet = "Rapport Financier Mensuel - " + nomFranchise + " (" + bilanSel.getMois() + "/" + bilanSel.getAnnee() + ")";
        
        String htmlContent = "<div style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #0f172a;'>"
            + "<div style='max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);'>"
            + "  <div style='background-color: #1e293b; color: white; padding: 20px; text-align: center;'>"
            + "    <h2 style='margin:0;'>boussole - Bilan Périodique</h2>"
            + "    <p style='margin:5px 0 0 0; opacity: 0.8;'>Période : " + bilanSel.getMois() + " / " + bilanSel.getAnnee() + "</p>"
            + "  </div>"
            + "  <div style='padding: 30px;'>"
            + "    <p>Bonjour le gérant de <b>" + nomFranchise + "</b>,</p>"
            + "    <p>Le siège a généré (ou mis à jour) votre bilan mensuel. Voici un résumé des opérations :</p>"
            + "    <table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>"
            + "      <tr>"
            + "        <td style='padding: 10px; border-bottom: 1px solid #e2e8f0; font-weight: bold;'>Total Recettes :</td>"
            + "        <td style='padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #10B981; font-weight: bold;'>" + String.format("%.2f", bilanSel.getTotalRecettes()) + " TND</td>"
            + "      </tr>"
            + "      <tr>"
            + "        <td style='padding: 10px; border-bottom: 1px solid #e2e8f0; font-weight: bold;'>Total Charges :</td>"
            + "        <td style='padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #EF4444; font-weight: bold;'>" + String.format("%.2f", bilanSel.getTotalCharges()) + " TND</td>"
            + "      </tr>"
            + "      <tr>"
            + "        <td style='padding: 10px; border-bottom: 2px solid #1e293b; font-weight: bold; font-size: 16px;'>Résultat Net :</td>"
            + "        <td style='padding: 10px; border-bottom: 2px solid #1e293b; text-align: right; font-weight: bold; font-size: 16px;" 
            + (bilanSel.getResultatNet() >= 0 ? " color: #10B981;" : " color: #EF4444;") + "'>" 
            + String.format("%.2f", bilanSel.getResultatNet()) + " TND</td>"
            + "      </tr>"
            + "    </table>"
            + "    <div style='text-align: center; margin-top: 30px;'>"
            + "      <p style='font-size: 14px; color: #64748b; margin-bottom: 10px;'>Aperçu Graphique</p>"
            + "      <img src='" + urlGraphique + "' alt='Graphique Répartition' style='max-width: 100%; height: auto; border: 1px solid #e2e8f0; border-radius: 4px;' />"
            + "    </div>"
            + "  </div>"
            + "  <div style='background-color: #f1f5f9; padding: 15px; text-align: center; font-size: 12px; color: #64748b;'>"
            + "    <p style='margin: 0;'>Ce message est généré automatiquement par l'ERP boussole. Merci de ne pas y répondre.</p>"
            + "  </div>"
            + "</div>"
            + "</div>";

        try {
            serviceEmail.envoyerEmailHTML(emailDestinataire, sujet, htmlContent);
            afficherMessageSucces("E-mail envoyé avec succès au gérant de " + nomFranchise + " à l'adresse " + emailDestinataire);
        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
