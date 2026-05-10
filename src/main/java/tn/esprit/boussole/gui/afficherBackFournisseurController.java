package tn.esprit.boussole.gui;

import java.io.IOException;
import java.sql.SQLException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import tn.esprit.boussole.models.Fournisseur;
import tn.esprit.boussole.service.EmailFournisseurService;
import tn.esprit.boussole.service.FournisseurService;

public class afficherBackFournisseurController {
  @FXML private TableView<Fournisseur> tableFournisseurs;
  @FXML private TableColumn<Fournisseur, String> colNom;
  @FXML private TableColumn<Fournisseur, String> colMatricule;
  @FXML private TableColumn<Fournisseur, String> colTelephone;
  @FXML private TableColumn<Fournisseur, String> colFranchiseId;
  @FXML private TableColumn<Fournisseur, Void> colModifier;
  @FXML private TableColumn<Fournisseur, Void> colSupprimer;
  @FXML private Button btnAjouter;
  // btnFront supprimé
  @FXML private ComboBox<String> comboTri;
  @FXML private Label lblTotal;
  @FXML private TextField txtRecherche;

  private final FournisseurService fournisseurService = new FournisseurService();
  private final EmailFournisseurService emailFournisseurService = new EmailFournisseurService();
  private final ObservableList<Fournisseur> fournisseursList = FXCollections.observableArrayList();
  private FilteredList<Fournisseur> filteredData;
  private SortedList<Fournisseur> sortedData;

  @FXML
  public void initialize() {
    tableFournisseurs.setEditable(true);

    // --- CLIC DROIT POUR L'EMAIL (CONSERVÉ) ---
    ContextMenu contextMenu = new ContextMenu();
    MenuItem itemPartager = new MenuItem("📧 Partager par Email");
    itemPartager.setOnAction(
        event -> {
          Fournisseur selected = tableFournisseurs.getSelectionModel().getSelectedItem();
          if (selected != null) {
            partagerFournisseurParEmail(selected);
          } else {
            showAlert("Information", "Veuillez sélectionner un fournisseur dans la table.");
          }
        });
    contextMenu.getItems().add(itemPartager);
    tableFournisseurs.setContextMenu(contextMenu);

    // --- CONFIGURATION DES COLONNES ---
    colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
    colNom.setCellFactory(TextFieldTableCell.forTableColumn());
    colNom.setOnEditCommit(
        event -> {
          Fournisseur f = event.getRowValue();
          f.setNom(event.getNewValue());
          updateFournisseurInDB(f);
        });

    colMatricule.setCellValueFactory(new PropertyValueFactory<>("matriculeFiscal"));
    colMatricule.setCellFactory(TextFieldTableCell.forTableColumn());
    colMatricule.setOnEditCommit(
        event -> {
          Fournisseur f = event.getRowValue();
          f.setMatriculeFiscal(event.getNewValue());
          updateFournisseurInDB(f);
        });

    colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
    colTelephone.setCellFactory(TextFieldTableCell.forTableColumn());
    colTelephone.setOnEditCommit(
        event -> {
          Fournisseur f = event.getRowValue();
          f.setTelephone(event.getNewValue());
          updateFournisseurInDB(f);
        });

    colFranchiseId.setCellValueFactory(new PropertyValueFactory<>("franchiseName"));

    addModifierButtonToTable();
    addSupprimerButtonToTable();

    filteredData = new FilteredList<>(fournisseursList, p -> true);
    sortedData = new SortedList<>(filteredData);
    sortedData.comparatorProperty().bind(tableFournisseurs.comparatorProperty());
    tableFournisseurs.setItems(sortedData);

    txtRecherche
        .textProperty()
        .addListener(
            (obs, old, newValue) -> {
              filteredData.setPredicate(
                  f -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    return f.getNom().toLowerCase().contains(newValue.toLowerCase());
                  });
              calculerTotal();
            });

    comboTri.setItems(
        FXCollections.observableArrayList("Matricule Croissant", "Matricule Décroissant"));
    comboTri.setOnAction(e -> trierFournisseurs());

