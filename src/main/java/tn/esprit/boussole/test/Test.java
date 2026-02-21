package tn.esprit.boussole.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.boussole.gui.common.NavbarController;

public class Test extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Choisir le type d'utilisateur : "Siege" ou "Franchise"
        String userType = "Siege"; // Changez ici pour tester

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/boussole/views/common/NavBarView.fxml"));
        Scene scene = new Scene(loader.load(), 1400, 800);

        NavbarController controller = loader.getController();
        controller.setUserType(userType);

        stage.setTitle("Boussole - " + (userType.equals("Siege") ? "Siège" : "Franchise"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}