package tn.esprit.boussole;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.boussole.utils.Database;

public class App extends Application {

  @Override
  public void start(Stage stage) {
    String[] views = {
      "/tn/esprit/boussole/support/reclamation.fxml",
      "/tn/esprit/boussole/support/adminReclamation.fxml",
      "/tn/esprit/boussole/support/alerteIA.fxml",
      "/tn/esprit/boussole/support/adminAlerteIA.fxml",
    };
    FXMLLoader loader = new FXMLLoader(getClass().getResource(views[0]));
    Parent root = null;
    try {

      root = loader.load();
    } catch (IOException e) {
      System.err.println("Error loading FXML:");
      e.printStackTrace();
      return;
    }

    Scene scene = new Scene(root, 1236, 676);
    stage.setTitle("Boussole");
    stage.setScene(scene);
    stage.show();
  }

  @Override
  public void stop() {
    Database.getInstance().closeConnection();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
