package tn.esprit.boussole.gui;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import tn.esprit.boussole.models.AlertReport;
import tn.esprit.boussole.models.AlerteIA;
import tn.esprit.boussole.service.AlerteIAService;
import tn.esprit.boussole.service.AlertReportService;
import tn.esprit.boussole.service.franchiseService;
import tn.esprit.boussole.utils.AlertUtil;
import tn.esprit.boussole.utils.CloudUploader;
import tn.esprit.boussole.utils.Gemini;
import tn.esprit.boussole.utils.PDFGenerator;

public class AdminAlerteIAController {
  AlerteIAService service = new AlerteIAService();
  @FXML private TableView<AlerteIA> table;

  @FXML private TableColumn<AlerteIA, String> colType;

  @FXML private TableColumn<AlerteIA, Float> colScore;

  @FXML private TableColumn<AlerteIA, java.util.Date> colDate;

  @FXML private TableColumn<AlerteIA, String> colFranchise;

  @FXML private TextArea messageArea;
  @FXML private Button pdfButton;

  @FXML private TextField searchTypeField;

  public void initialize() {
    colType.setCellValueFactory(new PropertyValueFactory<>("type_alerte"));
    colScore.setCellValueFactory(new PropertyValueFactory<>("score_gravite"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("date_detection"));
    colFranchise.setCellValueFactory(
        cellData -> {
          int id = cellData.getValue().getFranchiseId();
          String nomFranchise;
          try {
            // Initialize service and fetch the name
            franchiseService fs = new franchiseService();
            nomFranchise = fs.getNomById(id);
          } catch (SQLException e) {
            e.printStackTrace();
            nomFranchise = "Error ID: " + id;
          }
          return new SimpleStringProperty(nomFranchise);
        });

    // When a user clicks a row, update the "Details" TextArea on the right
    table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSelection, newSelection) -> {
              if (newSelection != null) {
                messageArea.setText(newSelection.getMessage());
              } else {
                messageArea.setText("Veuillez sélectionner une alerte pour voir les détails...");
              }
            });

