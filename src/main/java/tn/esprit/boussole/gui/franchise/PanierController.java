package tn.esprit.boussole.gui.franchise;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.boussole.models.Commande;
import tn.esprit.boussole.models.LigneCommande;
import tn.esprit.boussole.models.Produit;
import tn.esprit.boussole.service.CommandeService;
import tn.esprit.boussole.service.LigneCommandeService;
import tn.esprit.boussole.service.ProduitService;
import tn.esprit.boussole.utils.PanierManager;
import tn.esprit.boussole.utils.UserManager;
import tn.esprit.boussole.utils.NotificationManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class PanierController implements Initializable {

    @FXML private ListView<ProduitPanier> panierList;
    @FXML private Label totalLabel;
    @FXML private Label nbArticlesLabel;
    @FXML private Button validerCommandeBtn;
    @FXML private Button viderPanierBtn;

    private ProduitService produitService;
    private CommandeService commandeService;
    private LigneCommandeService ligneCommandeService;
    private ObservableList<ProduitPanier> panierItems;
    private SimpleDoubleProperty totalProperty = new SimpleDoubleProperty(0);

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Helper method to get current franchise ID
    private int getCurrentFranchiseId() {
        return UserManager.getCurrentUserFranchiseId();
    }

    public static class ProduitPanier {
        private Produit produit;
        private int quantite;

        public ProduitPanier(Produit produit, int quantite) {
            this.produit = produit;
            this.quantite = quantite;
        }

        public Produit getProduit() { return produit; }
        public void setProduit(Produit produit) { this.produit = produit; }
        public int getQuantite() { return quantite; }
        public void setQuantite(int quantite) { this.quantite = quantite; }
        public double getTotalLigne() { return produit.getPrix_achat() * quantite; }
        public String getNom() { return produit.getNom(); }
        public double getPrix() { return produit.getPrix_achat(); }
        public int getStock() { return produit.getStock_dispo(); }
        public String getImage() { return produit.getImage(); }
        public int getProduitId() { return produit.getId(); }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Vérifier si l'utilisateur a une franchise valide
        int franchiseId = getCurrentFranchiseId();
        if (!UserManager.isValidFranchiseId(franchiseId)) {
            NotificationManager.show(
                    panierList.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur de session",
                    "Aucune franchise associée à votre compte. Veuillez contacter l'administrateur."
            );
            return;
        }

        produitService = new ProduitService();
        commandeService = new CommandeService();
        ligneCommandeService = new LigneCommandeService();

        // Récupérer les items du panier depuis le manager
        panierItems = PanierManager.getInstance().getPanierItems();
        PanierManager.getInstance().setPanierController(this);

        // Configuration de la ListView avec des cellules personnalisées
        panierList.setCellFactory(param -> new PanierCell());
        panierList.setItems(panierItems);

        // Lier le total
        totalLabel.textProperty().bind(totalProperty.asString("%.2f DT"));

        mettreAJourCompteurs();
    }

    /**
     * Ajouter un produit au panier (appelé depuis CatalogueController)
     */
    public void ajouterProduit(Produit produit, int quantite) {
        // Vérifier si le produit est déjà dans le panier
        for (ProduitPanier item : panierItems) {
            if (item.getProduit().getId() == produit.getId()) {
                // Augmenter la quantité
                item.setQuantite(item.getQuantite() + quantite);
                panierList.refresh();
                mettreAJourCompteurs();
                return;
            }
        }

        // Sinon, ajouter un nouvel item
        panierItems.add(new ProduitPanier(produit, quantite));
        mettreAJourCompteurs();
    }

    @FXML
    private void handleValiderCommande() {
        if (panierItems.isEmpty()) {
            NotificationManager.show(
                    panierList.getScene().getWindow(),
                    NotificationManager.Type.WARNING,
                    "Panier vide",
                    "Votre panier est vide. Ajoutez des produits avant de valider."
            );
            return;
        }

        int franchiseId = getCurrentFranchiseId();
        if (!UserManager.isValidFranchiseId(franchiseId)) {
            NotificationManager.show(
                    panierList.getScene().getWindow(),
                    NotificationManager.Type.ERROR,
                    "Erreur de session",
                    "Impossible d'identifier votre franchise. Veuillez vous reconnecter."
            );
            return;
        }

        // Afficher le récapitulatif
        String recap = genererRecapitulatif();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Validation de la commande");
        confirm.setHeaderText("Confirmer votre commande");
        confirm.setContentText(recap + "\n\nVoulez-vous valider cette commande ?");

        // Styliser l'alerte
        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #0C0F1A;");
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white;");
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                creerCommande(franchiseId);
            } catch (SQLException e) {
                NotificationManager.show(
                        panierList.getScene().getWindow(),
                        NotificationManager.Type.ERROR,
                        "Erreur",
                        "Erreur lors de la création de la commande: " + e.getMessage()
                );
                e.printStackTrace();
            }
        }
    }

    /**
     * Créer la commande en base de données
     */
    private void creerCommande(int franchiseId) throws SQLException {
        // 1. Calculer le montant total
        double montantTotal = totalProperty.get();

        // 2. Créer la commande
        Commande commande = new Commande(
                LocalDateTime.now(),
                montantTotal,
                "EN_ATTENTE",
                franchiseId
        );

        // 3. Insérer la commande et récupérer l'ID généré
        commandeService.insertone(commande);
        int commandeId = commande.getId();

        System.out.println("✅ Commande créée avec ID: " + commandeId);

        // 4. Créer les lignes de commande
        for (ProduitPanier item : panierItems) {
            LigneCommande ligne = new LigneCommande(
                    item.getQuantite(),
                    item.getPrix(),
                    commandeId,
                    item.getProduitId()
            );
            ligneCommandeService.insertone(ligne);

            System.out.println("   ➜ Ligne ajoutée: " + item.getNom() + " x " + item.getQuantite() +
                    " = " + String.format("%.2f DT", item.getTotalLigne()));
        }

        // 5. Vider le panier
        PanierManager.getInstance().viderPanier();

        // 6. Afficher confirmation
        NotificationManager.show(
                panierList.getScene().getWindow(),
                NotificationManager.Type.SUCCESS,
                "Succès",
                "✅ Votre commande #" + commandeId + " a été créée avec succès !\nElle est en attente de validation par le siège."
        );
    }

    /**
     * Générer un récapitulatif du panier
     */
    private String genererRecapitulatif() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Récapitulatif de votre commande :\n\n");

        for (ProduitPanier item : panierItems) {
            sb.append("• ").append(item.getNom())
                    .append(" x ").append(item.getQuantite())
                    .append(" = ").append(String.format("%.2f DT", item.getTotalLigne()))
                    .append("\n");
        }

        sb.append("\n💰 Total : ").append(String.format("%.2f DT", totalProperty.get()));

        return sb.toString();
    }

    @FXML
    private void handleViderPanier() {
        if (panierItems.isEmpty()) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vider le panier");
        confirm.setHeaderText("Vider le panier");
        confirm.setContentText("Êtes-vous sûr de vouloir vider votre panier ?");

        // Styliser l'alerte
        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #0C0F1A;");
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white;");
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            PanierManager.getInstance().viderPanier();
            viderPanier();
        }
    }

    private void viderPanier() {
        panierItems.clear();
        mettreAJourCompteurs();
    }

    private void mettreAJourCompteurs() {
        int nbArticles = panierItems.stream().mapToInt(ProduitPanier::getQuantite).sum();
        double total = panierItems.stream().mapToDouble(ProduitPanier::getTotalLigne).sum();

        nbArticlesLabel.setText(String.valueOf(nbArticles));
        totalProperty.set(total);
    }

    /**
     * Rafraîchir l'affichage du panier (appelé par PanierManager)
     */
    public void rafraichir() {
        panierList.refresh();
        mettreAJourCompteurs();
    }

    // Cellule personnalisée pour l'affichage du panier avec images
    class PanierCell extends ListCell<ProduitPanier> {
        private final HBox content = new HBox(15);
        private final ImageView imageView = new ImageView();
        private final VBox details = new VBox(5);
        private final Label nomLabel = new Label();
        private final Label prixLabel = new Label();
        private final HBox quantiteBox = new HBox(10);
        private final Label quantiteLabel = new Label();
        private final Button moinsBtn = new Button();
        private final Button plusBtn = new Button();
        private final Button supprimerBtn = new Button();
        private final Label totalLigneLabel = new Label();

        public PanierCell() {
            super();

            // Configuration de l'image du produit
            imageView.setFitHeight(60);
            imageView.setFitWidth(60);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 5;");

            // Style des labels
            nomLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E8EDF5; -fx-font-size: 14px;");
            prixLabel.setStyle("-fx-text-fill: #00E5CC; -fx-font-size: 14px;");
            totalLigneLabel.setStyle("-fx-text-fill: #00E5CC; -fx-font-weight: bold; -fx-font-size: 16px;");
            quantiteLabel.setStyle("-fx-text-fill: #E8EDF5; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-color: #1E293B; -fx-background-radius: 5;");

            // Configuration des boutons avec images
            configurerBouton(moinsBtn, "/images/minus.png", "Moins");
            configurerBouton(plusBtn, "/images/plus.png", "Plus");
            configurerBouton(supprimerBtn, "/images/delete.png", "Supprimer");

            // Actions des boutons
            moinsBtn.setOnAction(e -> {
                ProduitPanier item = getItem();
                if (item != null && item.getQuantite() > 1) {
                    item.setQuantite(item.getQuantite() - 1);
                    mettreAJourAffichage(item);
                    rafraichir();
                    mettreAJourCompteurs();
                }
            });

            plusBtn.setOnAction(e -> {
                ProduitPanier item = getItem();
                if (item != null && item.getQuantite() < item.getStock()) {
                    item.setQuantite(item.getQuantite() + 1);
                    mettreAJourAffichage(item);
                    rafraichir();
                    mettreAJourCompteurs();
                }
            });

            supprimerBtn.setOnAction(e -> {
                ProduitPanier item = getItem();
                if (item != null) {
                    panierItems.remove(item);
                    mettreAJourCompteurs();
                }
            });

            // Assemblage
            quantiteBox.getChildren().addAll(moinsBtn, quantiteLabel, plusBtn, supprimerBtn);
            details.getChildren().addAll(nomLabel, prixLabel, quantiteBox);
            content.getChildren().addAll(imageView, details, totalLigneLabel);
            content.setPadding(new Insets(10));
            content.setStyle("-fx-background-color: #0C0F1A; -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10;");
            HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
            HBox.setMargin(totalLigneLabel, new Insets(0, 10, 0, 0));
        }

        private void configurerBouton(Button btn, String imagePath, String tooltip) {
            try {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
                icon.setFitHeight(16);
                icon.setFitWidth(16);
                icon.setPreserveRatio(true);
                btn.setGraphic(icon);
            } catch (Exception e) {
                System.err.println("Image non trouvée: " + imagePath);
                btn.setText(tooltip.equals("Moins") ? "-" : tooltip.equals("Plus") ? "+" : "🗑️");
            }

            btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-pref-width: 30; -fx-pref-height: 30; -fx-text-fill: #00E5CC;");
            btn.setTooltip(new Tooltip(tooltip));
        }

        private void mettreAJourAffichage(ProduitPanier item) {
            quantiteLabel.setText(String.valueOf(item.getQuantite()));
            totalLigneLabel.setText(String.format("%.2f DT", item.getTotalLigne()));
        }

        @Override
        protected void updateItem(ProduitPanier item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
            } else {
                // Charger l'image du produit
                chargerImageProduit(item);

                // Mettre à jour les textes
                nomLabel.setText(item.getNom());
                prixLabel.setText(String.format("%.2f DT", item.getPrix()));
                quantiteLabel.setText(String.valueOf(item.getQuantite()));
                totalLigneLabel.setText(String.format("%.2f DT", item.getTotalLigne()));

                setGraphic(content);
            }
        }

        private void chargerImageProduit(ProduitPanier item) {
            try {
                if (item.getImage() != null && !item.getImage().isEmpty()) {
                    String imagePath = item.getImage();
                    Image img = null;

                    if (imagePath.startsWith("http")) {
                        img = new Image(imagePath, 60, 60, true, true);
                    } else if (imagePath.startsWith("file:")) {
                        img = new Image(imagePath, 60, 60, true, true);
                    } else {
                        java.io.File file = new java.io.File(imagePath);
                        if (file.exists()) {
                            img = new Image(file.toURI().toString(), 60, 60, true, true);
                        }
                    }

                    if (img != null && !img.isError()) {
                        imageView.setImage(img);
                    } else {
                        imageView.setImage(null);
                    }
                } else {
                    imageView.setImage(null);
                }
            } catch (Exception e) {
                imageView.setImage(null);
            }
        }
    }
}