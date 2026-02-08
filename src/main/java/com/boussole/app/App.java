package com.boussole.app;

import com.boussole.app.utils.Database;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

  @Override
  public void start(Stage stage) {
    String[] views = {
      "/com/boussole/app/support/reclamation.fxml",
      "/com/boussole/app/support/alerteIA.fxml",
      "/com/boussole/app/support/adminReclamation.fxml",
      "/com/boussole/app/support/adminAlerteIA.fxml"
    };
    FXMLLoader loader = new FXMLLoader(getClass().getResource(views[1]));
    Parent root = null;
    try {

      root = loader.load();
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
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
