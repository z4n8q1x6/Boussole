package tn.esprit.boussole.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Test extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Par défaut, on lance la vue principale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/MainView.fxml"));
        Scene scene = new Scene(loader.load());

        // Pour tester directement une vue spécifique (décommentez celle que vous voulez)
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/ProduitView.fxml"));
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/CommandeView.fxml"));
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/LigneCommandeView.fxml"));

        stage.setTitle("Boussole - Gestion Commerciale");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}