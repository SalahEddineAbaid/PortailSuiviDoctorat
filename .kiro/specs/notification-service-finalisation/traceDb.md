# TRACEABILITY DB

## COVERAGE ANALYSIS

Total requirements: 99
Coverage: 44.44

## TRACEABILITY

### Property 1: Kafka deserialization

*For any* valid NotificationDTO serialized to Kafka format, deserializing it should produce an equivalent NotificationDTO with all fields preserved

**Validates**
- Criteria 1.1: WHEN a notification event is published to the notifications topic THEN the Notification-Service SHALL consume the event and deserialize it into a NotificationDTO

**Implementation tasks**
- Task 8.1: 8.1 Write property test for Kafka deserialization

**Implemented PBTs**
- No implemented PBTs found

### Property 2: Email validation

*For any* string, the email validation should correctly identify whether it matches valid email format (contains @, has domain, etc.)

**Validates**
- Criteria 1.2: WHEN a NotificationDTO is received THEN the Notification-Service SHALL validate that the destinataire field contains a valid email address

**Implementation tasks**
- Task 7.1: 7.1 Write property test for email validation

**Implemented PBTs**
- No implemented PBTs found

### Property 3: Type validation

*For any* NotificationDTO, the type validation should accept only recognized TypeNotification enum values and reject all others

**Validates**
- Criteria 1.3: WHEN a NotificationDTO is received THEN the Notification-Service SHALL validate that the type field matches a recognized TypeNotification enum value

**Implementation tasks**
- Task 7.2: 7.2 Write property test for type validation

**Implemented PBTs**
- No implemented PBTs found

### Property 4: Template selection

*For any* recognized TypeNotification type, the template selection should return the correct template file name corresponding to that type

**Validates**
- Criteria 1.4: WHEN a valid NotificationDTO is processed THEN the Notification-Service SHALL select the appropriate HTML template based on the type field

**Implementation tasks**
- Task 7.3: 7.3 Write property test for template selection

**Implemented PBTs**
- No implemented PBTs found

### Property 5: Variable interpolation

*For any* template string with {{variable}} placeholders and any data map, all placeholders should be replaced with corresponding values from the map

**Validates**
- Criteria 1.5: WHEN template variables are interpolated THEN the Notification-Service SHALL replace all {{variable}} placeholders with values from the donnees map

**Implementation tasks**
- Task 3.2: 3.2 Write property test for variable interpolation

**Implemented PBTs**
- No implemented PBTs found

### Property 6: Success persistence

*For any* notification that is successfully sent, the persisted record should have status SENT and a non-null dateEnvoi

**Validates**
- Criteria 1.6: WHEN a notification is successfully sent THEN the Notification-Service SHALL persist the notification record with status SENT

**Implementation tasks**
- Task 7.4: 7.4 Write property test for success persistence

**Implemented PBTs**
- No implemented PBTs found

### Property 7: Failure persistence

*For any* notification that fails to send, the persisted record should have status FAILED and contain the error message

**Validates**
- Criteria 1.7: WHEN a notification send fails THEN the Notification-Service SHALL persist the notification record with status FAILED and the error message

**Implementation tasks**
- Task 7.5: 7.5 Write property test for failure persistence

**Implemented PBTs**
- No implemented PBTs found

### Property 8: JavaMailSender usage

*For any* email send operation, the JavaMailSender.send() method should be invoked

**Validates**
- Criteria 2.1: WHEN an email is sent THEN the Notification-Service SHALL use JavaMailSender to send the email via SMTP

**Implementation tasks**
- Task 5.1: 5.1 Write property test for JavaMailSender usage

**Implemented PBTs**
- No implemented PBTs found

### Property 9: Sender address

*For any* email sent, the from address should always be noreply@portail-doctorat.ma

**Validates**
- Criteria 2.2: WHEN an email is sent THEN the Notification-Service SHALL set the sender address to noreply@portail-doctorat.ma

**Implementation tasks**
- Task 5.2: 5.2 Write property test for sender address

**Implemented PBTs**
- No implemented PBTs found

### Property 10: Content type

*For any* email sent, the Content-Type header should be set to text/html; charset=UTF-8

**Validates**
- Criteria 2.3: WHEN an email is sent THEN the Notification-Service SHALL set the Content-Type to text/html with charset UTF-8

**Implementation tasks**
- Task 5.3: 5.3 Write property test for content type

**Implemented PBTs**
- No implemented PBTs found

### Property 11: Template loading

*For any* valid template name, the template should be loaded from src/main/resources/templates/emails/ directory

