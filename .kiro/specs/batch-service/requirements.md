# Requirements Document

## Introduction

The batch-service is a critical microservice for the doctoral management portal that automates periodic tasks and batch processing operations. This service is an explicit requirement from the specifications that has not yet been implemented. It handles scheduled jobs for data cleanup, alerts, reporting, archiving, and data consistency verification across the microservices architecture.

## Glossary

- **Batch_Service**: The microservice responsible for executing scheduled batch jobs and periodic maintenance tasks
- **Job**: A scheduled or manually triggered batch processing task with defined steps and execution logic
- **Step**: An individual unit of work within a job (can be chunk-based or tasklet-based)
- **Chunk**: A batch processing pattern that reads, processes, and writes data in configurable batch sizes
- **Tasklet**: A single-operation step for tasks that don't fit the chunk pattern
- **ItemReader**: Component that reads data from a source (database, file, etc.)
- **ItemProcessor**: Component that transforms or validates data items
- **ItemWriter**: Component that writes processed data to a destination
- **CRON_Expression**: Time-based scheduling expression for automatic job execution
- **Token**: JWT refresh token or password reset token stored in the user database
- **Doctorant**: PhD student enrolled in the doctoral program
- **Dérogation**: Special authorization to extend the standard doctoral duration
- **Soutenance**: PhD defense/thesis defense
- **Archive**: Long-term storage of historical data with compression and encryption
- **Cohérence**: Data consistency across multiple microservice databases
- **Kafka_Event**: Asynchronous message published to Kafka topics for inter-service communication
- **Notification_Service**: Microservice responsible for sending notifications to users
- **User_Service**: Microservice managing user accounts and authentication
- **Inscription_Service**: Microservice managing doctoral enrollments
- **Defense_Service**: Microservice managing thesis defenses
- **Chunk_Size**: Number of items processed in a single transaction batch

## Requirements

### Requirement 1: Token Cleanup Job

**User Story:** As a system administrator, I want expired tokens to be automatically cleaned from the database, so that the system maintains optimal performance and security.

#### Acceptance Criteria

1. WHEN the scheduled time arrives (daily at 2:00 AM), THE Batch_Service SHALL execute the token cleanup job
2. WHEN the job executes, THE Batch_Service SHALL identify all refresh tokens where expiry_date is less than the current timestamp
3. WHEN the job executes, THE Batch_Service SHALL identify all password reset tokens where expiry_date is less than the current timestamp
4. WHEN expired tokens are identified, THE Batch_Service SHALL delete them in chunks of 100 items per transaction
5. WHEN tokens are deleted, THE Batch_Service SHALL log the count of refresh tokens deleted and password reset tokens deleted
6. IF the job fails, THEN THE Batch_Service SHALL publish a Kafka event to the notifications topic for admin alerting
7. WHEN the job completes successfully, THE Batch_Service SHALL record execution metrics including duration and items processed
8. THE Batch_Service SHALL use the CRON expression "0 0 2 \* \* ?" for daily scheduling at 2:00 AM

### Requirement 2: Doctoral Duration Alert Job

**User Story:** As a doctoral program director, I want to receive automatic alerts when students approach critical duration thresholds, so that I can ensure timely completion and compliance with regulations.

#### Acceptance Criteria

1. WHEN the scheduled time arrives (every Monday at 8:00 AM), THE Batch_Service SHALL execute the duration alert job
2. WHEN checking 3-year threshold, THE Batch_Service SHALL identify active doctorants where first enrollment date plus 2 years 9 months is less than current date AND first enrollment date plus 3 years is greater than current date
3. WHEN a doctorant approaches 3 years without a dérogation, THE Batch_Service SHALL publish a Kafka event of type ALERTE_DUREE_3_ANS with doctorant details, director email, duration, and time remaining
4. WHEN checking 6-year threshold, THE Batch_Service SHALL identify active doctorants where first enrollment date plus 5 years 9 months is less than current date AND first enrollment date plus 6 years is greater than current date
5. WHEN a doctorant approaches 6 years, THE Batch_Service SHALL publish a high-priority Kafka event of type ALERTE_DUREE_6_ANS
6. WHEN a doctorant exceeds 6 years without exceptional dérogation, THE Batch_Service SHALL publish an urgent Kafka event of type ALERTE_DEPASSEMENT_6_ANS AND update the enrollment status to BLOQUÉ
7. WHEN an alert is sent for a specific doctorant and threshold, THE Batch_Service SHALL record the alert in the alerte_duree_envoyee table to prevent duplicate notifications
8. WHEN processing duration alerts, THE Batch_Service SHALL use a chunk size of 50 enrollments per transaction
9. THE Batch_Service SHALL use the CRON expression "0 0 8 ? \* MON" for weekly Monday scheduling at 8:00 AM
10. WHEN the job completes, THE Batch_Service SHALL record metrics including count of 3-year alerts, 6-year alerts, and exceeded durations detected

