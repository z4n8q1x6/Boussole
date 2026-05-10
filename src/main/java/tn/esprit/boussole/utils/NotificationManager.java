package tn.esprit.boussole.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;

public class NotificationManager {

    public enum Type {
        SUCCESS("#10B981", "✅"),
        ERROR("#EF4444", "⛔"),
        WARNING("#F59E0B", "⚠️"),
        INFO("#3B82F6", "ℹ️");

        final String color;
        final String icon;

        Type(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    // --- Méthodes de commodité (Raccourcis) ---

    public static void showInfo(String title, String message) {
        show(null, Type.INFO, title, message);
    }

    public static void showError(String title, String message) {
        show(null, Type.ERROR, title, message);
    }

    public static void showSuccess(String title, String message) {
        show(null, Type.SUCCESS, title, message);
    }

    /**
     * Affiche une notification générique
     */
    public static void show(Window owner, Type type, String title, String message) {
        Platform.runLater(() -> createAndShowNotification(owner, type, title, message));
    }

    private static void createAndShowNotification(Window owner, Type type, String title, String message) {
        Popup popup = new Popup();
        popup.setAutoFix(true);

        // Conteneur principal (Design moderne type "Toast")
        HBox root = new HBox(15);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(15, 20, 15, 20));
        root.setPrefWidth(350);
        root.setStyle(
                "-fx-background-color: #0F172A;" + // Fond sombre (Slate 900)
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);"
        );

        // Barre de couleur latérale décorative
        Label colorBar = new Label();
        colorBar.setMinWidth(4);
        colorBar.setPrefHeight(40);
        colorBar.setStyle("-fx-background-color: " + type.color + "; -fx-background-radius: 2;");

        // Icône selon le type
        Label iconLabel = new Label(type.icon);
        iconLabel.setFont(Font.font("Segoe UI Emoji", 24));
        iconLabel.setTextFill(Color.web(type.color));

        // Conteneur de texte
        VBox textContainer = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Label messageLabel = new Label(message);
        messageLabel.setTextFill(Color.web("#94A3B8"));
        messageLabel.setFont(Font.font("Segoe UI", 12));
        messageLabel.setWrapText(true);

        textContainer.getChildren().addAll(titleLabel, messageLabel);

        root.getChildren().addAll(colorBar, iconLabel, textContainer);
        popup.getContent().add(root);

        // Calcul de la position (Bas Droite de l'écran principal)
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double x = screenBounds.getMaxX() - 370; // Largeur popup + marge
        double y = screenBounds.getMaxY() - 100; // Hauteur estimée + marge

        popup.show(owner, x, y);

        // --- Animations ---

        // Animation d'entrée (Glissement vers le haut + Fondu)
        root.setOpacity(0);
        root.setTranslateY(20);

        Timeline timelineIn = new Timeline();
        timelineIn.getKeyFrames().add(new KeyFrame(Duration.millis(300),
                new KeyValue(root.opacityProperty(), 1),
                new KeyValue(root.translateYProperty(), 0)
        ));
        timelineIn.play();

        // Animation de sortie (Fondu inverse)
        Timeline timelineOut = new Timeline();
        timelineOut.getKeyFrames().add(new KeyFrame(Duration.millis(300),
                new KeyValue(root.opacityProperty(), 0),
                new KeyValue(root.translateYProperty(), 20)
        ));
        timelineOut.setOnFinished(e -> popup.hide());

        // Délai avant fermeture automatique (4 secondes)
        new Timeline(new KeyFrame(Duration.seconds(4), e -> timelineOut.play())).play();

        // Fermer instantanément au clic sur la notification
        root.setOnMouseClicked(e -> timelineOut.play());
    }
}