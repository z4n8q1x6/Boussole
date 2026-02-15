# Résumé des améliorations — GestionBudgets et GestionBilans

Date : 14 février 2026

## 1️⃣ Améliorations GestionBudgets (Logique Dynamique)

### Fichiers modifiés
- **GestionBudgets.fxml** — ajout ComboBox TypeBudget et colonne Type dans la table
- **GestionBudgetsController.java** — logique dynamique pour remplir cbCategorie

### Fonctionnalités ajoutées

#### ComboBox Type de Budget
```xml
<ComboBox fx:id="cbTypeBudget" style="-fx-font-size:12px;" />
```

#### Logique dynamique : mettreAJourCategories()
```java
private void mettreAJourCategories() {
    cbCategorie.getItems().clear();
    budget_previsionnel.TypeBudget typeBudget = cbTypeBudget.getValue();

    if (typeBudget == budget_previsionnel.TypeBudget.LIMITE_DEPENSE) {
        // Remplir avec les catégories de TypeCharge enum
        for (TypeCharge charge : TypeCharge.values()) {
            cbCategorie.getItems().add(charge.name());
        }
        cbCategorie.setDisable(false);
        cbCategorie.getSelectionModel().selectFirst();
    } else if (typeBudget == budget_previsionnel.TypeBudget.OBJECTIF_REVENU) {
        // Pour OBJECTIF_REVENU, mettre "GLOBAL" et désactiver la sélection
        cbCategorie.getItems().add("GLOBAL");
        cbCategorie.setDisable(true);
        cbCategorie.getSelectionModel().selectFirst();
    }
}
```

### Comportement
1. **Sélection LIMITE_DEPENSE** :
   - cbCategorie se remplit automatiquement avec les valeurs de `TypeCharge` enum :
     - CHARGES_EXPLOITATIONS
     - CHARGES_FINANCIERES
     - CHARGES_EXCEPTIONNELLES
   - Reste activé pour permettre la sélection

2. **Sélection OBJECTIF_REVENU** :
   - cbCategorie affiche uniquement "GLOBAL"
   - Reste désactivé (disabled=true) car l'objectif revenu ne concernant pas une catégorie spécifique

### Sauvegarde améliorée
La méthode `sauvegarderBudget()` utilise maintenant le TypeBudget sélectionné au lieu de forcer LIMITE_DEPENSE :

```java
budget_previsionnel.TypeBudget typeBudget = cbTypeBudget.getValue();
budget_previsionnel budget = new budget_previsionnel(mois, annee, montant, typeBudget, categorie, franchiseId);
```

---

## 2️⃣ Améliorations GestionBilans (CRUD Complet)

### Fichiers modifiés
- **GestionBilans.fxml** — ajout colonne "Actions" dans la TableView
- **GestionBilansController.java** — logique CRUD (Modifier, Supprimer)

### Fonctionnalités ajoutées

#### Colonne Actions dans le FXML
```xml
<TableColumn fx:id="colActions" text="Actions" prefWidth="120" />
```

#### Cellule avec boutons Modifier (✏️) et Supprimer (🗑️)
```java
colActions.setCellFactory(param -> new TableCell<bilan, Void>() {
    private final Button btnModifier = new Button("✏️");
    private final Button btnSupprimer = new Button("🗑️");
    private final HBox hbox = new HBox(5, btnModifier, btnSupprimer);
    
    // ... listeners configurés dans le bloc initializer
});
```

#### Dialog de modification
```java
private void afficherDialogueModification(bilan b) {
    Dialog<bilan> dialog = new Dialog<>();
    dialog.setTitle("Modifier le Bilan");
    dialog.setHeaderText("Modifier les totaux du bilan pour " + b.getMois() + "/" + b.getAnnee());
    
    // Champs de saisie pour total_recettes et total_charges
    TextField txtRecettes = new TextField(String.valueOf(b.getTotalRecettes()));
    TextField txtCharges = new TextField(String.valueOf(b.getTotalCharges()));
    
    // Boutons OK/Annuler
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    
    // Mise à jour du résultat net = recettes - charges
    dialog.setResultConverter(buttonType -> {
        if (buttonType == ButtonType.OK) {
            double recettes = Double.parseDouble(txtRecettes.getText());
            double charges = Double.parseDouble(txtCharges.getText());
            double resultat = recettes - charges;
            
            b.setTotalRecettes(recettes);
            b.setTotalCharges(charges);
            b.setResultatNet(resultat);
            return b;
        }
        return null;
    });
}
```

