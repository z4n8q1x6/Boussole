package tn.esprit.boussole.Utilis;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

/**
 * Gestion centralisée des notifications "toast" pour l'application boussole.
 */
public class NotificationManager {

    private static final Duration DURATION = Duration.seconds(5);

    // Couleur de fond très prisée (Bleu profond #1d3557)
    // Remplacé par une couleur plus intégrée au projet (#0F172A)
    private static final String BACKGROUND_COLOR = "#0F172A";

    private static Notifications base(String title, String message, String borderColor, String iconText) {
        
        // Création du Node "Custom" pour la notification
        HBox container = new HBox();
        container.setSpacing(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";" +
                           "-fx-background-radius: 12;" +
                           "-fx-border-color: rgba(255, 255, 255, 0.05);" +
                           "-fx-border-width: 1;" +
                           "-fx-border-radius: 12;" +
                           "-fx-padding: 12 20;");
                           
        // Ligne de couleur sur le côté via un petit rectangle
        javafx.scene.shape.Rectangle colorBand = new javafx.scene.shape.Rectangle(4, 40);
        colorBand.setFill(Color.web(borderColor));
        colorBand.setArcWidth(4);
        colorBand.setArcHeight(4);

        // Ombre portée plus douce (Drop Shadow)
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(15.0);
        dropShadow.setOffsetX(0.0);
        dropShadow.setOffsetY(8.0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.6));
        container.setEffect(dropShadow);

        // Icone factice ou texte d'icône (ex: ✅ ou ⚠️)
        Label iconLabel = new Label(iconText);
        iconLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        iconLabel.setTextFill(Color.web(borderColor));

        // Contenu texte
        javafx.scene.layout.VBox textContainer = new javafx.scene.layout.VBox(3);
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(280);

        textContainer.getChildren().addAll(lblTitle, lblMessage);

        container.getChildren().addAll(colorBand, iconLabel, textContainer);

        return Notifications.create()
                .title("")
                .text("")
                .graphic(container)
                .hideAfter(DURATION)
                .position(Pos.TOP_RIGHT); // Plus moderne en haut à droite
    }

    public static void showSuccess(String title, String message) {
        // Vert fluo = #00FF7F ou #10B981
        base(title, message, "#00ff7f", "✅").show();
    }

    public static void showError(String title, String message) {
        // Rouge vif = #FF3B30 ou #EF4444
        base(title, message, "#ff3b30", "⚠️").show();
    }

    public static void showInfo(String title, String message) {
        // Bleu Cyan = #00E5CC
        base(title, message, "#00e5cc", "ℹ️").show();
    }
}

