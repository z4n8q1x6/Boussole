package tn.esprit.boussole.gui;

import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.boussole.models.AlerteIA;
import tn.esprit.boussole.service.AlerteIAService;
import tn.esprit.boussole.utils.AlertUtil;
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

  public void initialize() {
    colType.setCellValueFactory(new PropertyValueFactory<>("type_alerte"));
    colScore.setCellValueFactory(new PropertyValueFactory<>("score_gravite"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("date_detection"));
    colFranchise.setCellValueFactory(
        cellData -> {
          return new SimpleStringProperty("nom/ville#" + cellData.getValue().getFranchiseId());
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

    display();
  }

  @FXML
  public void delete() {
    AlerteIA selected = table.getSelectionModel().getSelectedItem();

    if (selected != null) {
      ButtonType result = AlertUtil.showConfirmation("Confirmation", "Supprimer cette alerte?");

      if (result == ButtonType.YES) {
        if (service.delete(selected.getId())) {
          display();
          System.out.println("Alerte deleted successfully.");
        }
      }
    } else {
      AlertUtil.showWarning("Aucune sélection", "Veuillez sélectionner une alerte à supprimer.");
    }
  }

  void display() {
    table.setItems(service.getAll());
  }

  @FXML
  public void pdf() {
    Stage stage = (Stage) pdfButton.getScene().getWindow();
    String result = PDFGenerator.generateAlertePDF(stage);
    if (result.equals("generated")) {
      AlertUtil.showInformation("Export Success", "PDF saved successfully!");
      System.out.println("PDF generated.");
    } else if (!result.isEmpty()) {
      AlertUtil.showError("Export Failed", "Error: " + result);
      System.out.println("PDF NOT generated.");
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
      alertSummary.append(
          String.format(
              "- Franchise #%d | Type: %s | Severity: %.1f/10\n  Message: %s\n\n",
              alert.getFranchiseId(),
              alert.getType_alerte(),
              alert.getScore_gravite(),
              alert.getMessage()));
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
            ├─ Franchise : #[id]
            └─ Action : [action immédiate et précise]

            🟡 #2 — [TYPE ALERTE] (Gravité [X]/10)
            ├─ Franchise : #[id]
            └─ Action : [action cette semaine]

            🟢 #3 — [TYPE ALERTE] (Gravité [X]/10)
            ├─ Franchise : #[id]
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
