package tn.esprit.boussole.gui;

import java.io.IOException;
import javafx.beans.property.SimpleStringProperty;
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

public class AdminReclamationController {
  private final ReclamationService service = new ReclamationService();

  @FXML private TableView<Reclamation> table;
  @FXML private TableColumn<Reclamation, String> colSujet;
  @FXML private TableColumn<Reclamation, String> colDescription;
  @FXML private TableColumn<Reclamation, String> colStatut;
  @FXML private TableColumn<Reclamation, String> colDate;
  @FXML private TableColumn<Reclamation, String> colFranchise;

  public void initialize() {
    // connects the table columns to Reclamation model getters
    colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
    colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
    colFranchise.setCellValueFactory(new PropertyValueFactory<>("franchiseId"));
    colFranchise.setCellValueFactory(
        cellData -> {
          return new SimpleStringProperty("nom/ville#" + cellData.getValue().getFranchiseId());
        });
    // makes description column scrollable
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

  public void display() {
    table.setItems(service.getAll());
  }

  @FXML
  private void handleUpdate() {
    Reclamation selected = table.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertUtil.showWarning("Aucune sélection", "Veuillez sélectionner une réclamation.");
      return;
    }
    try {
      FXMLLoader loader =
          new FXMLLoader(
              getClass().getResource("/tn/esprit/boussole/support/updateReclamation.fxml"));
      Parent root = loader.load();
      UpdateReclamationController controller = loader.getController();
      controller.setReclamation(selected);
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
}
