package tn.esprit.boussole.gui;

import java.util.HashMap;
import java.util.Map;
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
  public void franchiseHealthScore() {
    ObservableList<AlerteIA> all = service.getAll();

    if (all.isEmpty()) {
      AlertUtil.showInformation("No data", "No alerts to analyze");
      return;
    }

    // Group alerts by franchiseId
    Map<Integer, java.util.List<AlerteIA>> alertsByFranchise = new HashMap<>();
    for (AlerteIA alert : all) {
      alertsByFranchise
          .computeIfAbsent(alert.getFranchiseId(), k -> new java.util.ArrayList<>())
          .add(alert);
    }

    // Build summary for Gemini
    StringBuilder franchiseSummary = new StringBuilder();
    franchiseSummary.append("Analyze the health of each franchise based on their alerts:\n\n");

    for (Map.Entry<Integer, java.util.List<AlerteIA>> entry : alertsByFranchise.entrySet()) {
      int franchiseId = entry.getKey();
      java.util.List<AlerteIA> alerts = entry.getValue();

      franchiseSummary.append(String.format("FRANCHISE #%d:\n", franchiseId));
      franchiseSummary.append(String.format("  Total alerts: %d\n", alerts.size()));

      // Count critical alerts
      long criticalCount = alerts.stream().filter(a -> a.getScore_gravite() > 8).count();
      franchiseSummary.append(String.format("  Critical alerts (score > 8): %d\n", criticalCount));

      // Average severity
      double avgSeverity =
          alerts.stream().mapToDouble(AlerteIA::getScore_gravite).average().orElse(0);
      franchiseSummary.append(String.format("  Average severity: %.1f/10\n", avgSeverity));

      // Alert types
      franchiseSummary.append("  Alert types: ");
      alerts.stream()
          .map(AlerteIA::getType_alerte)
          .distinct()
          .forEach(type -> franchiseSummary.append(type).append(", "));
      franchiseSummary.append("\n\n");
    }

    String prompt =
        String.format(
            """
            %s

            For each franchise, provide:
            1. Health Score (0-10 scale, 10 is perfect)
            2. Status (Critical/At Risk/Healthy)
            3. Top 2-3 issues
            4. Recommended immediate actions

            Format as a professional scorecard for management review.
            """,
            franchiseSummary.toString());

    Optional<String> healthScores = Gemini.generateAdvice(prompt);

    if (healthScores.isPresent()) {
      AlertUtil.showInformation("Franchise Health Scorecard", healthScores.get());
    } else {
      AlertUtil.showError("Error", "Could not analyze");
    }
  }
}
