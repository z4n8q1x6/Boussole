# Boussole - Application de pilotage financier

Ce projet est une application JavaFX simple pour le pilotage financier.

Prérequis
- JDK 17+ installé
- Maven (pour build / exécution via plugin JavaFX) ou SDK JavaFX si tu veux lancer manuellement
- Base de données MySQL locale (configurée dans `MyBDConnexion`)

Exécution avec Maven (recommandé si tu as Maven installé):

```powershell
# Depuis le répertoire du projet
mvn clean javafx:run
```

Si Maven n'est pas installé, tu peux exécuter avec `java` en spécifiant le module-path vers le SDK JavaFX installé :

```powershell
# Exemple (adapte le chemin vers ton SDK JavaFX)
$env:PATH_TO_FX = 'C:\path\to\javafx-sdk-17\lib'
javac -d out -cp src\main\resources src\main\java\tn\esprit\Boussole\App.java
java --module-path $env:PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp out tn.esprit.Boussole.App
```

Notes
- Le fichier FXML `DashboardSiege.fxml` a été placé dans `src/main/resources/tn/esprit/Boussole/GUI` pour être trouvé par `App` via `getResource("/tn/esprit/Boussole/GUI/DashboardSiege.fxml")`.
- Si l'IDE signale des erreurs liées à JavaFX, vérifie que les dépendances OpenJFX sont bien présentes dans le `pom.xml` ou que le SDK JavaFX est configuré dans les libraries de ton IDE.
- La configuration de la base de données se trouve dans `tn.esprit.Boussole.Utilis.MyBDConnexion`.

Si tu veux, je peux :
- Ajouter un contrôleur `DashboardSiegeController` et préremplir des données pour les KPI et le BarChart.
- Ajouter des bindings pour rafraîchir les KPI depuis les services.
- T'aider à configurer Maven / IntelliJ pour JavaFX (facilement réalisable).