    loadFournisseurs();
    btnAjouter.setOnAction(e -> openAjoutForm());
    // Logique btnFront retirée ici
  }

  // --- ENVOI D'EMAIL (CONSERVÉ ET INTACT) ---
  private void partagerFournisseurParEmail(Fournisseur f) {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Partage par Email");
    dialog.setHeaderText("Partager les infos de : " + f.getNom());
    dialog.setContentText("Entrez l'adresse email de destination :");

    DialogPane dialogPane = dialog.getDialogPane();
    String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
    dialogPane.getStylesheets().add(css);
    dialogPane.getStyleClass().add("dialog-pane");

    dialog
        .getEditor()
        .setStyle(
            "-fx-background-color: #06080F; -fx-text-fill: white; -fx-border-color: #0EA5E9;"
                + " -fx-background-radius: 5; -fx-border-radius: 5;");

    dialog
        .showAndWait()
        .ifPresent(
            emailCible -> {
              String sujet = "Fiche Fournisseur - Boussole";
              String corps =
                  "Voici les informations du fournisseur :\n\n"
                      + "Nom : "
                      + f.getNom()
                      + "\n"
                      + "Matricule Fiscal : "
                      + f.getMatriculeFiscal()
                      + "\n"
                      + "Téléphone : "
                      + f.getTelephone()
                      + "\n\n"
                      + "Envoyé depuis l'application Boussole.";

              new Thread(
                      () -> {
                        try {
                          emailFournisseurService.envoyerFicheFournisseur(emailCible, sujet, corps);
                          Platform.runLater(
                              () -> showAlert("Succès", "L'email a été envoyé avec succès !"));
                        } catch (Exception e) {
                          Platform.runLater(
                              () -> showAlert("Erreur", "Échec de l'envoi : " + e.getMessage()));
                        }
                      })
                  .start();
            });
  }

  private void updateFournisseurInDB(Fournisseur f) {
    try {
      fournisseurService.updateone(f);
    } catch (SQLException e) {
      showAlert("Erreur", "MAJ impossible : " + e.getMessage());
      loadFournisseurs();
    }
  }

  private void loadFournisseurs() {
    fournisseursList.clear();
    try {
      fournisseursList.addAll(fournisseurService.selectAll(null));
      calculerTotal();
    } catch (SQLException e) {
      showAlert("Erreur", "Chargement impossible : " + e.getMessage());
    }
  }

  private void calculerTotal() {
    lblTotal.setText("Total : " + tableFournisseurs.getItems().size());
  }

  private void trierFournisseurs() {
    String selection = comboTri.getValue();
    if ("Matricule Croissant".equals(selection)) {
      colMatricule.setSortType(TableColumn.SortType.ASCENDING);
    } else {
      colMatricule.setSortType(TableColumn.SortType.DESCENDING);
    }
    tableFournisseurs.getSortOrder().setAll(colMatricule);
  }

  private void addModifierButtonToTable() {
    colModifier.setCellFactory(
        param ->
            new TableCell<>() {
              private final Button btn = new Button("✎");

              {
                btn.setStyle(
                    "-fx-background-color: rgba(0, 200, 180, 0.15);"
                        + "-fx-text-fill: #00C4B4;"
                        + "-fx-cursor: hand;"
                        + "-fx-background-radius: 6;"
                        + "-fx-font-size: 14px;"
                        + "-fx-padding: 4 10 4 10;");
                btn.setOnAction(e -> openModifierForm(getTableView().getItems().get(getIndex())));
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                  setGraphic(null);
                } else {
                  setGraphic(btn);
                  setAlignment(Pos.CENTER);
                }
              }
            });
  }

  private void addSupprimerButtonToTable() {
    colSupprimer.setCellFactory(
        param ->
            new TableCell<>() {
              private final Button btn = new Button("⊖");

              {
                btn.setStyle(
                    "-fx-background-color: rgba(231, 76, 60, 0.15);"
                        + "-fx-text-fill: #E74C3C;"
                        + "-fx-cursor: hand;"
                        + "-fx-background-radius: 6;"
                        + "-fx-font-size: 14px;"
                        + "-fx-padding: 4 10 4 10;");
                btn.setOnAction(
                    e -> supprimerFournisseur(getTableView().getItems().get(getIndex())));
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                  setGraphic(null);
                } else {
                  setGraphic(btn);
                  setAlignment(Pos.CENTER);
                }
              }
            });
  }

  private void supprimerFournisseur(Fournisseur f) {
    try {
      fournisseurService.deleteone(f);
      fournisseursList.remove(f);
      calculerTotal();
    } catch (SQLException e) {
      showAlert("Erreur", "Suppression impossible : " + e.getMessage());
    }
  }

  private void openAjoutForm() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterFournisseur.fxml"));
      Stage stage = new Stage();
      stage.setScene(new Scene(loader.load()));
      stage.showAndWait();
      loadFournisseurs();
    } catch (IOException e) {
      showAlert("Erreur", "Ouverture impossible : " + e.getMessage());
    }
  }

  private void openModifierForm(Fournisseur f) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/modifierFournisseur.fxml"));
      Parent root = loader.load();
      modifierFournisseurController controller = loader.getController();
      controller.setFournisseurActuel(f);
      Stage stage = new Stage();
      stage.setScene(new Scene(root));
      stage.showAndWait();
      loadFournisseurs();
    } catch (IOException e) {
      showAlert("Erreur", "Ouverture impossible : " + e.getMessage());
    }
  }

  private void showAlert(String titre, String msg) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(titre);
    alert.setHeaderText(null);
    alert.setContentText(msg);

    DialogPane dialogPane = alert.getDialogPane();
    String css = getClass().getResource("/styles/ChargesdepensesDash.css").toExternalForm();
    dialogPane.getStylesheets().add(css);
    dialogPane.getStyleClass().add("dialog-pane");

    alert.showAndWait();
  }
}

