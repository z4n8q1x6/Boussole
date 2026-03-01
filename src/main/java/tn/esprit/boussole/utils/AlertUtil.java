package tn.esprit.boussole.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public class AlertUtil {

  public static ButtonType showConfirmation(String header, String content) {
    Alert alert = new Alert(AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
    alert.setHeaderText(header);
    DialogPane dialogPane = alert.getDialogPane();
    dialogPane
        .getStylesheets()
        .add(
            AlertUtil.class
                .getResource("/alert.css")
                .toExternalForm());
    dialogPane.getStyleClass().add("custom-alert");
    alert.showAndWait();
    return alert.getResult();
  }

  public static void showWarning(String header, String content) {
    Alert alert = new Alert(AlertType.WARNING, content);
    alert.setHeaderText(header);
    DialogPane dialogPane = alert.getDialogPane();
    dialogPane
        .getStylesheets()
        .add(
            AlertUtil.class
                .getResource("/alert.css")
                .toExternalForm());
    dialogPane.getStyleClass().add("custom-alert");
    alert.showAndWait();
  }

  public static void showError(String header, String content) {
    Alert alert = new Alert(AlertType.ERROR, content);
    alert.setHeaderText(header);
    DialogPane dialogPane = alert.getDialogPane();
    dialogPane
        .getStylesheets()
        .add(
            AlertUtil.class
                .getResource("/alert.css")
                .toExternalForm());
    dialogPane.getStyleClass().add("custom-alert");
    alert.showAndWait();
  }

  public static void showInformation(String header, String content) {
    Alert alert = new Alert(AlertType.INFORMATION, content);
    alert.setHeaderText(header);
    DialogPane dialogPane = alert.getDialogPane();
    dialogPane
        .getStylesheets()
        .add(
            AlertUtil.class
                .getResource("/alert.css")
                .toExternalForm());
    dialogPane.getStyleClass().add("custom-alert");
    alert.showAndWait();
  }
}
