# Inscription Service

Service de gestion des inscriptions et réinscriptions doctorales.

## 🚀 Prérequis

- Java 17+
- MariaDB/MySQL
- Eureka Server (port 8761)

## 📦 Configuration

### 1. Créer la base de données

Avec HeidiSQL ou MySQL Workbench, exécutez :

```sql
CREATE DATABASE IF NOT EXISTS inscriptiondb 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

Ou utilisez le script fourni :
```bash
mysql -u root -p < create-database.sql
```

### 2. Configuration de l'application

Le fichier `application.properties` contient :
- Port : **8082**
- Base de données : **inscriptiondb**
- Credentials : root/amthroot (à modifier selon votre configuration)

## 🏃 Démarrage

```bash
./mvnw spring-boot:run
```

Le service démarre sur **http://localhost:8082**

## 📋 Endpoints disponibles

### Campagnes
- `POST /api/campagnes` - Créer une campagne (ADMIN)
- `GET /api/campagnes` - Liste des campagnes
- `GET /api/campagnes/actives` - Campagnes actives
- `GET /api/campagnes/{id}` - Détails d'une campagne
- `PUT /api/campagnes/{id}/fermer` - Fermer une campagne (ADMIN)
- `PUT /api/campagnes/{id}` - Modifier une campagne (ADMIN)

### Inscriptions
- `POST /api/inscriptions` - Créer une inscription (DOCTORANT)
- `POST /api/inscriptions/{id}/soumettre` - Soumettre pour validation (DOCTORANT)
- `GET /api/inscriptions/{id}` - Détails d'une inscription
- `GET /api/inscriptions/doctorant/{doctorantId}` - Inscriptions d'un doctorant
- `GET /api/inscriptions/directeur/{directeurId}/en-attente` - En attente directeur
- `POST /api/inscriptions/{id}/valider-directeur` - Validation directeur
- `GET /api/inscriptions/admin/en-attente` - En attente admin
- `POST /api/inscriptions/{id}/valider-admin` - Validation admin

### Documents
- `POST /api/documents/{inscriptionId}/upload` - Upload document (DOCTORANT)
- `GET /api/documents/{inscriptionId}` - Liste des documents
- `GET /api/documents/download/{documentId}` - Télécharger un document
- `DELETE /api/documents/{documentId}` - Supprimer un document (DOCTORANT)

## 🔒 Sécurité

La sécurité est actuellement désactivée pour les tests. Pour activer l'authentification JWT, intégrez avec le user-service.

## 📁 Structure du projet

```
inscription-service/
├── src/main/java/ma/emsi/inscriptionservice/
│   ├── config/          # Configuration (Security)
│   ├── controllers/     # REST Controllers
│   ├── DTOs/           # Data Transfer Objects
│   ├── entities/       # Entités JPA
│   ├── enums/          # Énumérations
│   ├── repositories/   # Repositories JPA
│   └── services/       # Services métier
└── src/main/resources/
    └── application.properties
```

## 🗄️ Modèle de données

- **Campagne** : Périodes d'inscription/réinscription
- **Inscription** : Demandes d'inscription
- **InfosDoctorant** : Informations personnelles
- **InfosThese** : Informations sur la thèse
- **DocumentInscription** : Documents joints
- **ValidationInscription** : Validations (directeur + admin)

## 🔗 Intégration

Ce service s'intègre avec :
- **Eureka Server** : Service discovery
- **User Service** : Gestion des utilisateurs (doctorants, directeurs, admin)
