# Gateway Service - Routes Configuration

## 🌐 Port
Le Gateway écoute sur le port **8888**

## 📍 Routes configurées

### User Service (USER-SERVICE)
| Route | Méthode | Description | Service |
|-------|---------|-------------|---------|
| `/api/users/**` | ALL | Gestion des utilisateurs | USER-SERVICE |
| `/api/auth/**` | ALL | Authentification (login, register, refresh) | USER-SERVICE |

**Exemples :**
- `http://localhost:8888/api/auth/login`
- `http://localhost:8888/api/auth/register`
- `http://localhost:8888/api/users/profile`

---

### Inscription Service (INSCRIPTION-SERVICE)
| Route | Méthode | Description | Service |
|-------|---------|-------------|---------|
| `/api/campagnes/**` | ALL | Gestion des campagnes d'inscription | INSCRIPTION-SERVICE |
| `/api/inscriptions/**` | ALL | Gestion des inscriptions | INSCRIPTION-SERVICE |
| `/api/documents/**` | ALL | Gestion des documents | INSCRIPTION-SERVICE |

**Exemples :**
- `http://localhost:8888/api/campagnes`
- `http://localhost:8888/api/inscriptions`
- `http://localhost:8888/api/documents/1/upload`

---

## 🔒 CORS Configuration

### Origines autorisées
- `http://localhost:4200` (Angular Frontend)

### Méthodes autorisées
- GET
- POST
- PUT
- DELETE
- OPTIONS

### Headers
- Tous les headers sont autorisés (`*`)
- Credentials autorisés (`allowCredentials: true`)

---

## 🚀 Utilisation

### Via le Gateway (Recommandé)
```bash
# Au lieu de http://localhost:8081/api/users/...
curl http://localhost:8888/api/users/profile

# Au lieu de http://localhost:8082/api/campagnes
curl http://localhost:8888/api/campagnes
```

### Avantages
✅ Point d'entrée unique  
✅ Load balancing automatique  
✅ CORS géré centralement  
✅ Possibilité d'ajouter des filtres globaux (auth, rate limiting, etc.)

---

## 📊 Architecture

```
Frontend (Angular :4200)
         ↓
Gateway (:8888)
         ↓
    ┌────┴────┐
    ↓         ↓
User-Service  Inscription-Service
  (:8081)        (:8082)
```

---

## 🔧 Configuration

### Load Balancing
Le Gateway utilise **Eureka** pour découvrir les services :
- `lb://USER-SERVICE` → Résolu via Eureka
- `lb://INSCRIPTION-SERVICE` → Résolu via Eureka

### Filtres par défaut
- `DedupeResponseHeader` : Évite les headers dupliqués pour CORS

---

## 🧪 Tests

### Tester le Gateway
```bash
# Vérifier que le Gateway est démarré
curl http://localhost:8888/actuator/health

# Tester une route User Service
curl http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password"}'

# Tester une route Inscription Service
curl http://localhost:8888/api/campagnes
```

### Vérifier les routes dans Eureka
1. Ouvrir http://localhost:8761
2. Vérifier que `GATEWAY-SERVICE`, `USER-SERVICE` et `INSCRIPTION-SERVICE` sont enregistrés

---

## 🔜 Routes à ajouter (futurs services)

### Defense Service
```yaml
- id: defense-service
  uri: lb://DEFENSE-SERVICE
  predicates:
    - Path=/api/defenses/**
```

### Notification Service
```yaml
- id: notification-service
  uri: lb://NOTIFICATION-SERVICE
  predicates:
    - Path=/api/notifications/**
```

---

## ⚠️ Troubleshooting

### Erreur 503 Service Unavailable
- Vérifier qu'Eureka Server est démarré
- Vérifier que le service cible est enregistré dans Eureka
- Vérifier les logs du Gateway

### Erreur CORS
- Vérifier que l'origine est dans `allowedOrigins`
- Vérifier que la méthode HTTP est autorisée
- Vérifier les headers de la requête

### Service non trouvé
```bash
# Vérifier les services enregistrés
curl http://localhost:8761/eureka/apps
```

---

## 📝 Notes

- Le Gateway utilise **Spring Cloud Gateway** (réactif)
- Les routes sont chargées au démarrage
- Pour ajouter une nouvelle route, redémarrer le Gateway
- Les timeouts par défaut sont de 30 secondes
