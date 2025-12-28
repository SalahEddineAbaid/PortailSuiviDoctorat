# Tests Gateway - Collection Postman

## 🌐 Base URL
```
http://localhost:8888
```

---

## 🧪 Tests User Service via Gateway

### 1. Register via Gateway
```
POST http://localhost:8888/api/auth/register
Content-Type: application/json
```
**Body:**
```json
{
  "firstName": "Ahmed",
  "lastName": "Bennani",
  "email": "ahmed@test.com",
  "password": "Test123!",
  "role": "DOCTORANT"
}
```

### 2. Login via Gateway
```
POST http://localhost:8888/api/auth/login
Content-Type: application/json
```
**Body:**
```json
{
  "email": "ahmed@test.com",
  "password": "Test123!"
}
```

### 3. Get Profile via Gateway
```
GET http://localhost:8888/api/users/profile
Authorization: Bearer YOUR_TOKEN
```

---

## 🧪 Tests Inscription Service via Gateway

### 1. Créer une campagne via Gateway
```
POST http://localhost:8888/api/campagnes
Content-Type: application/json
```
**Body:**
```json
{
  "libelle": "Campagne Inscription 2025-2026",
  "type": "INSCRIPTION",
  "dateDebut": "2025-09-01",
  "dateFin": "2025-10-31",
  "anneeUniversitaire": 2025
}
```

### 2. Lister les campagnes via Gateway
```
GET http://localhost:8888/api/campagnes
```

### 3. Créer une inscription via Gateway
```
POST http://localhost:8888/api/inscriptions
Content-Type: application/json
```
**Body:**
```json
{
  "doctorantId": 1,
  "directeurTheseId": 2,
  "campagneId": 1,
  "sujetThese": "Intelligence Artificielle appliquée à la médecine",
  "type": "PREMIERE_INSCRIPTION",
  "anneeInscription": 2024,
  "cin": "AB123456",
  "cne": "R123456789",
  "telephone": "0612345678",
  "adresse": "123 Rue Mohammed V",
  "ville": "Casablanca",
  "pays": "Maroc",
  "dateNaissance": "1995-05-15",
  "lieuNaissance": "Rabat",
  "nationalite": "Marocaine",
  "titreThese": "IA et diagnostic médical automatisé",
  "discipline": "Informatique",
  "laboratoire": "LRIT",
  "etablissementAccueil": "Faculté des Sciences",
  "cotutelle": false,
  "dateDebutPrevue": "2025-09-01"
}
```

### 4. Lister les inscriptions via Gateway
```
GET http://localhost:8888/api/inscriptions/doctorant/1
```

---

## 🔄 Comparaison Direct vs Gateway

### Accès Direct (sans Gateway)
```bash
# User Service
curl http://localhost:8081/api/auth/login

# Inscription Service
curl http://localhost:8082/api/campagnes
```

### Accès via Gateway (Recommandé)
```bash
# User Service via Gateway
curl http://localhost:8888/api/auth/login

# Inscription Service via Gateway
curl http://localhost:8888/api/campagnes
```

---

## ✅ Avantages du Gateway

1. **Point d'entrée unique** : Un seul port (8888) pour tous les services
2. **Load Balancing** : Distribution automatique des requêtes
3. **CORS centralisé** : Configuration CORS en un seul endroit
4. **Sécurité** : Possibilité d'ajouter des filtres d'authentification
5. **Monitoring** : Traçabilité centralisée des requêtes

---

## 🔧 Configuration Postman

### Variables d'environnement
```json
{
  "gateway_url": "http://localhost:8888",
  "user_service_url": "http://localhost:8081",
  "inscription_service_url": "http://localhost:8082",
  "token": ""
}
```

### Utilisation
```
POST {{gateway_url}}/api/auth/login
GET {{gateway_url}}/api/campagnes
```

---

## 🧪 Scénario de test complet

### Étape 1 : Authentification
1. Register via Gateway → `POST /api/auth/register`
2. Login via Gateway → `POST /api/auth/login`
3. Sauvegarder le token

### Étape 2 : Gestion des campagnes
1. Créer une campagne → `POST /api/campagnes`
2. Lister les campagnes → `GET /api/campagnes`

### Étape 3 : Gestion des inscriptions
1. Créer une inscription → `POST /api/inscriptions`
2. Soumettre l'inscription → `POST /api/inscriptions/{id}/soumettre`
3. Consulter l'inscription → `GET /api/inscriptions/{id}`

---

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8888/actuator/health
```

### Routes actives
```bash
curl http://localhost:8888/actuator/gateway/routes
```

### Métriques
```bash
curl http://localhost:8888/actuator/metrics
```
