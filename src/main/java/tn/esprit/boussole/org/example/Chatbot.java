package tn.esprit.boussole.org.example;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import tn.esprit.boussole.service.GeminiService;

public class Chatbot {
    private TextField t1;
    private TextArea t2;
    private String contexteDonnees;

    public Chatbot(String donnees) {
        this.contexteDonnees = (donnees != null) ? donnees : "";
    }

    public Scene creerSceneChatbot() {
        t1 = new TextField();
        t2 = new TextArea();
        Button sendBtn = new Button("Envoyer");

        Text title = new Text("Conseiller IA Boussole");
        title.setFill(Color.web("#00E5CC"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        t2.setEditable(false);
        t2.setWrapText(true);
        t2.setPrefHeight(450);
        t2.setStyle("-fx-control-inner-background: #0F1523; -fx-text-fill: white;");

        t1.setPromptText("Ex: Quel est le montant total des prêts accordés ?");
        sendBtn.setStyle("-fx-background-color: #00E5CC; -fx-font-weight: bold;");

        t2.appendText("Boussole IA : Je suis prêt à analyser vos données de prêts.\n\n");

        Runnable forcer = () -> {
            String question = t1.getText();
            if (!question.isEmpty()) {
                t2.appendText("Vous : " + question + "\n");
                t1.clear();

                // NOUVEAU PROMPT SANS CROCHETS ET PLUS DIRECT
                String promptComplet = "### RAPPORT FINANCIER BOUSSOLE ###\n"
                        + "Voici les données réelles de la base de données SQL :\n\n"
                        + contexteDonnees
                        + "\n\nINSTRUCTION : Réponds à la question suivante. Si c'est un calcul de montant total ou de moyenne, fais-le en utilisant les chiffres ci-dessus.\n"
                        + "Question : " + question;

                new Thread(() -> {
                    try {
                        String reponseIA = GeminiService.getGeminiResponse(promptComplet);
                        Platform.runLater(() -> t2.appendText("Boussole IA : " + reponseIA + "\n\n"));
                    } catch (Exception ex) {
                        Platform.runLater(() -> t2.appendText("Erreur : IA indisponible.\n\n"));
                    }
                }).start();
            }
        };

        t1.setOnAction(e -> forcer.run());
        sendBtn.setOnAction(e -> forcer.run());

        VBox root = new VBox(15, title, t2, t1, sendBtn);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #06080F;");

        return new Scene(root, 550, 650);
    }
}