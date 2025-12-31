# Requirements Document

## Introduction

Cette spécification définit les exigences pour finaliser le microservice notification-service, qui gère l'envoi de notifications par email dans le cadre du système de gestion du cycle doctoral. Le service est actuellement à 40% de réalisation et nécessite l'implémentation complète du système de notifications incluant la consommation d'événements Kafka, l'envoi d'emails HTML, la gestion de l'historique, et la résilience.

## Glossary

- **Notification-Service**: Le microservice responsable de l'envoi de notifications par email aux utilisateurs
- **Kafka Consumer**: Composant qui écoute et traite les événements de notification depuis Kafka
- **NotificationDTO**: Objet de transfert de données contenant les informations d'une notification
- **JavaMailSender**: API Spring pour l'envoi d'emails via SMTP
- **Mailtrap**: Service de test SMTP qui capture les emails sans les envoyer réellement
- **Template HTML**: Modèle d'email avec variables dynamiques pour personnalisation
- **DLQ**: Dead Letter Queue - file d'attente pour les messages qui échouent après tous les retries
- **Circuit Breaker**: Pattern de résilience qui ouvre un circuit pour éviter les appels répétés à un service défaillant
- **Retry**: Mécanisme de nouvelle tentative automatique en cas d'échec
- **Resilience4j**: Bibliothèque Java pour implémenter des patterns de résilience
- **JWT**: JSON Web Token - standard pour l'authentification et l'autorisation
- **MIME**: Multipurpose Internet Mail Extensions - format pour les emails HTML

## Requirements

### Requirement 1

**User Story:** As a system integrator, I want a Kafka consumer to process notification events, so that notifications are automatically sent when events occur in other services.

#### Acceptance Criteria

1. WHEN a notification event is published to the notifications topic THEN the Notification-Service SHALL consume the event and deserialize it into a NotificationDTO
2. WHEN a NotificationDTO is received THEN the Notification-Service SHALL validate that the destinataire field contains a valid email address
3. WHEN a NotificationDTO is received THEN the Notification-Service SHALL validate that the type field matches a recognized TypeNotification enum value
4. WHEN a valid NotificationDTO is processed THEN the Notification-Service SHALL select the appropriate HTML template based on the type field
5. WHEN template variables are interpolated THEN the Notification-Service SHALL replace all {{variable}} placeholders with values from the donnees map
6. WHEN a notification is successfully sent THEN the Notification-Service SHALL persist the notification record with status SENT
7. WHEN a notification send fails THEN the Notification-Service SHALL persist the notification record with status FAILED and the error message

### Requirement 2

**User Story:** As a system administrator, I want emails to be sent using HTML templates, so that notifications are professional and consistent.

#### Acceptance Criteria

1. WHEN an email is sent THEN the Notification-Service SHALL use JavaMailSender to send the email via SMTP
2. WHEN an email is sent THEN the Notification-Service SHALL set the sender address to noreply@portail-doctorat.ma
3. WHEN an email is sent THEN the Notification-Service SHALL set the Content-Type to text/html with charset UTF-8
4. WHEN an HTML template is loaded THEN the Notification-Service SHALL read the template file from src/main/resources/templates/emails/
5. WHEN template variables are interpolated THEN the Notification-Service SHALL replace all {{variable}} placeholders with corresponding values
6. WHEN a template variable is missing from the data THEN the Notification-Service SHALL replace it with an empty string
7. WHEN a template file is not found THEN the Notification-Service SHALL use a generic fallback template

### Requirement 3

**User Story:** As a doctoral student, I want to receive email notifications for inscription events, so that I am informed of the status of my registration.

#### Acceptance Criteria

1. WHEN an inscription is submitted THEN the Notification-Service SHALL send an email to the directeur using template_inscription_soumise_directeur.html
2. WHEN a directeur validates an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_validee_directeur_doctorant.html
3. WHEN a directeur validates an inscription THEN the Notification-Service SHALL send an email to the admin using template_inscription_validee_directeur_admin.html
4. WHEN a directeur rejects an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_rejetee_directeur.html including the commentaire
5. WHEN an admin validates an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_validee_admin.html
6. WHEN an admin rejects an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_rejetee_admin.html including the commentaire
7. WHEN a derogation is requested THEN the Notification-Service SHALL send an email to the PED using template_derogation_demandee.html

### Requirement 4

**User Story:** As a doctoral student, I want to receive email notifications for defense events, so that I am informed of the status of my thesis defense.

#### Acceptance Criteria

1. WHEN a defense request is submitted THEN the Notification-Service SHALL send an email to the directeur using template_demande_soutenance_soumise_directeur.html
2. WHEN a jury is proposed THEN the Notification-Service SHALL send an email to the admin using template_jury_propose_admin.html
3. WHEN a jury member is invited THEN the Notification-Service SHALL send an email to the member using template_jury_membre_invite.html
4. WHEN a jury member accepts THEN the Notification-Service SHALL send an email to the directeur using template_jury_membre_accepte_directeur.html
5. WHEN a jury member declines THEN the Notification-Service SHALL send an email to the directeur using template_jury_membre_decline_directeur.html
6. WHEN a report is submitted THEN the Notification-Service SHALL send an email to the directeur using template_rapport_soumis_directeur.html
7. WHEN a defense is authorized THEN the Notification-Service SHALL send an email to the doctorant using template_autorisation_soutenance_doctorant.html
8. WHEN a defense is scheduled THEN the Notification-Service SHALL send an email to all participants using template_soutenance_planifiee_tous.html

