package tn.esprit.boussole;

import javafx.application.Application;
import javafx.stage.Stage;
import tn.esprit.boussole.gui.MainGUI;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) {
        MainGUI mainGUI = new MainGUI();
        mainGUI.start(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}