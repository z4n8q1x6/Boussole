package tn.esprit.boussole.gui;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.boussole.models.AlertReport;
import tn.esprit.boussole.service.AlertReportService;
import tn.esprit.boussole.utils.AlertUtil;

public class AlertReportsController {

  @FXML private TableView<AlertReport> reportsTable;
  @FXML private TableColumn<AlertReport, String> dateColumn;
  @FXML private TableColumn<AlertReport, Integer> alertCountColumn;
  @FXML private TableColumn<AlertReport, Void> actionsColumn;

  private AlertReportService reportService;

  @FXML
  public void initialize() {
    reportService = new AlertReportService();

    dateColumn.setCellValueFactory(
        cellData -> {
          AlertReport report = cellData.getValue();
          String dateStr =
              report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
          return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

    alertCountColumn.setCellValueFactory(new PropertyValueFactory<>("alertCount"));

    setupActionsColumn();
    loadReports();
  }

  private void setupActionsColumn() {
    actionsColumn.setCellFactory(
        col ->
            new TableCell<AlertReport, Void>() {
              private final Button viewBtn = new Button("View");
              private final Button downloadBtn = new Button("Download");
              private final Button deleteBtn = new Button("Delete");

              {
                viewBtn.setStyle(
                    "-fx-padding: 8 16; "
                        + "-fx-background-color: rgba(59,130,246,0.2); "
                        + "-fx-text-fill: #60A5FA; "
                        + "-fx-border-color: rgba(59,130,246,0.4); "
                        + "-fx-border-radius: 4; "
                        + "-fx-cursor: hand; "
                        + "-fx-font-weight: bold; "
                        + "-fx-font-size: 11;");

                downloadBtn.setStyle(
                    "-fx-padding: 8 16; "
                        + "-fx-background-color: rgba(34,197,94,0.2); "
                        + "-fx-text-fill: #22C55E; "
                        + "-fx-border-color: rgba(34,197,94,0.4); "
                        + "-fx-border-radius: 4; "
                        + "-fx-cursor: hand; "
                        + "-fx-font-weight: bold; "
                        + "-fx-font-size: 11; "
                        + "-fx-margin: 0 0 0 8;");

                deleteBtn.setStyle(
                    "-fx-padding: 8 16; "
                        + "-fx-background-color: rgba(239,68,68,0.2); "
                        + "-fx-text-fill: #EF4444; "
                        + "-fx-border-color: rgba(239,68,68,0.4); "
                        + "-fx-border-radius: 4; "
                        + "-fx-cursor: hand; "
                        + "-fx-font-weight: bold; "
                        + "-fx-font-size: 11; "
                        + "-fx-margin: 0 0 0 8;");

                viewBtn.setOnAction(
                    e -> {
                      AlertReport report = getTableView().getItems().get(getIndex());
                      viewPdf(report);
                    });

                downloadBtn.setOnAction(
                    e -> {
                      AlertReport report = getTableView().getItems().get(getIndex());
                      downloadPdf(report);
                    });

                deleteBtn.setOnAction(
                    e -> {
                      AlertReport report = getTableView().getItems().get(getIndex());
                      deletePdf(report);
                    });
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                  setGraphic(null);
                } else {
                  HBox hbox = new HBox(8);
                  hbox.setAlignment(Pos.CENTER);
                  hbox.getChildren().addAll(viewBtn, downloadBtn, deleteBtn);
                  setGraphic(hbox);
                }
              }
            });
  }

  private void loadReports() {
    reportsTable.setItems(reportService.getAll());
  }

  private void viewPdf(AlertReport report) {
    try {
      Stage viewerStage = new Stage();
      viewerStage.setTitle(
          "PDF Viewer - "
              + report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

      WebView webView = new WebView();
      String googleViewerUrl =
          "https://docs.google.com/viewer?url="
              + java.net.URLEncoder.encode(report.getUrl(), "UTF-8")
              + "&embedded=true";
      webView.getEngine().load(googleViewerUrl);

      Scene scene = new Scene(webView, 1200, 800);
      viewerStage.setScene(scene);
      viewerStage.show();
    } catch (Exception e) {
      AlertUtil.showError("Viewer Error", "Could not open PDF viewer: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void downloadPdf(AlertReport report) {
    Task<Void> downloadTask =
        new Task<Void>() {
          // runs on bg
          @Override
          protected Void call() throws Exception {
            try {
              Stage parentStage = (Stage) reportsTable.getScene().getWindow();
              FileChooser fileChooser = new FileChooser();
              fileChooser.setTitle("Save PDF Report");
              fileChooser.setInitialFileName(
                  "Rapport_"
                      + report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                      + ".pdf");
              fileChooser
                  .getExtensionFilters()
                  .add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

              // runs after the call function is over
              Platform.runLater(
                  () -> {
                    File selectedFile = fileChooser.showSaveDialog(parentStage);
                    if (selectedFile != null) {
                      downloadToFile(report.getUrl(), selectedFile);
                    }
                  });
            } catch (Exception e) {
              Platform.runLater(
                  () ->
                      AlertUtil.showError(
                          "Download Error", "Error initiating download: " + e.getMessage()));
              e.printStackTrace();
            }
            return null;
          }
        };

    new Thread(downloadTask).start(); // task need to be wrap into a thread to start
  }

  private void downloadToFile(String pdfUrl, File destinationFile) {
    Task<Void> fileDownloadTask =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            try {
              URL url = new URL(pdfUrl);
              Files.copy(url.openStream(), Paths.get(destinationFile.getAbsolutePath()));
              Platform.runLater(
                  () ->
                      AlertUtil.showInformation(
                          "Download Complete",
                          "PDF saved to: " + destinationFile.getAbsolutePath()));
            } catch (Exception e) {
              Platform.runLater(
                  () ->
                      AlertUtil.showError(
                          "Download Failed", "Error downloading file: " + e.getMessage()));
              e.printStackTrace();
            }
            return null;
          }
        };

    new Thread(fileDownloadTask).start();
  }

  private void deletePdf(AlertReport report) {
    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
    confirmDialog
        .getDialogPane()
        .getStylesheets()
        .add(getClass().getResource("/alert.css").toExternalForm());
    confirmDialog.setTitle("Delete Report");
    confirmDialog.setHeaderText("Delete Alert Report?");
    confirmDialog.setContentText(
        "Are you sure you want to delete this report generated on "
            + report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            + "?\n\nThis will remove the PDF from the cloud and the database.");

    if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      Task<Boolean> deleteTask =
          new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
              try {
                boolean cloudDeleteSuccess =
                    tn.esprit.boussole.utils.CloudUploader.deleteFromCloudinary(report.getUrl());

                if (!cloudDeleteSuccess) {
                  Platform.runLater(
                      () ->
                          AlertUtil.showError(
                              "Delete Failed",
                              "Failed to delete PDF from cloud. Database not modified."));
                  return false;
                }

                reportService.delete(report.getId());

                Platform.runLater(
                    () -> {
                      reportsTable.getItems().remove(report);
                      AlertUtil.showInformation(
                          "Success", "Report deleted successfully from cloud and database.");
                    });

                return true;
              } catch (Exception e) {
                Platform.runLater(
                    () ->
                        AlertUtil.showError(
                            "Delete Error", "Error deleting report: " + e.getMessage()));
                e.printStackTrace();
                return false;
              }
            }
          };

      new Thread(deleteTask).start();
    }
  }

  @FXML
  public void goBack() {
    Stage stage = (Stage) reportsTable.getScene().getWindow();
    stage.close();
  }
}
