package tn.esprit.chargesdepenses.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainChargesdepenses extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Chargement de la page initiale (vous pouvez changer pour afficherBackCharge.fxml si vous préférez)
        Parent root = FXMLLoader.load(getClass().getResource("/ajouterCharge.fxml"));
        
        Scene scene = new Scene(root);
        
        // Application du style global dès le démarrage
        String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        primaryStage.setTitle("Gestion Charges & Dépenses");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
