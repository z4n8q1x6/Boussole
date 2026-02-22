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

            Vous êtes un conseiller expert en gestion. Sur la base des alertes ci-dessus, générez un RAPPORT D'ESCALADE URGENT.

            Pour chaque alerte, assignez :
            1. Niveau de priorité : IMMÉDIAT (24h) / URGENT (cette semaine) / À SURVEILLER (ce mois)
            2. Responsable : PDG / DAF / Directeur des Opérations / Responsable Financier / Responsable IT
            3. Action spécifique à entreprendre

            Formatez votre réponse exactement comme ceci pour chaque alerte :

            🔴 ACTION IMMÉDIATE REQUISE / 🟡 URGENT / 🟢 À SURVEILLER
            ├─ Alerte : "[type]" (Gravité X/10)
            ├─ Franchise : #[id]
            ├─ Responsable : [rôle]
            └─ Action : [action spécifique]

            Terminez avec une section résumé :
            RÉSUMÉ
            - X problèmes nécessitent une attention immédiate aujourd'hui
            - X problèmes nécessitent une attention cette semaine
            - X problèmes peuvent être planifiés pour une révision mensuelle

            Soyez concis et orienté action. Format prêt pour la direction uniquement.
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
