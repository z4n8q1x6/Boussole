package tn.esprit.boussole.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Test extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Fenêtre plus grande : 1300x800
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 1300, 800);

        stage.setTitle("Boussole - Gestion Commerciale");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}   