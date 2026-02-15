# 🚀 Guide de test — Améliorations GestionBudgets et GestionBilans

## Avant de commencer

1. **Compile le projet** : `Build > Rebuild Project` (IntelliJ)
2. **Lance l'application** : `mvn javafx:run` ou via `App.java` avec VM options

---

## ✅ Scénario de test GestionBudgets (Logique Dynamique)

### Étape 1 : Naviguer vers Gestion Budgets
- Depuis le Dashboard, clique sur le bouton "Budgets" (sidebar gauche)
- Tu dois voir l'écran "Stratégie Budgétaire"

### Étape 2 : Tester LIMITE_DEPENSE
1. **Dans le formulaire** :
   - Mois : choisir "2" (février)
   - **Type de Budget** : sélectionner "LIMITE_DEPENSE"
   
2. **Observer le comportement** :
   - La ComboBox "Catégorie" doit se remplir automatiquement avec :
     - ✅ CHARGES_EXPLOITATIONS
     - ✅ CHARGES_FINANCIERES
     - ✅ CHARGES_EXCEPTIONNELLES
   - Le champ Catégorie reste **activé** (pas grisé)

3. **Compléter et sauvegarder** :
   - Catégorie : "CHARGES_EXPLOITATIONS"
   - Montant : "5000"
   - Clique "Sauvegarder"
   
4. **Vérifier la table** :
   - Un budget doit apparaître avec :
     - Mois: 2
     - Année: 2026
     - **Type: LIMITE_DEPENSE** (nouvelle colonne)
     - Catégorie: CHARGES_EXPLOITATIONS
     - Montant: 5000.00

### Étape 3 : Tester OBJECTIF_REVENU
1. **Dans le formulaire** :
   - Mois : choisir "3" (mars)
   - **Type de Budget** : sélectionner "OBJECTIF_REVENU"
   
2. **Observer le comportement** :
   - La ComboBox "Catégorie" doit afficher uniquement "GLOBAL"
   - Le champ Catégorie doit être **grisé** (disabled)

3. **Compléter et sauvegarder** :
   - Montant : "50000" (objectif revenu du mois)
   - Clique "Sauvegarder"
   
4. **Vérifier la table** :
   - Un budget doit apparaître avec :
     - Mois: 3
     - Année: 2026
     - **Type: OBJECTIF_REVENU**
     - Catégorie: GLOBAL
     - Montant: 50000.00

### Résultat attendu
| Mois | Type | Catégorie | Montant |
|------|------|-----------|---------|
| 2 | LIMITE_DEPENSE | CHARGES_EXPLOITATIONS | 5000.00 |
| 3 | OBJECTIF_REVENU | GLOBAL | 50000.00 |

✅ **Test réussi** si :
- Les catégories changent automatiquement
- OBJECTIF_REVENU désactive la ComboBox
- Les budgets sont sauvegardés avec le bon type

---

## ✅ Scénario de test GestionBilans (CRUD Complet)

### Étape 1 : Naviguer vers Gestion Bilans
- Depuis le Dashboard ou GestionBudgets, clique sur "Bilans" (sidebar gauche)
- Tu dois voir l'écran "Reporting Financier"

### Étape 2 : Générer un bilan (optionnel)
1. Clique sur le bouton "📊 Générer Bilan Mensuel"
2. Un bilan pour février 2026 doit être créé (s'il n'existe pas)
3. Tu dois voir une ligne dans la table "Historique des Bilans"

### Étape 3 : Tester le bouton Modifier (✏️)
1. **Dans la table "Historique des Bilans"** :
   - Clique sur le bouton **✏️** (crayon) d'une ligne

2. **Un Dialog doit s'ouvrir** :
   - Titre : "Modifier le Bilan"
   - Sous-titre : "Modifier les totaux du bilan pour [mois]/[année]"
   - Deux champs : "Total Recettes (TND)" et "Total Charges (TND)"
   - Boutons : "OK" et "Annuler"

3. **Modifier les valeurs** :
   - Change les valeurs (ex: Recettes 10000, Charges 3000)
   - Clique "OK"

4. **Vérifier le résultat** :
   - La table se rafraîchit automatiquement
   - Les nouvelles valeurs apparaissent
   - Le "Résultat Net" doit se recalculer : 10000 - 3000 = 7000

