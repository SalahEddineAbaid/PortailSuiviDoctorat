# Design Document - Notification Service Finalisation

## Overview

The notification-service is a Spring Boot microservice responsible for sending email notifications to users in the doctoral management system. It consumes notification events from Kafka, processes them using HTML templates, sends emails via SMTP, and maintains a complete audit trail of all notifications. The service implements comprehensive resilience patterns including retry with exponential backoff, circuit breaker, timeout protection, and bulkhead isolation.

### Key Responsibilities

- Consume notification events from Kafka topics
- Validate notification data (email format, notification type)
- Load and interpolate HTML email templates
- Send emails via SMTP using JavaMailSender
- Persist notification history with status tracking
- Implement resilience patterns for fault tolerance
- Provide REST API for notification querying and management
- Handle failed notifications via Dead Letter Queue (DLQ)
- Secure endpoints with JWT authentication
- Expose metrics and logging for observability

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

### Component Interaction Flow

1. **Event Reception**: NotificationConsumer listens to Kafka notifications topic
2. **Validation**: NotificationProcessingService validates email and type
3. **Template Processing**: EmailTemplateService loads and interpolates HTML template
4. **Email Sending**: EmailService sends email with resilience patterns applied
5. **Persistence**: NotificationHistoryService persists notification with status
6. **Error Handling**: Failed notifications sent to DLQ for retry/investigation


## Components and Interfaces

### 1. NotificationConsumer

**Responsibility**: Consume notification events from Kafka and delegate processing

**Key Methods**:
- `@KafkaListener consumeNotification(NotificationDTO dto)`: Listens to notifications topic
- Deserializes JSON messages into NotificationDTO objects
- Delegates to NotificationProcessingService for processing
- Handles deserialization errors gracefully

**Dependencies**: NotificationProcessingService, KafkaTemplate

### 2. NotificationProcessingService

**Responsibility**: Orchestrate the notification processing workflow

**Key Methods**:
- `processNotification(NotificationDTO dto)`: Main orchestration method
- `validateNotification(NotificationDTO dto)`: Validates email and type
- `selectTemplate(TypeNotification type)`: Selects appropriate template
- `prepareTemplateVariables(Map<String, Object> donnees)`: Prepares variables for interpolation
- `handleSuccess(Notification notification)`: Updates status to SENT
- `handleFailure(Notification notification, Exception e)`: Updates status to FAILED
- `sendToDLQ(NotificationDTO dto)`: Publishes to DLQ topic

**Dependencies**: EmailTemplateService, EmailService, NotificationHistoryService, KafkaTemplate

### 3. EmailTemplateService

**Responsibility**: Load and process HTML email templates

**Key Methods**:
- `loadTemplate(String templateName)`: Loads template from resources
- `interpolateVariables(String template, Map<String, Object> variables)`: Replaces {{variable}} placeholders
- `getTemplateForNotificationType(TypeNotification type)`: Maps type to template file
- `getFallbackTemplate()`: Returns generic template

**Template Mapping**:
- INSCRIPTION_SOUMISE_DIRECTEUR → template_inscription_soumise_directeur.html
- INSCRIPTION_VALIDEE_DIRECTEUR → template_inscription_validee_directeur_doctorant.html
- INSCRIPTION_REJETEE_DIRECTEUR → template_inscription_rejetee_directeur.html
- (21 total notification types)

**Dependencies**: ResourceLoader


### 4. EmailService

**Responsibility**: Send emails via SMTP with resilience patterns

**Key Methods**:
- `sendEmail(String to, String subject, String htmlBody)`: Sends HTML email
- `sendEmailWithAttachment(String to, String subject, String htmlBody, File attachment)`: Sends email with attachment
- `fallbackSendEmail(Exception e)`: Fallback method when circuit breaker opens

**Resilience Annotations**:
- `@CircuitBreaker(name = "emailService", fallbackMethod = "fallbackSendEmail")`
- `@Retry(name = "emailService")`
- `@TimeLimiter(name = "emailService")`
- `@Bulkhead(name = "emailService")`

**Configuration**:
- Sender: noreply@portail-doctorat.ma
- Content-Type: text/html; charset=UTF-8
- SMTP: Mailtrap for testing

