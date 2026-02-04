package com.boussole.app;

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
      "/com/boussole/app/support/reclamations.fxml",
      "/com/boussole/app/support/alerteIAs.fxml",
      "/com/boussole/app/support/adminReclamations.fxml"
    };
    FXMLLoader loader = new FXMLLoader(getClass().getResource(views[1]));
    Parent root = null;
    try {
      root = loader.load();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    Scene scene = new Scene(root, 1236, 676);
    stage.setTitle("Boussole");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
