# Configuration Feign & Kafka - Inscription Service

## 🔗 Client Feign (User Service)

### Configuration
Le client Feign est configuré pour communiquer avec le microservice `USER-SERVICE` via Eureka.

### Endpoints disponibles
- `GET /api/users/{id}` - Récupérer un utilisateur par ID
- `GET /api/users/email/{email}` - Récupérer un utilisateur par email

### Utilisation
```java
@Autowired
private UserServiceClient userServiceClient;

UserDTO user = userServiceClient.getUserById(1L);
```

### Configuration
```properties
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=5000
feign.client.config.default.loggerLevel=basic
```

---

## 📨 Producer Kafka (Notifications)

### Configuration
Le producer Kafka envoie des notifications au topic `notifications`.

### Types de notifications
- `NOUVELLE_DEMANDE_DIRECTEUR` - Nouvelle demande pour le directeur
- `NOUVELLE_DEMANDE_ADMIN` - Nouvelle demande pour l'administration
- `VALIDATION_DIRECTEUR` - Validation par le directeur
- `REJET_DIRECTEUR` - Rejet par le directeur
- `VALIDATION_ADMIN` - Validation par l'administration
- `REJET_ADMIN` - Rejet par l'administration
- `VALIDATION_DEFINITIVE` - Validation définitive
- `RAPPEL_DOCUMENTS` - Rappel pour documents manquants

### Structure du message
```json
{
  "destinataireEmail": "user@example.com",
  "destinataireNom": "John Doe",
  "sujet": "Nouvelle demande d'inscription",
  "message": "Contenu du message...",
  "type": "NOUVELLE_DEMANDE_DIRECTEUR",
  "inscriptionId": 1,
  "dateEnvoi": "2025-01-01T10:00:00"
}
```

### Configuration Kafka
```properties
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.notifications=notifications
```

---

## 🚀 Démarrage

### Prérequis
1. **Kafka** doit être démarré sur `localhost:9092`
2. **Eureka Server** doit être démarré sur `localhost:8761`
3. **User Service** doit être démarré et enregistré dans Eureka

### Démarrer Kafka (Windows)

#### Option 1 : Avec Docker
```bash
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
```

#### Option 2 : Installation locale
1. Télécharger Kafka : https://kafka.apache.org/downloads
2. Extraire l'archive
3. Démarrer Zookeeper :
```bash
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```
4. Démarrer Kafka :
```bash
bin\windows\kafka-server-start.bat config\server.properties
```

### Créer le topic notifications
```bash
bin\windows\kafka-topics.bat --create --topic notifications --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### Vérifier les messages (Consumer de test)
```bash
bin\windows\kafka-console-consumer.bat --topic notifications --from-beginning --bootstrap-server localhost:9092
```

---

## 🧪 Tests

### Tester Feign Client
```java
@Test
void testGetUserById() {
    UserDTO user = userServiceClient.getUserById(1L);
    assertNotNull(user);
    assertEquals("John", user.getFirstName());
}
```

### Tester Kafka Producer
```java
@Test
void testSendNotification() {
    NotificationDTO notification = NotificationDTO.builder()
            .destinataireEmail("test@example.com")
            .sujet("Test")
            .message("Message de test")
            .type(NotificationDTO.TypeNotification.NOUVELLE_DEMANDE_DIRECTEUR)
            .inscriptionId(1L)
            .dateEnvoi(LocalDateTime.now())
            .build();
    
    kafkaTemplate.send("notifications", notification);
}
```

---

## 🔧 Troubleshooting

### Feign : Connection refused
- Vérifier qu'Eureka Server est démarré
- Vérifier que User Service est enregistré dans Eureka
- Vérifier les logs : `logging.level.ma.emsi.inscriptionservice.client=DEBUG`

### Kafka : Connection refused
- Vérifier que Kafka est démarré sur le port 9092
- Vérifier que le topic `notifications` existe
- Tester la connexion : `telnet localhost 9092`

### Kafka : Topic not found
```bash
# Créer le topic
bin\windows\kafka-topics.bat --create --topic notifications --bootstrap-server localhost:9092
```

---

## 📊 Monitoring

### Actuator Endpoints
- `/actuator/health` - État de santé
- `/actuator/metrics` - Métriques
- `/actuator/prometheus` - Métriques Prometheus

### Métriques Kafka
- `kafka.producer.record-send-total` - Nombre de messages envoyés
- `kafka.producer.record-error-total` - Nombre d'erreurs

### Métriques Feign
- `feign.Client.requests` - Nombre de requêtes
- `feign.Client.errors` - Nombre d'erreurs
