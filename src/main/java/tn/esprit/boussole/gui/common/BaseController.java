package tn.esprit.boussole.gui.common;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public abstract class BaseController {

    // Ces champs peuvent être null si le FXML ne les contient pas
    @FXML protected Label lblPageTitle;
    @FXML protected Label lblUsername;
    @FXML protected StackPane contentArea;

    protected String userNom = "Utilisateur";
    protected String userType = "Franchise"; // "Franchise" ou "Siege"

    @FXML
    public void initialize() {
        // Vérifier que les composants existent avant de les utiliser
        if (lblUsername != null) {
            lblUsername.setText(userNom);
        }
        chargerPremiereVue();
    }

    protected abstract void chargerPremiereVue();

    protected void chargerVue(String fxmlPath, String titre) {
        try {
            Parent vue = FXMLLoader.load(getClass().getResource(fxmlPath));
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(vue);
            }
            if (lblPageTitle != null) {
                lblPageTitle.setText(titre);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur chargement vue: " + fxmlPath);
        }
    }
}