# Boussole - Guide d'implémentation des écrans JavaFX

## Résumé des fichiers créés

### 1. **GestionBudgets.fxml + GestionBudgetsController**
- **Fichier FXML** : `src/main/resources/tn/esprit/Boussole/GUI/GestionBudgets.fxml`
- **Contrôleur** : `src/main/java/tn/esprit/Boussole/GUI/GestionBudgetsController.java`
- **Contenu** :
  - Sidebar identique au Dashboard (boutons de navigation)
  - Titre : "Stratégie Budgétaire"
  - Formulaire avec ComboBox (Mois, Catégorie), TextField (Montant), CheckBox (Réseau), Bouton Sauvegarder
  - TableView affichant les budgets existants (Mois, Année, Catégorie, Montant)
- **Fonctionnalités** :
  - `initialize()` : charge les budgets depuis `ServiceBudgetPrevisionnel.getAllByFranchise(franchiseId)`
  - Bouton "Sauvegarder" : appelle `serviceBudget.add(budget)` et rafraîchit le tableau
  - Boutons sidebar : navigation vers Dashboard, Budgets, Bilans

### 2. **GestionBilans.fxml + GestionBilansController**
- **Fichier FXML** : `src/main/resources/tn/esprit/Boussole/GUI/GestionBilans.fxml`
- **Contrôleur** : `src/main/java/tn/esprit/Boussole/GUI/GestionBilansController.java`
- **Contenu** :
  - Sidebar identique au Dashboard
  - Titre : "Reporting Financier"
  - Bouton : "📊 Générer Bilan Mensuel" (avec icône emoji)
  - TableView affichant l'historique des bilans (Mois, Année, Recettes, Charges, Résultat Net)
- **Fonctionnalités** :
  - `initialize()` : charge l'historique via `ServiceBilan.getHistorique(franchiseId)`
  - Bouton "Générer Bilan" : appelle `serviceBilan.genererBilan(mois, annee, franchiseId)` et rafraîchit le tableau
  - Boutons sidebar : navigation vers Dashboard, Budgets, Bilans

### 3. **DashboardSiegeController (mise à jour)**
- **Fichier** : `src/main/java/tn/esprit/Boussole/GUI/DashboardSiegeController.java`
- **Modifications** :
  - Ajout d'imports : `ActionEvent`, `FXMLLoader`, `Parent`, `Scene`, `Stage`, `IOException`
  - Ajout de `@FXML` pour les boutons sidebar : `btnDashboard`, `btnBudgets`, `btnBilans`
  - Configuration des listeners `setOnAction(...)` dans `initialize()` pour les 3 boutons
  - Ajout de la méthode `changerPage(ActionEvent, String)` pour la navigation

### 4. **DashboardSiege.fxml (mise à jour)**
- **Fichier** : `src/main/resources/tn/esprit/Boussole/GUI/DashboardSiege.fxml`
- **Modification** : ajout des `fx:id` aux boutons sidebar
  - `btnDashboard`, `btnBudgets`, `btnBilans`

---

## Méthode `changerPage(...)` — Code réutilisable

Cette méthode est implémentée dans les 3 contrôleurs et permet de naviguer entre les écrans FXML :

```java
private void changerPage(ActionEvent event, String fxmlPath) {
    try {
        // Charger le nouveau FXML
        URL fxmlUrl = getClass().getResource(fxmlPath);
        if (fxmlUrl == null) {
            System.err.println("Erreur : fichier FXML non trouvé : " + fxmlPath);
            return;
        }

        Parent root = FXMLLoader.load(fxmlUrl);
        Scene scene = new Scene(root);

        // Obtenir la stage actuelle depuis le bouton source et changer la scène
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Boussole - " + fxmlPath);
        stage.show();

    } catch (IOException e) {
        System.err.println("Erreur lors du chargement du FXML : " + e.getMessage());
        e.printStackTrace();
    } catch (Exception e) {
        System.err.println("Erreur inattendue lors du changement de page : " + e.getMessage());
        e.printStackTrace();
    }
}
```

### Comment l'utiliser dans un nouveau contrôleur :
```java
// Dans la méthode initialize()
btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"));
btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBudgets.fxml"));
btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBilans.fxml"));
```

---

## Architecture et flux de navigation

