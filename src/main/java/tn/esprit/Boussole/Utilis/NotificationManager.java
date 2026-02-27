package tn.esprit.Boussole.Utilis;

import javafx.geometry.Pos;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

/**
 * Gestion centralisée des notifications "toast" pour l'application Boussole.
 */
public class NotificationManager {

    private static final Duration DURATION = Duration.seconds(5);

    private static Notifications base(String title, String message) {
        return Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(DURATION)
                .position(Pos.BOTTOM_RIGHT)
                .darkStyle(); // cohérent avec thème sombre
    }

    public static void showSuccess(String title, String message) {
        base(title, message)
                .showInformation(); // visuel vert/positif par défaut
    }

    public static void showError(String title, String message) {
        base(title, message)
                .showError();
    }

    public static void showInfo(String title, String message) {
        base(title, message)
                .showInformation();
    }
}

