# Boussole — Plateforme de Gestion de Franchise

> Application desktop JavaFX pour la gestion centralisée d'un réseau de franchises. Développée dans le cadre d'un projet académique à **ESPRIT** (École Supérieure Privée d'Ingénierie et de Technologies).

---

## Table des matières

1. [Présentation du projet](#-présentation-du-projet)
2. [Architecture technique](#-architecture-technique)
3. [Prérequis & Installation](#-prérequis--installation)
4. [Structure du projet](#-structure-du-projet)
5. [Modules fonctionnels](#-modules-fonctionnels)
6. [Base de données](#-base-de-données)
7. [Intégrations & APIs externes](#-intégrations--apis-externes)
8. [Sécurité](#-sécurité)
9. [Lancement](#-lancement)
10. [Captures d'écran](#-captures-décran)
11. [Équipe](#-équipe)

---

## Présentation du projet

**Boussole** est une application de gestion complète dédiée aux réseaux de franchises. Elle permet au **Siège** (administration centrale) de superviser l'ensemble du réseau, et à chaque **Franchise** (entreprise locale) de gérer ses propres opérations financières, commerciales et logistiques.

### Fonctionnalités principales

| Rôle | Fonctionnalités |
|------|----------------|
| **Siège (ADMIN)** | Dashboard global, gestion des utilisateurs, entreprises, fournisseurs, charges, réclamations, catalogue produits, commandes reçues, bilans, budgets, alertes IA, carte des franchises |
| **Franchise (ENTREPRISE)** | Dashboard personnel, transactions (recettes/dépenses), historique financier, fournisseurs, charges, réclamations, marketplace, panier, commandes |

---

## Architecture technique

```
┌────────────────────────────────────────────────────┐
│                   PRÉSENTATION                      │
│           JavaFX 21 + FXML + CSS                    │
│         (Contrôleurs GUI + Vues FXML)               │
├────────────────────────────────────────────────────┤
│                  LOGIQUE MÉTIER                      │
│              Services (CRUD + Métier)                │
│         (ServiceTransaction, ChargeService, ...)     │
├────────────────────────────────────────────────────┤
│                   DONNÉES                            │
│           MySQL 8.4 via JDBC                         │
│          (MyBdConnexion - Singleton)                 │
├────────────────────────────────────────────────────┤
│               APIS EXTERNES                          │
│  Google OAuth2 │ Gemini AI │ Face++ │ Cloudinary     │
│  ExchangeRate  │ Géolocalisation  │ Email SMTP       │
└────────────────────────────────────────────────────┘
```

### Stack technologique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 17 |
| UI Framework | JavaFX | 21.0.2 |
| Build Tool | Maven | 3.x |
| Base de données | MySQL | 8.4.7 |
| Connecteur DB | MySQL Connector/J | 8.0.33 |
| Sérialisation | Jackson | 2.15.2 |
| HTTP Client | OkHttp | 4.12.0 |
| Tests | JUnit | 5.10.0 |
| Logging | SLF4J | 2.0.9 |

---

## ⚙ Prérequis & Installation

### Prérequis

- **JDK 17+** (Oracle ou OpenJDK)
- **Maven 3.6+**
- **MySQL 8.x** (WAMP / XAMPP / standalone)
- **IDE** : IntelliJ IDEA recommandé

### Installation

```bash
# 1. Cloner le dépôt
git clone https://github.com/z4n8q1x6/Boussole.git
cd Boussole

# 2. Créer la base de données MySQL
mysql -u root -e "CREATE DATABASE IF NOT EXISTS boussole;"
# Importer le schéma (si fichier SQL fourni)

# 3. Configurer les variables d'environnement
# Copier le fichier .env.example en .env et remplir les valeurs
cp .env.example .env

# 4. Compiler et lancer
mvn clean javafx:run
```

### Configuration `.env`

```env
# Base de données
DATABASE_URL=mysql://root:@localhost:3306/boussole

# API Keys
GEMINI_API_KEY=votre_clé_gemini
CLOUDINARY_URL=cloudinary://...
FACEPLUSPLUS_API_KEY=votre_clé
FACEPLUSPLUS_API_SECRET=votre_secret

# Google OAuth2
GOOGLE_CLIENT_ID=votre_client_id
GOOGLE_CLIENT_SECRET=votre_client_secret

# Email
EMAIL_USER=votre_email@gmail.com
EMAIL_PASSWORD=votre_app_password
```

---

## Structure du projet

```
Boussole/
├── pom.xml                          # Configuration Maven
├── .env                             # Variables d'environnement (non versionné)
├── .gitignore                       # Fichiers exclus de Git
│
└── src/main/
    ├── java/tn/esprit/boussole/
    │   ├── api/                     # Couche API externe
    │   │   ├── clients/             # Clients HTTP (Email, Géo)
    │   │   ├── models/              # DTOs API (Coordonnees, EmailRequest)
    │   │   └── services/            # Services API (Géolocalisation)
    │   │
    │   ├── gui/                     # Contrôleurs JavaFX
    │   │   ├── loginController.java           # Authentification (email + Google OAuth2)
    │   │   ├── forgotPasswordController.java  # Réinitialisation mot de passe
    │   │   ├── dashController.java            # Layout principal Siège
    │   │   ├── dashUserController.java        # Layout principal Franchise
    │   │   ├── DashboardSiegeController.java  # Dashboard admin (KPIs réseau)
    │   │   ├── DashboardFranchiseController.java  # Dashboard franchise (KPIs locaux)
    │   │   ├── JournalFranchiseController.java    # Historique des transactions
    │   │   ├── GestionBilansController.java       # Gestion des bilans financiers
    │   │   ├── GestionBudgetsController.java      # Gestion budgets prévisionnels
    │   │   ├── usersController.java               # CRUD utilisateurs
    │   │   ├── entrepriseController.java          # CRUD franchises/entreprises
    │   │   ├── afficherBackChargeController.java  # Charges côté Siège
    │   │   ├── afficherFrontChargeController.java # Charges côté Franchise
    │   │   ├── afficherBackFournisseurController.java  # Fournisseurs côté Siège
    │   │   ├── afficherFrontFournisseurController.java # Fournisseurs côté Franchise
    │   │   ├── AdminReclamationController.java    # Réclamations côté Siège
    │   │   ├── ReclamationController.java         # Réclamations côté Franchise
    │   │   ├── AdminAlerteIAController.java       # Alertes IA côté Siège
    │   │   ├── AlerteIAController.java            # Alertes IA côté Franchise
    │   │   ├── siege/                             # Contrôleurs spécifiques Siège
    │   │   │   ├── CarteFranchisesController.java     # Carte géographique des franchises
    │   │   │   ├── GestionCatalogueController.java    # Gestion catalogue produits
    │   │   │   └── CommandesRecuesController.java     # Gestion commandes reçues
    │   │   └── franchise/                         # Contrôleurs spécifiques Franchise
    │   │       ├── CatalogueController.java           # Navigation catalogue (marketplace)
    │   │       ├── PanierController.java               # Gestion du panier
    │   │       └── MesCommandesController.java         # Suivi des commandes
    │   │
    │   ├── models/                  # Entités / POJOs
    │   │   ├── user.java                    # Utilisateur (id, nom, email, rôle, face_token)
    │   │   ├── franchise.java               # Franchise (id, nom, solde_actuel, points)
    │   │   ├── transaction.java             # Transaction (RECETTE / DEPENSE)
    │   │   ├── Charge.java                  # Charge financière
    │   │   ├── Fournisseur.java             # Fournisseur
    │   │   ├── bilan.java                   # Bilan financier mensuel
    │   │   ├── budget_previsionnel.java     # Budget prévisionnel (OBJECTIF_REVENU / LIMITE_DEPENSE)
    │   │   ├── Reclamation.java             # Réclamation
    │   │   ├── AlerteIA.java                # Alerte générée par l'IA
    │   │   ├── AlertReport.java             # Rapport d'alerte
    │   │   ├── Produit.java                 # Produit du catalogue
    │   │   ├── Commande.java                # Commande
    │   │   ├── LigneCommande.java           # Ligne de commande
    │   │   ├── FranchiseData.java           # DTO données franchise
    │   │   └── StatutReclamation.java       # Enum statuts réclamation
    │   │
    │   ├── service/                 # Couche métier / DAO
    │   │   ├── crud.java                    # Interface CRUD générique
    │   │   ├── ServiceTransaction.java      # CRUD + calculs transactions
    │   │   ├── ChargeService.java           # CRUD charges
    │   │   ├── FournisseurService.java      # CRUD fournisseurs
    │   │   ├── ServiceBilan.java            # CRUD + export bilans
    │   │   ├── ServiceBudgetPrevisionnel.java # CRUD budgets
    │   │   ├── franchiseService.java        # CRUD franchises
    │   │   ├── userService.java             # CRUD utilisateurs
    │   │   ├── ReclamationService.java      # CRUD réclamations
    │   │   ├── AlerteIAService.java         # CRUD alertes IA
    │   │   ├── AlertReportService.java      # CRUD rapports alertes
    │   │   ├── ProduitService.java          # CRUD produits
    │   │   ├── CommandeService.java         # CRUD commandes
    │   │   ├── LigneCommandeService.java    # CRUD lignes commande
    │   │   ├── AuthService.java             # Authentification (hash, vérification)
    │   │   ├── ServiceDevise.java           # Conversion devises (API ExchangeRate)
    │   │   ├── CurrencyService.java         # Service monnaie
    │   │   ├── ServiceClustering.java       # Clustering / analyse données
    │   │   ├── ServiceQuickChart.java       # Génération de graphiques (QuickChart.io)
    │   │   ├── ServiceExportExcel.java      # Export Excel
    │   │   ├── ServiceEmail.java            # Envoi d'emails
    │   │   ├── EmailFournisseurService.java # Emails fournisseurs
    │   │   └── FacePlusPlusService.java     # Reconnaissance faciale (Face++)
    │   │
    │   ├── utils/                   # Utilitaires
    │   │   ├── MyBdConnexion.java           # Singleton connexion MySQL
    │   │   ├── Gemini.java                  # Client API Google Gemini (IA)
    │   │   ├── CloudUploader.java           # Upload images Cloudinary
    │   │   ├── EmailService.java            # Service SMTP Gmail
    │   │   ├── PDFGenerator.java            # Génération de rapports PDF
    │   │   ├── NotificationManager.java     # Notifications toast JavaFX
    │   │   ├── AlertUtil.java               # Utilitaires alertes JavaFX
    │   │   ├── DialogManager.java           # Gestion des dialogues
    │   │   ├── ThemeManager.java            # Gestion thème clair/sombre
    │   │   ├── ThemeManagerS.java           # Thème spécifique Siège
    │   │   ├── FinancialDataHelper.java     # Aide calculs financiers
    │   │   ├── PanierManager.java           # Singleton gestion panier
    │   │   ├── UserManager.java             # Gestion session utilisateur
    │   │   └── UIUtils.java                 # Utilitaires UI
    │   │
    │   └── test/                    # Tests unitaires
    │       ├── TestDB.java                  # Test connexion BDD
    │       ├── TestModuleC.java             # Tests module C
    │       └── test.java                    # Tests généraux
    │
    └── resources/                   # Ressources FXML + CSS + Images
        ├── login.fxml                       # Page de connexion
        ├── forgotPassword.fxml              # Réinitialisation mot de passe
        ├── dash.fxml                        # Layout Siège
        ├── dashUser.fxml                    # Layout Franchise
        ├── DashboardSiege.fxml              # Vue d'ensemble Siège
        ├── DashboardFranchise.fxml          # Tableau de bord Franchise
        ├── JournalFranchise.fxml            # Historique transactions
        ├── GestionBilans.fxml               # Bilans financiers
        ├── GestionBudgets.fxml              # Budgets prévisionnels
        ├── users.fxml / adduser.fxml        # Gestion utilisateurs
        ├── entreprise.fxml                  # Gestion entreprises
        ├── afficherBackCharge.fxml          # Charges (Back-office)
        ├── afficherFrontCharge.fxml         # Charges (Front-office)
        ├── afficherBackFournisseur.fxml     # Fournisseurs (Back)
        ├── afficherFrontFournisseur.fxml    # Fournisseurs (Front)
        ├── adminReclamation.fxml            # Réclamations (Admin)
        ├── reclamation.fxml                 # Réclamations (Franchise)
        ├── adminAlerteIA.fxml               # Alertes IA (Admin)
        ├── alerteIA.fxml                    # Alertes IA (Franchise)
        ├── alertReports.fxml                # Rapports d'alertes
        ├── CarteFranchisesView.fxml         # Carte géographique
        ├── GestionCatalogueView.fxml        # Catalogue produits
        ├── CommandesRecuesView.fxml         # Commandes reçues
        ├── CatalogueView.fxml               # Marketplace
        ├── PanierView.fxml                  # Panier
        ├── MesCommandesView.fxml            # Mes commandes
        ├── styles.css                       # Feuille de style principale
        ├── dash.css                         # Styles du dashboard
        ├── dark-table.css                   # Thème sombre des tableaux
        └── images/                          # Ressources graphiques
```

---

## Modules fonctionnels

### 1. Authentification & Sécurité

| Fonctionnalité | Description |
|----------------|-------------|
| Login classique | Email + mot de passe (hashé BCrypt) |
| Google OAuth2 | Connexion via compte Google (flux installed app, port 8888) |
| Reconnaissance faciale | Authentification biométrique via Face++ |
| Mot de passe oublié | Réinitialisation par email SMTP |
| Gestion des sessions | Stockage via `java.util.prefs.Preferences` |

### 2. Dashboard Siège — Vue d'ensemble

> Fichier : `DashboardSiegeController.java` → `DashboardSiege.fxml`

- **KPIs globaux** : Solde Total, Revenus, Dépenses (calculés depuis la table `transaction`)
- **Graphique comparatif** : Réel (Bénéfice Net) vs Budget Prévu (3 derniers mois)
- **Radar de Performance** : Analyse multi-critères des franchises
- **Classement Financier** : Ranking des franchises par performance
- **Assistant IA** : Chat intégré avec Google Gemini

### 3. Dashboard Franchise — Tableau de bord

> Fichier : `DashboardFranchiseController.java` → `DashboardFranchise.fxml`

- **Solde Disponible** : Lu directement depuis `franchises.solde_actuel`
- **Limite Dépenses** : Depuis `budget_previsionnel` (franchise spécifique)
- **Objectif Revenu** : Depuis `budget_previsionnel` (franchise spécifique)
- **Conversion devises** : EUR, USD, GBP, CAD en temps réel
- **Nouvelle Transaction** : Saisie rapide de recettes
- **Derniers Mouvements** : 5 dernières transactions + charges

### 4. Gestion Financière

| Module | Table SQL | Fonctionnalités |
|--------|-----------|-----------------|
| Transactions | `transaction` | CRUD recettes/dépenses par franchise |
| Charges | `charge` | Déclaration, validation, suivi des charges |
| Bilans | `bilan` | Rapports mensuels, export PDF/Excel, envoi par email |
| Budgets | `budget_previsionnel` | Objectifs revenus & limites dépenses par franchise/mois |
| Historique | `transaction` + `charge` | Journal filtrable, stats agrégées |

### 5. Gestion des Entités

| Module | Description |
|--------|-------------|
| Utilisateurs | CRUD complet, attribution de rôles (SIEGE / ENTREPRISE), activation/désactivation |
| Franchises | CRUD, suivi du solde, points de fidélité, géolocalisation sur carte |
| Fournisseurs | CRUD, envoi d'emails directs, affichage back-office et front-office |

### 6. Marketplace (E-commerce interne)

| Côté Siège | Côté Franchise |
|------------|---------------|
| Gestion du catalogue produits | Navigation catalogue & recherche |
| Upload images (Cloudinary) | Ajout au panier |
| Gestion des commandes reçues | Suivi de mes commandes |
| Traitement & expédition | Historique des achats |

### 7. Réclamations & Alertes

- **Réclamations** : Création, suivi, traitement (statuts : EN_ATTENTE, EN_COURS, RESOLUE)
- **Alertes IA** : Détection automatique d'anomalies financières via Gemini
- **Rapports d'alertes** : Historique et archivage des alertes déclenchées

---

## Base de données

### Schéma `boussole` — Tables principales

```
┌──────────────┐    ┌────────────────────┐    ┌──────────────┐
│  utilisateur  │───▶│     franchises      │◀───│  transaction  │
│  (id_user)    │    │  (id, solde_actuel) │    │  (RECETTE/   │
│  id_franchise │    │                     │    │   DEPENSE)   │
└──────────────┘    └────────────────────┘    └──────────────┘
                           │
                    ┌──────┴──────┐
              ┌─────▼────┐  ┌────▼────────────┐
              │  charge   │  │budget_previsionnel│
              │ (montant, │  │ (OBJECTIF_REVENU │
              │  titre)   │  │  LIMITE_DEPENSE) │
              └──────────┘  └─────────────────┘
```

| Table | Description | Colonnes clés |
|-------|-------------|---------------|
| `utilisateur` | Comptes utilisateurs | `id_user`, `email`, `mot_de_passe`, `role`, `id_franchise`, `face_token`, `google_id` |
| `franchises` | Entreprises du réseau | `id`, `nom`, `email`, `solde_actuel`, `points_fidelite`, `actif` |
| `transaction` | Mouvements financiers | `id`, `date`, `montant`, `type` (RECETTE/DEPENSE), `description`, `franchise_id` |
| `charge` | Charges déclarées | `id`, `titre`, `montant`, `date_charge`, `type`, `status_validation`, `franchise_id` |
| `bilan` | Bilans mensuels | `id`, `mois`, `annee`, `total_recettes`, `total_depenses`, `franchise_id` |
| `budget_previsionnel` | Budgets cibles | `id`, `mois`, `annee`, `montant_cible`, `type_budget`, `franchise_id` |
| `fournisseur` | Fournisseurs | `id`, `nom`, `email`, `telephone`, `adresse` |
| `reclamations` | Réclamations | `id`, `sujet`, `description`, `statut`, `franchise_id` |
| `alerteias` | Alertes IA | `id`, `type`, `message`, `franchise_id` |
| `produit` | Catalogue produits | `id`, `nom`, `prix`, `description`, `image_url`, `categorie` |
| `commande` | Commandes | `id`, `date`, `statut`, `total`, `franchise_id` |
| `ligne_commande` | Détails commande | `id`, `commande_id`, `produit_id`, `quantite`, `prix_unitaire` |

### Connexion

```java
// Singleton — MyBdConnexion.java
String url = "jdbc:mysql://localhost:3306/boussole";
String user = "root";
String password = "";
```

---

## 🔌 Intégrations & APIs externes

| API | Usage | Fichier |
|-----|-------|---------|
| **Google OAuth2** | Authentification sociale | `loginController.java` |
| **Google Gemini** | Assistant IA, analyse financière, alertes intelligentes | `Gemini.java` |
| **Face++** | Reconnaissance faciale pour login biométrique | `FacePlusPlusService.java` |
| **Cloudinary** | Hébergement d'images produits et preuves de charges | `CloudUploader.java` |
| **ExchangeRate API** | Conversion de devises en temps réel (TND → EUR/USD/GBP) | `ServiceDevise.java` |
| **QuickChart.io** | Génération de graphiques pour rapports email | `ServiceQuickChart.java` |
| **SMTP Gmail** | Envoi d'emails (bilans, alertes, fournisseurs) | `EmailService.java` |
| **API Géolocalisation** | Carte interactive des franchises | `GeolocalisationService.java` |

---

## Sécurité

- **Hashage des mots de passe** : BCrypt (via `AuthService.java`)
- **Secrets externalisés** : Variables sensibles dans `.env` (non versionné via `.gitignore`)
- **Google OAuth2** : Flux "installed application" avec serveur local temporaire (port 8888)
- **Protection GitHub** : Push Protection activée contre la fuite de secrets
- **Sessions** : Gérées via `java.util.prefs.Preferences` (stockage local sécurisé)
- **Validation des entrées** : Contrôle côté contrôleur avant insertion en BDD

---

## Lancement

### Depuis l'IDE (IntelliJ IDEA)

1. Ouvrir le projet dans IntelliJ
2. S'assurer que le JDK 17 est configuré
3. Exécuter la classe `Main` ou la configuration Maven `javafx:run`

### Depuis le terminal

```bash
mvn clean javafx:run
```

### Rôles et accès

| Rôle | Accès | Exemple d'identifiant |
|------|-------|-----------------------|
| **SIEGE** (Admin) | Dashboard global, gestion complète du réseau | `admin@gmail.com` |
| **ENTREPRISE** (Franchise) | Dashboard local, transactions, marketplace | `siwar.raouafi1@gmail.com` |

---

## Captures d'écran

### Page de connexion
- Login par email/mot de passe
- Bouton Google OAuth2
- Reconnaissance faciale Face++
- Lien "Mot de passe oublié"

### Dashboard Siège (Vue d'ensemble)
- KPIs : Solde Total, Revenus, Dépenses
- Graphique Réel vs Budget (3 derniers mois)
- Onglets : Statistiques Globales, Radar de Performance, Classement Financier, Assistant IA

### Dashboard Franchise (Tableau de bord)
- KPIs : Solde Disponible, Limite Dépenses, Objectif Revenu
- Conversion devises en temps réel
- Formulaire nouvelle transaction
- Tableau des derniers mouvements