```
┌──────────────────────────────────────────────────────────────┐
│                     App.java (main)                           │
│                                                               │
│  launch(args) → FXMLLoader.load(DashboardSiege.fxml)         │
└────────────┬──────────────────────────────────────────────────┘
             │
             ├─→ DashboardSiege.fxml
             │   └─→ DashboardSiegeController
             │       ├─ btnDashboard → changerPage(...DashboardSiege.fxml)
             │       ├─ btnBudgets → changerPage(...GestionBudgets.fxml)
             │       └─ btnBilans → changerPage(...GestionBilans.fxml)
             │
             ├─→ GestionBudgets.fxml
             │   └─→ GestionBudgetsController
             │       ├─ btnDashboard → changerPage(...DashboardSiege.fxml)
             │       ├─ btnBudgets → changerPage(...GestionBudgets.fxml)
             │       ├─ btnBilans → changerPage(...GestionBilans.fxml)
             │       └─ btnSauvegarder → serviceBudget.add(budget)
             │
             └─→ GestionBilans.fxml
                 └─→ GestionBilansController
                     ├─ btnDashboard → changerPage(...DashboardSiege.fxml)
                     ├─ btnBudgets → changerPage(...GestionBudgets.fxml)
                     ├─ btnBilans → changerPage(...GestionBilans.fxml)
                     └─ btnGenererBilan → serviceBilan.genererBilan(...)
```

---

## Utilisation des services

### ServiceBudgetPrevisionnel
```java
ServiceBudgetPrevisionnel serviceBudget = new ServiceBudgetPrevisionnel();

// Charger tous les budgets pour une franchise
List<budget_previsionnel> budgets = serviceBudget.getAllByFranchise(franchiseId);

// Ajouter ou mettre à jour un budget
budget_previsionnel budget = new budget_previsionnel(10, 2025, 5000.0, 
    budget_previsionnel.TypeBudget.LIMITE_DEPENSE, "MARKETING", franchiseId);
serviceBudget.add(budget);
```

### ServiceBilan
```java
ServiceBilan serviceBilan = new ServiceBilan();

// Générer un bilan pour une période donnée
serviceBilan.genererBilan(mois, annee, franchiseId);

// Récupérer l'historique des bilans
List<bilan> bilans = serviceBilan.getHistorique(franchiseId);
```

---

## Notes importantes

1. **franchiseId hardcodé** : dans les contrôleurs, l'ID franchise est actuellement fixé à `1`. À adapter selon ton contexte (login utilisateur, session, etc.).

2. **Rafraîchissement du tableau** : après une action (sauvegarde, génération), le tableau est rafraîchi via :
   ```java
   tableBudgets.getItems().clear();
   List<budget_previsionnel> budgets = serviceBudget.getAllByFranchise(franchiseId);
   tableBudgets.getItems().addAll(budgets);
   ```

3. **Génération PDF** : la méthode `genererBilan()` actuellement génère un bilan en base de données. Pour exporter en PDF, il faudrait ajouter une dépendance (iText, Apache PDFBox, etc.) — non implémenté dans cette version.

4. **ComboBox Catégories** : les catégories sont actuellement des valeurs statiques ("MARKETING", "OPERATIONS", etc.). À adapter selon tes données réelles.

5. **Validation** : la saisie du montant n'a pas de validation sophistiquée. À améliorer pour des entiers/décimales valides.

---

## Prochaines étapes possibles

1. **Intégration authentification** : lier l'ID franchise au login utilisateur.
2. **Validation de formulaire** : améliorer la saisie des montants et dates.
3. **Génération PDF** : ajouter une dépendance et implémenter l'export.
4. **Persistance de l'année** : récupérer l'année courante ou permettre la sélection.
5. **Édition/suppression** : ajouter des colonnes Actions dans les TableView pour modifier/supprimer.
6. **Responsive design** : adapter les largeurs/hauteurs selon la taille de la fenêtre.

---

## Lancer l'application

```powershell
# Via Maven (recommandé)
mvn javafx:run

# Ou lancer App.java depuis IntelliJ avec VM options :
--module-path "C:\javafx-sdk-17.0.8\lib" --add-modules javafx.controls,javafx.fxml
```

Après lancement, tu peux naviguer entre les 3 écrans via les boutons de la sidebar.

