# Guide de Test Frontend - Plateforme de Gestion des Thèses

## 📋 Table des matières

1. [Prérequis](#prérequis)
2. [Configuration des bases de données](#configuration-des-bases-de-données)
3. [Configuration des services backend](#configuration-des-services-backend)
4. [Démarrage des services](#démarrage-des-services)
5. [Configuration et démarrage du frontend](#configuration-et-démarrage-du-frontend)
6. [Scénarios de test](#scénarios-de-test)
7. [Checklist de validation](#checklist-de-validation)
8. [Résolution des problèmes](#résolution-des-problèmes)

---

## 🔧 Prérequis

### Logiciels requis

- **Java 17** ou supérieur
- **Maven 3.8+**
- **Node.js 18+** et **npm 9+**
- **MariaDB 10.6+** ou **MySQL 8.0+**
- **Redis** (pour le gateway)
- **Kafka** (pour les notifications)
- **Git**

### Vérification des versions

```bash
java -version
mvn -version
node -version
npm -version
mysql --version
redis-cli --version
```

---

## 🗄️ Configuration des bases de données

### 1. Créer les bases de données

#### Pour inscription-service

```bash
# Se connecter à MariaDB/MySQL
mysql -u root -p

# Exécuter le script
source inscription-service/database-setup.sql
```

#### Pour notification-service

```bash
# Se connecter à MariaDB/MySQL
mysql -u root -p

# Exécuter le script
source notification-service/database-setup.sql
```

### 2. Vérifier les bases de données créées

```sql
SHOW DATABASES;
USE inscription_db;
SHOW TABLES;
USE notification_db;
SHOW TABLES;
```

### 3. Créer un utilisateur dédié (recommandé)

```sql
CREATE USER 'thesis_user'@'localhost' IDENTIFIED BY 'thesis_password';
GRANT ALL PRIVILEGES ON inscription_db.* TO 'thesis_user'@'localhost';
GRANT ALL PRIVILEGES ON notification_db.* TO 'thesis_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## ⚙️ Configuration des services backend

### 1. Configuration d'inscription-service

Créer/modifier `inscription-service/src/main/resources/application.properties` :

```properties
spring.application.name=inscription-service
server.port=8084

# Database Configuration
spring.datasource.url=jdbc:mariadb://localhost:3306/inscription_db
spring.datasource.username=thesis_user
spring.datasource.password=thesis_password
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect

# Eureka Client
eureka.client.service-url.defaultZone=http://eureka:eureka123@localhost:8761/eureka/
eureka.instance.prefer-ip-address=true

# JWT Configuration
jwt.secret=your-secret-key-change-this-in-production-min-256-bits
jwt.expiration=86400000

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
upload.path=./uploads/inscriptions

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

### 2. Configuration de notification-service

Créer/modifier `notification-service/src/main/resources/application.properties` :

```properties
spring.application.name=notification-service
server.port=8086

# Database Configuration
spring.datasource.url=jdbc:mariadb://localhost:3306/notification_db
spring.datasource.username=thesis_user
spring.datasource.password=thesis_password
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect

# Eureka Client
eureka.client.service-url.defaultZone=http://eureka:eureka123@localhost:8761/eureka/
eureka.instance.prefer-ip-address=true

# JWT Configuration
jwt.secret=your-secret-key-change-this-in-production-min-256-bits
jwt.expiration=86400000

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*

# Mail Configuration (à adapter selon votre serveur SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

### 3. Vérifier les autres services

Assurez-vous que les configurations suivantes sont présentes :

- **user-service** : port 8081
- **defense-service** : port 8082
- **batch-service** : port 8085
- **gateway-service** : port 8080
- **eureka-server** : port 8761

---

## 🚀 Démarrage des services

### Ordre de démarrage recommandé

#### 1. Démarrer Eureka Server

```bash
cd eureka-server
mvn clean install
mvn spring-boot:run
```

Vérifier : http://localhost:8761

#### 2. Démarrer Config Server (si utilisé)

```bash
cd config-server
mvn clean install
mvn spring-boot:run
```

#### 3. Démarrer les services métier (dans des terminaux séparés)

**User Service**

```bash
cd user-service
mvn clean install
mvn spring-boot:run
```

**Inscription Service**

```bash
cd inscription-service
mvn clean install
mvn spring-boot:run
```

**Notification Service**

```bash
cd notification-service
mvn clean install
mvn spring-boot:run
```

**Defense Service**

```bash
cd defense-service
mvn clean install
mvn spring-boot:run
```

**Batch Service**

```bash
cd batch-service
mvn clean install
mvn spring-boot:run
```

#### 4. Démarrer Gateway Service

```bash
cd gateway-service
mvn clean install
mvn spring-boot:run
```

### Vérification des services

Vérifier que tous les services sont enregistrés dans Eureka :

- Ouvrir http://localhost:8761
- Vérifier que tous les services apparaissent dans "Instances currently registered with Eureka"

---

## 🎨 Configuration et démarrage du frontend

### 1. Installation des dépendances

```bash
cd frontend
npm install
```

### 2. Vérifier la configuration

Fichier `frontend/src/app/environments/environment.ts` :

```typescript
export const environment = {
  production: false,
  apiUrl: "http://localhost:8080/api", // Via Gateway
  tokenKey: "accessToken",
  refreshTokenKey: "refreshToken",
  apiTimeout: 30000,
  features: {
    registration: true,
    forgotPassword: true,
    emailVerification: false,
  },
  debug: true,
  logLevel: "debug",
};
```

### 3. Démarrer le serveur de développement

```bash
npm start
```

L'application sera accessible sur : http://localhost:4200

---

## 🧪 Scénarios de test

### Scénario 1 : Authentification

#### Test 1.1 : Inscription d'un nouvel utilisateur

1. Accéder à http://localhost:4200
2. Cliquer sur "S'inscrire"
3. Remplir le formulaire :
   - Nom : Test
   - Prénom : User
   - Email : test@example.com
   - Mot de passe : Test123!
   - Rôle : DOCTORANT
4. Soumettre le formulaire
5. ✅ Vérifier : Message de succès + redirection vers login

#### Test 1.2 : Connexion

1. Sur la page de login
2. Saisir : test@example.com / Test123!
3. Cliquer sur "Se connecter"
4. ✅ Vérifier : Redirection vers le dashboard approprié

#### Test 1.3 : Mot de passe oublié

1. Cliquer sur "Mot de passe oublié"
2. Saisir l'email
3. ✅ Vérifier : Message de confirmation

### Scénario 2 : Gestion des inscriptions

#### Test 2.1 : Consulter les campagnes actives

1. Se connecter en tant que DOCTORANT
2. Naviguer vers "Inscriptions" > "Campagnes"
3. ✅ Vérifier : Liste des campagnes actives affichée

#### Test 2.2 : Créer une nouvelle inscription

1. Cliquer sur "Nouvelle inscription"
2. Sélectionner une campagne
3. Remplir les informations personnelles :
   - CIN, CNE, Date de naissance, etc.
4. Remplir les informations de thèse :
   - Titre, Domaine, Laboratoire, Directeur
5. ✅ Vérifier : Inscription créée avec statut "BROUILLON"

#### Test 2.3 : Upload de documents

1. Dans le détail de l'inscription
2. Cliquer sur "Ajouter un document"
3. Sélectionner le type de document
4. Uploader un fichier PDF
5. ✅ Vérifier : Document ajouté à la liste

#### Test 2.4 : Soumettre l'inscription

1. Vérifier que tous les documents requis sont présents
2. Cliquer sur "Soumettre l'inscription"
3. Confirmer la soumission
4. ✅ Vérifier :
   - Statut passe à "SOUMISE"
   - Notification reçue
   - Email envoyé (vérifier les logs)

### Scénario 3 : Validation des inscriptions (Admin)

#### Test 3.1 : Consulter les inscriptions en attente

1. Se connecter en tant qu'ADMIN
2. Naviguer vers "Administration" > "Validation des dossiers"
3. ✅ Vérifier : Liste des inscriptions "SOUMISE"

#### Test 3.2 : Valider une inscription

1. Cliquer sur une inscription
2. Examiner les documents
3. Cliquer sur "Valider"
4. Ajouter un commentaire (optionnel)
5. Confirmer
6. ✅ Vérifier :
   - Statut passe à "VALIDEE"
   - Notification envoyée au doctorant
   - Email de confirmation

#### Test 3.3 : Rejeter une inscription

1. Cliquer sur une inscription
2. Cliquer sur "Rejeter"
3. Saisir le motif de rejet
4. Confirmer
5. ✅ Vérifier :
   - Statut passe à "REJETEE"
   - Notification avec motif envoyée

### Scénario 4 : Notifications

#### Test 4.1 : Consulter les notifications

1. Cliquer sur l'icône de notification (cloche)
2. ✅ Vérifier : Liste des notifications non lues

#### Test 4.2 : Marquer comme lu

1. Cliquer sur une notification
2. ✅ Vérifier :
   - Notification marquée comme lue
   - Badge de compteur mis à jour

#### Test 4.3 : Paramètres de notification

1. Naviguer vers "Profil" > "Notifications"
2. Modifier les préférences
3. ✅ Vérifier : Préférences sauvegardées

### Scénario 5 : Gestion des soutenances

#### Test 5.1 : Demander une soutenance

1. Se connecter en tant que DOCTORANT
2. Naviguer vers "Soutenances"
3. Cliquer sur "Nouvelle demande"
4. Remplir le formulaire :
   - Date souhaitée
   - Lieu
   - Titre de la thèse
5. Proposer un jury
6. ✅ Vérifier : Demande créée avec statut "EN_ATTENTE"

#### Test 5.2 : Valider une soutenance (Directeur)

1. Se connecter en tant que DIRECTEUR
2. Consulter les demandes de soutenance
3. Valider ou rejeter
4. ✅ Vérifier : Notification envoyée

### Scénario 6 : Documents générés

#### Test 6.1 : Générer une attestation

1. Dans le détail d'une inscription validée
2. Cliquer sur "Générer attestation"
3. ✅ Vérifier :
   - PDF généré
   - Téléchargement automatique
   - Document sauvegardé dans la liste

#### Test 6.2 : Générer un QR Code

1. Dans le détail d'une inscription
2. Cliquer sur "Générer QR Code"
3. ✅ Vérifier : QR Code affiché et téléchargeable

### Scénario 7 : Dashboard et statistiques

#### Test 7.1 : Dashboard Doctorant

1. Se connecter en tant que DOCTORANT
2. ✅ Vérifier l'affichage de :
   - Statut de l'inscription
   - Documents manquants
   - Prochaines échéances
   - Notifications récentes

#### Test 7.2 : Dashboard Admin

1. Se connecter en tant qu'ADMIN
2. ✅ Vérifier l'affichage de :
   - Statistiques globales
   - Inscriptions par statut
   - Graphiques
   - Actions rapides

### Scénario 8 : Gestion des utilisateurs (Admin)

#### Test 8.1 : Créer un utilisateur

1. Naviguer vers "Administration" > "Utilisateurs"
2. Cliquer sur "Nouvel utilisateur"
3. Remplir le formulaire
4. ✅ Vérifier : Utilisateur créé

#### Test 8.2 : Modifier les rôles

1. Sélectionner un utilisateur
2. Modifier ses rôles
3. Sauvegarder
4. ✅ Vérifier : Rôles mis à jour

### Scénario 9 : Paramétrage (Admin)

#### Test 9.1 : Gérer les campagnes

1. Naviguer vers "Administration" > "Paramétrage"
2. Créer une nouvelle campagne
3. Définir les dates
4. ✅ Vérifier : Campagne créée et active

#### Test 9.2 : Configurer les types de documents

1. Dans le paramétrage
2. Ajouter/modifier les types de documents requis
3. ✅ Vérifier : Configuration sauvegardée

### Scénario 10 : Tests de sécurité

#### Test 10.1 : Accès non autorisé

1. Se déconnecter
2. Essayer d'accéder à une page protégée
3. ✅ Vérifier : Redirection vers login

#### Test 10.2 : Rôles et permissions

1. Se connecter en tant que DOCTORANT
2. Essayer d'accéder aux pages admin
3. ✅ Vérifier : Accès refusé (403)

#### Test 10.3 : Expiration du token

1. Se connecter
2. Attendre l'expiration du token (ou le modifier)
3. Faire une action
4. ✅ Vérifier : Redirection vers login

---

## ✅ Checklist de validation

### Frontend

- [ ] Toutes les pages se chargent sans erreur
- [ ] Les formulaires valident correctement les données
- [ ] Les messages d'erreur sont clairs et en français
- [ ] Le design est responsive (mobile, tablette, desktop)
- [ ] Les transitions et animations fonctionnent
- [ ] Pas d'erreurs dans la console du navigateur
- [ ] Les icônes et images s'affichent correctement

### Backend

- [ ] Tous les services démarrent sans erreur
- [ ] Les services s'enregistrent dans Eureka
- [ ] Les endpoints répondent correctement
- [ ] Les données sont persistées en base
- [ ] Les logs sont clairs et informatifs
- [ ] Les métriques sont accessibles (/actuator/health)

### Intégration

- [ ] L'authentification fonctionne de bout en bout
- [ ] Les notifications sont envoyées et reçues
- [ ] Les documents sont uploadés et téléchargés
- [ ] Les emails sont envoyés (vérifier les logs)
- [ ] Les événements Kafka sont traités
- [ ] Le cache fonctionne correctement

### Performance

- [ ] Les pages se chargent en moins de 2 secondes
- [ ] Les requêtes API répondent en moins de 500ms
- [ ] Pas de fuite mémoire côté frontend
- [ ] Les images sont optimisées

### Accessibilité

- [ ] Navigation au clavier possible
- [ ] Contraste des couleurs suffisant
- [ ] Labels sur tous les champs de formulaire
- [ ] Messages d'erreur accessibles

---

## 🔧 Résolution des problèmes

### Problème : Service ne démarre pas

**Symptôme** : Erreur au démarrage d'un service
**Solutions** :

1. Vérifier que le port n'est pas déjà utilisé
2. Vérifier la configuration de la base de données
3. Vérifier que Eureka est démarré
4. Consulter les logs : `mvn spring-boot:run`

### Problème : Erreur de connexion à la base de données

**Symptôme** : `Communications link failure`
**Solutions** :

1. Vérifier que MariaDB/MySQL est démarré
2. Vérifier les credentials dans application.properties
3. Tester la connexion : `mysql -u thesis_user -p`
4. Vérifier que la base existe : `SHOW DATABASES;`

### Problème : Frontend ne se connecte pas au backend

**Symptôme** : Erreur CORS ou 404
**Solutions** :

1. Vérifier que le Gateway est démarré (port 8080)
2. Vérifier l'URL dans environment.ts
3. Ouvrir la console du navigateur (F12)
4. Vérifier les logs du Gateway

### Problème : Notifications non reçues

**Symptôme** : Pas de notification après une action
**Solutions** :

1. Vérifier que Kafka est démarré
2. Vérifier les logs de notification-service
3. Vérifier la configuration Kafka
4. Tester manuellement l'endpoint de notification

### Problème : Upload de fichier échoue

**Symptôme** : Erreur lors de l'upload
**Solutions** :

1. Vérifier la taille du fichier (max 10MB)
2. Vérifier le type MIME accepté
3. Vérifier les permissions du dossier uploads/
4. Consulter les logs du service

### Problème : Token JWT invalide

**Symptôme** : 401 Unauthorized
**Solutions** :

1. Vérifier que le secret JWT est identique dans tous les services
2. Vérifier l'expiration du token
3. Se déconnecter et se reconnecter
4. Vider le localStorage du navigateur

---

## 📊 Monitoring et logs

### Consulter les métriques

- Eureka Dashboard : http://localhost:8761
- Actuator endpoints : http://localhost:808X/actuator/health
- Prometheus metrics : http://localhost:808X/actuator/prometheus

### Logs importants à surveiller

```bash
# Logs d'un service
tail -f logs/application.log

# Logs Kafka
tail -f kafka-logs/server.log

# Logs MariaDB
tail -f /var/log/mysql/error.log
```

---

## 🎯 Prochaines étapes

Après avoir validé tous les scénarios :

1. Effectuer des tests de charge
2. Tester la résilience (arrêter un service)
3. Tester la reprise après panne
4. Documenter les bugs trouvés
5. Préparer le déploiement en production

---

## 📞 Support

En cas de problème :

1. Consulter les logs des services
2. Vérifier la console du navigateur (F12)
3. Consulter la documentation des services
4. Créer une issue sur le repository Git

---

**Bonne chance pour vos tests ! 🚀**
