# Méthode utilitaire `changerPage(...)` — Guide d'intégration

## Description

La méthode `changerPage(ActionEvent event, String fxmlPath)` est une méthode **réutilisable** dans tous les contrôleurs JavaFX pour naviguer entre les écrans FXML de l'application Boussole.

---

## Code complet à copier-coller

```java
/**
 * Méthode utilitaire pour changer de page (navigation entre écrans FXML).
 * À utiliser comme handler de bouton : btnNavigation.setOnAction(event -> changerPage(event, "/chemin/vers/Page.fxml"));
 *
 * @param event ActionEvent du bouton cliqué
 * @param fxmlPath chemin absolu de la ressource FXML, ex: "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"
 */
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

---

## Imports requis (ajouter à l'en-tête du contrôleur)

```java
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
```

---

## Intégration dans une classe contrôleur

### Exemple 1 : Dans `initialize()` (recommandé)

```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    // ... autres initialisations ...

    // Configurer les boutons de navigation
    btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"));
    btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBudgets.fxml"));
    btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBilans.fxml"));
}
```

### Exemple 2 : Avec une méthode nommée dans le FXML (alternative)

**FXML** :
```xml
<Button text="Aller au Dashboard" onAction="#allerAuDashboard" />
```

**Contrôleur** :
```java
@FXML
public void allerAuDashboard(ActionEvent event) {
    changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml");
}
```

### Exemple 3 : Navigation depuis un événement personnalisé

```java
// Dans une méthode quelconque (ex: après sauvegarde réussie)
private void sauvegarderEtRetourner(Button sourceButton) {
    try {
        // Effectuer la sauvegarde
        serviceBudget.add(budget);
        
        // Créer un événement ActionEvent simulé et naviguer
        ActionEvent event = new ActionEvent(sourceButton, sourceButton);
        changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml");
    } catch (Exception e) {
        System.out.println("Erreur : " + e.getMessage());
    }
}
```

---

## Chemins FXML valides (à utiliser)

| Écran | Chemin FXML |
|-------|-------------|
| Dashboard | `/tn/esprit/Boussole/GUI/DashboardSiege.fxml` |
| Gestion Budgets | `/tn/esprit/Boussole/GUI/GestionBudgets.fxml` |
| Gestion Bilans | `/tn/esprit/Boussole/GUI/GestionBilans.fxml` |

---

## Gestion d'erreurs

La méthode inclut plusieurs niveaux de gestion d'erreurs :

1. **Vérification d'existence du fichier FXML**
   ```java
   URL fxmlUrl = getClass().getResource(fxmlPath);
   if (fxmlUrl == null) {
       System.err.println("Erreur : fichier FXML non trouvé : " + fxmlPath);
       return;
   }
   ```

2. **Gestion des IOException** (erreur de chargement du fichier)
   ```java
   } catch (IOException e) {
       System.err.println("Erreur lors du chargement du FXML : " + e.getMessage());
       e.printStackTrace();
   }
   ```

3. **Gestion des exceptions inattendues**
   ```java
   } catch (Exception e) {
       System.err.println("Erreur inattendue lors du changement de page : " + e.getMessage());
       e.printStackTrace();
   }
   ```

---

## Conseils et bonnes pratiques

### ✅ À faire

1. **Utiliser des chemins absolus** (commençant par `/`) pour éviter les ambiguïtés :
   ```java
   changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"); // ✅ BON
   ```

2. **Vérifier que les @FXML fx:id des boutons existent**
   ```xml
   <Button fx:id="btnDashboard" ... />  <!-- ✅ Doit correspondre à la variable @FXML du contrôleur -->
   ```

3. **Initialiser les listeners dans `initialize()`** pour éviter les NullPointerException
   ```java
   @Override
   public void initialize(URL location, ResourceBundle resources) {
       if (btnDashboard != null) {
           btnDashboard.setOnAction(event -> changerPage(event, "..."));
       }
   }
   ```

### ❌ À éviter

1. **Utiliser des chemins relatifs** (sans `/`)
   ```java
   changerPage(event, "DashboardSiege.fxml"); // ❌ MAUVAIS
   ```

2. **Oublier de déclarer les boutons avec @FXML**
   ```java
   private Button btnDashboard;  // ❌ Injection non faite
   
   @FXML
   private Button btnDashboard;  // ✅ Injection correcte
   ```

3. **Faire la navigation en dehors de `initialize()`** sans vérification nullité
   ```java
   btnDashboard.setOnAction(event -> ...);  // ❌ Risque NullPointerException si btnDashboard est null
   ```

---

## Exemple complet : ajout dans un nouveau contrôleur

**Fichier** : `MonNouveauControleur.java`

```java
package tn.esprit.Boussole.GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MonNouveauControleur implements Initializable {

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnBudgets;

    @FXML
    private Button btnBilans;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurer les boutons de navigation
        btnDashboard.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml"));
        btnBudgets.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBudgets.fxml"));
        btnBilans.setOnAction(event -> changerPage(event, "/tn/esprit/Boussole/GUI/GestionBilans.fxml"));
    }

    /**
     * Méthode utilitaire pour changer de page.
     */
    private void changerPage(ActionEvent event, String fxmlPath) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Erreur : fichier FXML non trouvé : " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root);

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
}
```

---

## FAQ

**Q : Où dois-je placer les lignes `btnDashboard.setOnAction(...)`?**  
**A :** Dans la méthode `initialize()` après avoir instancié/reçu les services et configuré les autres éléments d'interface.

**Q : Puis-je appeler `changerPage()` depuis d'autres méthodes?**  
**A :** Oui, il suffit d'avoir un `ActionEvent` valide. Si tu n'en as pas, crée un événement simulé :
```java
ActionEvent event = new ActionEvent(btnDashboard, btnDashboard);
changerPage(event, "/tn/esprit/Boussole/GUI/DashboardSiege.fxml");
```

**Q : Comment puis-je passer des paramètres à la page suivante?**  
**A :** Il faudrait utiliser un pattern Singleton ou une classe de contexte partagé. Non implémenté actuellement; demande si tu as besoin de cette fonctionnalité.

**Q : Les données non sauvegardées sont-elles perdues lors du changement de page?**  
**A :** Oui, actuellement aucune sauvegarde automatique. À améliorer si critiques.

---

## Résumé rapide

- **Copie la méthode** `changerPage(...)` dans chaque contrôleur qui en a besoin.
- **Ajoute les imports** au début du fichier.
- **Configure les listeners** dans `initialize()` avec `btnX.setOnAction(event -> changerPage(event, "...")`.
- **Utilise les chemins absolus** (commençant par `/`) pour les FXML.

C'est tout ! Navigation seamless entre tes écrans JavaFX. 🚀