### Requirement 3: Monthly Report Generation Job

**User Story:** As an administrator, I want comprehensive monthly statistical reports automatically generated and delivered, so that I can monitor system activity and make informed decisions.

#### Acceptance Criteria

1. WHEN the scheduled time arrives (1st of each month at 9:00 AM), THE Batch_Service SHALL execute the monthly report generation job
2. WHEN generating enrollment statistics, THE Batch_Service SHALL collect total enrollments, status distribution, reinscriptions count, dérogation statistics, discipline distribution, laboratory distribution, average processing time, director validation rate, and admin validation rate for the previous month
3. WHEN generating defense statistics, THE Batch_Service SHALL collect defense requests count, completed defenses count, mention distribution, jury count, submitted reports count, jury member acceptance rate, average time from request to authorization, and average time from authorization to defense
4. WHEN generating notification statistics, THE Batch_Service SHALL collect total notifications sent, distribution by type, success rate, failed notifications count, and average send time
5. WHEN generating user statistics, THE Batch_Service SHALL collect total active users, role distribution, new users count, and connection rate
6. WHEN all data is collected, THE Batch_Service SHALL generate a structured PDF document with header, KPI dashboard, charts, detailed tables, alerts section, and footer
7. WHEN the PDF is generated, THE Batch_Service SHALL store it in the configured reports directory with filename format rapport_YYYY_MM.pdf
8. WHEN the PDF is stored, THE Batch_Service SHALL send the PDF via email to all users with ROLE_ADMIN
9. WHEN the PDF is stored, THE Batch_Service SHALL publish a Kafka event with the PDF as attachment
10. THE Batch_Service SHALL use the CRON expression "0 0 9 1 \* ?" for monthly scheduling on the 1st at 9:00 AM
11. WHEN the job completes, THE Batch_Service SHALL record metrics including PDF size, generation duration, and send status

### Requirement 4: Archive Job

**User Story:** As a system administrator, I want old enrollment and defense records automatically archived, so that the system maintains optimal performance and storage efficiency.

#### Acceptance Criteria

1. WHEN the scheduled time arrives (1st of each quarter at 3:00 AM), THE Batch_Service SHALL execute the archive job
2. WHEN archiving enrollments, THE Batch_Service SHALL identify enrollments with status VALIDÉ or REJETÉ where validation/rejection date is greater than 1 year ago AND not yet archived
3. WHEN an enrollment is archived, THE Batch_Service SHALL copy the record to inscription_archive table, copy associated documents to archives directory, compress documents to ZIP format, delete original documents, and mark the enrollment as archived
4. WHEN archiving defenses, THE Batch_Service SHALL identify completed defenses where defense date is greater than 1 year ago AND signed PV is available AND not yet archived
5. WHEN a defense is archived, THE Batch_Service SHALL copy the record to defense_archive table, copy documents to archives directory, compress to ZIP, delete originals, and mark as archived
6. WHEN cleaning logs, THE Batch_Service SHALL delete application logs older than 6 months AND delete notification logs older than 3 months where notification was sent successfully
7. WHEN cleaning logs, THE Batch_Service SHALL retain all error logs indefinitely
8. WHEN archiving completes, THE Batch_Service SHALL execute database optimization commands (VACUUM/OPTIMIZE) on affected tables
9. WHEN processing archives, THE Batch_Service SHALL use a chunk size of 20 items per transaction
10. WHEN creating archives, THE Batch_Service SHALL encrypt archive files using AES-256 encryption
11. WHEN archiving any record, THE Batch_Service SHALL create an audit trail entry with timestamp, archived entity, and archive location
12. THE Batch_Service SHALL use the CRON expression "0 0 3 1 1,4,7,10 ?" for quarterly scheduling on the 1st at 3:00 AM
13. WHEN the job completes, THE Batch_Service SHALL record metrics including enrollments archived, defenses archived, disk space freed, and execution duration

### Requirement 5: Data Consistency Verification Job

**User Story:** As a system administrator, I want automatic verification and correction of data inconsistencies across services, so that the system maintains data integrity and reliability.

#### Acceptance Criteria