**Dependencies**: JavaMailSender, MetricsService

### 5. NotificationHistoryService

**Responsibility**: Persist and query notification history

**Key Methods**:
- `saveNotification(Notification notification)`: Persists new notification
- `updateNotificationStatus(Long id, StatutNotification status)`: Updates status
- `incrementRetryCount(Long id)`: Increments retry counter
- `getNotificationsByUser(String email, Pageable pageable)`: Queries by user
- `getNotificationsByStatus(StatutNotification status, Pageable pageable)`: Queries by status
- `getNotificationsStats()`: Calculates statistics
- `retryFailedNotification(Long id)`: Retries a failed notification

**Dependencies**: NotificationRepository

### 6. NotificationController

**Responsibility**: Expose REST API for notification management

**Endpoints**:
- `GET /api/notifications`: List all notifications (paginated)
- `GET /api/notifications/{id}`: Get notification by ID
- `GET /api/notifications/user/{email}`: Get user's notifications
- `GET /api/notifications/status/{status}`: Get notifications by status
- `GET /api/notifications/stats`: Get notification statistics
- `POST /api/notifications/{id}/retry`: Retry failed notification
- `GET /api/notifications/failed`: Get all failed notifications
- `GET /api/notifications/search`: Search with filters
- `POST /api/notifications/dlq/retry-all`: Retry all DLQ messages

**Security**: All endpoints require JWT authentication, admin endpoints require ROLE_ADMIN

**Dependencies**: NotificationHistoryService, NotificationProcessingService


### 7. DLQConsumer

**Responsibility**: Consume and persist failed notifications from DLQ

**Key Methods**:
- `@KafkaListener consumeDLQMessage(NotificationDTO dto)`: Listens to notifications-dlq topic
- Logs DLQ message details
- Persists to notification_dlq table for audit

**Dependencies**: NotificationDLQRepository

### 8. MetricsService

**Responsibility**: Track notification metrics using Micrometer

**Metrics**:
- `notifications.sent.total`: Counter for successfully sent notifications
- `notifications.failed.total`: Counter for failed notifications
- `notifications.pending.total`: Counter for pending notifications
- `notifications.retry.total`: Counter for retry attempts
- `notifications.dlq.total`: Counter for DLQ messages
- `notifications.email.send.duration`: Histogram for email send duration

**Dependencies**: MeterRegistry

### 9. SecurityConfig

**Responsibility**: Configure JWT-based security

**Key Components**:
- `SecurityFilterChain`: Configures HTTP security
- `JwtAuthenticationFilter`: Validates JWT tokens
- `JwtTokenProvider`: Extracts claims from JWT