    setupSearchFilter();
  }

  private void setupSearchFilter() {
    // Get the initial data
    ObservableList<AlerteIA> fullList = service.getAll();

    // Wrap in FilteredList
    FilteredList<AlerteIA> filteredList = new FilteredList<>(fullList, p -> true);

    // Wrap in SortedList to maintain sorting
    SortedList<AlerteIA> sortedList = new SortedList<>(filteredList);
    sortedList.comparatorProperty().bind(table.comparatorProperty());

    // Bind filtered list to table
    table.setItems(sortedList);

    // Add listener to search TextField
    searchTypeField
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              filteredList.setPredicate(
                  alerte -> {
                    // If search field is empty, show all
                    if (newValue == null || newValue.isEmpty()) {
                      return true;
                    }

                    // Get the type_alerte and convert to lowercase for case-insensitive search
                    String typeAlerte = alerte.getType_alerte();
                    if (typeAlerte == null) {
                      return false;
                    }

                    // Check if type_alerte contains the search text (case-insensitive)
                    return typeAlerte.toLowerCase().contains(newValue.toLowerCase());
                  });
            });
  }

  @FXML
  public void delete() {
    AlerteIA selected = table.getSelectionModel().getSelectedItem();

    if (selected != null) {
      ButtonType result = AlertUtil.showConfirmation("Confirmation", "Supprimer cette alerte?");

      if (result == ButtonType.YES) {
        if (service.delete(selected.getId())) {
          setupSearchFilter();
          System.out.println("Alerte deleted successfully.");
        }
      }
    } else {
      AlertUtil.showWarning("Aucune sélection", "Veuillez sélectionner une alerte à supprimer.");
    }
  }

  @FXML
  public void pdf() {
    Task<Void> pdfTask = new Task<Void>() {
      @Override
      protected Void call() {
        try {
          File pdfFile = PDFGenerator.generateAlertePDFToTemp();
          if (pdfFile == null || !pdfFile.exists()) {
            Platform.runLater(() -> AlertUtil.showError("PDF Generation Failed", "Could not generate PDF file."));
            return null;
          }

          Platform.runLater(() -> updateMessage("Uploading to cloud..."));
          String cloudUrl = CloudUploader.uploadToCloudinary(pdfFile);

          if (cloudUrl == null || cloudUrl.isEmpty()) {
            Platform.runLater(() -> AlertUtil.showError("Upload Failed", "Could not upload PDF to Cloudinary."));
            pdfFile.delete();
            return null;
          }

          AlerteIAService alertService = new AlerteIAService();
          int alertCount = alertService.getAll().size();

          AlertReport report = new AlertReport(cloudUrl, LocalDateTime.now(), alertCount);
          AlertReportService reportService = new AlertReportService();
          boolean saved = reportService.add(report);

          pdfFile.delete();

          if (saved) {
            Platform.runLater(() -> {
              AlertUtil.showInformation("Success", "PDF generated, uploaded, and archived successfully!");
              System.out.println("PDF generated and uploaded: " + cloudUrl);
            });
          } else {
            Platform.runLater(() -> AlertUtil.showError("Database Error", "PDF uploaded but failed to save metadata."));
          }
        } catch (Exception e) {
          Platform.runLater(() -> AlertUtil.showError("Error", "PDF generation failed: " + e.getMessage()));
          e.printStackTrace();
        }
        return null;
      }
    };

    new Thread(pdfTask).start();
  }

  @FXML
  public void viewArchives() {
    try {
      Stage archivesStage = new Stage();
      archivesStage.setTitle("Archives des Rapports");
      
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/alertReports.fxml"));
      Scene scene = new Scene(loader.load(), 1000, 600);
      archivesStage.setScene(scene);
      archivesStage.show();
    } catch (Exception e) {
      AlertUtil.showError("Error", "Could not open archives: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  public void escalationAdvisor() {
    ObservableList<AlerteIA> all = service.getAll();

    if (all.isEmpty()) {
      AlertUtil.showInformation("Aucune donnée", "Aucune alerte à analyser.");
      return;
    }

    StringBuilder alertSummary = new StringBuilder();
    alertSummary.append("Voici toutes les alertes actuelles pour toutes les franchises :\n\n");

    for (AlerteIA alert : all) {
      String franchise_nom = "Unknown";
      try {
        franchiseService fs = new franchiseService();
        franchise_nom = fs.getNomById(alert.getFranchiseId());
      } catch (SQLException e) {
        e.printStackTrace();
      }
      alertSummary.append(
          String.format(
              "- Franchise %s | Type: %s | Severity: %.1f/10\n  Message: %s\n\n",
              franchise_nom, alert.getType_alerte(), alert.getScore_gravite(), alert.getMessage()));
    }

    String prompt =
        String.format(
            """
            %s

            Vous êtes un conseiller expert en gestion de crise. Analysez ces alertes et identifiez les 3 plus critiques.

            RÈGLES STRICTES :
            - Sélectionnez UNIQUEMENT les 3 alertes les plus urgentes
            - Basez-vous sur le score de gravité ET le type de risque
            - Soyez ultra-concis, chaque action max 1 ligne
            - Aucun texte supplémentaire en dehors du format demandé

            FORMAT OBLIGATOIRE (respectez exactement) :

            🚨 TOP 3 ALERTES CRITIQUES
            ━━━━━━━━━━━━━━━━━━━━━━━━

            🔴 #1 — [TYPE ALERTE] (Gravité [X]/10)
            ├─ Franchise : [nom]
            └─ Action : [action immédiate et précise]

            🟡 #2 — [TYPE ALERTE] (Gravité [X]/10)
            ├─ Franchise : [nom]
            └─ Action : [action cette semaine]

            🟢 #3 — [TYPE ALERTE] (Gravité [X]/10)
            ├─ Franchise : [nom]
            └─ Action : [action ce mois]

            ━━━━━━━━━━━━━━━━━━━━━━━━
            """,
            alertSummary.toString());

    Optional<String> result = Gemini.generateAdvice(prompt);

    if (result.isPresent()) {
      AlertUtil.showInformation("Rapport d'Escalade", result.get());
    } else {
      AlertUtil.showError("Erreur", "Impossible de générer le rapport d'escalade.");
    }
  }
}
