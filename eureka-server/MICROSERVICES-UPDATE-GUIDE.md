# Guide de Mise à Jour - Microservices pour Eureka Sécurisé

## 🔐 Changement Important

L'Eureka Server nécessite maintenant une **authentification Basic** pour tous les enregistrements de service.

**Identifiants par défaut**:
- Username: `eureka`
- Password: `eureka123`

---

## 📝 Modifications Requises

Tous les microservices doivent mettre à jour leur configuration Eureka pour inclure les identifiants.

### Format de Configuration

**Ancien format** (ne fonctionne plus):
```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

**Nouveau format** (avec authentification):
```properties
eureka.client.service-url.defaultZone=http://eureka:eureka123@localhost:8761/eureka/,http://eureka:eureka123@localhost:8762/eureka/
```

---

## 🔧 Services à Mettre à Jour

### 1. user-service
📁 `c:\Users\hp\Desktop\PortailSuiviDoctorat\user-service\src\main\resources\application.properties`

### 2. defense-service  
📁 `c:\Users\hp\Desktop\PortailSuiviDoctorat\defense-service\src\main\resources\application.properties`

### 3. inscription-service
📁 `c:\Users\hp\Desktop\PortailSuiviDoctorat\inscription-service\src\main\resources\application.properties`

### 4. notification-service
📁 `c:\Users\hp\Desktop\PortailSuiviDoctorat\notification-service\src\main\resources\application.properties`

### 5. batch-service
📁 `c:\Users\hp\Desktop\PortailSuiviDoctorat\batch-service\src\main\resources\application.properties`

### 6. gateway-service
📁 `c:\Users\hp\Desktop\PortailSuiviDoctorat\gateway-service\src\main\resources\application.properties`

### 7. config-server
📁 `c:\Users\hp\Desktop\PortailSuiviDoctorat\config-server\src\main\resources\application.properties`

---

## ✅ Exemple de Mise à Jour

**Avant**:
```properties
spring.application.name=user-service
server.port=8081
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

**Après**:
```properties
spring.application.name=user-service
server.port=8081
eureka.client.service-url.defaultZone=http://eureka:eureka123@localhost:8761/eureka/,http://eureka:eureka123@localhost:8762/eureka/
```

---

## 🚀 Haute Disponibilité

La nouvelle configuration inclut **deux URLs Eureka** pour la redondance:
- **peer1**: http://localhost:8761
- **peer2**: http://localhost:8762

Si un serveur Eureka tombe en panne, les services continueront à fonctionner avec l'autre.

---

## ⚠️ Notes Importantes

1. **Redémarrage requis**: Après modification, redémarrez le microservice
2. **Vérification**: Vérifiez que le service apparaît dans le dashboard Eureka (http://localhost:8761)
3. **Identifiants**: Les identifiants par défaut sont pour le développement. Changez-les en production!
4. **Cluster mode**: Si vous exécutez Eureka en mode standalone, utilisez uniquement l'URL du peer1

---

## 🧪 Test de Configuration

Après modification, vérifiez l'enregistrement:

1. **Démarrez votre microservice**
2. **Accédez au dashboard Eureka**: http://localhost:8761 (eureka/eureka123)
3. **Vérifiez**: Votre service doit apparaître dans la section "Instances currently registered with Eureka"

Si le service n'apparaît pas, vérifiez:
- Les identifiants sont corrects
- L'URL Eureka inclut bien `eureka:eureka123@`
- Le serveur Eureka est démarré
- Les logs du microservice pour les erreurs de connexion
