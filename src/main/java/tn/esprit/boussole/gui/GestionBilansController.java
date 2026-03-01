package tn.esprit.boussole.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import tn.esprit.boussole.models.bilan;
import tn.esprit.boussole.models.franchise;
import tn.esprit.boussole.service.ServiceBilan;

import java.util.prefs.Preferences;
import tn.esprit.boussole.utils.MyBdConnexion;

import tn.esprit.boussole.utils.ThemeManagerS;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javax.mail.*;
import javax.mail.internet.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;
import javafx.scene.control.SelectionMode;

public class GestionBilansController implements Initializable {

    // Éléments de navigation (Potentiellement null si supprimés du FXML)
    @FXML private Button btnDashboard;
    @FXML private Button btnBudgets;
    @FXML private Button btnBilans;

    // Éléments principaux
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
        Preferences prefs = Preferences.userRoot().node(loginController.class.getName());
        String role = prefs.get("role", "");
        String email = prefs.get("email", "");

        serviceBilan = new ServiceBilan();

        // 2. Initialisation Sécurisée de la Navigation
        // On vérifie si les boutons existent avant de leur assigner des actions
        if (btnDashboard != null) btnDashboard.setOnAction(event -> changerPage(event, "/DashboardSiege.fxml"));
        if (btnBudgets != null) btnBudgets.setOnAction(event -> changerPage(event, "/GestionBudgets.fxml"));
        if (btnBilans != null) btnBilans.setOnAction(event -> changerPage(event, "/GestionBilans.fxml"));

        // 3. Configuration de la Table et des ComboBox
        configurerInterface(role, email);
        configurerColonnesTable();

        // Actions principales
        if (btnGenererBilan != null) btnGenererBilan.setOnAction(event -> genererBilan());
        if (btnExporterPDF != null) btnExporterPDF.setOnAction(event -> exporterPDF());
        if (btnEnvoyerEmail != null) btnEnvoyerEmail.setOnAction(event -> envoyerBilanEmail());