### Étape 4 : Tester le bouton Supprimer (🗑️)
1. **Dans la table "Historique des Bilans"** :
   - Clique sur le bouton **🗑️** (poubelle) d'une ligne

2. **Comportement attendu** :
   - La ligne est supprimée immédiatement
   - La table se rafraîchit
   - La base de données est mise à jour

### Étape 5 : Tester la génération + modification
1. Génère un nouveau bilan ("📊 Générer Bilan Mensuel")
2. Immédiatement après, modifie ses valeurs (bouton ✏️)
3. Supprime le bilan (bouton 🗑️)

### Résultat attendu
| Action | Comportement |
|--------|-------------|
| Clic ✏️ | Dialog s'ouvre avec valeurs actuelles |
| Modification + OK | Table se met à jour, résultat_net recalculé |
| Clic 🗑️ | Ligne supprimée, table rafraîchie |
| Annuler dans Dialog | Aucun changement, Dialog ferme |

✅ **Test réussi** si :
- Dialog s'ouvre et se ferme correctement
- Les modifications sont sauvegardées en BD
- La suppression fonctionne
- La table se rafraîchit après chaque action
- Résultat net = Recettes - Charges (calcul automatique)

---

## 🔍 Points de vérification clés

### GestionBudgets
- [ ] Type de Budget dans la table
- [ ] Catégories dynamiques pour LIMITE_DEPENSE
- [ ] GLOBAL uniquement pour OBJECTIF_REVENU
- [ ] Catégorie désactivée pour OBJECTIF_REVENU
- [ ] Sauvegarde correcte avec le bon type

### GestionBilans
- [ ] Colonne Actions visible (✏️ et 🗑️)
- [ ] Dialog s'ouvre avec les bonnes données
- [ ] Résultat Net = Recettes - Charges
- [ ] Table rafraîchie après modification
- [ ] Table rafraîchie après suppression
- [ ] Bouton OK/Annuler dans Dialog fonctionne

---

## 🐛 Dépannage

### GestionBudgets : Catégories ne changent pas
**Problème** : Tu sélectionnes LIMITE_DEPENSE mais les catégories ne s'affichent pas.
**Cause** : TypeCharge enum non trouvé.
**Solution** : Vérifie que `tn.esprit.Boussole.Models.TypeCharge` existe :
```bash
# Cherche le fichier
find src/ -name "*TypeCharge*"
```

### GestionBilans : Dialog ne s'ouvre pas
**Problème** : Clique sur ✏️ mais rien ne se passe.
**Cause** : Exception silencieuse (vérifier console).
**Solution** : Regarde la console pour l'erreur exacte, puis ouvre une Issue.

### GestionBilans : Résultat Net ne se recalcule pas
**Problème** : Après modification, Résultat Net = Recettes - Charges ne s'affiche pas.
**Cause** : Peut être un problème d'affichage TableView (données mises à jour mais pas affichées).
**Solution** : Relance l'app ou clique sur une autre ligne puis reviens.

### Tableau ne se rafraîchit pas
**Problème** : Après sauvegarder/supprimer, la ligne n'apparaît pas/disparaît pas.
**Cause** : Exception non catchée.
**Solution** : Vérife la console pour les erreurs SQL.

---

## 📋 Checklist avant livraison

- [ ] Compilation sans erreurs bloquantes
- [ ] GestionBudgets : catégories dynamiques fonctionnelles
- [ ] GestionBudgets : colonne Type affiche correctement
- [ ] GestionBilans : Dialog Modifier fonctionne
- [ ] GestionBilans : Suppression fonctionne
- [ ] GestionBilans : Résultat Net se recalcule
- [ ] Navigation (boutons sidebar) fonctionne partout
- [ ] Pas de crash lors du changement de page

---

## 🎯 Notes importantes

- **FRANCHISE_ID hardcodé** : actuellement 1. À adapter si besoin.
- **Année courante** : fixée à 2026 dans GestionBudgets. À adapter dynamiquement si besoin.
- **Validation** : minimaliste. À améliorer pour production (AlertDialog d'erreur, etc.).
- **Imports de TypeCharge** : assure-toi que l'enum existe dans le projet.

---

**Bon testing ! 🚀 N'hésite pas à signaler tout bug ou amélioration à ajouter.**