**Validates**
- Criteria 2.4: WHEN an HTML template is loaded THEN the Notification-Service SHALL read the template file from src/main/resources/templates/emails/

**Implementation tasks**
- Task 3.1: 3.1 Write property test for template loading

**Implemented PBTs**
- No implemented PBTs found

### Property 12: Missing variable handling

*For any* template with {{variable}} placeholders and incomplete data map, missing variables should be replaced with empty strings

**Validates**
- Criteria 2.6: WHEN a template variable is missing from the data THEN the Notification-Service SHALL replace it with an empty string

**Implementation tasks**
- Task 3.3: 3.3 Write property test for missing variable handling

**Implemented PBTs**
- No implemented PBTs found

### Property 13: Template fallback

*For any* non-existent template name, the system should return the generic fallback template

**Validates**
- Criteria 2.7: WHEN a template file is not found THEN the Notification-Service SHALL use a generic fallback template

**Implementation tasks**
- Task 3.4: 3.4 Write property test for template fallback

**Implemented PBTs**
- No implemented PBTs found

### Property 14: Initial persistence

*For any* notification received from Kafka, the initial persisted record should have status PENDING

**Validates**
- Criteria 5.1: WHEN a notification is received from Kafka THEN the Notification-Service SHALL create a Notification entity with status PENDING

**Implementation tasks**
- Task 6.1: 6.1 Write property test for initial persistence

**Implemented PBTs**
- No implemented PBTs found

### Property 15: Success status update

*For any* notification marked as successfully sent, the status should be updated to SENT and dateEnvoi should be set to current time

**Validates**
- Criteria 5.2: WHEN a notification is successfully sent THEN the Notification-Service SHALL update the status to SENT and set the dateEnvoi

**Implementation tasks**
- Task 6.2: 6.2 Write property test for success status update

**Implemented PBTs**
- No implemented PBTs found

### Property 16: Failure status update

*For any* notification that fails, the status should be updated to FAILED and erreurMessage should contain the exception message

**Validates**
- Criteria 5.3: WHEN a notification send fails THEN the Notification-Service SHALL update the status to FAILED and record the erreurMessage

**Implementation tasks**
- Task 6.3: 6.3 Write property test for failure status update

**Implemented PBTs**
- No implemented PBTs found

### Property 17: Retry counter increment

*For any* notification that is retried, the nombreTentatives counter should increment by exactly 1

**Validates**
- Criteria 5.4: WHEN a notification is retried THEN the Notification-Service SHALL increment the nombreTentatives counter

**Implementation tasks**
- Task 6.4: 6.4 Write property test for retry counter increment

**Implemented PBTs**
- No implemented PBTs found

### Property 18: Retry status update

*For any* notification being retried, the status should be updated to RETRYING during the retry process

**Validates**
- Criteria 5.5: WHEN a notification is retried THEN the Notification-Service SHALL update the status to RETRYING

**Implementation tasks**
- Task 2.3: 2.3 Write property test for retry status update

**Implemented PBTs**
- No implemented PBTs found

### Property 19: Data serialization

*For any* notification with a donnees map, the persisted data should be valid JSON that can be deserialized back to an equivalent map

**Validates**
- Criteria 5.6: WHEN notification data is persisted THEN the Notification-Service SHALL serialize the donnees map to JSON format

**Implementation tasks**
- Task 2.1: 2.1 Write property test for data serialization

**Implemented PBTs**
- No implemented PBTs found

### Property 20: Dual message storage

*For any* notification persisted, both messageTexte and messageHtml fields should be populated

**Validates**
- Criteria 5.7: WHEN notification data is persisted THEN the Notification-Service SHALL store both messageTexte and messageHtml versions

**Implementation tasks**
- Task 2.2: 2.2 Write property test for dual message storage

**Implemented PBTs**
- No implemented PBTs found

### Property 21: Notification retrieval by ID

*For any* persisted notification, retrieving it by ID should return a notification with matching ID and all fields preserved

**Validates**
- Criteria 6.2: WHEN an admin requests GET /api/notifications/{id} THEN the Notification-Service SHALL return the notification with the specified ID

**Implementation tasks**
- Task 10.1: 10.1 Write property test for notification retrieval by ID

**Implemented PBTs**
- No implemented PBTs found

### Property 22: User notifications filtering

*For any* email address and set of notifications, filtering by that email should return only notifications where destinataire matches the email

**Validates**
- Criteria 6.3: WHEN a user requests GET /api/notifications/user/{email} THEN the Notification-Service SHALL return only notifications for that email address

