package tn.esprit.boussole.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

public class DialogManager {

    public static Optional<ButtonType> showConfirmationDialog(Window owner, String title, String message) {
        // 1. Créer un dialogue standard
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.TRANSPARENT); // Important pour un style personnalisé

        // 2. Récupérer le DialogPane pour le styliser
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("custom-dialog-pane");
        
        // Appliquer les feuilles de style de la scène principale
        if (owner != null && owner.getScene() != null) {
            dialogPane.getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        // 3. Créer le contenu personnalisé
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(20, 30, 30, 30));

        Label icon = new Label("❓");
        icon.setStyle("-fx-font-size: 36px;");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 4. Créer les boutons personnalisés
        Button confirmButton = new Button("Confirmer");
        confirmButton.getStyleClass().add("dialog-confirm-button");
        confirmButton.setOnAction(e -> dialog.setResult(ButtonType.OK));

        Button cancelButton = new Button("Annuler");
        cancelButton.getStyleClass().add("dialog-cancel-button");
        cancelButton.setOnAction(e -> dialog.setResult(ButtonType.CANCEL));

        HBox buttonBox = new HBox(15, cancelButton, confirmButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        content.getChildren().addAll(icon, titleLabel, messageLabel, buttonBox);

        // 5. Appliquer le contenu et afficher
        dialogPane.setContent(content);

        return dialog.showAndWait();
    }
}