**Security Rules**:
- All /api/notifications/** endpoints require authentication
- Admin endpoints require ROLE_ADMIN
- User endpoints verify email matching
- CORS configured for Angular frontend

**Dependencies**: JwtTokenProvider


## Data Models

### Notification Entity

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;
    
    @Column(nullable = false)
    private String destinataire;
    
    @Column(nullable = false)
    private String sujet;
    
    @Column(columnDefinition = "TEXT")
    private String messageTexte;
    
    @Column(columnDefinition = "TEXT")
    private String messageHtml;
    
    @Column(columnDefinition = "JSON")
    private String donnees;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutNotification statut;
    
    @Enumerated(EnumType.STRING)
    private PrioriteNotification priorite;
    
    private LocalDateTime dateCreation;
    private LocalDateTime dateEnvoi;
    
    private Integer nombreTentatives = 0;
    
    @Column(columnDefinition = "TEXT")
    private String erreurMessage;
}
```

### NotificationDTO

```java
public class NotificationDTO {
    private TypeNotification type;
    private String destinataire;
    private String sujet;
    private Map<String, Object> donnees;
    private PrioriteNotification priorite;
}
```

### Enums

```java
public enum TypeNotification {
    INSCRIPTION_SOUMISE_DIRECTEUR,
    INSCRIPTION_VALIDEE_DIRECTEUR,
    INSCRIPTION_REJETEE_DIRECTEUR,
    INSCRIPTION_VALIDEE_ADMIN,
    INSCRIPTION_REJETEE_ADMIN,
    DEROGATION_DEMANDEE,
    DEMANDE_SOUTENANCE_SOUMISE_DIRECTEUR,
    JURY_PROPOSE_ADMIN,
    JURY_MEMBRE_INVITE,
    JURY_MEMBRE_ACCEPTE_DIRECTEUR,
    JURY_MEMBRE_DECLINE_DIRECTEUR,
    RAPPORT_SOUMIS_DIRECTEUR,
    AUTORISATION_SOUTENANCE_DOCTORANT,
    SOUTENANCE_PLANIFIEE_TOUS,
    CAMPAGNE_OUVERTE,
    CAMPAGNE_FERMEE,
    ALERTE_DUREE
}

public enum StatutNotification {
    PENDING,
    SENT,
    FAILED,
    RETRYING
}

public enum PrioriteNotification {
    NORMALE,
    HAUTE,
    URGENTE
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Kafka deserialization
*For any* valid NotificationDTO serialized to Kafka format, deserializing it should produce an equivalent NotificationDTO with all fields preserved
**Validates: Requirements 1.1**

### Property 2: Email validation
*For any* string, the email validation should correctly identify whether it matches valid email format (contains @, has domain, etc.)
**Validates: Requirements 1.2**

### Property 3: Type validation
*For any* NotificationDTO, the type validation should accept only recognized TypeNotification enum values and reject all others
**Validates: Requirements 1.3**

### Property 4: Template selection
*For any* recognized TypeNotification type, the template selection should return the correct template file name corresponding to that type
**Validates: Requirements 1.4**

### Property 5: Variable interpolation
*For any* template string with {{variable}} placeholders and any data map, all placeholders should be replaced with corresponding values from the map
**Validates: Requirements 1.5**

### Property 6: Success persistence
*For any* notification that is successfully sent, the persisted record should have status SENT and a non-null dateEnvoi
**Validates: Requirements 1.6**

### Property 7: Failure persistence
*For any* notification that fails to send, the persisted record should have status FAILED and contain the error message
**Validates: Requirements 1.7**

### Property 8: JavaMailSender usage
*For any* email send operation, the JavaMailSender.send() method should be invoked
**Validates: Requirements 2.1**

### Property 9: Sender address
*For any* email sent, the from address should always be noreply@portail-doctorat.ma
**Validates: Requirements 2.2**

### Property 10: Content type
*For any* email sent, the Content-Type header should be set to text/html; charset=UTF-8
**Validates: Requirements 2.3**

### Property 11: Template loading
*For any* valid template name, the template should be loaded from src/main/resources/templates/emails/ directory
**Validates: Requirements 2.4**

### Property 12: Missing variable handling
*For any* template with {{variable}} placeholders and incomplete data map, missing variables should be replaced with empty strings
**Validates: Requirements 2.6**

### Property 13: Template fallback
*For any* non-existent template name, the system should return the generic fallback template
**Validates: Requirements 2.7**

### Property 14: Initial persistence
*For any* notification received from Kafka, the initial persisted record should have status PENDING
**Validates: Requirements 5.1**

### Property 15: Success status update
*For any* notification marked as successfully sent, the status should be updated to SENT and dateEnvoi should be set to current time
**Validates: Requirements 5.2**

### Property 16: Failure status update
*For any* notification that fails, the status should be updated to FAILED and erreurMessage should contain the exception message
**Validates: Requirements 5.3**

### Property 17: Retry counter increment
*For any* notification that is retried, the nombreTentatives counter should increment by exactly 1
**Validates: Requirements 5.4**


### Property 18: Retry status update
*For any* notification being retried, the status should be updated to RETRYING during the retry process
**Validates: Requirements 5.5**

### Property 19: Data serialization
*For any* notification with a donnees map, the persisted data should be valid JSON that can be deserialized back to an equivalent map
**Validates: Requirements 5.6**

### Property 20: Dual message storage
*For any* notification persisted, both messageTexte and messageHtml fields should be populated
**Validates: Requirements 5.7**

### Property 21: Notification retrieval by ID
*For any* persisted notification, retrieving it by ID should return a notification with matching ID and all fields preserved
**Validates: Requirements 6.2**

### Property 22: User notifications filtering
*For any* email address and set of notifications, filtering by that email should return only notifications where destinataire matches the email
**Validates: Requirements 6.3**

### Property 23: Status filtering
*For any* status and set of notifications, filtering by that status should return only notifications with matching status
**Validates: Requirements 6.4**

### Property 24: Statistics accuracy
*For any* set of notifications with known distribution, the calculated statistics (total, sent, failed, pending counts and success rate) should match the expected values
**Validates: Requirements 6.5**

### Property 25: Retry triggering
*For any* failed notification, triggering a retry should result in the notification being reprocessed
**Validates: Requirements 6.6**

### Property 26: Failed notifications query
*For any* set of notifications, querying for failed notifications should return only those with status FAILED
**Validates: Requirements 6.7**

### Property 27: Search filtering
*For any* search criteria (destinataire, type, status, date range) and set of notifications, the search results should include only notifications matching all specified criteria
**Validates: Requirements 6.8**

### Property 28: Retry count
*For any* notification that fails with MessagingException, the system should retry exactly 3 times before marking as FAILED
**Validates: Requirements 7.1**

### Property 29: Final failure status
*For any* notification that fails after 3 retries, the final status should be FAILED
**Validates: Requirements 7.4**

### Property 30: DLQ publishing after retries
*For any* notification that fails after all retries, an event should be published to the notifications-dlq topic
**Validates: Requirements 7.5**

### Property 31: Retry success handling
*For any* notification that succeeds on retry, the status should be SENT and no further retries should occur
**Validates: Requirements 7.7**

### Property 32: Circuit breaker threshold
*For any* sequence of email sends where 50% or more fail within a sliding window of 10 calls, the circuit breaker should open
**Validates: Requirements 8.1**

### Property 33: Timeout application
*For any* email send operation, a 30-second timeout should be configured
**Validates: Requirements 9.1**

### Property 34: Concurrency limit
*For any* set of concurrent email send operations, the system should limit concurrent executions to 10 simultaneous operations
**Validates: Requirements 10.1**

### Property 35: DLQ persistence
*For any* message received on the DLQ topic, it should be persisted in the notification_dlq table
**Validates: Requirements 11.3**


### Property 36: JWT requirement
*For any* request to /api/notifications endpoints without a valid JWT token, the system should return HTTP 401 Unauthorized
**Validates: Requirements 12.1**

### Property 37: Admin role verification
*For any* request to admin endpoints without ROLE_ADMIN, the system should return HTTP 403 Forbidden
**Validates: Requirements 12.2**

### Property 38: Email matching verification
*For any* user request for notifications, the email in the JWT should match the requested email or the request should be rejected
**Validates: Requirements 12.3**

### Property 39: Invalid JWT rejection
*For any* invalid or expired JWT token, the system should return HTTP 401 Unauthorized
**Validates: Requirements 12.4**

### Property 40: Insufficient role rejection
*For any* request with insufficient role permissions, the system should return HTTP 403 Forbidden
**Validates: Requirements 12.5**

### Property 41: SecurityContext population
*For any* valid JWT token, the SecurityContext should be populated with the correct email and roles extracted from the token
**Validates: Requirements 12.6**

### Property 42: Sent counter increment
*For any* notification successfully sent, the notifications.sent.total counter metric should increment by exactly 1
**Validates: Requirements 13.5**

### Property 43: Failed counter increment
*For any* notification that fails, the notifications.failed.total counter metric should increment by exactly 1
**Validates: Requirements 13.6**

### Property 44: Duration recording
*For any* email send operation, the duration should be recorded in the notifications.email.send.duration histogram metric
**Validates: Requirements 13.7**


## Error Handling

### Exception Hierarchy

```java
NotificationServiceException (base)
├── InvalidEmailException
├── InvalidNotificationTypeException
├── TemplateNotFoundException
└── EmailSendException
```

### Error Handling Strategy

**Validation Errors**:
- Invalid email format → InvalidEmailException → Mark as FAILED immediately, no retry
- Invalid notification type → InvalidNotificationTypeException → Mark as FAILED immediately, no retry
- Missing template → TemplateNotFoundException → Use fallback template

**Email Sending Errors**:
- MessagingException → Retry up to 3 times with exponential backoff
- TimeoutException → Retry up to 3 times
- IllegalArgumentException → Mark as FAILED immediately, no retry
- After 3 retries → Mark as FAILED, send to DLQ

**Circuit Breaker**:
- When open → Execute fallback method, mark notification as FAILED
- Fallback method logs error and returns gracefully

**Bulkhead**:
- When full → BulkheadFullException → Mark as FAILED, trigger retry

### Global Exception Handler

`@RestControllerAdvice` handles all REST API exceptions:
- InvalidEmailException → 400 Bad Request
- InvalidNotificationTypeException → 400 Bad Request
- TemplateNotFoundException → 500 Internal Server Error
- EmailSendException → 500 Internal Server Error
- UnauthorizedException → 401 Unauthorized
- ForbiddenException → 403 Forbidden
- Generic exceptions → 500 Internal Server Error

All error responses follow standard format:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid email format: invalid-email",
  "path": "/api/notifications"
}
```


## Testing Strategy

### Dual Testing Approach

The notification-service employs both unit testing and property-based testing to ensure comprehensive coverage:

**Unit Tests**:
- Verify specific examples and edge cases
- Test integration points between components
- Mock external dependencies (JavaMailSender, Kafka, Database)
- Focus on concrete scenarios and error conditions
- Target: 70% code coverage minimum

**Property-Based Tests**:
- Verify universal properties that should hold across all inputs
- Use JUnit QuickCheck for property-based testing in Java
- Generate random test data to explore input space
- Each property test runs minimum 100 iterations
- Each property-based test tagged with: `**Feature: notification-service-finalisation, Property {number}: {property_text}**`
- Each correctness property implemented by a SINGLE property-based test

### Property-Based Testing Framework

**Library**: JUnit QuickCheck (net.java.quickcheck:quickcheck)

**Configuration**:
- Minimum 100 iterations per property test
- Custom generators for domain objects (NotificationDTO, TypeNotification, etc.)
- Edge case generators for invalid inputs

**Test Organization**:
- Property tests in separate test classes (e.g., `NotificationPropertiesTest`)
- Each property test method annotated with `@Property`
- Clear naming: `property{N}_{PropertyName}` (e.g., `property1_KafkaDeserialization`)

### Unit Testing Strategy

**Test Coverage**:
- NotificationConsumerTest: Kafka message consumption and error handling
- EmailServiceTest: Email sending with mocked JavaMailSender
- EmailTemplateServiceTest: Template loading and variable interpolation
- NotificationHistoryServiceTest: Persistence operations with mocked repository
- NotificationProcessingServiceTest: Orchestration logic and error handling
- NotificationControllerTest: REST endpoints with mocked services

**Testing Tools**:
- JUnit 5 for test framework
- Mockito for mocking dependencies
- EmbeddedKafka for Kafka integration tests
- H2 in-memory database for persistence tests
- MockMvc for REST API testing

### Integration Testing Strategy

**Integration Test Scenarios**:
- NotificationFlowIntegrationTest: End-to-end notification flow with EmbeddedKafka and H2
- ResilienceIntegrationTest: Circuit breaker, retry, and timeout behavior
- DLQIntegrationTest: Dead Letter Queue flow and reprocessing
- SecurityIntegrationTest: JWT authentication and authorization

**Testing Tools**:
- Testcontainers for MariaDB (optional, can use H2)
- EmbeddedKafka for Kafka broker simulation
- Spring Boot Test for application context

### Test Data Generators

**Custom Generators for Property Tests**:
- NotificationDTOGenerator: Generates random valid NotificationDTOs
- InvalidEmailGenerator: Generates invalid email formats for validation testing
- TemplateVariablesGenerator: Generates random variable maps
- TypeNotificationGenerator: Generates all notification types

**Generator Requirements**:
- Produce both valid and edge-case data
- Cover all enum values
- Generate boundary conditions (empty strings, null values, max lengths)