**Implementation tasks**
- Task 10.2: 10.2 Write property test for user notifications filtering

**Implemented PBTs**
- No implemented PBTs found

### Property 23: Status filtering

*For any* status and set of notifications, filtering by that status should return only notifications with matching status

**Validates**
- Criteria 6.4: WHEN an admin requests GET /api/notifications/status/{status} THEN the Notification-Service SHALL return all notifications with the specified status

**Implementation tasks**
- Task 10.3: 10.3 Write property test for status filtering

**Implemented PBTs**
- No implemented PBTs found

### Property 24: Statistics accuracy

*For any* set of notifications with known distribution, the calculated statistics (total, sent, failed, pending counts and success rate) should match the expected values

**Validates**
- Criteria 6.5: WHEN an admin requests GET /api/notifications/stats THEN the Notification-Service SHALL return statistics including total, sent, failed, pending counts and success rate

**Implementation tasks**
- Task 6.5: 6.5 Write property test for statistics accuracy

**Implemented PBTs**
- No implemented PBTs found

### Property 25: Retry triggering

*For any* failed notification, triggering a retry should result in the notification being reprocessed

**Validates**
- Criteria 6.6: WHEN an admin requests POST /api/notifications/{id}/retry THEN the Notification-Service SHALL retry sending the failed notification

**Implementation tasks**
- Task 10.4: 10.4 Write property test for retry triggering

**Implemented PBTs**
- No implemented PBTs found

### Property 26: Failed notifications query

*For any* set of notifications, querying for failed notifications should return only those with status FAILED

**Validates**
- Criteria 6.7: WHEN an admin requests GET /api/notifications/failed THEN the Notification-Service SHALL return all notifications with status FAILED

**Implementation tasks**
- Task 10.5: 10.5 Write property test for failed notifications query

**Implemented PBTs**
- No implemented PBTs found

### Property 27: Search filtering

*For any* search criteria (destinataire, type, status, date range) and set of notifications, the search results should include only notifications matching all specified criteria

**Validates**
- Criteria 6.8: WHEN an admin requests GET /api/notifications/search THEN the Notification-Service SHALL filter notifications by destinataire, type, status, and date range

**Implementation tasks**
- Task 10.6: 10.6 Write property test for search filtering

**Implemented PBTs**
- No implemented PBTs found

### Property 28: Retry count

*For any* notification that fails with MessagingException, the system should retry exactly 3 times before marking as FAILED

**Validates**
- Criteria 7.1: WHEN an email send fails with MessagingException THEN the Notification-Service SHALL retry up to 3 times

**Implementation tasks**
- Task 5.4: 5.4 Write property test for retry count

**Implemented PBTs**
- No implemented PBTs found

### Property 29: Final failure status

*For any* notification that fails after 3 retries, the final status should be FAILED

**Validates**
- Criteria 7.4: WHEN an email send fails after 3 retries THEN the Notification-Service SHALL mark the notification as FAILED

**Implementation tasks**
- Task 7.7: 7.7 Write property test for final failure status

**Implemented PBTs**
- No implemented PBTs found

### Property 30: DLQ publishing after retries

*For any* notification that fails after all retries, an event should be published to the notifications-dlq topic

**Validates**
- Criteria 7.5: WHEN an email send fails after 3 retries THEN the Notification-Service SHALL send the event to the notifications-dlq topic

**Implementation tasks**
- Task 7.6: 7.6 Write property test for DLQ publishing after retries

**Implemented PBTs**
- No implemented PBTs found

### Property 31: Retry success handling

*For any* notification that succeeds on retry, the status should be SENT and no further retries should occur

**Validates**
- Criteria 7.7: WHEN a retry succeeds THEN the Notification-Service SHALL mark the notification as SENT and stop retrying

**Implementation tasks**
- Task 7.8: 7.8 Write property test for retry success handling

**Implemented PBTs**
- No implemented PBTs found

### Property 32: Circuit breaker threshold

*For any* sequence of email sends where 50% or more fail within a sliding window of 10 calls, the circuit breaker should open

**Validates**
- Criteria 8.1: WHEN 50% of email sends fail within a sliding window of 10 calls THEN the Notification-Service SHALL open the circuit breaker

**Implementation tasks**
- Task 5.5: 5.5 Write property test for circuit breaker opening

**Implemented PBTs**
- No implemented PBTs found

### Property 33: Timeout application

*For any* email send operation, a 30-second timeout should be configured