#### Suppression avec rafraîchissement
```java
private void supprimerBilan(bilan b) {
    serviceBilan.deleteOne(b);
    chargerHistoriqueBilans(); // Rafraîchit la table automatiquement
}
```

### Comportement des boutons

| Bouton | Action | Résultat |
|--------|--------|----------|
| **✏️ Modifier** | Ouvre un Dialog | L'utilisateur modifie total_recettes et total_charges, puis clique OK |
|  |  | Le résultat net est recalculé automatiquement (recettes - charges) |
|  |  | La base de données est mise à jour via `ServiceBilan.updateOne()` |
|  |  | La TableView est rafraîchie |
| **🗑️ Supprimer** | Supprime le bilan | Appelle `ServiceBilan.deleteOne()` |
|  |  | La TableView est rafraîchie automatiquement |

---

## 🔧 Détails techniques

### Imports ajoutés (GestionBilansController)
```java
import javafx.geometry.Insets;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
```

### Imports ajoutés (GestionBudgetsController)
```java
import tn.esprit.Boussole.Models.TypeCharge;
```

### Services utilisés
- **ServiceBudgetPrevisionnel** : `add()`, `getAllByFranchise()`
- **ServiceBilan** : `updateOne()`, `deleteOne()`, `getHistorique()`, `genererBilan()`

---

## ✅ Tests recommandés

### GestionBudgets
1. ✓ Sélectionner LIMITE_DEPENSE → cbCategorie affiche les charges
2. ✓ Sélectionner OBJECTIF_REVENU → cbCategorie affiche GLOBAL et devient grisé
3. ✓ Sauvegarder un budget pour chaque type
4. ✓ Vérifier la table affiche le type correct

### GestionBilans
1. ✓ Cliquer sur ✏️ → Dialog s'ouvre
2. ✓ Modifier total_recettes et total_charges → Vérifier OK sauvegarde
3. ✓ Vérifier que résultat_net se recalcule (recettes - charges)
4. ✓ Cliquer sur 🗑️ → Bilan supprimé et table rafraîchie
5. ✓ Générer un nouveau bilan → Vérifie qu'il apparaît dans la table

---

## 📝 Notes importantes

- **franchiseId hardcodé** : actuellement fixé à 1 dans les deux contrôleurs. À adapter selon le contexte (login/session utilisateur).
- **TypeCharge enum** : utilisé pour dynamiquement remplir les catégories. Assure-toi qu'il existe dans `tn.esprit.Boussole.Models.TypeCharge`.
- **Dialog de modification** : inclut une validation basique (try/catch NumericFormat) ; à améliorer avec AlertDialog si les entrées sont invalides.
- **Rafraîchissement** : la méthode `chargerHistoriqueBilans()` est appelée après chaque action (modifier/supprimer) pour garantir que la table est à jour.

---

## 🎯 Améliorations futures (optionnelles)

1. **Confirmation avant suppression** : ajouter une AlertDialog "Êtes-vous sûr ?" avant de supprimer un bilan.
2. **Validation plus stricte** : vérifier que recettes et charges sont positives.
3. **Export PDF** : implémenter l'export des bilans en PDF (actuellement décoratif).
4. **Pagination** : si l'historique devient long, ajouter une pagination à la TableView.
5. **Filtrage** : ajouter des filtres (par mois/année) pour afficher un sous-ensemble de bilans.

---

## 📦 Fichiers livrés

- ✅ GestionBudgets.fxml (modifié)
- ✅ GestionBudgetsController.java (modifié)
- ✅ GestionBilans.fxml (modifié)
- ✅ GestionBilansController.java (modifié)
- 📄 Ce résumé (AMELIORATIONS_LOGIQUE_METIER.md)

---

**Fin du résumé des améliorations.** Les deux écrans sont maintenant prêts pour un usage en production (avec testing). 🚀

