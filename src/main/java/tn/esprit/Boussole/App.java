package tn.esprit.Boussole;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.Boussole.Utilis.SessionManager;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Chargement de l'écran de Login
            URL fxmlUrl = getClass().getResource("/tn/esprit/Boussole/GUI/Login.fxml");

            if (fxmlUrl == null) {
                System.err.println("!!! ERREUR CRITIQUE !!!");
                System.err.println("Le fichier Login.fxml est introuvable.");
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);

            // Charger la feuille CSS si elle existe
            URL cssUrl = getClass().getResource("/tn/esprit/Boussole/GUI/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setScene(scene);
            primaryStage.setTitle("Boussole - Connexion");
            primaryStage.show();

        } catch (Exception e) {
            System.err.println(">>> EXCEPTION AU DEMARRAGE : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Diagnostic: vérifier si la classe JavaFX Application est accessible
        try {
            Class.forName("javafx.application.Application");
        } catch (ClassNotFoundException ex) {
            System.err.println("ERREUR : Les composants d'exécution JavaFX sont manquants pour exécuter cette application.");
            System.err.println();
            System.err.println("Options pour corriger le problème :");
            System.err.println("1) Lancer via Maven (recommandé si le projet est Maven) : mvn javafx:run");
            System.err.println("   - IntelliJ > Maven > plugins > javafx > javafx:run");
            System.err.println("2) Ajouter les VM options dans la configuration Run d'IntelliJ (si tu utilises le JavaFX SDK local) :");
            System.err.println("   --module-path \"C:\\chemin\\vers\\javafx-sdk-17.0.8\\lib\" --add-modules javafx.controls,javafx.fxml");
            System.err.println("   (Remplace le chemin par le dossier lib du SDK JavaFX que tu as téléchargé)");
            System.err.println("3) Vérifier que Project SDK est Java 11+ (File > Project Structure > Project SDK)");
            System.err.println();
            System.err.println("Java runtime détecté : " + System.getProperty("java.version"));
            System.exit(1);
        }

        launch(args);
    }
}