**Validates**
- Criteria 9.1: WHEN an email send operation is initiated THEN the Notification-Service SHALL apply a 30-second timeout

**Implementation tasks**
- Task 5.6: 5.6 Write property test for timeout application

**Implemented PBTs**
- No implemented PBTs found

### Property 34: Concurrency limit

*For any* set of concurrent email send operations, the system should limit concurrent executions to 10 simultaneous operations

**Validates**
- Criteria 10.1: WHEN processing notifications THEN the Notification-Service SHALL limit concurrent email sends to 10 simultaneous operations

**Implementation tasks**
- Task 5.7: 5.7 Write property test for concurrency limit

**Implemented PBTs**
- No implemented PBTs found

### Property 35: DLQ persistence

*For any* message received on the DLQ topic, it should be persisted in the notification_dlq table

**Validates**
- Criteria 11.3: WHEN a DLQ consumer receives a message THEN the Notification-Service SHALL persist the failure in a notification_dlq table

**Implementation tasks**
- Task 9.1: 9.1 Write property test for DLQ persistence

**Implemented PBTs**
- No implemented PBTs found

### Property 36: JWT requirement

*For any* request to /api/notifications endpoints without a valid JWT token, the system should return HTTP 401 Unauthorized

**Validates**
- Criteria 12.1: WHEN a request is made to any /api/notifications endpoint THEN the Notification-Service SHALL require a valid JWT token in the Authorization header

**Implementation tasks**
- Task 11.1: 11.1 Write property test for JWT requirement

**Implemented PBTs**
- No implemented PBTs found

### Property 37: Admin role verification

*For any* request to admin endpoints without ROLE_ADMIN, the system should return HTTP 403 Forbidden

**Validates**
- Criteria 12.2: WHEN a request is made to admin endpoints THEN the Notification-Service SHALL verify the user has ROLE_ADMIN

**Implementation tasks**
- Task 11.2: 11.2 Write property test for admin role verification

**Implemented PBTs**
- No implemented PBTs found

### Property 38: Email matching verification

*For any* user request for notifications, the email in the JWT should match the requested email or the request should be rejected

**Validates**
- Criteria 12.3: WHEN a user requests their own notifications THEN the Notification-Service SHALL verify the email in the JWT matches the requested email

**Implementation tasks**
- Task 11.3: 11.3 Write property test for email matching verification

**Implemented PBTs**
- No implemented PBTs found

### Property 39: Invalid JWT rejection

*For any* invalid or expired JWT token, the system should return HTTP 401 Unauthorized

**Validates**
- Criteria 12.4: WHEN a JWT is invalid or expired THEN the Notification-Service SHALL return HTTP 401 Unauthorized

**Implementation tasks**
- Task 11.4: 11.4 Write property test for invalid JWT rejection

**Implemented PBTs**
- No implemented PBTs found

### Property 40: Insufficient role rejection

*For any* request with insufficient role permissions, the system should return HTTP 403 Forbidden

**Validates**
- Criteria 12.5: WHEN a user lacks required role THEN the Notification-Service SHALL return HTTP 403 Forbidden

**Implementation tasks**
- Task 11.5: 11.5 Write property test for insufficient role rejection

**Implemented PBTs**
- No implemented PBTs found

### Property 41: SecurityContext population

*For any* valid JWT token, the SecurityContext should be populated with the correct email and roles extracted from the token

**Validates**
- Criteria 12.6: WHEN extracting user info from JWT THEN the Notification-Service SHALL populate the SecurityContext with email and roles

**Implementation tasks**
- Task 11.6: 11.6 Write property test for SecurityContext population

**Implemented PBTs**
- No implemented PBTs found

### Property 42: Sent counter increment

*For any* notification successfully sent, the notifications.sent.total counter metric should increment by exactly 1

**Validates**
- Criteria 13.5: WHEN a notification is sent THEN the Notification-Service SHALL increment the notifications.sent.total counter metric

**Implementation tasks**
- Task 12.1: 12.1 Write property test for sent counter increment

**Implemented PBTs**
- No implemented PBTs found

### Property 43: Failed counter increment

*For any* notification that fails, the notifications.failed.total counter metric should increment by exactly 1

**Validates**
- Criteria 13.6: WHEN a notification fails THEN the Notification-Service SHALL increment the notifications.failed.total counter metric

**Implementation tasks**
- Task 12.2: 12.2 Write property test for failed counter increment

**Implemented PBTs**
- No implemented PBTs found

### Property 44: Duration recording

*For any* email send operation, the duration should be recorded in the notifications.email.send.duration histogram metric

