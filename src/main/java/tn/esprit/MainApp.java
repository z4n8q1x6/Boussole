package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.boussole.service.userService;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialiser l'admin si nécessaire
        userService userService = new userService();
        userService.initializeAdmin();

        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
        primaryStage.setTitle("Boussole - Login");
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Plein écran maximisé
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
