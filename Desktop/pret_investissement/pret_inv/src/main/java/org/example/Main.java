package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Chargement de la liste par défaut
        Parent root = FXMLLoader.load(getClass().getResource("/view/ListePrets.fxml"));
        Scene scene = new Scene(root, 1100, 700);

        primaryStage.setTitle("BOUSSOLE - Gestion des Prêts");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}