**Validates**
- Criteria 13.7: WHEN an email is sent THEN the Notification-Service SHALL record the duration in the notifications.email.send.duration histogram metric

**Implementation tasks**
- Task 12.3: 12.3 Write property test for duration recording

**Implemented PBTs**
- No implemented PBTs found

## DATA

### ACCEPTANCE CRITERIA (99 total)
- 1.1: WHEN a notification event is published to the notifications topic THEN the Notification-Service SHALL consume the event and deserialize it into a NotificationDTO (covered)
- 1.2: WHEN a NotificationDTO is received THEN the Notification-Service SHALL validate that the destinataire field contains a valid email address (covered)
- 1.3: WHEN a NotificationDTO is received THEN the Notification-Service SHALL validate that the type field matches a recognized TypeNotification enum value (covered)
- 1.4: WHEN a valid NotificationDTO is processed THEN the Notification-Service SHALL select the appropriate HTML template based on the type field (covered)
- 1.5: WHEN template variables are interpolated THEN the Notification-Service SHALL replace all {{variable}} placeholders with values from the donnees map (covered)
- 1.6: WHEN a notification is successfully sent THEN the Notification-Service SHALL persist the notification record with status SENT (covered)
- 1.7: WHEN a notification send fails THEN the Notification-Service SHALL persist the notification record with status FAILED and the error message (covered)
- 2.1: WHEN an email is sent THEN the Notification-Service SHALL use JavaMailSender to send the email via SMTP (covered)
- 2.2: WHEN an email is sent THEN the Notification-Service SHALL set the sender address to noreply@portail-doctorat.ma (covered)
- 2.3: WHEN an email is sent THEN the Notification-Service SHALL set the Content-Type to text/html with charset UTF-8 (covered)
- 2.4: WHEN an HTML template is loaded THEN the Notification-Service SHALL read the template file from src/main/resources/templates/emails/ (covered)
- 2.5: WHEN template variables are interpolated THEN the Notification-Service SHALL replace all {{variable}} placeholders with corresponding values (not covered)
- 2.6: WHEN a template variable is missing from the data THEN the Notification-Service SHALL replace it with an empty string (covered)
- 2.7: WHEN a template file is not found THEN the Notification-Service SHALL use a generic fallback template (covered)
- 3.1: WHEN an inscription is submitted THEN the Notification-Service SHALL send an email to the directeur using template_inscription_soumise_directeur.html (not covered)
- 3.2: WHEN a directeur validates an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_validee_directeur_doctorant.html (not covered)
- 3.3: WHEN a directeur validates an inscription THEN the Notification-Service SHALL send an email to the admin using template_inscription_validee_directeur_admin.html (not covered)
- 3.4: WHEN a directeur rejects an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_rejetee_directeur.html including the commentaire (not covered)
- 3.5: WHEN an admin validates an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_validee_admin.html (not covered)
- 3.6: WHEN an admin rejects an inscription THEN the Notification-Service SHALL send an email to the doctorant using template_inscription_rejetee_admin.html including the commentaire (not covered)
- 3.7: WHEN a derogation is requested THEN the Notification-Service SHALL send an email to the PED using template_derogation_demandee.html (not covered)
- 4.1: WHEN a defense request is submitted THEN the Notification-Service SHALL send an email to the directeur using template_demande_soutenance_soumise_directeur.html (not covered)
- 4.2: WHEN a jury is proposed THEN the Notification-Service SHALL send an email to the admin using template_jury_propose_admin.html (not covered)
- 4.3: WHEN a jury member is invited THEN the Notification-Service SHALL send an email to the member using template_jury_membre_invite.html (not covered)
- 4.4: WHEN a jury member accepts THEN the Notification-Service SHALL send an email to the directeur using template_jury_membre_accepte_directeur.html (not covered)
- 4.5: WHEN a jury member declines THEN the Notification-Service SHALL send an email to the directeur using template_jury_membre_decline_directeur.html (not covered)
- 4.6: WHEN a report is submitted THEN the Notification-Service SHALL send an email to the directeur using template_rapport_soumis_directeur.html (not covered)
- 4.7: WHEN a defense is authorized THEN the Notification-Service SHALL send an email to the doctorant using template_autorisation_soutenance_doctorant.html (not covered)
- 4.8: WHEN a defense is scheduled THEN the Notification-Service SHALL send an email to all participants using template_soutenance_planifiee_tous.html (not covered)
- 5.1: WHEN a notification is received from Kafka THEN the Notification-Service SHALL create a Notification entity with status PENDING (covered)
- 5.2: WHEN a notification is successfully sent THEN the Notification-Service SHALL update the status to SENT and set the dateEnvoi (covered)
- 5.3: WHEN a notification send fails THEN the Notification-Service SHALL update the status to FAILED and record the erreurMessage (covered)
- 5.4: WHEN a notification is retried THEN the Notification-Service SHALL increment the nombreTentatives counter (covered)
- 5.5: WHEN a notification is retried THEN the Notification-Service SHALL update the status to RETRYING (covered)
- 5.6: WHEN notification data is persisted THEN the Notification-Service SHALL serialize the donnees map to JSON format (covered)
- 5.7: WHEN notification data is persisted THEN the Notification-Service SHALL store both messageTexte and messageHtml versions (covered)
- 6.1: WHEN an admin requests GET /api/notifications THEN the Notification-Service SHALL return a paginated list of all notifications (not covered)
- 6.2: WHEN an admin requests GET /api/notifications/{id} THEN the Notification-Service SHALL return the notification with the specified ID (covered)
- 6.3: WHEN a user requests GET /api/notifications/user/{email} THEN the Notification-Service SHALL return only notifications for that email address (covered)
- 6.4: WHEN an admin requests GET /api/notifications/status/{status} THEN the Notification-Service SHALL return all notifications with the specified status (covered)
- 6.5: WHEN an admin requests GET /api/notifications/stats THEN the Notification-Service SHALL return statistics including total, sent, failed, pending counts and success rate (covered)
- 6.6: WHEN an admin requests POST /api/notifications/{id}/retry THEN the Notification-Service SHALL retry sending the failed notification (covered)
- 6.7: WHEN an admin requests GET /api/notifications/failed THEN the Notification-Service SHALL return all notifications with status FAILED (covered)
- 6.8: WHEN an admin requests GET /api/notifications/search THEN the Notification-Service SHALL filter notifications by destinataire, type, status, and date range (covered)
- 7.1: WHEN an email send fails with MessagingException THEN the Notification-Service SHALL retry up to 3 times (covered)
- 7.2: WHEN retrying a failed email THEN the Notification-Service SHALL wait 5 seconds before the first retry (not covered)
- 7.3: WHEN retrying a failed email THEN the Notification-Service SHALL use exponential backoff with intervals 5s, 10s, 20s (not covered)
- 7.4: WHEN an email send fails after 3 retries THEN the Notification-Service SHALL mark the notification as FAILED (covered)
- 7.5: WHEN an email send fails after 3 retries THEN the Notification-Service SHALL send the event to the notifications-dlq topic (covered)
- 7.6: WHEN an IllegalArgumentException occurs THEN the Notification-Service SHALL not retry and mark as FAILED immediately (not covered)
- 7.7: WHEN a retry succeeds THEN the Notification-Service SHALL mark the notification as SENT and stop retrying (covered)
- 8.1: WHEN 50% of email sends fail within a sliding window of 10 calls THEN the Notification-Service SHALL open the circuit breaker (covered)
- 8.2: WHEN the circuit breaker is open THEN the Notification-Service SHALL reject email send attempts for 60 seconds (not covered)
- 8.3: WHEN 60 seconds have elapsed in open state THEN the Notification-Service SHALL transition to half-open state (not covered)
- 8.4: WHEN in half-open state THEN the Notification-Service SHALL permit 3 test calls to the email service (not covered)
- 8.5: WHEN test calls succeed in half-open state THEN the Notification-Service SHALL close the circuit breaker (not covered)
- 8.6: WHEN test calls fail in half-open state THEN the Notification-Service SHALL reopen the circuit breaker for another 60 seconds (not covered)
- 8.7: WHEN the circuit breaker opens THEN the Notification-Service SHALL execute the fallback method to mark notifications as FAILED (not covered)
- 9.1: WHEN an email send operation is initiated THEN the Notification-Service SHALL apply a 30-second timeout (covered)
- 9.2: WHEN an email send exceeds 30 seconds THEN the Notification-Service SHALL cancel the operation and throw TimeoutException (not covered)
- 9.3: WHEN a timeout occurs THEN the Notification-Service SHALL mark the notification as FAILED with error message "Email send timeout" (not covered)
- 9.4: WHEN a timeout occurs THEN the Notification-Service SHALL trigger the retry mechanism (not covered)
- 10.1: WHEN processing notifications THEN the Notification-Service SHALL limit concurrent email sends to 10 simultaneous operations (covered)
- 10.2: WHEN the concurrent limit is reached THEN the Notification-Service SHALL queue additional requests with a maximum wait time of 5 seconds (not covered)
- 10.3: WHEN the wait time exceeds 5 seconds THEN the Notification-Service SHALL reject the request with BulkheadFullException (not covered)
- 10.4: WHEN a bulkhead rejection occurs THEN the Notification-Service SHALL mark the notification as FAILED and trigger retry (not covered)
- 11.1: WHEN a notification fails after all retries THEN the Notification-Service SHALL publish the event to the notifications-dlq topic (not covered)
- 11.2: WHEN a DLQ consumer receives a message THEN the Notification-Service SHALL log the failure details (not covered)
- 11.3: WHEN a DLQ consumer receives a message THEN the Notification-Service SHALL persist the failure in a notification_dlq table (covered)
- 11.4: WHEN an admin requests POST /api/notifications/dlq/retry-all THEN the Notification-Service SHALL reprocess all messages from the DLQ (not covered)
- 11.5: WHEN reprocessing a DLQ message succeeds THEN the Notification-Service SHALL remove it from the DLQ and mark as SENT (not covered)
- 11.6: WHEN reprocessing a DLQ message fails THEN the Notification-Service SHALL leave it in the DLQ for manual investigation (not covered)
- 12.1: WHEN a request is made to any /api/notifications endpoint THEN the Notification-Service SHALL require a valid JWT token in the Authorization header (covered)
- 12.2: WHEN a request is made to admin endpoints THEN the Notification-Service SHALL verify the user has ROLE_ADMIN (covered)
- 12.3: WHEN a user requests their own notifications THEN the Notification-Service SHALL verify the email in the JWT matches the requested email (covered)
- 12.4: WHEN a JWT is invalid or expired THEN the Notification-Service SHALL return HTTP 401 Unauthorized (covered)
- 12.5: WHEN a user lacks required role THEN the Notification-Service SHALL return HTTP 403 Forbidden (covered)
- 12.6: WHEN extracting user info from JWT THEN the Notification-Service SHALL populate the SecurityContext with email and roles (covered)
- 13.1: WHEN a notification is successfully sent THEN the Notification-Service SHALL log at INFO level with notificationId, destinataire, and type (not covered)
- 13.2: WHEN a notification fails THEN the Notification-Service SHALL log at ERROR level with notificationId, destinataire, type, and error message (not covered)
- 13.3: WHEN a retry is attempted THEN the Notification-Service SHALL log at INFO level with attempt number (not covered)
- 13.4: WHEN the circuit breaker opens THEN the Notification-Service SHALL log at WARN level (not covered)
- 13.5: WHEN a notification is sent THEN the Notification-Service SHALL increment the notifications.sent.total counter metric (covered)
- 13.6: WHEN a notification fails THEN the Notification-Service SHALL increment the notifications.failed.total counter metric (covered)
- 13.7: WHEN an email is sent THEN the Notification-Service SHALL record the duration in the notifications.email.send.duration histogram metric (covered)
- 14.1: WHEN running unit tests THEN the Notification-Service SHALL achieve at least 70% code coverage (not covered)
- 14.2: WHEN testing the Kafka consumer THEN the Notification-Service SHALL use EmbeddedKafka to simulate event consumption (not covered)
- 14.3: WHEN testing email sending THEN the Notification-Service SHALL mock JavaMailSender to avoid actual SMTP calls (not covered)
- 14.4: WHEN testing persistence THEN the Notification-Service SHALL use an in-memory H2 database (not covered)
- 14.5: WHEN testing resilience THEN the Notification-Service SHALL simulate failures to verify circuit breaker and retry behavior (not covered)
- 14.6: WHEN testing the DLQ THEN the Notification-Service SHALL verify messages are sent to the DLQ topic after failures (not covered)
- 14.7: WHEN testing REST endpoints THEN the Notification-Service SHALL verify authentication and authorization rules (not covered)
- 15.1: WHEN the application starts THEN the Notification-Service SHALL load SMTP configuration from spring.mail.* properties (not covered)
- 15.2: WHEN the application starts THEN the Notification-Service SHALL load Kafka configuration from spring.kafka.* properties (not covered)
- 15.3: WHEN the application starts THEN the Notification-Service SHALL load database configuration from spring.datasource.* properties (not covered)
- 15.4: WHEN the application starts THEN the Notification-Service SHALL load resilience configuration from resilience4j.* properties (not covered)
- 15.5: WHEN the application starts THEN the Notification-Service SHALL load email sender address from notification.email.from property (not covered)
- 15.6: WHEN sensitive properties are needed THEN the Notification-Service SHALL read them from environment variables (DB_USERNAME, DB_PASSWORD, MAILTRAP_USERNAME, MAILTRAP_PASSWORD) (not covered)
- 15.7: WHEN the application starts THEN the Notification-Service SHALL register with Eureka Server using the configured service URL (not covered)

