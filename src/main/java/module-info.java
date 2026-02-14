module tn.esprit.boussole {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens tn.esprit.boussole.models to javafx.base;
    opens tn.esprit.boussole.gui to javafx.fxml;
    opens tn.esprit.boussole.controllers to javafx.fxml; // Maintenant il existe

    exports tn.esprit.boussole;
    exports tn.esprit.boussole.models;
    exports tn.esprit.boussole.services;
    exports tn.esprit.boussole.gui;
    exports tn.esprit.boussole.utils;
    exports tn.esprit.boussole.controllers; // Maintenant il existe
}