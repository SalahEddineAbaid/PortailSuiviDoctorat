# Implementation Plan

- [x] 1. Set up project dependencies and configuration





  - Add Spring Kafka dependencies to pom.xml
  - Add Spring Mail dependencies to pom.xml
  - Add Resilience4j dependencies (circuit-breaker, retry, timelimiter, bulkhead) to pom.xml
  - Add JUnit QuickCheck for property-based testing to pom.xml
  - Add Testcontainers and EmbeddedKafka for integration testing to pom.xml
  - Configure application.properties with Kafka, MariaDB, Mailtrap, and Resilience4j settings
  - Create database schema for notifications table
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7_

- [x] 2. Implement core data models and enums





  - Create TypeNotification enum with all 21 notification types
  - Create StatutNotification enum (PENDING, SENT, FAILED, RETRYING)
  - Create PrioriteNotification enum (NORMALE, HAUTE, URGENTE)
  - Create Notification entity with all fields and JPA annotations
  - Create NotificationDTO class
  - Create NotificationStatsDTO class
  - Create NotificationRepository with custom query methods
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

- [ ]* 2.1 Write property test for data serialization
  - **Property 19: Data serialization**
  - **Validates: Requirements 5.6**

- [ ]* 2.2 Write property test for dual message storage
  - **Property 20: Dual message storage**
  - **Validates: Requirements 5.7**

- [ ]* 2.3 Write property test for retry status update
  - **Property 18: Retry status update**
  - **Validates: Requirements 5.5**

- [x] 3. Implement EmailTemplateService





  - Create EmailTemplateService class
  - Implement loadTemplate method to read HTML files from resources
  - Implement interpolateVariables method with {{variable}} replacement
  - Implement getTemplateForNotificationType method with type-to-template mapping
  - Handle missing variables by replacing with empty string
  - Implement fallback to generic template if specific template not found
  - _Requirements: 2.4, 2.5, 2.6, 2.7_

- [ ]* 3.1 Write property test for template loading
  - **Property 11: Template loading**
  - **Validates: Requirements 2.4**

- [ ]* 3.2 Write property test for variable interpolation
  - **Property 5: Variable interpolation**
  - **Validates: Requirements 1.5**

- [ ]* 3.3 Write property test for missing variable handling
  - **Property 12: Missing variable handling**
  - **Validates: Requirements 2.6**

- [ ]* 3.4 Write property test for template fallback
  - **Property 13: Template fallback**
  - **Validates: Requirements 2.7**

