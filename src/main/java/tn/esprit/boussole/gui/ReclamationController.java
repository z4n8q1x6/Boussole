package tn.esprit.boussole.gui;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Reclamation;
import tn.esprit.boussole.service.ReclamationService;
import tn.esprit.boussole.utils.AlertUtil;
import tn.esprit.boussole.utils.UserManager; // --- IMPORTÉ POUR RÉCUPÉRER L'ID ---

public class ReclamationController {
  private final ReclamationService service = new ReclamationService();

  @FXML private TableView<Reclamation> table;
  @FXML private TableColumn<Reclamation, String> colSujet;
  @FXML private TableColumn<Reclamation, String> colDescription;
  @FXML private TableColumn<Reclamation, String> colStatut;
  @FXML private TableColumn<Reclamation, String> colDate;
  @FXML private Button btnNewReclamation;

  // --- ID DE LA FRANCHISE (Dynamique) ---
  private int franchise_id = -1;

  public void initialize() {
    // --- RÉCUPÉRER L'ID DE LA FRANCHISE DE L'UTILISATEUR CONNECTÉ ---
    this.franchise_id = UserManager.getCurrentUserFranchiseId();
    System.out.println("ReclamationController DEBUG: Franchise ID chargé = " + this.franchise_id);

    // Connects the table columns to Reclamation model getters
    colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
    colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

    // Makes description column scrollable
    colDescription.setCellFactory(
            column -> {
              return new TableCell<Reclamation, String>() {
                private final TextArea textArea = new TextArea();

                {
                  textArea.setEditable(false);
                  textArea.setWrapText(true);

                  // Style it to blend in: no border, transparent background
                  textArea.setStyle(
                          "-fx-background-color: transparent; "
                                  + "-fx-background-insets: 0; "
                                  + "-fx-padding: 5;");

                  // pref height
                  textArea.setPrefHeight(40);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                  super.updateItem(item, empty);
                  if (empty || item == null) {
                    setGraphic(null);
                  } else {
                    textArea.setText(item);
                    setGraphic(textArea);
                  }
                }
              };
            });

    display();
  }

  @FXML
  private void handleAddReclamation() {
    FXMLLoader loader =
            new FXMLLoader(getClass().getResource("/addReclamation.fxml"));
    try {
      Parent root = loader.load();
      AddReclamationController controller = loader.getController();

      // --- PASSER L'ID DYNAMIQUE AU CONTRÔLEUR D'AJOUT ---
      controller.setFranchiseId(this.franchise_id);

      Stage stage = new Stage();
      stage.setTitle("Ajouter une réclamation");
      stage.setScene(new Scene(root));
      stage.showAndWait();
    } catch (IOException e) {
      System.err.println("Error loading FXML:");
      e.printStackTrace();
    }
    display();
  }

  public void display() {
    // Affiche les réclamations pour la franchise spécifique
    table.setItems(service.getByFranchise(franchise_id));
  }

  @FXML
  public void delete() {
    Reclamation selected = table.getSelectionModel().getSelectedItem();

    if (selected != null) {
      ButtonType result =
              AlertUtil.showConfirmation("Confirmation", "Supprimer cette réclamation ?");

      if (result == ButtonType.YES) {
        if (service.delete(selected.getId())) {
          display();
          System.out.println("Reclamation deleted successfully.");
        }
      }
    } else {
      AlertUtil.showWarning(
              "Aucune sélection", "Veuillez sélectionner une réclamation à supprimer.");
    }
  }
}