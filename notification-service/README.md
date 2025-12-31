# Notification Service

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Notification Types](#notification-types)
- [Email Templates](#email-templates)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Overview

The **Notification Service** is a Spring Boot microservice responsible for sending email notifications to users in the doctoral management system (Portail Suivi Doctorat). It provides a robust, fault-tolerant email delivery system with comprehensive resilience patterns.

### Key Features

- **Kafka-based Event Processing**: Consumes notification events from other microservices
- **HTML Email Templates**: Professional, responsive email templates with variable interpolation
- **Resilience Patterns**: Circuit breaker, retry with exponential backoff, timeout protection, and bulkhead isolation
- **Notification History**: Complete audit trail of all notifications with status tracking
- **Dead Letter Queue (DLQ)**: Failed notifications are captured for investigation and reprocessing
- **REST API**: Query notification history, statistics, and trigger manual retries
- **JWT Security**: Secure endpoints with role-based access control
- **Metrics & Monitoring**: Prometheus metrics and comprehensive logging
- **Property-Based Testing**: Extensive test coverage with JUnit QuickCheck

### Technology Stack

- **Java 17**
- **Spring Boot 3.5.6**
- **Spring Kafka** - Event consumption
- **Spring Mail** - SMTP email sending
- **Resilience4j** - Fault tolerance patterns
- **MariaDB** - Notification persistence
- **Mailtrap** - Email testing (SMTP sandbox)
- **Eureka Client** - Service discovery
- **JWT** - Authentication & authorization
- **Micrometer** - Metrics collection
- **SpringDoc OpenAPI** - API documentation
- **JUnit QuickCheck** - Property-based testing

## Architecture

### High-Level Architecture

```
┌─────────────────┐
│ Inscription     │
│ Service         │──┐
└─────────────────┘  │
                     │
┌─────────────────┐  │    ┌──────────────────────────────────────┐
│ Defense         │  │    │                                      │
│ Service         │──┼───▶│  Kafka Topic: notifications          │
└─────────────────┘  │    │                                      │
                     │    └──────────────────────────────────────┘
┌─────────────────┐  │                     │
│ Other           │  │                     │
│ Services        │──┘                     ▼
└─────────────────┘              ┌─────────────────────┐
                                 │ NotificationConsumer│
                                 └─────────────────────┘
                                           │
                                           ▼
                          ┌────────────────────────────────┐
                          │ NotificationProcessingService  │
                          └────────────────────────────────┘
                                           │
                    ┌──────────────────────┼──────────────────────┐
                    │                      │                      │
                    ▼                      ▼                      ▼
         ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
         │ EmailTemplate    │  │ EmailService     │  │ NotificationHistory│
         │ Service          │  │ (Resilience4j)   │  │ Service          │
         └──────────────────┘  └──────────────────┘  └──────────────────┘
                                        │                      │
                                        ▼                      ▼
                              ┌──────────────────┐  ┌──────────────────┐
                              │ SMTP Server      │  │ MariaDB          │
                              │ (Mailtrap)       │  │ Database         │
                              └──────────────────┘  └──────────────────┘
```

### Component Flow

1. **Event Reception**: NotificationConsumer listens to Kafka `notifications` topic
2. **Validation**: NotificationProcessingService validates email format and notification type
3. **Template Processing**: EmailTemplateService loads and interpolates HTML template with variables
4. **Email Sending**: EmailService sends email with resilience patterns (circuit breaker, retry, timeout, bulkhead)
5. **Persistence**: NotificationHistoryService persists notification with status (PENDING → SENT/FAILED)
6. **Error Handling**: Failed notifications are sent to `notifications-dlq` topic for investigation

### Resilience Patterns

- **Circuit Breaker**: Opens after 50% failure rate in 10 calls, stays open for 60 seconds
- **Retry**: Up to 3 attempts with exponential backoff (5s, 10s, 20s)
- **Timeout**: 30-second timeout for email send operations
- **Bulkhead**: Maximum 10 concurrent email sends with 5-second wait time

## Prerequisites

Before running the notification-service, ensure you have the following installed:

### Required Software

- **Java 17** or higher
  ```bash
  java -version
  ```

- **Maven 3.8+**
  ```bash
  mvn -version
  ```

- **MariaDB 10.6+**
  ```bash
  mysql --version
  ```

- **Apache Kafka 3.0+** (with Zookeeper)
  ```bash
  kafka-topics.sh --version
  ```

### Required Services

- **Eureka Server** (running on port 8761)
- **Kafka Broker** (running on port 9092)
- **MariaDB Server** (running on port 3306)
- **Mailtrap Account** (for email testing)

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd notification-service
```

### 2. Create Database

Create the MariaDB database for the notification service:

```sql
CREATE DATABASE notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The service will automatically create the required tables on startup using Hibernate DDL auto-update.

### 3. Configure Environment Variables

Set the following environment variables (or update `application.properties`):

```bash
# Database credentials
export DB_USERNAME=root
export DB_PASSWORD=your_password

# Mailtrap credentials (get from https://mailtrap.io)
export MAILTRAP_USERNAME=your_mailtrap_username
export MAILTRAP_PASSWORD=your_mailtrap_password

# JWT secret (must match user-service)
export JWT_SECRET=mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Service

```bash
mvn spring-boot:run
```

The service will start on **port 8084**.

### 6. Verify Service Registration

Check that the service has registered with Eureka:
- Open http://localhost:8761
- Verify `NOTIFICATION-SERVICE` appears in the instances list

## Configuration

### Application Properties

The service is configured via `src/main/resources/application.properties`. Key configuration sections:

#### Database Configuration

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/notification_db
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
spring.jpa.hibernate.ddl-auto=update
```

#### Kafka Configuration

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service-group
kafka.topic.notifications=notifications
kafka.topic.notifications-dlq=notifications-dlq
```

#### Mail Configuration (Mailtrap)

```properties
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=${MAILTRAP_USERNAME}
spring.mail.password=${MAILTRAP_PASSWORD}
notification.email.from=noreply@portail-doctorat.ma
```

#### Resilience4j Configuration

```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.emailService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.emailService.wait-duration-in-open-state=60s

# Retry
resilience4j.retry.instances.emailService.max-attempts=3
resilience4j.retry.instances.emailService.wait-duration=5s
resilience4j.retry.instances.emailService.enable-exponential-backoff=true

# Timeout
resilience4j.timelimiter.instances.emailService.timeout-duration=30s

# Bulkhead
resilience4j.bulkhead.instances.emailService.max-concurrent-calls=10
```

#### Eureka Configuration

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
```

#### Logging Configuration

```properties
logging.level.ma.emsi.notificationservice=DEBUG
logging.level.org.springframework.kafka=INFO
logging.level.org.springframework.mail=DEBUG
logging.level.io.github.resilience4j=DEBUG
```

### Environment-Specific Configuration

For production deployment, create `application-prod.properties` with production-specific settings:

```properties
# Use real SMTP server instead of Mailtrap
spring.mail.host=smtp.gmail.com
spring.mail.port=587

# Disable SQL logging
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=WARN
```

Run with production profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## API Endpoints

All endpoints require JWT authentication. Admin endpoints require `ROLE_ADMIN`.

### Base URL
```
http://localhost:8084/api/notifications
```

### Authentication

Include JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

### Endpoints

#### 1. Get All Notifications (Admin)

```http
GET /api/notifications?page=0&size=20&sortBy=dateCreation&sortDir=DESC
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "type": "INSCRIPTION_VALIDEE_ADMIN",
      "destinataire": "doctorant@emsi.ma",
      "sujet": "Inscription validée",
      "statut": "SENT",
      "dateCreation": "2024-01-15T10:30:00",
      "dateEnvoi": "2024-01-15T10:30:05",
      "nombreTentatives": 1
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0
}
```

#### 2. Get Notification by ID

```http
GET /api/notifications/{id}
```

**Response:**
```json
{
  "id": 1,
  "type": "INSCRIPTION_VALIDEE_ADMIN",
  "destinataire": "doctorant@emsi.ma",
  "sujet": "Inscription validée",
  "messageTexte": "Votre inscription a été validée...",
  "messageHtml": "<html>...</html>",
  "donnees": "{\"nomDoctorant\":\"Ahmed\",\"prenomDoctorant\":\"Ali\"}",
  "statut": "SENT",
  "priorite": "NORMALE",
  "dateCreation": "2024-01-15T10:30:00",
  "dateEnvoi": "2024-01-15T10:30:05",
  "nombreTentatives": 1,
  "erreurMessage": null
}
```

#### 3. Get User's Notifications

```http
GET /api/notifications/user/{email}?page=0&size=20
```

Users can only access their own notifications unless they have ADMIN role.

#### 4. Get Notifications by Status (Admin)

```http
GET /api/notifications/status/FAILED?page=0&size=20
```

Valid statuses: `PENDING`, `SENT`, `FAILED`, `RETRYING`

#### 5. Get Notification Statistics (Admin)

```http
GET /api/notifications/stats
```

**Response:**
```json
{
  "total": 1000,
  "sent": 950,
  "failed": 30,
  "pending": 10,
  "retrying": 10,
  "successRate": 96.94
}
```

#### 6. Retry Failed Notification (Admin)

```http
POST /api/notifications/{id}/retry
```

**Response:**
```json
{
  "success": true,
  "message": "Notification queued for retry",
  "notificationId": 123,
  "status": "RETRYING"
}
```

#### 7. Get Failed Notifications (Admin)

```http
GET /api/notifications/failed?page=0&size=20
```

#### 8. Search Notifications (Admin)

```http
GET /api/notifications/search?destinataire=doctorant@emsi.ma&type=INSCRIPTION_VALIDEE_ADMIN&status=SENT&dateDebut=2024-01-01T00:00:00&dateFin=2024-12-31T23:59:59&page=0&size=20
```

All query parameters are optional.

#### 9. Retry All DLQ Messages (Admin)

```http
POST /api/notifications/dlq/retry-all
```

**Response:**
```json
{
  "success": true,
  "message": "DLQ retry operation completed",
  "totalProcessed": 50,
  "successCount": 45,
  "failureCount": 5
}
```

### API Documentation (Swagger)

Interactive API documentation is available at:
```
http://localhost:8084/swagger-ui.html
```

OpenAPI JSON specification:
```
http://localhost:8084/api-docs
```

## Notification Types

The service supports 21 notification types across different workflows:

### Inscription Workflow

| Type | Description | Recipients |
|------|-------------|-----------|
| `INSCRIPTION_SOUMISE_DIRECTEUR` | Inscription submitted | Directeur de thèse |
| `INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT` | Directeur validated inscription | Doctorant |
| `INSCRIPTION_VALIDEE_DIRECTEUR_ADMIN` | Directeur validated inscription | Admin |
| `INSCRIPTION_REJETEE_DIRECTEUR` | Directeur rejected inscription | Doctorant |
| `INSCRIPTION_VALIDEE_ADMIN` | Admin validated inscription | Doctorant |
| `INSCRIPTION_REJETEE_ADMIN` | Admin rejected inscription | Doctorant |
| `DEROGATION_DEMANDEE` | Derogation requested | PED |

### Defense Workflow

| Type | Description | Recipients |
|------|-------------|-----------|
| `DEMANDE_SOUTENANCE_SOUMISE_DIRECTEUR` | Defense request submitted | Directeur de thèse |
| `JURY_PROPOSE_ADMIN` | Jury proposed | Admin |
| `JURY_MEMBRE_INVITE` | Jury member invited | Jury member |
| `JURY_MEMBRE_ACCEPTE_DIRECTEUR` | Jury member accepted | Directeur de thèse |
| `JURY_MEMBRE_DECLINE_DIRECTEUR` | Jury member declined | Directeur de thèse |
| `RAPPORT_SOUMIS_DIRECTEUR` | Report submitted | Directeur de thèse |
| `AUTORISATION_SOUTENANCE_DOCTORANT` | Defense authorized | Doctorant |
| `SOUTENANCE_PLANIFIEE_TOUS` | Defense scheduled | All participants |

### System Notifications

| Type | Description |
|------|-------------|
| `RAPPEL_ECHEANCE` | Deadline reminder |
| `DOCUMENT_MANQUANT` | Missing document alert |
| `MODIFICATION_CAMPAGNE` | Campaign modification |
| `ALERTE_SYSTEME` | System alert |
| `CONFIRMATION_INSCRIPTION` | Inscription confirmation |
| `NOTIFICATION_GENERALE` | General notification |

## Email Templates

Email templates are located in `src/main/resources/templates/emails/`.

### Template Structure

Each template is an HTML file with variable placeholders using `{{variable}}` syntax:

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; }
        .container { max-width: 600px; margin: 0 auto; }
        .header { background-color: #003366; color: white; padding: 20px; }
        .content { padding: 20px; background-color: #F5F5F5; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>{{titre}}</h1>
        </div>
        <div class="content">
            <p>Bonjour {{nomDoctorant}} {{prenomDoctorant}},</p>
            <p>{{message}}</p>
        </div>
    </div>
</body>
</html>
```

### Available Templates

- `template_inscription_soumise_directeur.html`
- `template_inscription_validee_directeur_doctorant.html`
- `template_inscription_validee_directeur_admin.html`
- `template_inscription_rejetee_directeur.html`
- `template_inscription_validee_admin.html`
- `template_inscription_rejetee_admin.html`
- `template_derogation_demandee.html`
- `template_demande_soutenance_soumise_directeur.html`
- `template_jury_propose_admin.html`
- `template_jury_membre_invite.html`
- `template_jury_membre_accepte_directeur.html`
- `template_jury_membre_decline_directeur.html`
- `template_rapport_soumis_directeur.html`
- `template_autorisation_soutenance_doctorant.html`
- `template_soutenance_planifiee_tous.html`
- `template_generic.html` (fallback template)

### Template Variables

Common variables used across templates:

- `{{nomDoctorant}}` - Doctoral student last name
- `{{prenomDoctorant}}` - Doctoral student first name
- `{{nomDirecteur}}` - Thesis director last name
- `{{prenomDirecteur}}` - Thesis director first name
- `{{sujetThese}}` - Thesis subject
- `{{commentaire}}` - Comments (for rejections)
- `{{dateDebut}}` - Start date
- `{{dateFin}}` - End date
- `{{lienPortail}}` - Portal link

### Creating Custom Templates

1. Create a new HTML file in `src/main/resources/templates/emails/`
2. Use `{{variable}}` syntax for dynamic content
3. Follow responsive design principles (max-width: 600px)
4. Use inline CSS for email client compatibility
5. Test with Mailtrap before production use

## Testing

The service includes comprehensive testing with both unit tests and property-based tests.

### Running All Tests

```bash
mvn test
```

### Running Specific Test Classes

```bash
# Unit tests
mvn test -Dtest=NotificationConsumerTest
mvn test -Dtest=EmailServiceTest
mvn test -Dtest=NotificationHistoryServiceTest

# Property-based tests
mvn test -Dtest=NotificationPropertiesTest

# Integration tests
mvn test -Dtest=NotificationFlowIntegrationTest
mvn test -Dtest=ResilienceIntegrationTest
```

### Test Coverage

Generate code coverage report:

```bash
mvn clean test jacoco:report
```

View report at: `target/site/jacoco/index.html`

Target: **70% code coverage minimum**

### Test Types

#### Unit Tests
- Mock external dependencies (JavaMailSender, Kafka, Database)
- Test individual components in isolation
- Fast execution
- Located in `src/test/java/ma/emsi/notificationservice/`

#### Property-Based Tests
- Use JUnit QuickCheck for random test data generation
- Verify universal properties across all inputs
- Each property test runs 100 iterations
- Test correctness properties from design document
- Located in `src/test/java/ma/emsi/notificationservice/properties/`

#### Integration Tests
- Use EmbeddedKafka for Kafka simulation
- Use H2 in-memory database
- Test end-to-end flows
- Test resilience patterns with simulated failures
- Located in `src/test/java/ma/emsi/notificationservice/integration/`

### Testing with Mailtrap

1. Sign up for a free account at https://mailtrap.io
2. Get your SMTP credentials from the inbox settings
3. Set environment variables:
   ```bash
   export MAILTRAP_USERNAME=your_username
   export MAILTRAP_PASSWORD=your_password
   ```
4. Run the service and trigger notifications
5. Check Mailtrap inbox to verify email delivery

### Manual Testing with Kafka

Send a test notification event to Kafka:

```bash
# Start Kafka console producer
kafka-console-producer.sh --broker-list localhost:9092 --topic notifications

# Send JSON message
{"type":"INSCRIPTION_VALIDEE_ADMIN","destinataire":"test@emsi.ma","sujet":"Test Notification","donnees":{"nomDoctorant":"Test","prenomDoctorant":"User"},"priorite":"NORMALE"}
```

Check the service logs and Mailtrap inbox for the email.

## Monitoring

### Health Check

```http
GET http://localhost:8084/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "kafka": { "status": "UP" },
    "mail": { "status": "UP" }
  }
}
```

### Metrics

Prometheus metrics endpoint:
```http
GET http://localhost:8084/actuator/prometheus
```

#### Custom Metrics

- `notifications_sent_total` - Counter for successfully sent notifications
- `notifications_failed_total` - Counter for failed notifications
- `notifications_pending_total` - Counter for pending notifications
- `notifications_retry_total` - Counter for retry attempts
- `notifications_dlq_total` - Counter for DLQ messages
- `notifications_email_send_duration_seconds` - Histogram for email send duration

#### Resilience4j Metrics

- `resilience4j_circuitbreaker_state` - Circuit breaker state (0=closed, 1=open, 2=half-open)
- `resilience4j_circuitbreaker_failure_rate` - Failure rate percentage
- `resilience4j_retry_calls` - Retry attempt counts
- `resilience4j_bulkhead_available_concurrent_calls` - Available bulkhead slots

### Logging

Logs are written to console with structured format:

```
2024-01-15 10:30:00 [main] INFO  ma.emsi.notificationservice.consumers.NotificationConsumer - Received notification: type=INSCRIPTION_VALIDEE_ADMIN, destinataire=doctorant@emsi.ma
2024-01-15 10:30:05 [main] INFO  ma.emsi.notificationservice.services.EmailService - Email sent successfully to doctorant@emsi.ma
```

#### Log Levels

- **DEBUG**: Notification received, template loading, variable interpolation
- **INFO**: Notification sent successfully, retry attempts
- **WARN**: Circuit breaker state changes, DLQ messages
- **ERROR**: Notification failed, email send errors

#### Viewing Logs

```bash
# Follow logs in real-time
tail -f logs/notification-service.log

# Search for failed notifications
grep "ERROR" logs/notification-service.log

# Search for specific notification ID
grep "notificationId=123" logs/notification-service.log
```

## Troubleshooting

### Common Issues

#### 1. Service Fails to Start

**Symptom**: Application crashes on startup

**Possible Causes:**
- Database connection failure
- Kafka broker not available
- Eureka server not running

**Solution:**
```bash
# Check database connection
mysql -u root -p -h localhost -P 3306 notification_db

# Check Kafka broker
kafka-topics.sh --list --bootstrap-server localhost:9092

# Check Eureka server
curl http://localhost:8761/eureka/apps
```

#### 2. Emails Not Being Sent

**Symptom**: Notifications stuck in PENDING status

**Possible Causes:**
- Mailtrap credentials incorrect
- SMTP connection blocked
- Circuit breaker open

**Solution:**
```bash
# Check Mailtrap credentials
echo $MAILTRAP_USERNAME
echo $MAILTRAP_PASSWORD

# Check circuit breaker state
curl http://localhost:8084/actuator/metrics/resilience4j.circuitbreaker.state

# Check email service logs
grep "EmailService" logs/notification-service.log
```

#### 3. Kafka Consumer Not Receiving Messages

**Symptom**: No notifications being processed

**Possible Causes:**
- Kafka topic doesn't exist
- Consumer group offset issue
- Deserialization error

**Solution:**
```bash
# Check if topic exists
kafka-topics.sh --list --bootstrap-server localhost:9092 | grep notifications

# Create topic if missing
kafka-topics.sh --create --topic notifications --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Reset consumer group offset
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group notification-service-group --reset-offsets --to-earliest --topic notifications --execute

# Check consumer logs
grep "NotificationConsumer" logs/notification-service.log
```

#### 4. Circuit Breaker Stuck Open

**Symptom**: All email sends failing immediately

**Possible Causes:**
- SMTP server was down and circuit breaker opened
- Failure rate threshold exceeded

**Solution:**
```bash
# Wait for circuit breaker to transition to half-open (60 seconds)
# Or restart the service to reset circuit breaker state
mvn spring-boot:run

# Check circuit breaker metrics
curl http://localhost:8084/actuator/metrics/resilience4j.circuitbreaker.state
```

#### 5. High Memory Usage

**Symptom**: Service consuming excessive memory

**Possible Causes:**
- Too many concurrent email sends
- Large email attachments
- Memory leak

**Solution:**
```bash
# Adjust JVM heap size
export MAVEN_OPTS="-Xmx512m -Xms256m"
mvn spring-boot:run

# Reduce bulkhead concurrent calls in application.properties
resilience4j.bulkhead.instances.emailService.max-concurrent-calls=5

# Monitor memory usage
jconsole
```

#### 6. Database Connection Pool Exhausted

**Symptom**: `HikariPool - Connection is not available` errors

**Solution:**
Add to `application.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

#### 7. JWT Authentication Failures

**Symptom**: 401 Unauthorized errors

**Possible Causes:**
- JWT secret mismatch with user-service
- Token expired
- Invalid token format

**Solution:**
```bash
# Verify JWT secret matches user-service
echo $JWT_SECRET

# Check token expiration
# Decode JWT at https://jwt.io

# Check security logs
grep "JwtAuthenticationFilter" logs/notification-service.log
```

### Debug Mode

Enable debug logging for troubleshooting:

```properties
logging.level.ma.emsi.notificationservice=DEBUG
logging.level.org.springframework.kafka=DEBUG
logging.level.org.springframework.mail=DEBUG
logging.level.io.github.resilience4j=DEBUG
```

### Getting Help

If you encounter issues not covered here:

1. Check the service logs: `logs/notification-service.log`
2. Check Actuator health endpoint: `http://localhost:8084/actuator/health`
3. Review Swagger documentation: `http://localhost:8084/swagger-ui.html`
4. Check Kafka consumer lag: `kafka-consumer-groups.sh --describe --group notification-service-group --bootstrap-server localhost:9092`
5. Review DLQ messages: `GET /api/notifications/dlq/retry-all`

---

## License

This project is part of the Portail Suivi Doctorat system developed by EMSI.

## Contact

For questions or support, contact the development team.