### Requirement 5

**User Story:** As a system administrator, I want all notifications to be persisted in the database, so that I can track notification history and audit email delivery.

#### Acceptance Criteria

1. WHEN a notification is received from Kafka THEN the Notification-Service SHALL create a Notification entity with status PENDING
2. WHEN a notification is successfully sent THEN the Notification-Service SHALL update the status to SENT and set the dateEnvoi
3. WHEN a notification send fails THEN the Notification-Service SHALL update the status to FAILED and record the erreurMessage
4. WHEN a notification is retried THEN the Notification-Service SHALL increment the nombreTentatives counter
5. WHEN a notification is retried THEN the Notification-Service SHALL update the status to RETRYING
6. WHEN notification data is persisted THEN the Notification-Service SHALL serialize the donnees map to JSON format
7. WHEN notification data is persisted THEN the Notification-Service SHALL store both messageTexte and messageHtml versions

### Requirement 6

**User Story:** As a system administrator, I want to query notification history via REST API, so that I can monitor email delivery and troubleshoot issues.

#### Acceptance Criteria

1. WHEN an admin requests GET /api/notifications THEN the Notification-Service SHALL return a paginated list of all notifications
2. WHEN an admin requests GET /api/notifications/{id} THEN the Notification-Service SHALL return the notification with the specified ID
3. WHEN a user requests GET /api/notifications/user/{email} THEN the Notification-Service SHALL return only notifications for that email address
4. WHEN an admin requests GET /api/notifications/status/{status} THEN the Notification-Service SHALL return all notifications with the specified status
5. WHEN an admin requests GET /api/notifications/stats THEN the Notification-Service SHALL return statistics including total, sent, failed, pending counts and success rate
6. WHEN an admin requests POST /api/notifications/{id}/retry THEN the Notification-Service SHALL retry sending the failed notification
7. WHEN an admin requests GET /api/notifications/failed THEN the Notification-Service SHALL return all notifications with status FAILED
8. WHEN an admin requests GET /api/notifications/search THEN the Notification-Service SHALL filter notifications by destinataire, type, status, and date range

### Requirement 7

**User Story:** As a system operator, I want automatic retry with exponential backoff for failed emails, so that temporary failures are handled gracefully.

#### Acceptance Criteria

1. WHEN an email send fails with MessagingException THEN the Notification-Service SHALL retry up to 3 times
2. WHEN retrying a failed email THEN the Notification-Service SHALL wait 5 seconds before the first retry
3. WHEN retrying a failed email THEN the Notification-Service SHALL use exponential backoff with intervals 5s, 10s, 20s
4. WHEN an email send fails after 3 retries THEN the Notification-Service SHALL mark the notification as FAILED
5. WHEN an email send fails after 3 retries THEN the Notification-Service SHALL send the event to the notifications-dlq topic
6. WHEN an IllegalArgumentException occurs THEN the Notification-Service SHALL not retry and mark as FAILED immediately
7. WHEN a retry succeeds THEN the Notification-Service SHALL mark the notification as SENT and stop retrying

### Requirement 8

**User Story:** As a system operator, I want circuit breaker protection for the email service, so that the system degrades gracefully when the SMTP server is unavailable.

#### Acceptance Criteria

1. WHEN 50% of email sends fail within a sliding window of 10 calls THEN the Notification-Service SHALL open the circuit breaker
2. WHEN the circuit breaker is open THEN the Notification-Service SHALL reject email send attempts for 60 seconds
3. WHEN 60 seconds have elapsed in open state THEN the Notification-Service SHALL transition to half-open state
4. WHEN in half-open state THEN the Notification-Service SHALL permit 3 test calls to the email service
5. WHEN test calls succeed in half-open state THEN the Notification-Service SHALL close the circuit breaker
6. WHEN test calls fail in half-open state THEN the Notification-Service SHALL reopen the circuit breaker for another 60 seconds
7. WHEN the circuit breaker opens THEN the Notification-Service SHALL execute the fallback method to mark notifications as FAILED

### Requirement 9

**User Story:** As a system operator, I want timeout protection for email sends, so that slow SMTP connections do not block the system.

#### Acceptance Criteria

1. WHEN an email send operation is initiated THEN the Notification-Service SHALL apply a 30-second timeout
2. WHEN an email send exceeds 30 seconds THEN the Notification-Service SHALL cancel the operation and throw TimeoutException
3. WHEN a timeout occurs THEN the Notification-Service SHALL mark the notification as FAILED with error message "Email send timeout"
4. WHEN a timeout occurs THEN the Notification-Service SHALL trigger the retry mechanism

### Requirement 10

**User Story:** As a system operator, I want concurrent email sending to be limited, so that the SMTP server is not overwhelmed.

