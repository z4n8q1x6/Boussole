package com.boussole.app.controllers;

import com.boussole.app.models.Reclamation;
import com.boussole.app.services.ReclamationService;
import com.boussole.app.utils.UIUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ReclamationController {
  private final ReclamationService service = new ReclamationService();
  @FXML private TextField sujetField;
  @FXML private TextArea descriptionArea;
  @FXML private Label errorLabel;

  @FXML private TableView<Reclamation> table;
  @FXML private TableColumn<Reclamation, String> colSujet;
  @FXML private TableColumn<Reclamation, String> colDescription;
  @FXML private TableColumn<Reclamation, String> colStatut;
  @FXML private TableColumn<Reclamation, String> colDate;

  // temp
  private int franchise_id = 1;

  public void initialize() {
    // connects the table columns to Reclamation model getters
    colSujet.setCellValueFactory(new PropertyValueFactory<>("sujet"));
    colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
    // remove error message when start typing
    sujetField
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              errorLabel.setVisible(false);
              sujetField.setStyle("-fx-border-color: #cbd5e0;");
            });

    descriptionArea
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              errorLabel.setVisible(false);
              descriptionArea.setStyle("-fx-border-color: #cbd5e0;");
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
  public void insert() {
    Reclamation reclamation = new Reclamation();
    if (sujetField.getText().trim().isEmpty() || descriptionArea.getText().trim().isEmpty()) {
      errorLabel.setVisible(true);
      errorLabel.setManaged(true);
      // Highlight the fields in red
      sujetField.setStyle("-fx-border-color: #e74c3c;");
      descriptionArea.setStyle("-fx-border-color: #e74c3c;");
      return;
    }
    errorLabel.setVisible(false);
    errorLabel.setManaged(false);
    // Reset styles
    sujetField.setStyle("-fx-border-color: #cbd5e0;");
    descriptionArea.setStyle("-fx-border-color: #cbd5e0;");

    reclamation.setSujet(sujetField.getText());
    reclamation.setDescription(descriptionArea.getText());
    reclamation.setFranchiseId(franchise_id);
    if (service.add(reclamation)) {
      display();
      UIUtils.clear(sujetField, descriptionArea);
      System.out.println("Reclamation added successfully.");
    } else {
      System.out.println("Failed to add reclamation.");
    }
  }

  public void display() {
    table.setItems(service.getByFranchise(franchise_id));
  }

  @FXML
  public void delete() {
    Reclamation selected = table.getSelectionModel().getSelectedItem();

    if (selected != null) {
      Alert alert =
          new Alert(
              Alert.AlertType.CONFIRMATION,
              "Supprimer cette réclamation ?",
              ButtonType.YES,
              ButtonType.NO);
      alert.showAndWait();

      if (alert.getResult() == ButtonType.YES) {
        if (service.delete(selected.getId())) {
          display();
          System.out.println("Reclamation deleted successfully.");
        }
      }
    } else {
      Alert alert =
          new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une réclamation à supprimer.");
      alert.setHeaderText("Aucune sélection");
      alert.showAndWait();
    }
  }
}