        // Chargement initial
        rafraichirTable();
    }

    private void configurerInterface(String role, String email) {
        try {
            Connection cnx = MyBdConnexion.getinstance().getCnx();
            cbFranchiseCible.getItems().add(new franchise(0, "TOUT LE RÉSEAU", "", "", "", null, true, 0.0));

            String sql = "SELECT id, nom FROM franchises";
            try (PreparedStatement ps = cnx.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cbFranchiseCible.getItems().add(new franchise(rs.getInt("id"), rs.getString("nom"), "", "", "", null, true, 0.0));
                }
            }

            cbFranchiseCible.setConverter(new javafx.util.StringConverter<franchise>() {
                @Override public String toString(franchise f) { return f == null ? "" : f.getNom(); }
                @Override public franchise fromString(String s) { return null; }
            });

            if ("SIEGE".equals(role) || "ROLE_SIEGE".equals(role)) {
                cbFranchiseCible.getSelectionModel().selectFirst();
            } else {
                int fId = fetchFranchiseId(email);
                cbFranchiseCible.getItems().stream().filter(f -> f.getId() != null && f.getId() == fId).findFirst().ifPresent(cbFranchiseCible.getSelectionModel()::select);
                cbFranchiseCible.setDisable(true);
            }
            cbFranchiseCible.setOnAction(e -> rafraichirTable());

        } catch (SQLException e) {
            System.err.println("Erreur SQL Initialisation: " + e.getMessage());
        }

        // Remplissage dates
        comboMois.getItems().addAll("1 - Janvier", "2 - Février", "3 - Mars", "4 - Avril", "5 - Mai", "6 - Juin", "7 - Juillet", "8 - Août", "9 - Septembre", "10 - Octobre", "11 - Novembre", "12 - Décembre");
        comboMois.getSelectionModel().select(java.time.LocalDate.now().getMonthValue() - 1);
        for (int yr = 2024; yr <= 2030; yr++) comboAnnee.getItems().add(yr);
        comboAnnee.getSelectionModel().select(Integer.valueOf(java.time.LocalDate.now().getYear()));
    }

    private void configurerColonnesTable() {
        javafx.util.StringConverter<Double> currencyConverter = new javafx.util.StringConverter<Double>() {
            @Override public String toString(Double d) { return d == null ? "0,00 TND" : String.format("%.2f TND", d); }
            @Override public Double fromString(String s) { return Double.parseDouble(s.replaceAll("[^\\d.]", "")); }
        };

        colFranchise.setCellValueFactory(new PropertyValueFactory<>("franchiseId"));
        colFranchise.setCellFactory(column -> new TableCell<bilan, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String nom = "Franchise " + item;
                    for (franchise f : cbFranchiseCible.getItems()) {
                        if (f.getId() != null && f.getId().equals(item)) {
                            nom = f.getNom();
                            break;
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

        // Rendre les colonnes éditables
        tableBilans.setEditable(true);
        // Activer la sélection multiple
        tableBilans.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        colRecettes.setCellFactory(TextFieldTableCell.forTableColumn(currencyConverter));
        colCharges.setCellFactory(TextFieldTableCell.forTableColumn(currencyConverter));

        // Gestion du statut (Couleur dynamique + icônes)
        colStatut.setCellValueFactory(new PropertyValueFactory<>("resultatNet"));
        colStatut.setCellFactory(column -> new TableCell<bilan, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item > 0) {
                    setText("✓ Bénéficiaire");
                    setStyle("-fx-text-fill: #00E5CC; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else if (item < 0) {
                    setText("△□ Déficitaire");
                    setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else {
                    setText("— Équilibré");
                    setStyle("-fx-text-fill: #CBD5E1; -fx-font-weight: normal; -fx-font-size: 13px;");
                }
            }
        });

        // Menu contextuel (Clic droit pour supprimer)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("🗑 Supprimer ce bilan");
        deleteItem.setOnAction(e -> {
            bilan b = tableBilans.getSelectionModel().getSelectedItem();
            if (b != null) supprimerBilan(b);
        });
        contextMenu.getItems().add(deleteItem);
        tableBilans.setContextMenu(contextMenu);
    }

    private void rafraichirTable() {
        if (tableBilans == null) return;
        try {
            tableBilans.getItems().clear();
            franchise f = cbFranchiseCible.getSelectionModel().getSelectedItem();
            int fid = (f != null && f.getId() != null) ? f.getId() : 0;
            tableBilans.getItems().addAll(serviceBilan.getHistorique(fid));
        } catch (Exception e) {
            System.err.println("Erreur rafraîchissement table: " + e.getMessage());
        }
    }

    private void supprimerBilan(bilan b) {
        if (confirmerAction("Supprimer définitivement ce bilan ?")) {
            try {
                serviceBilan.deleteone(b);
                rafraichirTable();
            } catch (Exception e) {
                afficherMessageErreur("Erreur: " + e.getMessage());
            }
        }
    }

    private void genererBilan() {
        try {
            int moisSelectionne = comboMois.getSelectionModel().getSelectedIndex() + 1;
            int annee = comboAnnee.getSelectionModel().getSelectedItem();
            franchise f = cbFranchiseCible.getSelectionModel().getSelectedItem();
            int fid = (f != null) ? f.getId() : 0;

            // Générer/Recalculer pour le mois sélectionné ET tous les mois précédents de l'année
            for (int mois = 1; mois <= moisSelectionne; mois++) {
                serviceBilan.genererBilan(mois, annee, fid);
            }
            rafraichirTable();
            afficherMessageSucces("Bilans générés du mois 1 au mois " + moisSelectionne + " / " + annee + " !");
        } catch (Exception e) {
            afficherMessageErreur("Erreur génération: " + e.getMessage());
        }
    }

    // --- Méthodes utilitaires communes ---

    private void changerPage(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            ThemeManagerS.getInstance().applyCurrentTheme(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void afficherMessageSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.show();
    }

    private void afficherMessageErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.show();
    }

    private boolean confirmerAction(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private int fetchFranchiseId(String email) {
        String sql = "SELECT id_franchise FROM utilisateur WHERE email = ? LIMIT 1";
        try (Connection conn = MyBdConnexion.getinstance().getCnx();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id_franchise");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @FXML private void exporterPDF() {
        if (tableBilans.getItems().isEmpty()) {
            afficherMessageErreur("Aucune donnée à exporter.");
            return;
        }

        List<bilan> selectedItems = tableBilans.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            selectedItems = tableBilans.getItems(); // If none selected, export all
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Bilan PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(btnExporterPDF.getScene().getWindow());

        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // Iterating and generating a specific layout for each selected Bilan
                com.itextpdf.text.Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
                com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
                com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
                com.itextpdf.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);

                for (bilan b : selectedItems) {
                    
                    document.add(new Paragraph("\n"));

                    Paragraph title = new Paragraph("Bilan Financier - Mois " + b.getMois() + " / Année " + b.getAnnee(), titleFont);
                    title.setAlignment(Element.ALIGN_CENTER);
                    title.setSpacingAfter(20);
                    document.add(title);
                    
                    String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
                    Paragraph dateGen = new Paragraph("Date de génération : " + dateStr, subTitleFont);
                    dateGen.setSpacingAfter(15);
                    document.add(dateGen);

                    // Table with 2 columns: Libellé | Montant
                    PdfPTable pdfTable = new PdfPTable(2);
                    pdfTable.setWidthPercentage(100);
                    float[] columnWidths = {2f, 2f};
                    pdfTable.setWidths(columnWidths);

                    // Headers
                    PdfPCell header1 = new PdfPCell(new Phrase("Libellé", boldFont));
                    header1.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header1.setPadding(5);
                    pdfTable.addCell(header1);

                    PdfPCell header2 = new PdfPCell(new Phrase("Montant (TND)", boldFont));
                    header2.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header2.setPadding(5);
                    pdfTable.addCell(header2);

                    // Data Row 1: Recettes
                    PdfPCell libRecette = new PdfPCell(new Phrase("Total Recettes", rowFont));
                    libRecette.setPadding(5);
                    pdfTable.addCell(libRecette);

                    PdfPCell valRecette = new PdfPCell(new Phrase(String.format("%.2f", b.getTotalRecettes()), rowFont));
                    valRecette.setPadding(5);
                    pdfTable.addCell(valRecette);

                    // Data Row 2: Charges
                    PdfPCell libCharge = new PdfPCell(new Phrase("Total Charges", rowFont));
                    libCharge.setPadding(5);
                    pdfTable.addCell(libCharge);

                    PdfPCell valCharge = new PdfPCell(new Phrase(String.format("%.2f", b.getTotalCharges()), rowFont));
                    valCharge.setPadding(5);
                    pdfTable.addCell(valCharge);

                    // Data Row 3: Resultat Net
                    PdfPCell libResultat = new PdfPCell(new Phrase("Résultat Net", boldFont));
                    libResultat.setBackgroundColor(BaseColor.YELLOW);
                    libResultat.setPadding(5);
                    pdfTable.addCell(libResultat);

                    PdfPCell valResultat = new PdfPCell(new Phrase(String.format("%.2f", b.getResultatNet()), boldFont));
                    valResultat.setBackgroundColor(BaseColor.YELLOW);
                    valResultat.setPadding(5);
                    pdfTable.addCell(valResultat);

                    document.add(pdfTable);
                    document.add(new Paragraph("\n\n"));
                }

                document.close();

                afficherMessageSucces("Le PDF a été exporté avec succès !");
            } catch (Exception e) {
                afficherMessageErreur("Erreur lors de l'exportation PDF : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML private void envoyerBilanEmail() {
        if (tableBilans.getItems().isEmpty()) {
            afficherMessageErreur("Aucune donnée à envoyer.");
            return;
        }

        List<bilan> selectedItems = tableBilans.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            selectedItems = tableBilans.getItems(); // If none selected, send all
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Envoyer Bilan par Email");
        dialog.setHeaderText("Envoi du bilan sous forme graphique (QuickChart.io)");
        dialog.setContentText("Saisissez l'adresse email destinataire :");

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            final String recipient = result.get().trim();
            // Créer une copie persistante pour éviter les accès de Thread non-JavaFX
            final List<bilan> dataToSend = new java.util.ArrayList<>(selectedItems);
            final String franchiseNomBase = cbFranchiseCible.getSelectionModel().getSelectedItem() != null ? cbFranchiseCible.getSelectionModel().getSelectedItem().getNom() : "Reseau";
            
            if (btnEnvoyerEmail != null) btnEnvoyerEmail.setDisable(true);

            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // Construction de l'Email HTML (Style Boussole)
                    StringBuilder htmlBuilder = new StringBuilder();
                    htmlBuilder.append("<html><body style='background-color: #000000; padding: 20px; font-family: sans-serif;'>");

                    for (bilan b : dataToSend) {
                        String periodeStr = b.getMois() + " / " + b.getAnnee();
                        String resultatColor = b.getResultatNet() >= 0 ? "#10B981" : "#EF4444"; // Vert ou Rouge
                        
                        // Graphique QuickChart.io (Doughnut)
                        String chartConfig = "{type:'doughnut',data:{labels:['Recettes','Charges'],datasets:[{data:[" + 
                                            b.getTotalRecettes() + "," + b.getTotalCharges() + 
                                            "],backgroundColor:['%2310B981','%23EF4444'],borderWidth:0}]}," +
                                            "options:{plugins:{legend:{display:false}},cutout:'65%'}}";
                        String chartUrl = "https://quickchart.io/chart?c=" + chartConfig + "&w=300&h=300&bkg=transparent";

                        // Card Conteneur
                        htmlBuilder.append("<div style='background-color: #0F172A; max-width: 450px; margin: 0 auto; border-radius: 12px; overflow: hidden; margin-bottom: 20px; border: 1px solid #1E293B;'>");
                        
                        // En-tête Bleu Foncé
                        htmlBuilder.append("<div style='background-color: #1E293B; padding: 20px; text-align: center;'>");
                        htmlBuilder.append("<h2 style='margin: 0; color: #FFFFFF; font-size: 22px;'>Boussole - Bilan Périodique</h2>");
                        htmlBuilder.append("<p style='margin: 8px 0 0 0; color: #94A3B8; font-size: 14px;'>Période : ").append(periodeStr).append("</p>");
                        htmlBuilder.append("</div>");
                        
                        // Corps du message
                        htmlBuilder.append("<div style='padding: 25px;'>");
                        htmlBuilder.append("<p style='color: #E2E8F0; font-weight: bold; font-size: 16px; margin-top: 0;'>Bonjour le gérant de ").append(franchiseNomBase).append(",</p>");
                        htmlBuilder.append("<p style='color: #94A3B8; line-height: 1.6; font-size: 14px;'>Le siège a généré (ou mis à jour) votre bilan mensuel. Voici un résumé des opérations :</p>");
                        
                        // Tableau des stats
                        htmlBuilder.append("<table style='width: 100%; border-collapse: collapse; margin-top: 25px;'>");
                        
                        // Recettes
                        htmlBuilder.append("<tr style='border-bottom: 1px solid #334155;'>");
                        htmlBuilder.append("<td style='padding: 15px 0; color: #E2E8F0; font-weight: bold;'>Total<br>Recettes :</td>");
                        htmlBuilder.append("<td style='padding: 15px 0; text-align: right; color: #10B981; font-weight: bold;'>").append(String.format("%.2f", b.getTotalRecettes())).append(" TND</td>");
                        htmlBuilder.append("</tr>");
                        
                        // Charges
                        htmlBuilder.append("<tr style='border-bottom: 1px solid #334155;'>");
                        htmlBuilder.append("<td style='padding: 15px 0; color: #E2E8F0; font-weight: bold;'>Total<br>Charges :</td>");
                        htmlBuilder.append("<td style='padding: 15px 0; text-align: right; color: #EF4444; font-weight: bold;'>").append(String.format("%.2f", b.getTotalCharges())).append(" TND</td>");
                        htmlBuilder.append("</tr>");
                        
                        // Résultat
                        htmlBuilder.append("<tr>");
                        htmlBuilder.append("<td style='padding: 18px 0 5px 0; color: #F8FAFC; font-size: 18px; font-weight: bold;'>Résultat<br>Net :</td>");
                        htmlBuilder.append("<td style='padding: 18px 0 5px 0; text-align: right; color: ").append(resultatColor).append("; font-size: 18px; font-weight: bold;'>").append(String.format("%.2f", b.getResultatNet())).append(" TND</td>");
                        htmlBuilder.append("</tr>");
                        htmlBuilder.append("</table>");
                        
                        // Graphique
                        htmlBuilder.append("<div style='text-align: center; margin-top: 40px;'>");
                        htmlBuilder.append("<p style='color: #94A3B8; margin-bottom: 15px; font-size: 14px;'>Aperçu Graphique</p>");
                        
                        // Légende personnalisée (puisqu'on la cache dans QuickChart pour avoir un look épuré)
                        htmlBuilder.append("<div style='display: flex; justify-content: center; gap: 15px; margin-bottom: 10px; font-size: 12px; color: #94A3B8;'>");
                        htmlBuilder.append("<span><span style='color: #10B981;'>■</span> Recettes</span>");
                        htmlBuilder.append("<span><span style='color: #EF4444;'>■</span> Charges</span>");
                        htmlBuilder.append("</div>");

                        htmlBuilder.append("<img src=\"").append(chartUrl).append("\" width=\"180\" height=\"180\" style='display: block; margin: 0 auto;'>");
                        htmlBuilder.append("</div>");
                        
                        htmlBuilder.append("</div></div>");
                    }
                    htmlBuilder.append("</body></html>");

                    // Envoi JavaMail
                    final String senderEmail = "siwar.raouafi1@gmail.com"; 
                    final String senderAppPassword = "evcnhbhnswgaqhkz";

                    Properties props = new Properties();
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.host", "smtp.gmail.com");
                    props.put("mail.smtp.port", "587");

                    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(senderEmail, senderAppPassword);
                        }
                    });

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail, "Boussole Reporting"));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                    message.setSubject("Boussole - Bilan Périodique (" + franchiseNomBase + ")");
                    message.setContent(htmlBuilder.toString(), "text/html; charset=utf-8");

                    Transport.send(message);
                    
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                if (btnEnvoyerEmail != null) btnEnvoyerEmail.setDisable(false);
                afficherMessageSucces("L'email a été envoyé avec succès à " + recipient);
            });

            task.setOnFailed(e -> {
                if (btnEnvoyerEmail != null) btnEnvoyerEmail.setDisable(false);
                Throwable exception = task.getException();
                if (exception instanceof AuthenticationFailedException) {
                    afficherMessageErreur("Echec d'envoi: Les identifiants (Mot de passe d'application Google) sont incorrects dans le code source.");
                } else {
                    afficherMessageErreur("Erreur d'envoi d'email : " + exception.getMessage());
                }
            });

            new Thread(task).start();
        }
    }
}