#### Acceptance Criteria

1. WHEN processing notifications THEN the Notification-Service SHALL limit concurrent email sends to 10 simultaneous operations
2. WHEN the concurrent limit is reached THEN the Notification-Service SHALL queue additional requests with a maximum wait time of 5 seconds
3. WHEN the wait time exceeds 5 seconds THEN the Notification-Service SHALL reject the request with BulkheadFullException
4. WHEN a bulkhead rejection occurs THEN the Notification-Service SHALL mark the notification as FAILED and trigger retry

### Requirement 11

**User Story:** As a system administrator, I want failed notifications to be sent to a Dead Letter Queue, so that they can be investigated and reprocessed.

#### Acceptance Criteria

1. WHEN a notification fails after all retries THEN the Notification-Service SHALL publish the event to the notifications-dlq topic
2. WHEN a DLQ consumer receives a message THEN the Notification-Service SHALL log the failure details
3. WHEN a DLQ consumer receives a message THEN the Notification-Service SHALL persist the failure in a notification_dlq table
4. WHEN an admin requests POST /api/notifications/dlq/retry-all THEN the Notification-Service SHALL reprocess all messages from the DLQ
5. WHEN reprocessing a DLQ message succeeds THEN the Notification-Service SHALL remove it from the DLQ and mark as SENT
6. WHEN reprocessing a DLQ message fails THEN the Notification-Service SHALL leave it in the DLQ for manual investigation

### Requirement 12

**User Story:** As a system administrator, I want REST endpoints to be secured with JWT, so that only authorized users can access notification data.

#### Acceptance Criteria

1. WHEN a request is made to any /api/notifications endpoint THEN the Notification-Service SHALL require a valid JWT token in the Authorization header
2. WHEN a request is made to admin endpoints THEN the Notification-Service SHALL verify the user has ROLE_ADMIN
3. WHEN a user requests their own notifications THEN the Notification-Service SHALL verify the email in the JWT matches the requested email
4. WHEN a JWT is invalid or expired THEN the Notification-Service SHALL return HTTP 401 Unauthorized
5. WHEN a user lacks required role THEN the Notification-Service SHALL return HTTP 403 Forbidden
6. WHEN extracting user info from JWT THEN the Notification-Service SHALL populate the SecurityContext with email and roles

### Requirement 13

**User Story:** As a system operator, I want comprehensive logging and metrics, so that I can monitor notification delivery and diagnose issues.

#### Acceptance Criteria

1. WHEN a notification is successfully sent THEN the Notification-Service SHALL log at INFO level with notificationId, destinataire, and type
2. WHEN a notification fails THEN the Notification-Service SHALL log at ERROR level with notificationId, destinataire, type, and error message
3. WHEN a retry is attempted THEN the Notification-Service SHALL log at INFO level with attempt number
4. WHEN the circuit breaker opens THEN the Notification-Service SHALL log at WARN level
5. WHEN a notification is sent THEN the Notification-Service SHALL increment the notifications.sent.total counter metric
6. WHEN a notification fails THEN the Notification-Service SHALL increment the notifications.failed.total counter metric
7. WHEN an email is sent THEN the Notification-Service SHALL record the duration in the notifications.email.send.duration histogram metric

### Requirement 14

**User Story:** As a developer, I want comprehensive tests for the notification service, so that I can ensure reliability and prevent regressions.

#### Acceptance Criteria

1. WHEN running unit tests THEN the Notification-Service SHALL achieve at least 70% code coverage
2. WHEN testing the Kafka consumer THEN the Notification-Service SHALL use EmbeddedKafka to simulate event consumption
3. WHEN testing email sending THEN the Notification-Service SHALL mock JavaMailSender to avoid actual SMTP calls
4. WHEN testing persistence THEN the Notification-Service SHALL use an in-memory H2 database
5. WHEN testing resilience THEN the Notification-Service SHALL simulate failures to verify circuit breaker and retry behavior
6. WHEN testing the DLQ THEN the Notification-Service SHALL verify messages are sent to the DLQ topic after failures
7. WHEN testing REST endpoints THEN the Notification-Service SHALL verify authentication and authorization rules

### Requirement 15

**User Story:** As a system operator, I want the service to be configurable via properties, so that I can adjust behavior without code changes.

#### Acceptance Criteria

1. WHEN the application starts THEN the Notification-Service SHALL load SMTP configuration from spring.mail.* properties
2. WHEN the application starts THEN the Notification-Service SHALL load Kafka configuration from spring.kafka.* properties
3. WHEN the application starts THEN the Notification-Service SHALL load database configuration from spring.datasource.* properties
4. WHEN the application starts THEN the Notification-Service SHALL load resilience configuration from resilience4j.* properties
5. WHEN the application starts THEN the Notification-Service SHALL load email sender address from notification.email.from property
6. WHEN sensitive properties are needed THEN the Notification-Service SHALL read them from environment variables (DB_USERNAME, DB_PASSWORD, MAILTRAP_USERNAME, MAILTRAP_PASSWORD)
7. WHEN the application starts THEN the Notification-Service SHALL register with Eureka Server using the configured service URL