1. WHEN the scheduled time arrives (daily at 11:00 PM), THE Batch_Service SHALL execute the consistency verification job
2. WHEN verifying user-enrollment consistency, THE Batch_Service SHALL check that each enrollment has a valid user in user-service
3. IF an enrollment references a non-existent user, THEN THE Batch_Service SHALL log the anomaly, mark the enrollment as SUSPENDU, and notify the admin
4. WHEN verifying enrollment-defense consistency, THE Batch_Service SHALL check that each defense request has a valid enrollment with status VALIDÉ
5. IF a defense request has an invalid or non-validated enrollment, THEN THE Batch_Service SHALL log the anomaly, block the defense request, and notify the director and admin
6. WHEN verifying user roles, THE Batch_Service SHALL check that doctorants with validated enrollments have ROLE_DOCTORANT_ACTIF
7. WHEN a doctorant has a successfully completed defense, THE Batch_Service SHALL remove ROLE_DOCTORANT_ACTIF, add ROLE_DOCTEUR, and synchronize roles
8. WHEN checking for orphaned documents, THE Batch_Service SHALL identify files in uploads directory without database references
9. WHEN orphaned documents are found, THE Batch_Service SHALL move them to uploads/orphelins directory and log the file list
10. WHEN checking pending notifications, THE Batch_Service SHALL identify notifications in PENDING status for more than 24 hours
11. WHEN stale pending notifications are found, THE Batch_Service SHALL retry sending automatically
12. IF notification retry fails, THEN THE Batch_Service SHALL mark the notification as FAILED and notify the technical admin
13. WHEN anomalies are detected, THE Batch_Service SHALL generate a PDF anomaly report with type, occurrence count, and corrective actions
14. WHEN anomalies are detected, THE Batch_Service SHALL send the anomaly report via email to the technical admin
15. THE Batch_Service SHALL use the CRON expression "0 0 23 \* \* ?" for daily scheduling at 11:00 PM
16. WHEN the job completes, THE Batch_Service SHALL record metrics including total anomalies detected, anomalies auto-corrected, and anomalies requiring manual intervention

### Requirement 6: Manual Job Execution API

**User Story:** As an administrator, I want to manually trigger batch jobs via REST API, so that I can execute maintenance tasks on-demand without waiting for scheduled execution.

#### Acceptance Criteria

1. WHEN an admin sends POST request to /api/batch/jobs/{jobName}/run, THE Batch_Service SHALL validate the user has ROLE_ADMIN
2. WHEN a valid job trigger request is received, THE Batch_Service SHALL start the specified job and return the job execution ID, status, start time, and confirmation message
3. WHEN an admin sends GET request to /api/batch/jobs, THE Batch_Service SHALL return a list of all available jobs with name, description, CRON expression, last execution time, and last status
4. WHEN an admin sends GET request to /api/batch/jobs/{jobName}/executions, THE Batch_Service SHALL return paginated execution history for the specified job
5. WHEN an admin sends GET request to /api/batch/jobs/executions/{executionId}, THE Batch_Service SHALL return detailed execution information including execution ID, job name, status, start time, end time, duration, items processed, items failed, and exit message
6. WHEN an admin sends POST request to /api/batch/jobs/executions/{executionId}/stop, THE Batch_Service SHALL stop the running execution and return confirmation
7. WHEN an admin sends GET request to /api/batch/stats, THE Batch_Service SHALL return global statistics including total executions, successful executions, failed executions, success rate, average duration, and last failure details
8. WHEN an admin sends GET request to /api/batch/reports, THE Batch_Service SHALL return a list of generated monthly reports with download links
9. WHEN an admin sends GET request to /api/batch/reports/{fileName}/download, THE Batch_Service SHALL stream the PDF file for download
10. THE Batch_Service SHALL require JWT authentication for all /api/batch/\*\* endpoints
11. THE Batch_Service SHALL implement rate limiting on manual job trigger endpoints to prevent abuse

### Requirement 7: Multi-Database Access Configuration

**User Story:** As a system architect, I want the batch service to securely access multiple microservice databases, so that batch jobs can read and process data across the entire system.

#### Acceptance Criteria

