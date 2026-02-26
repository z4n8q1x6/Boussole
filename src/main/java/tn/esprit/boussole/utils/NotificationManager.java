package tn.esprit.boussole.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
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

    public static void show(Window owner, Type type, String title, String message) {
        Platform.runLater(() -> createAndShowNotification(owner, type, title, message));
    }

    private static void createAndShowNotification(Window owner, Type type, String title, String message) {
        Popup popup = new Popup();
        popup.setAutoFix(true);

        // Conteneur principal (Carte)
        HBox root = new HBox(15);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(15, 20, 15, 20));
        root.setPrefWidth(350);
        root.setStyle(
                "-fx-background-color: #0F172A;" + // Fond sombre
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(255,255,255,0.1);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);"
        );

        // Barre de couleur latérale
        Label colorBar = new Label();
        colorBar.setMinWidth(4);
        colorBar.setPrefHeight(40);
        colorBar.setStyle("-fx-background-color: " + type.color + "; -fx-background-radius: 2;");

        // Icône
        Label iconLabel = new Label(type.icon);
        iconLabel.setFont(Font.font("Segoe UI Emoji", 24));
        iconLabel.setTextFill(Color.web(type.color));

        // Texte
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

        // Positionnement (Bas Droite)
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        // Si une fenêtre propriétaire est fournie, on essaie de se positionner par rapport à elle, sinon écran principal
        double x = screenBounds.getMaxX() - 370; // Largeur popup + marge
        double y = screenBounds.getMaxY() - 100; // Hauteur popup + marge

        popup.show(owner, x, y);

        // Animation d'entrée (Slide Up + Fade In)
        root.setOpacity(0);
        root.setTranslateY(20);

        Timeline timelineIn = new Timeline();
        timelineIn.getKeyFrames().add(new KeyFrame(Duration.millis(300),
                new KeyValue(root.opacityProperty(), 1),
                new KeyValue(root.translateYProperty(), 0)
        ));
        timelineIn.play();

        // Fermeture automatique après 4 secondes
        Timeline timelineOut = new Timeline();
        timelineOut.getKeyFrames().add(new KeyFrame(Duration.seconds(4),
                new KeyValue(root.opacityProperty(), 0),
                new KeyValue(root.translateYProperty(), 20)
        ));
        timelineOut.setOnFinished(e -> popup.hide());
        
        // Délai avant fermeture
        new Timeline(new KeyFrame(Duration.seconds(4), e -> timelineOut.play())).play();
        
        // Fermer au clic
        root.setOnMouseClicked(e -> {
            timelineOut.play();
        });
    }
}