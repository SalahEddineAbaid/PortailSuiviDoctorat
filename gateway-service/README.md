# Gateway Service

Point d'entrée unique pour tous les microservices du système de gestion doctorale.

## 🚀 Démarrage

### Prérequis
1. **Eureka Server** doit être démarré sur le port 8761
2. Les microservices doivent être enregistrés dans Eureka

### Lancer le Gateway
```bash
./mvnw spring-boot:run
```

Le Gateway démarre sur **http://localhost:8888**

---

## 📍 Routes configurées

### User Service
- `GET/POST /api/users/**` → USER-SERVICE (port 8081)
- `GET/POST /api/auth/**` → USER-SERVICE (port 8081)

### Inscription Service
- `GET/POST/PUT/DELETE /api/campagnes/**` → INSCRIPTION-SERVICE (port 8082)
- `GET/POST/PUT/DELETE /api/inscriptions/**` → INSCRIPTION-SERVICE (port 8082)
- `GET/POST/DELETE /api/documents/**` → INSCRIPTION-SERVICE (port 8082)

---

## 🔒 CORS

Le Gateway gère automatiquement CORS pour :
- **Origine** : `http://localhost:4200` (Angular)
- **Méthodes** : GET, POST, PUT, DELETE, OPTIONS
- **Headers** : Tous autorisés
- **Credentials** : Autorisés

---

## 🧪 Tests

### Via curl
```bash
# User Service via Gateway
curl http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password"}'

# Inscription Service via Gateway
curl http://localhost:8888/api/campagnes
```

### Via Postman
Voir `GATEWAY_TESTS.md` pour la collection complète.

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

---

## 🏗️ Architecture

```
┌─────────────────┐
│  Frontend :4200 │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Gateway :8888  │
└────────┬────────┘
         │
    ┌────┴────┐
    ↓         ↓
┌─────────┐ ┌──────────────┐
│  User   │ │ Inscription  │
│ :8081   │ │   :8082      │
└─────────┘ └──────────────┘
```

---

## 📝 Configuration

Voir `application.yml` pour la configuration complète des routes.

Voir `ROUTES.md` pour la documentation détaillée des routes.

---

## 🔧 Dépendances

- Spring Cloud Gateway
- Spring Cloud Netflix Eureka Client
- Spring Boot Starter

---

## ⚠️ Notes

- Le Gateway utilise **Spring Cloud Gateway** (réactif, basé sur WebFlux)
- Ne pas utiliser Spring MVC dans le Gateway (incompatible)
- Les routes sont résolues via Eureka (`lb://SERVICE-NAME`)
- Le load balancing est automatique si plusieurs instances existent