1. THE Batch_Service SHALL configure a primary DataSource for batchdb with read/write permissions
2. THE Batch_Service SHALL configure a secondary DataSource for userdb with read and delete permissions on token tables
3. THE Batch_Service SHALL configure a secondary DataSource for inscriptiondb with read-only permissions
4. THE Batch_Service SHALL configure a secondary DataSource for defensedb with read-only permissions
5. THE Batch_Service SHALL configure a secondary DataSource for notificationdb with read-only permissions
6. WHEN configuring DataSources, THE Batch_Service SHALL use HikariCP connection pooling for each database
7. WHEN configuring DataSources, THE Batch_Service SHALL create dedicated JdbcTemplate instances for each database
8. WHEN configuring DataSources, THE Batch_Service SHALL externalize database credentials to environment variables
9. THE Batch_Service SHALL use service accounts with minimum required permissions for each database connection
10. THE Batch_Service SHALL encrypt database connection passwords in configuration

### Requirement 8: Job Execution Monitoring and Metrics

**User Story:** As a system administrator, I want comprehensive monitoring and metrics for batch jobs, so that I can track performance, detect issues, and ensure reliable operation.

#### Acceptance Criteria

1. WHEN a job executes, THE Batch_Service SHALL expose metrics for total executions, successful executions, and failed executions per job
2. WHEN a job executes, THE Batch_Service SHALL expose metrics for average duration and total items processed per job
3. WHEN a job executes, THE Batch_Service SHALL expose the last execution status per job
4. THE Batch_Service SHALL expose global metrics for active jobs count, scheduled jobs count, and overall success rate
5. THE Batch_Service SHALL configure Spring Boot Actuator to expose health checks for each DataSource
6. THE Batch_Service SHALL configure Spring Boot Actuator to expose Prometheus-compatible metrics
7. THE Batch_Service SHALL configure Spring Boot Actuator to expose information about scheduled jobs
8. WHEN a job fails, THE Batch_Service SHALL detect the failure and publish an urgent Kafka notification event
9. WHEN a job fails, THE Batch_Service SHALL send an email alert to the technical team
10. WHEN a job execution completes, THE Batch_Service SHALL record execution details in the job_execution_history table

### Requirement 9: Batch Service Infrastructure

**User Story:** As a developer, I want a well-structured batch service with proper Spring Batch configuration, so that jobs are maintainable, testable, and follow best practices.

#### Acceptance Criteria

1. THE Batch_Service SHALL create dedicated JobConfiguration classes for each job (TokenCleanupJobConfig, DureeDoctoratAlertJobConfig, MonthlyReportJobConfig, ArchiveJobConfig, DataConsistencyJobConfig)
2. WHEN defining a job configuration, THE Batch_Service SHALL specify the job name, restart policy, steps, readers, processors, writers, and listeners
3. THE Batch_Service SHALL implement custom ItemReaders for reading from MariaDB using JdbcCursorItemReader or JpaPagingItemReader
4. THE Batch_Service SHALL implement custom ItemProcessors for data transformation, metric calculation, business logic application, and Kafka event preparation
5. THE Batch_Service SHALL implement custom ItemWriters for database writes, Kafka event publishing, and file generation
6. THE Batch_Service SHALL implement Tasklets for unit tasks including PDF generation, complex SQL queries, file system operations, and notification sending
7. THE Batch_Service SHALL implement JobExecutionListener and StepExecutionListener for logging, exception capture, metric collection, and failure notifications
8. THE Batch_Service SHALL configure Spring Batch to initialize schema automatically in batchdb
9. THE Batch_Service SHALL disable automatic job execution on application startup (spring.batch.job.enabled=false)
10. THE Batch_Service SHALL register with Eureka service discovery on port 8085

### Requirement 10: Security and Audit

**User Story:** As a security officer, I want batch operations to be secure, audited, and compliant with data protection requirements, so that sensitive data is protected and all operations are traceable.

#### Acceptance Criteria

1. WHEN accessing REST endpoints, THE Batch_Service SHALL require ROLE_ADMIN authorization for all /api/batch/\*\* paths
2. WHEN accessing REST endpoints, THE Batch_Service SHALL validate JWT tokens via the API Gateway
3. WHEN connecting to databases, THE Batch_Service SHALL use dedicated service accounts with minimum required permissions
4. WHEN creating archive files, THE Batch_Service SHALL encrypt files using AES-256 encryption
5. WHEN creating archive files, THE Batch_Service SHALL store encryption keys in a secure key vault
6. WHEN accessing archive files, THE Batch_Service SHALL log all access attempts with timestamp, user, and file accessed
7. WHEN executing any job, THE Batch_Service SHALL create audit log entries that are retained indefinitely
8. WHEN executing any manual job via API, THE Batch_Service SHALL log the admin user who triggered the execution
9. THE Batch_Service SHALL retain all audit logs indefinitely and prevent deletion
10. THE Batch_Service SHALL implement rate limiting on manual job trigger endpoints to prevent abuse
