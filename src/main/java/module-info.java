module tn.esprit.boussole {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens tn.esprit.boussole to javafx.fxml;
    exports tn.esprit.boussole;
}