package tn.esprit.boussole.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class AlertUtil {

  public static ButtonType showConfirmation(String header, String content) {
    Alert alert = new Alert(AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
    alert.setHeaderText(header);
    alert.showAndWait();
    return alert.getResult();
  }

  public static void showWarning(String header, String content) {
    Alert alert = new Alert(AlertType.WARNING, content);
    alert.setHeaderText(header);
  }

  public static void showError(String header, String content) {
    Alert alert = new Alert(AlertType.ERROR, content);
    alert.setHeaderText(header);
  }

  public static void showInformation(String header, String content) {
    Alert alert = new Alert(AlertType.INFORMATION, content);
    alert.setHeaderText(header);
  }
}