- [x] 4. Create HTML email templates





  - Create template_inscription_soumise_directeur.html
  - Create template_inscription_validee_directeur_doctorant.html
  - Create template_inscription_validee_directeur_admin.html
  - Create template_inscription_rejetee_directeur.html
  - Create template_inscription_validee_admin.html
  - Create template_inscription_rejetee_admin.html
  - Create template_derogation_demandee.html
  - Create template_demande_soutenance_soumise_directeur.html
  - Create template_jury_propose_admin.html
  - Create template_jury_membre_invite.html
  - Create template_jury_membre_accepte_directeur.html
  - Create template_jury_membre_decline_directeur.html
  - Create template_rapport_soumis_directeur.html
  - Create template_autorisation_soutenance_doctorant.html
  - Create template_soutenance_planifiee_tous.html
  - Create template_generic.html (fallback)
  - Use inline CSS, institutional colors (#003366, #FFFFFF, #F5F5F5)
  - Make templates responsive (max-width 600px)
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

- [x] 5. Implement EmailService with resilience patterns





  - Create EmailService class
  - Implement sendEmail method with JavaMailSender
  - Set sender address to noreply@portail-doctorat.ma
  - Set Content-Type to text/html; charset=UTF-8
  - Implement sendEmailWithAttachment method
  - Apply @CircuitBreaker annotation with fallback method
  - Apply @Retry annotation with exponential backoff
  - Apply @TimeLimiter annotation with 30s timeout
  - Apply @Bulkhead annotation with max 10 concurrent calls
  - Implement fallbackSendEmail method
  - _Requirements: 2.1, 2.2, 2.3, 7.1, 7.2, 7.3, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 9.1, 9.2, 10.1, 10.2_

- [ ]* 5.1 Write property test for JavaMailSender usage
  - **Property 8: JavaMailSender usage**
  - **Validates: Requirements 2.1**

- [ ]* 5.2 Write property test for sender address
  - **Property 9: Sender address**
  - **Validates: Requirements 2.2**

- [ ]* 5.3 Write property test for content type
  - **Property 10: Content type**
  - **Validates: Requirements 2.3**

- [ ]* 5.4 Write property test for retry count
  - **Property 28: Retry count**
  - **Validates: Requirements 7.1**

- [ ]* 5.5 Write property test for circuit breaker opening
  - **Property 32: Circuit breaker threshold**
  - **Validates: Requirements 8.1**

- [ ]* 5.6 Write property test for timeout application
  - **Property 33: Timeout application**
  - **Validates: Requirements 9.1**

- [ ]* 5.7 Write property test for concurrency limit
  - **Property 34: Concurrency limit**
  - **Validates: Requirements 10.1**

- [x] 6. Implement NotificationHistoryService





  - Create NotificationHistoryService class
  - Implement saveNotification method
  - Implement updateNotificationStatus method
  - Implement incrementRetryCount method
  - Implement getNotificationsByUser method
  - Implement getNotificationsByStatus method
  - Implement getNotificationsStats method with success rate calculation
  - Implement retryFailedNotification method
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.3, 6.4, 6.5, 6.6_

- [ ]* 6.1 Write property test for initial persistence
  - **Property 14: Initial persistence**
  - **Validates: Requirements 5.1**

- [ ]* 6.2 Write property test for success status update
  - **Property 15: Success status update**
  - **Validates: Requirements 5.2**

- [ ]* 6.3 Write property test for failure status update
  - **Property 16: Failure status update**
  - **Validates: Requirements 5.3**

- [ ]* 6.4 Write property test for retry counter increment
  - **Property 17: Retry counter increment**
  - **Validates: Requirements 5.4**

- [ ]* 6.5 Write property test for statistics accuracy
  - **Property 24: Statistics accuracy**
  - **Validates: Requirements 6.5**

- [x] 7. Implement NotificationProcessingService





  - Create NotificationProcessingService class
  - Implement processNotification orchestration method
  - Implement validateNotification method (email format, type validation)
  - Implement selectTemplate method
  - Implement prepareTemplateVariables method
  - Implement handleSuccess method (update status to SENT)
  - Implement handleFailure method (update status to FAILED, send to DLQ)
  - Implement sendToDLQ method with KafkaTemplate
  - _Requirements: 1.2, 1.3, 1.4, 1.6, 1.7, 7.4, 7.5, 11.1_

- [ ]* 7.1 Write property test for email validation
  - **Property 2: Email validation**
  - **Validates: Requirements 1.2**

- [ ]* 7.2 Write property test for type validation
  - **Property 3: Type validation**
  - **Validates: Requirements 1.3**

- [ ]* 7.3 Write property test for template selection
  - **Property 4: Template selection**
  - **Validates: Requirements 1.4**

- [ ]* 7.4 Write property test for success persistence
  - **Property 6: Success persistence**
  - **Validates: Requirements 1.6**

- [ ]* 7.5 Write property test for failure persistence
  - **Property 7: Failure persistence**
  - **Validates: Requirements 1.7**

- [ ]* 7.6 Write property test for DLQ publishing after retries
  - **Property 30: DLQ publishing after retries**
  - **Validates: Requirements 7.5**

- [ ]* 7.7 Write property test for final failure status
  - **Property 29: Final failure status**
  - **Validates: Requirements 7.4**

- [ ]* 7.8 Write property test for retry success handling
  - **Property 31: Retry success handling**
  - **Validates: Requirements 7.7**

- [x] 8. Implement Kafka NotificationConsumer





  - Create NotificationConsumer class
  - Implement @KafkaListener method for notifications topic
  - Configure JSON deserialization with trusted packages
  - Delegate to NotificationProcessingService
  - Handle exceptions and log errors
  - Implement error handling for deserialization failures
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

- [ ]* 8.1 Write property test for Kafka deserialization
  - **Property 1: Kafka deserialization**
  - **Validates: Requirements 1.1**

- [x] 9. Implement DLQ consumer and reprocessing





  - Create DLQConsumer class
  - Implement @KafkaListener method for notifications-dlq topic
  - Log DLQ messages with full details
  - Create NotificationDLQ entity for audit
  - Create NotificationDLQRepository
  - Persist DLQ messages in notification_dlq table
  - _Requirements: 11.2, 11.3_

- [ ]* 9.1 Write property test for DLQ persistence
  - **Property 35: DLQ persistence**
  - **Validates: Requirements 11.3**

- [x] 10. Implement NotificationController REST API











  - Create NotificationController class
  - Implement GET /api/notifications with pagination
  - Implement GET /api/notifications/{id}
  - Implement GET /api/notifications/user/{email}
  - Implement GET /api/notifications/status/{status}
  - Implement GET /api/notifications/stats
  - Implement POST /api/notifications/{id}/retry
  - Implement GET /api/notifications/failed
  - Implement GET /api/notifications/search with filters
  - Implement POST /api/notifications/dlq/retry-all
  - Add @PreAuthorize annotations for admin endpoints
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 11.4_

- [ ]* 10.1 Write property test for notification retrieval by ID
  - **Property 21: Notification retrieval by ID**
  - **Validates: Requirements 6.2**

- [ ]* 10.2 Write property test for user notifications filtering
  - **Property 22: User notifications filtering**
  - **Validates: Requirements 6.3**

- [ ]* 10.3 Write property test for status filtering
  - **Property 23: Status filtering**
  - **Validates: Requirements 6.4**

- [ ]* 10.4 Write property test for retry triggering
  - **Property 25: Retry triggering**
  - **Validates: Requirements 6.6**

- [ ]* 10.5 Write property test for failed notifications query
  - **Property 26: Failed notifications query**
  - **Validates: Requirements 6.7**

- [ ]* 10.6 Write property test for search filtering
  - **Property 27: Search filtering**
  - **Validates: Requirements 6.8**

- [x] 11. Implement JWT security configuration





  - Create SecurityConfig class with @EnableWebSecurity
  - Configure SecurityFilterChain
  - Create JwtAuthenticationFilter
  - Implement JWT token validation
  - Extract email and roles from JWT
  - Populate SecurityContext
  - Configure CORS for Angular frontend
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6_

- [ ]* 11.1 Write property test for JWT requirement
  - **Property 36: JWT requirement**
  - **Validates: Requirements 12.1**

- [ ]* 11.2 Write property test for admin role verification
  - **Property 37: Admin role verification**
  - **Validates: Requirements 12.2**

- [ ]* 11.3 Write property test for email matching verification
  - **Property 38: Email matching verification**
  - **Validates: Requirements 12.3**

- [ ]* 11.4 Write property test for invalid JWT rejection
  - **Property 39: Invalid JWT rejection**
  - **Validates: Requirements 12.4**

- [ ]* 11.5 Write property test for insufficient role rejection
  - **Property 40: Insufficient role rejection**
  - **Validates: Requirements 12.5**

- [ ]* 11.6 Write property test for SecurityContext population
  - **Property 41: SecurityContext population**
  - **Validates: Requirements 12.6**

- [x] 12. Implement custom metrics with Micrometer





  - Create MetricsService class
  - Implement counter for notifications.sent.total
  - Implement counter for notifications.failed.total
  - Implement counter for notifications.pending.total
  - Implement counter for notifications.retry.total
  - Implement counter for notifications.dlq.total
  - Implement histogram for notifications.email.send.duration
  - Integrate metrics into EmailService and NotificationProcessingService
  - _Requirements: 13.5, 13.6, 13.7_

- [ ]* 12.1 Write property test for sent counter increment
  - **Property 42: Sent counter increment**
  - **Validates: Requirements 13.5**

- [ ]* 12.2 Write property test for failed counter increment
  - **Property 43: Failed counter increment**
  - **Validates: Requirements 13.6**

- [ ]* 12.3 Write property test for duration recording
  - **Property 44: Duration recording**
  - **Validates: Requirements 13.7**

- [x] 13. Implement comprehensive logging




  - Add structured logging for notification received (DEBUG level)
  - Add logging for notification sent successfully (INFO level)
  - Add logging for notification failed (ERROR level)
  - Add logging for retry attempts (INFO level)
  - Add logging for circuit breaker state changes (WARN level)
  - Add logging for DLQ messages (WARN level)
  - Configure log levels in application.properties
  - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [x] 14. Create custom generators for property-based tests










  - Create NotificationDTOGenerator for random NotificationDTOs
  - Create InvalidEmailGenerator for invalid email formats
  - Create TemplateVariablesGenerator for random variable maps
  - Create TypeNotificationGenerator for all notification types
  - Ensure generators produce valid and edge-case data
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

- [x] 15. Write unit tests for all services







  - Write NotificationConsumerTest with mocked dependencies
  - Write EmailServiceTest with mocked JavaMailSender
  - Write EmailTemplateServiceTest
  - Write NotificationHistoryServiceTest with mocked repository
  - Write NotificationProcessingServiceTest
  - Write NotificationControllerTest with mocked service
  - Achieve 70% code coverage minimum
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

- [x] 16. Write integration tests





  - Write NotificationFlowIntegrationTest with EmbeddedKafka and H2
  - Write ResilienceIntegrationTest for circuit breaker and retry
  - Write DLQIntegrationTest for DLQ flow
  - Write SecurityIntegrationTest for JWT authentication
  - Use Testcontainers for MariaDB if needed
  - _Requirements: 14.2, 14.5, 14.6_

- [x] 17. Configure Resilience4j properties





  - Configure circuit breaker properties in application.properties
  - Configure retry properties with exponential backoff
  - Configure time limiter properties
  - Configure bulkhead properties
  - Test resilience patterns with simulated failures
  - _Requirements: 7.1, 7.2, 7.3, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 9.1, 9.2, 10.1, 10.2_

- [x] 18. Create exception handling





  - Create NotificationServiceException base class
  - Create InvalidEmailException
  - Create InvalidNotificationTypeException
  - Create TemplateNotFoundException
  - Create EmailSendException
  - Create GlobalExceptionHandler with @RestControllerAdvice
  - Return standardized error responses
  - _Requirements: 1.2, 1.3, 2.7_

- [x] 19. Configure Eureka client registration





  - Add Eureka client dependency
  - Configure eureka.client.service-url.defaultZone
  - Configure eureka.instance.prefer-ip-address
  - Test service registration with Eureka Server
  - _Requirements: 15.7_

- [x] 20. Create Swagger/OpenAPI documentation





  - Add SpringDoc OpenAPI dependency
  - Configure Swagger UI
  - Add API documentation annotations to controller
  - Add examples for request/response DTOs
  - Test Swagger UI at http://localhost:8084/swagger-ui.html
  - _Requirements: Documentation_

- [x] 21. Write README.md documentation





  - Document service description and purpose
  - Add architecture diagram
  - List prerequisites (Java 17, Maven, MariaDB, Kafka, Mailtrap)
  - Provide installation instructions
  - Document configuration properties
  - List all API endpoints
  - Document notification types and templates
  - Add testing instructions
  - Add troubleshooting guide
  - _Requirements: Documentation_

- [x] 22. Final checkpoint - Ensure all tests pass





  - Run all unit tests
  - Run all property-based tests
  - Run all integration tests
  - Verify 70% code coverage achieved
  - Verify all 55 correctness properties are tested
  - Test with Mailtrap to verify email sending
  - Test Kafka integration with inscription-service
  - Test REST API endpoints with Postman
  - Verify Eureka registration
  - Verify Actuator metrics