### IMPORTANT ACCEPTANCE CRITERIA (0 total)

### CORRECTNESS PROPERTIES (44 total)
- Property 1: Kafka deserialization
- Property 2: Email validation
- Property 3: Type validation
- Property 4: Template selection
- Property 5: Variable interpolation
- Property 6: Success persistence
- Property 7: Failure persistence
- Property 8: JavaMailSender usage
- Property 9: Sender address
- Property 10: Content type
- Property 11: Template loading
- Property 12: Missing variable handling
- Property 13: Template fallback
- Property 14: Initial persistence
- Property 15: Success status update
- Property 16: Failure status update
- Property 17: Retry counter increment
- Property 18: Retry status update
- Property 19: Data serialization
- Property 20: Dual message storage
- Property 21: Notification retrieval by ID
- Property 22: User notifications filtering
- Property 23: Status filtering
- Property 24: Statistics accuracy
- Property 25: Retry triggering
- Property 26: Failed notifications query
- Property 27: Search filtering
- Property 28: Retry count
- Property 29: Final failure status
- Property 30: DLQ publishing after retries
- Property 31: Retry success handling
- Property 32: Circuit breaker threshold
- Property 33: Timeout application
- Property 34: Concurrency limit
- Property 35: DLQ persistence
- Property 36: JWT requirement
- Property 37: Admin role verification
- Property 38: Email matching verification
- Property 39: Invalid JWT rejection
- Property 40: Insufficient role rejection
- Property 41: SecurityContext population
- Property 42: Sent counter increment
- Property 43: Failed counter increment
- Property 44: Duration recording

