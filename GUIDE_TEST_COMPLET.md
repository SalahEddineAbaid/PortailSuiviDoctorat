# Guide de Test Complet - Application de Gestion des Thèses

## 🔧 Corrections Effectuées

### Backend

1. **JWT Configuration** - Tous les services utilisent maintenant le même secret JWT

   - `notification-service/application.properties`
   - `inscription-service/application.properties`
   - `batch-service/application.properties`

2. **inscription-service**

   - Correction du format des rôles (ROLE_DOCTORANT → DOCTORANT)
   - Correction du UserServiceClient pour utiliser les bons endpoints
   - Correction du UserDTO pour mapper correctement les champs

3. **notification-service**
   - Ajout de l'endpoint `/api/notifications/user/{userId}/unread`
   - Ajout des champs `userId` et `lu` à l'entité Notification

### Frontend

- Correction de l'utilisation de `getCurrentUser()` (Observable vs synchrone)
- Correction du format des rôles dans les comparaisons
- Amélioration de la gestion des erreurs dans dashboard.service.ts

---

## 🚀 Étapes pour Démarrer l'Application

### Étape 1: Démarrer les Services Infrastructure

```bash
# 1. MariaDB (doit être déjà en cours)
# 2. Kafka (si utilisé)
docker-compose -f kafka-docker-compose.yml up -d
```

### Étape 2: Démarrer les Microservices (dans l'ordre)

#### Terminal 1 - Eureka Server

```bash
cd eureka-server
mvnw spring-boot:run
```

Attendre que Eureka soit prêt sur http://localhost:8761

#### Terminal 2 - Config Server (optionnel)

```bash
cd config-server
mvnw spring-boot:run
```

#### Terminal 3 - Gateway Service

```bash
cd gateway-service
mvnw spring-boot:run
```

Port: 8080

#### Terminal 4 - User Service

```bash
cd user-service
mvnw spring-boot:run
```

Port: 8081

#### Terminal 5 - Inscription Service

```bash
cd inscription-service
mvnw spring-boot:run
```

Port: 8084

#### Terminal 6 - Notification Service

```bash
cd notification-service
mvnw spring-boot:run
```

Port: 8086

#### Terminal 7 - Defense Service (optionnel)

```bash
cd defense-service
mvnw spring-boot:run
```

Port: 8083

### Étape 3: Démarrer le Frontend

```bash
cd frontend
npm install
ng serve
```

URL: http://localhost:4200

---

## 🧪 Tests Fonctionnels

### 1. Authentification

- [ ] **Connexion Doctorant**
  - Email: (utilisateur existant avec ROLE_DOCTORANT)
  - Vérifier la redirection vers `/dashboard/doctorant`
- [ ] **Connexion Directeur**
  - Email: (utilisateur existant avec ROLE_DIRECTEUR)
  - Vérifier la redirection vers `/dashboard/directeur`
- [ ] **Connexion Admin**

  - Email: (utilisateur existant avec ROLE_ADMIN)
  - Vérifier la redirection vers `/dashboard/admin`

- [ ] **Déconnexion**
  - Cliquer sur le bouton de déconnexion
  - Vérifier la redirection vers `/login`

### 2. Dashboard Doctorant

- [ ] Affichage des statistiques (inscriptions totales, validées, en attente)
- [ ] Affichage de la progression de thèse
- [ ] Actions rapides fonctionnelles
- [ ] Notifications affichées

### 3. Module Inscription

- [ ] **Nouvelle Inscription**
  - Cliquer sur "Nouvelle inscription"
  - Remplir le formulaire étape par étape
  - Uploader les documents requis
  - Soumettre l'inscription
- [ ] **Liste des Inscriptions**

  - Voir toutes les inscriptions
  - Filtrer par statut
  - Voir les détails d'une inscription

- [ ] **Réinscription**
  - Accessible si une inscription validée existe
  - Pré-remplissage des données

### 4. Module Soutenance

- [ ] Liste des soutenances
- [ ] Détails d'une soutenance
- [ ] Formulaire de soutenance (si applicable)

### 5. Module Notifications

- [ ] Affichage des notifications non lues
- [ ] Marquer comme lu
- [ ] Liste complète des notifications

### 6. Profil Utilisateur

- [ ] Affichage des informations
- [ ] Modification du profil
- [ ] Changement de mot de passe

### 7. Administration (ROLE_ADMIN)

- [ ] **Gestion des Utilisateurs**
  - Liste des utilisateurs
  - Activer/Désactiver un utilisateur
  - Modifier les rôles
- [ ] **Gestion des Campagnes**
  - Créer une campagne
  - Modifier une campagne
  - Activer/Désactiver
- [ ] **Validation des Dossiers**
  - Liste des dossiers en attente
  - Valider/Rejeter un dossier

---

## 🔍 Vérification des Services

### Eureka Dashboard

URL: http://localhost:8761

- Vérifier que tous les services sont enregistrés:
  - USER-SERVICE
  - GATEWAY-SERVICE
  - INSCRIPTION-SERVICE
  - NOTIFICATION-SERVICE
  - DEFENSE-SERVICE (optionnel)
  - BATCH-SERVICE (optionnel)

### API Gateway Health

```bash
curl http://localhost:8080/actuator/health
```

### User Service Health

```bash
curl http://localhost:8081/actuator/health
```

---

## 🐛 Dépannage

### Erreur 401 Unauthorized

- Vérifier que le token JWT est valide
- Vérifier que tous les services utilisent le même secret JWT
- Redémarrer les services après modification de la config

### Erreur 500 Internal Server Error

- Vérifier les logs du service concerné
- Vérifier la connexion à la base de données
- Vérifier que les tables sont créées

### Erreur CORS

- Vérifier la configuration CORS dans gateway-service
- Vérifier que le frontend utilise le bon port (8080 pour l'API)

### Service non trouvé dans Eureka

- Vérifier que le service est démarré
- Vérifier la configuration Eureka dans application.properties/yml
- Attendre quelques secondes pour l'enregistrement

---

## 📝 Comptes de Test (Créés automatiquement)

Les comptes suivants sont créés automatiquement au démarrage du user-service :

| Rôle          | Email             | Mot de passe    |
| ------------- | ----------------- | --------------- |
| **Admin**     | admin@emsi.ma     | Admin@2025!     |
| **Directeur** | directeur@emsi.ma | Directeur@2025! |
| **Doctorant** | doctorant@emsi.ma | Doctorant@2025! |

---

## ✅ Checklist Finale

- [ ] Tous les services démarrés et enregistrés dans Eureka
- [ ] Frontend accessible sur http://localhost:4200
- [ ] Connexion fonctionnelle pour tous les rôles
- [ ] Dashboard affiché correctement
- [ ] Navigation entre les modules fonctionnelle
- [ ] Pas d'erreurs dans la console du navigateur
- [ ] Pas d'erreurs critiques dans les logs des services
