package tn.esprit.boussole.org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Vérifiez que ce chemin correspond à l'emplacement de votre fichier FXML
        Parent root = FXMLLoader.load(getClass().getResource("/view/DemandePret.fxml"));
        primaryStage.setTitle("Boussole - Gestion de Prêts");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}