### IMPLEMENTATION TASKS (66 total)
1. Set up project dependencies and configuration
2. Implement core data models and enums
2.1 Write property test for data serialization
2.2 Write property test for dual message storage
2.3 Write property test for retry status update
3. Implement EmailTemplateService
3.1 Write property test for template loading
3.2 Write property test for variable interpolation
3.3 Write property test for missing variable handling
3.4 Write property test for template fallback
4. Create HTML email templates
5. Implement EmailService with resilience patterns
5.1 Write property test for JavaMailSender usage
5.2 Write property test for sender address
5.3 Write property test for content type
5.4 Write property test for retry count
5.5 Write property test for circuit breaker opening
5.6 Write property test for timeout application
5.7 Write property test for concurrency limit
6. Implement NotificationHistoryService
6.1 Write property test for initial persistence
6.2 Write property test for success status update
6.3 Write property test for failure status update
6.4 Write property test for retry counter increment
6.5 Write property test for statistics accuracy
7. Implement NotificationProcessingService
7.1 Write property test for email validation
7.2 Write property test for type validation
7.3 Write property test for template selection
7.4 Write property test for success persistence
7.5 Write property test for failure persistence
7.6 Write property test for DLQ publishing after retries
7.7 Write property test for final failure status
7.8 Write property test for retry success handling
8. Implement Kafka NotificationConsumer
8.1 Write property test for Kafka deserialization
9. Implement DLQ consumer and reprocessing
9.1 Write property test for DLQ persistence
10. Implement NotificationController REST API
10.1 Write property test for notification retrieval by ID
10.2 Write property test for user notifications filtering
10.3 Write property test for status filtering
10.4 Write property test for retry triggering
10.5 Write property test for failed notifications query
10.6 Write property test for search filtering
11. Implement JWT security configuration
11.1 Write property test for JWT requirement
11.2 Write property test for admin role verification
11.3 Write property test for email matching verification
11.4 Write property test for invalid JWT rejection
11.5 Write property test for insufficient role rejection
11.6 Write property test for SecurityContext population
12. Implement custom metrics with Micrometer
12.1 Write property test for sent counter increment
12.2 Write property test for failed counter increment
12.3 Write property test for duration recording
13. Implement comprehensive logging
14. Create custom generators for property-based tests
15. Write unit tests for all services
16. Write integration tests
17. Configure Resilience4j properties
18. Create exception handling
19. Configure Eureka client registration
20. Create Swagger/OpenAPI documentation
21. Write README.md documentation
22. Final checkpoint - Ensure all tests pass

### IMPLEMENTED PBTS (0 total)