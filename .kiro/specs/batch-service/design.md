# Design Document: Batch Service

## Overview

The batch-service is a Spring Boot microservice built on Spring Batch framework that automates periodic maintenance tasks and batch processing operations for the doctoral management portal. It operates as an autonomous service that accesses multiple microservice databases, processes data in configurable chunks, and publishes events to Kafka for cross-service communication.

### Key Responsibilities

- Execute scheduled jobs using CRON expressions for automated maintenance
- Clean expired authentication and password reset tokens from user database
- Monitor doctoral duration and send proactive alerts at critical thresholds
- Generate comprehensive monthly statistical reports with PDF output
- Archive historical enrollment and defense records with encryption
- Verify and correct data consistency across microservice databases
- Expose REST API for manual job execution and monitoring
- Publish Kafka events for notifications and cross-service coordination
- Collect and expose metrics for observability and monitoring

### Technology Stack

- **Framework**: Spring Boot 3.x with Spring Batch 5.x
- **Language**: Java 17
- **Database**: MariaDB (primary: batchdb, secondary: userdb, inscriptiondb, defensedb, notificationdb)
- **Messaging**: Apache Kafka for event publishing
- **Scheduling**: Spring @Scheduled with CRON expressions
- **PDF Generation**: iText 7 for report generation
- **Charts**: JFreeChart for statistical visualizations
- **Connection Pooling**: HikariCP for all DataSources
- **Service Discovery**: Eureka Client
- **Monitoring**: Spring Boot Actuator with Prometheus metrics
- **Security**: JWT authentication via API Gateway, AES-256 encryption for archives

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Batch Service                             │
│                         (Port 8085)                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Job Scheduler│  │  REST API    │  │   Listeners  │          │
│  │   (CRON)     │  │ Controllers  │  │  (Job/Step)  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│  ┌──────▼──────────────────▼──────────────────▼───────┐         │
│  │              Job Configurations                      │         │
│  │  (TokenCleanup, DureeAlert, Report, Archive, etc.)  │         │
│  └──────┬───────────────────────────────────────┬──────┘         │
│         │                                        │                │
│  ┌──────▼──────────┐  ┌──────────────┐  ┌──────▼──────────┐    │
│  │   ItemReaders   │  │  Processors  │  │   ItemWriters   │    │
│  │ (JDBC/JPA/File) │  │  (Transform) │  │ (DB/Kafka/File) │    │
│  └─────────────────┘  └──────────────┘  └─────────────────┘    │
│                                                                   │
└───────────┬───────────────────────────────────┬─────────────────┘
            │                                   │
    ┌───────▼────────┐                  ┌───────▼────────┐
    │  Multi-Database│                  │  Kafka Broker  │
    │    Access      │                  │  (Events)      │
    ├────────────────┤                  └────────────────┘
    │ • batchdb      │
    │ • userdb       │
    │ • inscriptiondb│
    │ • defensedb    │
    │ • notificationdb│
    └────────────────┘
```

### Service Interactions

1. **Scheduled Execution**: Jobs run automatically based on CRON schedules
2. **Manual Execution**: Admins trigger jobs via REST API
3. **Database Access**: Read/write to batchdb, read from other service databases
4. **Event Publishing**: Publish notifications and alerts to Kafka topics
5. **File Operations**: Generate PDFs, create archives, manage uploads
6. **Metrics Exposure**: Actuator endpoints for monitoring and health checks

## Components and Interfaces

### 1. Job Configurations

Each batch job is defined in a dedicated configuration class that specifies the complete job structure.

**TokenCleanupJobConfig**

- Job: tokenCleanupJob
- Steps:
  - cleanupRefreshTokensStep (chunk-based, size 100)
  - cleanupPasswordResetTokensStep (chunk-based, size 100)
- Reader: JdbcCursorItemReader querying expired tokens from userdb
- Processor: PassThroughItemProcessor (no transformation needed)
- Writer: JdbcBatchItemWriter executing DELETE statements
- Listener: TokenCleanupJobListener for metrics and failure notifications

**DureeDoctoratAlertJobConfig**

- Job: dureeDoctoratAlertJob
- Steps:
  - check3YearThresholdStep (chunk-based, size 50)
  - check6YearThresholdStep (chunk-based, size 50)
  - checkExceeded6YearStep (chunk-based, size 50)
- Reader: JpaPagingItemReader from inscriptiondb
- Processor: DureeAlertProcessor (calculates duration, creates Kafka events)
- Writer: KafkaItemWriter (publishes to notifications topic)
- Listener: DureeAlertJobListener for metrics collection

**MonthlyReportJobConfig**

- Job: monthlyReportJob
- Steps:
  - collectEnrollmentStatsTasklet
  - collectDefenseStatsTasklet
  - collectNotificationStatsTasklet
  - collectUserStatsTasklet
  - generatePdfTasklet
  - sendReportNotificationTasklet
- All steps are Tasklet-based (single operations)
- Listener: ReportJobListener for tracking generation time

**ArchiveJobConfig**

- Job: archiveJob
- Steps:
  - archiveEnrollmentsStep (chunk-based, size 20)
  - archiveDefensesStep (chunk-based, size 20)
  - cleanupLogsTasklet
  - optimizeDatabaseTasklet
- Reader: JdbcCursorItemReader for old records
- Processor: ArchiveProcessor (compression, encryption)
- Writer: ArchiveWriter (copy to archive tables, file operations)
- Listener: ArchiveJobListener for space freed metrics

**DataConsistencyJobConfig**

- Job: dataConsistencyJob
- Steps:
  - verifyUserEnrollmentConsistencyTasklet
  - verifyEnrollmentDefenseConsistencyTasklet
  - verifyUserRolesTasklet
  - checkOrphanedDocumentsTasklet
  - retryPendingNotificationsTasklet
  - generateAnomalyReportTasklet (conditional)
- All steps are Tasklet-based
- Listener: ConsistencyJobListener for anomaly tracking

### 2. ItemReaders

**ExpiredTokenReader**

- Type: JdbcCursorItemReader<Token>
- DataSource: userDataSource
- SQL: `SELECT * FROM refresh_token WHERE expiry_date < NOW()` (or password_reset_token)
- Fetch Size: 100
- Returns: Token objects for processing

**EnrollmentReader**

- Type: JpaPagingItemReader<Inscription>
- DataSource: inscriptionDataSource
- Query: Custom JPQL with date calculations for duration thresholds
- Page Size: 50
- Returns: Inscription entities with calculated duration

**ArchiveCandidateReader**

- Type: JdbcCursorItemReader<ArchiveCandidate>
- DataSource: inscriptionDataSource or defenseDataSource
- SQL: Identifies records older than retention period
- Fetch Size: 20
- Returns: Records eligible for archiving

### 3. ItemProcessors

**DureeAlertProcessor**

- Input: Inscription entity
- Processing:
  - Calculate exact duration from first enrollment date
  - Determine alert type (3_ANS, 6_ANS, DEPASSEMENT)
  - Check if alert already sent (query alerte_duree_envoyee)
  - Build Kafka event DTO with all required fields
  - For DEPASSEMENT: prepare status update to BLOQUÉ
- Output: AlertEvent DTO ready for Kafka publishing

**ArchiveProcessor**

- Input: ArchiveCandidate (enrollment or defense record)
- Processing:
  - Copy record data to archive entity
  - Locate associated documents in file system
  - Compress documents to ZIP format
  - Encrypt ZIP file with AES-256
  - Generate archive location path
  - Prepare audit trail entry
- Output: ArchivePackage with encrypted file and metadata

**ConsistencyCheckProcessor**

- Input: Entity requiring validation
- Processing:
  - Execute cross-service validation queries
  - Detect inconsistencies (missing references, invalid states)
  - Determine corrective action (suspend, block, notify)
  - Build anomaly report entry
- Output: ConsistencyResult with anomalies and corrections

### 4. ItemWriters

**JdbcBatchItemWriter (Token Deletion)**

- DataSource: userDataSource
- SQL: `DELETE FROM refresh_token WHERE id = :id`
- Batch Size: 100
- Transaction: Commits per chunk

**KafkaItemWriter (Alert Publishing)**

- Kafka Template: Configured for JSON serialization
- Topic: notifications
- Key: doctorant_id
- Value: AlertEvent DTO
- Error Handling: Retry with exponential backoff

**ArchiveWriter**

- Operations:
  - Insert into archive table (inscription_archive or defense_archive)
  - Write encrypted ZIP to file system
  - Insert audit trail entry
  - Update original record (set archived = true)
  - Delete original documents
- Transaction: All operations in single transaction
- Rollback: On any failure, entire chunk rolls back

**FileWriter (PDF Reports)**

- Output: File system at configured path
- Format: PDF with iText 7
- Naming: rapport_YYYY_MM.pdf
- Permissions: Read-only for non-admin users

### 5. Tasklets

**CollectStatsTasklet**

- Purpose: Execute complex aggregation queries across multiple databases
- Implementation:
  - Use JdbcTemplate for each DataSource
  - Execute multiple SQL queries
  - Aggregate results into statistics DTO
  - Store in job execution context for next step
- Return: RepeatStatus.FINISHED

**GeneratePdfTasklet**

- Purpose: Create PDF report from collected statistics
- Implementation:
  - Retrieve statistics from execution context
  - Use iText Document and PdfWriter
  - Add header with logo and title
  - Create KPI dashboard section
  - Generate charts with JFreeChart
  - Add detailed tables
  - Add alerts section if anomalies exist
  - Add footer with generation timestamp
  - Save to configured reports directory
- Return: RepeatStatus.FINISHED

**SendNotificationTasklet**

- Purpose: Send report via email and Kafka
- Implementation:
  - Retrieve PDF path from execution context
  - Query all ROLE_ADMIN users
  - Send email with PDF attachment to each admin
  - Publish Kafka event with PDF reference
  - Log send status
- Return: RepeatStatus.FINISHED

**DatabaseOptimizationTasklet**

- Purpose: Optimize database after archiving
- Implementation:
  - Execute OPTIMIZE TABLE on affected tables
  - Regenerate table statistics
  - Verify index integrity
  - Log optimization results
- Return: RepeatStatus.FINISHED

### 6. Listeners

**JobExecutionListener**

- beforeJob(): Log job start, record start time, initialize metrics
- afterJob(): Log job completion, calculate duration, record metrics in job_execution_history, send failure notification if failed

**StepExecutionListener**

- beforeStep(): Log step start, initialize step context
- afterStep(): Log step completion, collect step metrics (read count, write count, skip count), handle step failures

**ChunkListener**

- beforeChunk(): Initialize chunk transaction
- afterChunk(): Log chunk completion, update progress metrics
- afterChunkError(): Log chunk error, prepare for retry or skip

### 7. REST Controllers

**BatchJobController**

- Base Path: /api/batch/jobs
- Endpoints:
  - POST /{jobName}/run - Trigger job manually
  - GET / - List all jobs with metadata
  - GET /{jobName}/executions - Get execution history
  - GET /executions/{executionId} - Get execution details
  - POST /executions/{executionId}/stop - Stop running job
  - GET /stats - Get global statistics
- Security: @PreAuthorize("hasRole('ADMIN')")
- Rate Limiting: @RateLimiter(name = "jobTrigger", fallbackMethod = "rateLimitFallback")

**BatchReportController**

- Base Path: /api/batch/reports
- Endpoints:
  - GET / - List available reports
  - GET /{fileName}/download - Download report PDF
- Security: @PreAuthorize("hasRole('ADMIN')")
- Response: StreamingResponseBody for efficient file transfer

## Data Models

### Entities

**JobExecutionHistory**

```java
@Entity
@Table(name = "job_execution_history")
public class JobExecutionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // COMPLETED, FAILED, STOPPED
    private String exitMessage;
    private Integer itemsProcessed;
    private Integer itemsFailed;
    private LocalDateTime createdAt;
}
```

**AlerteDureeEnvoyee**

```java
@Entity
@Table(name = "alerte_duree_envoyee",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inscription_id", "type_alerte"}))
public class AlerteDureeEnvoyee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long inscriptionId;
    private Long doctorantId;
    private String typeAlerte; // 3_ANS, 6_ANS, DEPASSEMENT
    private LocalDateTime dateEnvoi;
}
```

**InscriptionArchive**

```java
@Entity
@Table(name = "inscription_archive")
public class InscriptionArchive {
    @Id
    private Long id; // Same as original inscription ID

    // All fields from Inscription entity
    private Long doctorantId;
    private String status;
    private LocalDate dateValidation;
    // ... other fields

    // Archive-specific fields
    private LocalDateTime archivedDate;
    private String archivedBy;
    private String archiveLocation; // Path to encrypted ZIP
}
```

**DefenseArchive**

```java
@Entity
@Table(name = "defense_archive")
public class DefenseArchive {
    @Id
    private Long id; // Same as original defense ID

    // All fields from Defense entity
    private Long inscriptionId;
    private LocalDate defenseDate;
    private String mention;
    // ... other fields

    // Archive-specific fields
    private LocalDateTime archivedDate;
    private String archivedBy;
    private String archiveLocation;
}
```

### DTOs

**AlertEventDTO**

```java
public class AlertEventDTO {
    private String type; // ALERTE_DUREE_3_ANS, ALERTE_DUREE_6_ANS, ALERTE_DEPASSEMENT_6_ANS
    private String doctorantEmail;
    private String doctorantNom;
    private String directeurEmail;
    private LocalDate datePremiereInscription;
    private String dureeActuelle; // "2 ans 10 mois"
    private String tempsRestant; // "2 mois"
    private String seuil; // "3 ans" or "6 ans"
    private String actionRequise; // "Demander dérogation" or "Régularisation urgente"
    private String priority; // NORMAL, HIGH, URGENT
}
```

**JobExecutionDTO**

```java
public class JobExecutionDTO {
    private Long executionId;
    private String jobName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration; // Formatted as "5m 23s"
    private Integer itemsProcessed;
    private Integer itemsFailed;
    private String exitMessage;
}
```

**JobInfoDTO**

```java
public class JobInfoDTO {
    private String name;
    private String description;
    private String cronExpression;
    private LocalDateTime lastExecution;
    private String lastStatus;
    private Boolean isRunning;
}
```

**GlobalStatsDTO**

```java
public class GlobalStatsDTO {
    private Long totalExecutions;
    private Long successfulExecutions;
    private Long failedExecutions;
    private Double successRate;
    private String averageDuration;
    private JobFailureDTO lastFailure;
}
```

**MonthlyReportDataDTO**

```java
public class MonthlyReportDataDTO {
    private EnrollmentStatsDTO enrollmentStats;
    private DefenseStatsDTO defenseStats;
    private NotificationStatsDTO notificationStats;
    private UserStatsDTO userStats;
    private LocalDate reportMonth;
}
```

### Configuration Classes

**MultiDataSourceConfig**

```java
@Configuration
public class MultiDataSourceConfig {

    @Primary
    @Bean(name = "batchDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource batchDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "userDataSource")
    @ConfigurationProperties(prefix = "datasource.userdb")
    public DataSource userDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "inscriptionDataSource")
    @ConfigurationProperties(prefix = "datasource.inscriptiondb")
    public DataSource inscriptionDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "defenseDataSource")
    @ConfigurationProperties(prefix = "datasource.defensedb")
    public DataSource defenseDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "notificationDataSource")
    @ConfigurationProperties(prefix = "datasource.notificationdb")
    public DataSource notificationDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    // JdbcTemplate beans for each DataSource
    @Bean(name = "userJdbcTemplate")
    public JdbcTemplate userJdbcTemplate(@Qualifier("userDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    // ... similar for other DataSources
}
```

**KafkaProducerConfig**

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Token Cleanup Job Properties

**Property 1: Expired Token Identification**
_For any_ set of refresh tokens in the database, the token cleanup job should identify exactly those tokens where expiry_date < current_timestamp
**Validates: Requirements 1.2**

**Property 2: Password Reset Token Identification**
_For any_ set of password reset tokens in the database, the token cleanup job should identify exactly those tokens where expiry_date < current_timestamp
**Validates: Requirements 1.3**

**Property 3: Chunk-Based Deletion**
_For any_ number N of expired tokens, the deletion process should execute in chunks of 100, resulting in ceiling(N/100) transactions
**Validates: Requirements 1.4**

**Property 4: Deletion Count Logging**
_For any_ job execution that deletes tokens, the log should contain the exact count of refresh tokens deleted and password reset tokens deleted
**Validates: Requirements 1.5**

**Property 5: Failure Notification**
_For any_ token cleanup job execution that fails, a Kafka event should be published to the notifications topic with failure details
**Validates: Requirements 1.6**

**Property 6: Success Metrics Recording**
_For any_ successful token cleanup job execution, the job_execution_history table should contain a record with status COMPLETED, duration, and items_processed count
**Validates: Requirements 1.7**

### Doctoral Duration Alert Job Properties

**Property 7: Three-Year Threshold Identification**
_For any_ active enrollment with first_enrollment_date D, if current_date is between D + 2y9m and D + 3y, and no dérogation exists, the enrollment should be identified for 3-year alert
**Validates: Requirements 2.2**

**Property 8: Three-Year Alert Publishing**
_For any_ doctorant identified for 3-year alert, a Kafka event of type ALERTE_DUREE_3_ANS should be published containing doctorant email, director email, duration, time remaining, and action required
**Validates: Requirements 2.3**

**Property 9: Six-Year Threshold Identification**
_For any_ active enrollment with first_enrollment_date D, if current_date is between D + 5y9m and D + 6y, the enrollment should be identified for 6-year alert
**Validates: Requirements 2.4**

**Property 10: Six-Year Alert Publishing**
_For any_ doctorant identified for 6-year alert, a high-priority Kafka event of type ALERTE_DUREE_6_ANS should be published
**Validates: Requirements 2.5**

**Property 11: Exceeded Duration Handling**
_For any_ active enrollment where current_date > first_enrollment_date + 6y and no exceptional dérogation exists, an urgent Kafka event of type ALERTE_DEPASSEMENT_6_ANS should be published AND enrollment status should be updated to BLOQUÉ
**Validates: Requirements 2.6**

**Property 12: Alert Idempotence**
_For any_ doctorant and alert type combination, if an alert has been sent (recorded in alerte_duree_envoyee), subsequent job executions should not send duplicate alerts for the same threshold
**Validates: Requirements 2.7**

**Property 13: Duration Alert Metrics**
_For any_ duration alert job execution, the recorded metrics should include accurate counts of 3-year alerts sent, 6-year alerts sent, and exceeded durations detected
**Validates: Requirements 2.10**

### Monthly Report Generation Properties

**Property 14: Enrollment Statistics Completeness**
_For any_ monthly report generation, the enrollment statistics should include all required fields: total enrollments, status distribution, reinscriptions count, dérogation statistics, discipline distribution, laboratory distribution, average processing time, director validation rate, and admin validation rate
**Validates: Requirements 3.2**

**Property 15: Defense Statistics Completeness**
_For any_ monthly report generation, the defense statistics should include all required fields: defense requests count, completed defenses count, mention distribution, jury count, submitted reports count, jury member acceptance rate, average time from request to authorization, and average time from authorization to defense
**Validates: Requirements 3.3**

**Property 16: Notification Statistics Completeness**
_For any_ monthly report generation, the notification statistics should include all required fields: total notifications sent, distribution by type, success rate, failed notifications count, and average send time
**Validates: Requirements 3.4**

**Property 17: User Statistics Completeness**
_For any_ monthly report generation, the user statistics should include all required fields: total active users, role distribution, new users count, and connection rate
**Validates: Requirements 3.5**

**Property 18: PDF Structure Completeness**
_For any_ generated monthly report PDF, the document should contain all required sections: header, KPI dashboard, charts, detailed tables, alerts section, and footer
**Validates: Requirements 3.6**

**Property 19: Report File Naming**
_For any_ monthly report generated for month M and year Y, the PDF file should be stored with filename rapport_Y_M.pdf in the configured reports directory
**Validates: Requirements 3.7**

**Property 20: Admin Email Distribution**
_For any_ generated monthly report, the PDF should be sent via email to all users with ROLE_ADMIN
**Validates: Requirements 3.8**

**Property 21: Report Kafka Event**
_For any_ stored monthly report PDF, a Kafka event should be published with the PDF reference as attachment
**Validates: Requirements 3.9**

**Property 22: Report Generation Metrics**
_For any_ monthly report job execution, the recorded metrics should include PDF size in bytes, generation duration, and send status
**Validates: Requirements 3.11**

### Archive Job Properties

**Property 23: Enrollment Archive Identification**
_For any_ enrollment with status VALIDÉ or REJETÉ where validation/rejection date is more than 1 year ago and archived flag is false, the enrollment should be identified for archiving
**Validates: Requirements 4.2**

**Property 24: Enrollment Archival Process**
_For any_ enrollment being archived, the process should: copy record to inscription_archive table, copy documents to archives directory, compress documents to ZIP, encrypt ZIP with AES-256, delete original documents, and set archived flag to true—all within a single transaction
**Validates: Requirements 4.3**

**Property 25: Defense Archive Identification**
_For any_ defense with status COMPLETED where defense date is more than 1 year ago, signed PV exists, and archived flag is false, the defense should be identified for archiving
**Validates: Requirements 4.4**

**Property 26: Defense Archival Process**
_For any_ defense being archived, the process should: copy record to defense_archive table, copy documents to archives directory, compress to ZIP, encrypt with AES-256, delete originals, and set archived flag to true
**Validates: Requirements 4.5**

**Property 27: Log Cleanup Retention**
_For any_ log cleanup execution, application logs older than 6 months should be deleted AND notification logs older than 3 months with successful send status should be deleted
**Validates: Requirements 4.6**

**Property 28: Error Log Retention**
_For any_ log cleanup execution, all logs with error level should be retained regardless of age
**Validates: Requirements 4.7**

**Property 29: Archive Encryption**
_For any_ created archive file, the file should be encrypted using AES-256 and should be decryptable using the stored encryption key
**Validates: Requirements 4.10**

**Property 30: Archive Audit Trail**
_For any_ archived record (enrollment or defense), an audit trail entry should be created with timestamp, entity type, entity ID, and archive location
**Validates: Requirements 4.11**

**Property 31: Archive Job Metrics**
_For any_ archive job execution, the recorded metrics should include count of enrollments archived, count of defenses archived, disk space freed in bytes, and execution duration
**Validates: Requirements 4.13**

### Data Consistency Verification Properties

**Property 32: User-Enrollment Consistency Check**
_For any_ enrollment record, if the referenced user_id does not exist in user-service, the enrollment should be marked as SUSPENDU, an anomaly should be logged, and an admin notification should be sent
**Validates: Requirements 5.2, 5.3**

**Property 33: Enrollment-Defense Consistency Check**
_For any_ defense request, if the referenced enrollment does not exist or has status other than VALIDÉ, the defense should be blocked, an anomaly should be logged, and director and admin should be notified
**Validates: Requirements 5.4, 5.5**

**Property 34: Doctorant Role Consistency**
_For any_ user with a validated enrollment and no ROLE_DOCTORANT_ACTIF, the role should be added automatically
**Validates: Requirements 5.6**

**Property 35: Doctorate Completion Role Transition**
_For any_ doctorant with a successfully completed defense (status COMPLETED with passing mention), ROLE_DOCTORANT_ACTIF should be removed and ROLE_DOCTEUR should be added
**Validates: Requirements 5.7**

**Property 36: Orphaned Document Detection**
_For any_ file in the uploads directory, if no database record references the file path, the file should be moved to uploads/orphelins directory and logged
**Validates: Requirements 5.8, 5.9**

**Property 37: Stale Notification Identification**
_For any_ notification with status PENDING where created_date is more than 24 hours ago, the notification should be identified for retry
**Validates: Requirements 5.10**

**Property 38: Notification Retry Handling**
_For any_ stale pending notification, a retry attempt should be made, and if the retry fails, the notification status should be updated to FAILED and technical admin should be notified
**Validates: Requirements 5.11, 5.12**

**Property 39: Anomaly Report Generation**
_For any_ consistency check execution that detects anomalies, a PDF anomaly report should be generated containing anomaly type, occurrence count, and corrective actions taken
**Validates: Requirements 5.13**

**Property 40: Anomaly Report Distribution**
_For any_ generated anomaly report, the PDF should be sent via email to the technical admin
**Validates: Requirements 5.14**

**Property 41: Consistency Check Metrics**
_For any_ consistency check job execution, the recorded metrics should include total anomalies detected, count of anomalies auto-corrected, and count of anomalies requiring manual intervention
**Validates: Requirements 5.16**

### Manual Job Execution API Properties

**Property 42: Admin Authorization Enforcement**
_For any_ request to /api/batch/jobs/{jobName}/run, if the authenticated user does not have ROLE_ADMIN, the request should be rejected with 403 Forbidden
**Validates: Requirements 6.1**

**Property 43: Job Trigger Response Completeness**
_For any_ valid job trigger request, the response should include job execution ID, status, start time, and confirmation message
**Validates: Requirements 6.2**

**Property 44: Job List Response Completeness**
_For any_ request to /api/batch/jobs, the response should include all available jobs with name, description, CRON expression, last execution time, and last status for each job
**Validates: Requirements 6.3**

**Property 45: Execution History Pagination**
_For any_ request to /api/batch/jobs/{jobName}/executions with page and size parameters, the response should return a paginated list of executions for that job
**Validates: Requirements 6.4**

**Property 46: Execution Details Completeness**
_For any_ request to /api/batch/jobs/executions/{executionId}, the response should include execution ID, job name, status, start time, end time, duration, items processed, items failed, and exit message
**Validates: Requirements 6.5**

**Property 47: Job Stop Confirmation**
_For any_ request to stop a running job execution, if the execution is successfully stopped, a confirmation response should be returned
**Validates: Requirements 6.6**

**Property 48: Global Statistics Completeness**
_For any_ request to /api/batch/stats, the response should include total executions, successful executions, failed executions, success rate, average duration, and last failure details
**Validates: Requirements 6.7**

**Property 49: Report List Response**
_For any_ request to /api/batch/reports, the response should include all generated monthly reports with download links
**Validates: Requirements 6.8**

**Property 50: Report Download Streaming**
_For any_ request to /api/batch/reports/{fileName}/download with valid filename, the PDF file should be streamed for download
**Validates: Requirements 6.9**

**Property 51: JWT Authentication Requirement**
_For any_ request to /api/batch/** endpoints without valid JWT token, the request should be rejected with 401 Unauthorized
**Validates: Requirements 6.10\*\*

**Property 52: Rate Limiting Enforcement**
_For any_ sequence of rapid requests to job trigger endpoints exceeding the configured rate limit, subsequent requests should be rejected with 429 Too Many Requests
**Validates: Requirements 6.11**

### Monitoring and Metrics Properties

**Property 53: Job Execution Metrics Exposure**
_For any_ job execution, metrics for total executions, successful executions, and failed executions should be exposed and accessible via Actuator endpoints
**Validates: Requirements 8.1**

**Property 54: Job Duration Metrics Exposure**
_For any_ job execution, metrics for average duration and total items processed should be exposed and accessible via Actuator endpoints
**Validates: Requirements 8.2**

**Property 55: Last Execution Status Exposure**
_For any_ job, the last execution status should be exposed and accessible via Actuator endpoints
**Validates: Requirements 8.3**

**Property 56: Global Metrics Exposure**
_For all_ jobs collectively, metrics for active jobs count, scheduled jobs count, and overall success rate should be exposed via Actuator endpoints
**Validates: Requirements 8.4**

**Property 57: Job Failure Detection and Notification**
_For any_ job execution that fails, an urgent Kafka notification event should be published AND an email alert should be sent to the technical team
**Validates: Requirements 8.8, 8.9**

**Property 58: Execution History Recording**
_For any_ job execution (successful or failed), a record should be inserted into job_execution_history table with all execution details
**Validates: Requirements 8.10**

### Security and Audit Properties

**Property 59: REST Endpoint Authorization**
_For any_ request to /api/batch/** endpoints, if the authenticated user does not have ROLE_ADMIN, the request should be rejected with 403 Forbidden
**Validates: Requirements 10.1\*\*

**Property 60: JWT Token Validation**
_For any_ request to /api/batch/** endpoints, the JWT token should be validated via API Gateway before processing
**Validates: Requirements 10.2\*\*

**Property 61: Archive File Encryption**
_For any_ archive file created, the file should be encrypted using AES-256 and should only be decryptable with the correct encryption key
**Validates: Requirements 10.4**

**Property 62: Archive Access Logging**
_For any_ access to an archive file, a log entry should be created with timestamp, user identifier, and file path accessed
**Validates: Requirements 10.6**

**Property 63: Job Execution Audit Logging**
_For any_ job execution (scheduled or manual), an audit log entry should be created and retained indefinitely
**Validates: Requirements 10.7**

**Property 64: Manual Execution User Tracking**
_For any_ manually triggered job execution via API, the audit log should record the admin user who triggered the execution
**Validates: Requirements 10.8**

**Property 65: Audit Log Immutability**
_For any_ audit log entry, deletion attempts should fail and the entry should be retained indefinitely
**Validates: Requirements 10.9**

**Property 66: Rate Limiting on Manual Triggers**
_For any_ sequence of manual job trigger requests from the same user exceeding the configured rate limit, subsequent requests should be rejected
**Validates: Requirements 10.10**

## Error Handling

### Job-Level Error Handling

**Job Restart Policy**

- Failed jobs can be restarted from the last successful step
- Use Spring Batch's built-in restart capability
- Store execution context to preserve state between restarts

**Job Failure Notifications**

- On job failure, publish urgent Kafka event to notifications topic
- Send email alert to technical admin team
- Include: job name, failure time, exception message, stack trace summary
- Log full exception details for debugging

**Retry Strategy**

- Configure retry policy for transient failures (database connection, network issues)
- Use exponential backoff: 1s, 2s, 4s, 8s, 16s
- Maximum 5 retry attempts per operation
- Skip items that fail after all retries (log for manual review)

### Step-Level Error Handling

**Chunk Transaction Rollback**

- If any item in a chunk fails processing, rollback entire chunk
- Log failed chunk details for investigation
- Option to skip failed chunks and continue (configurable per job)

**Skip Policy**

- Configure skip limit per step (e.g., skip up to 10 failed items)
- Log all skipped items with reason
- If skip limit exceeded, fail the entire step

**Item-Level Exception Handling**

- Catch and log exceptions during read, process, write operations
- Classify exceptions: Skippable vs. Fatal
- Skippable: Invalid data format, missing optional fields
- Fatal: Database connection loss, Kafka broker unavailable

### Database Error Handling

**Connection Pool Exhaustion**

- Configure HikariCP with appropriate pool size (minimum 5, maximum 20 per DataSource)
- Set connection timeout: 30 seconds
- Set idle timeout: 10 minutes
- On pool exhaustion, wait and retry with exponential backoff

**Deadlock Detection**

- Detect SQL deadlocks during batch operations
- Automatically retry deadlocked transactions (up to 3 attempts)
- If deadlock persists, fail the chunk and log for manual intervention

**Transaction Timeout**

- Set transaction timeout based on chunk size and expected processing time
- Token cleanup: 60 seconds per chunk
- Archive operations: 300 seconds per chunk (due to file I/O)
- If timeout occurs, rollback and retry with smaller chunk size

### Kafka Error Handling

**Producer Failures**

- Configure Kafka producer with retries: 3 attempts
- Use idempotent producer to prevent duplicate events
- On persistent failure, log event locally and schedule for retry
- Alert admin if Kafka is unavailable for > 5 minutes

**Serialization Errors**

- Validate DTO before sending to Kafka
- Catch serialization exceptions and log invalid data
- Skip invalid events (don't fail entire job)

### File System Error Handling

**Disk Space Exhaustion**

- Check available disk space before archive operations
- Minimum required: 10 GB free space
- If insufficient space, fail job and alert admin immediately
- Provide clear error message with space requirements

**File Permission Errors**

- Verify read/write permissions before file operations
- If permission denied, log error with file path and required permissions
- Fail job gracefully with actionable error message

**File Corruption**

- Verify file integrity after compression and encryption
- Use checksums (SHA-256) to detect corruption
- If corruption detected, retry operation once
- If retry fails, log error and skip file (don't fail entire job)

### PDF Generation Error Handling

**iText Exceptions**

- Catch PDF generation exceptions (font loading, image embedding, etc.)
- Log detailed error with context (which section failed)
- Attempt to generate partial report if possible
- If complete failure, send text-based report via email as fallback

**Chart Generation Errors**

- If JFreeChart fails to generate a chart, include placeholder text in PDF
- Log chart generation error with data that caused failure
- Continue with report generation (charts are non-critical)

### Recovery Mechanisms

**Automatic Recovery**

- Transient failures: Automatic retry with exponential backoff
- Database deadlocks: Automatic retry up to 3 times
- Network timeouts: Automatic retry with increased timeout

**Manual Recovery**

- Persistent failures: Alert admin with detailed error information
- Provide manual job trigger API for retry after issue resolution
- Maintain execution context for resuming from failure point

**Data Consistency Recovery**

- If job fails mid-execution, ensure no partial data corruption
- Use database transactions to maintain atomicity
- Rollback incomplete operations on failure
- Verify data consistency before marking job as complete

## Testing Strategy

### Dual Testing Approach

The batch-service requires both unit tests and property-based tests to ensure comprehensive correctness:

- **Unit Tests**: Verify specific examples, edge cases, error conditions, and integration points
- **Property-Based Tests**: Verify universal properties hold across all inputs through randomization

Both testing approaches are complementary and necessary. Unit tests catch concrete bugs in specific scenarios, while property-based tests verify general correctness across the input space.

### Property-Based Testing Framework

**Framework**: jqwik (Java property-based testing library)

- Integrates seamlessly with JUnit 5
- Provides powerful generators for complex data types
- Supports stateful testing for multi-step processes
- Minimum 100 iterations per property test (configurable up to 1000 for critical properties)

**Test Tagging Convention**
Each property test must reference its design document property:

```java
@Property
@Tag("Feature: batch-service, Property 1: Expired Token Identification")
void expiredTokenIdentification(@ForAll List<Token> tokens) {
    // Test implementation
}
```

### Unit Testing Strategy

**Configuration Tests**

- Verify all DataSources are correctly configured
- Verify CRON expressions are valid and match requirements
- Verify chunk sizes match specifications
- Verify Kafka producer configuration
- Verify file paths and directories exist

**Component Tests**

- Test ItemReaders with mock data sources
- Test ItemProcessors with various input scenarios
- Test ItemWriters with mock outputs
- Test Tasklets in isolation
- Test Listeners for correct callback behavior

**Integration Tests**

- Test complete job execution with embedded database (H2)
- Test Kafka event publishing with embedded Kafka (Testcontainers)
- Test file operations with temporary directories
- Test multi-step jobs with step transitions
- Test job restart after failure

**Controller Tests**

- Test REST endpoints with MockMvc
- Test authorization with different roles
- Test rate limiting behavior
- Test error responses (400, 401, 403, 404, 429, 500)
- Test file download streaming

**Edge Cases and Error Conditions**

- Empty result sets (no tokens to delete, no enrollments to alert)
- Null values in optional fields
- Database connection failures
- Kafka broker unavailability
- Disk space exhaustion
- File permission errors
- Invalid PDF generation data
- Concurrent job executions

### Property-Based Testing Strategy

**Token Cleanup Properties**

- Generate random sets of tokens with various expiry dates
- Verify only expired tokens are identified and deleted
- Verify chunk-based processing with different token counts
- Verify metrics accuracy across different execution scenarios

**Duration Alert Properties**

- Generate random enrollments with various first enrollment dates
- Verify correct threshold identification (3-year, 6-year, exceeded)
- Verify alert idempotence (no duplicates)
- Verify Kafka event structure and content
- Verify status updates for exceeded durations

**Report Generation Properties**

- Generate random statistics data
- Verify all required fields are collected
- Verify PDF structure completeness
- Verify file naming convention
- Verify email distribution to all admins

**Archive Properties**

- Generate random old enrollments and defenses
- Verify correct identification of archive candidates
- Verify complete archival process (copy, compress, encrypt, delete)
- Verify audit trail creation
- Verify encryption/decryption round-trip

**Consistency Check Properties**

- Generate random data with intentional inconsistencies
- Verify all inconsistencies are detected
- Verify corrective actions are applied
- Verify notifications are sent
- Verify anomaly report generation

**API Properties**

- Generate random job trigger requests
- Verify authorization enforcement
- Verify response completeness
- Verify rate limiting behavior
- Verify pagination correctness

**Generators for Property Tests**

```java
// Example: Generate tokens with random expiry dates
@Provide
Arbitrary<Token> tokens() {
    return Combinators.combine(
        Arbitraries.longs().between(1, 10000),
        Arbitraries.strings().alpha().ofLength(64),
        Arbitraries.localDateTimes()
    ).as((id, token, expiry) -> new Token(id, token, expiry));
}

// Example: Generate enrollments with various durations
@Provide
Arbitrary<Inscription> enrollments() {
    return Combinators.combine(
        Arbitraries.longs().between(1, 10000),
        Arbitraries.localDates().between(
            LocalDate.now().minusYears(7),
            LocalDate.now()
        ),
        Arbitraries.of(InscriptionStatus.values())
    ).as((id, firstDate, status) ->
        new Inscription(id, firstDate, status));
}
```

### Test Data Management

**Test Databases**

- Use H2 in-memory database for unit and integration tests
- Use Testcontainers with MariaDB for integration tests requiring full SQL compatibility
- Flyway migrations for test schema setup
- DBUnit for test data fixtures

**Test Kafka**

- Use Testcontainers Kafka for integration tests
- Verify event publishing and consumption
- Test serialization/deserialization

**Test File System**

- Use JUnit TemporaryFolder for file operations
- Clean up after each test
- Test with various file sizes and formats

### Test Coverage Goals

**Minimum Coverage Targets**

- Overall code coverage: 70%
- Service layer: 80%
- Controller layer: 75%
- Configuration classes: 60%
- Processors and Writers: 85%

**Coverage Tools**

- JaCoCo for code coverage measurement
- SonarQube for code quality analysis
- Fail build if coverage drops below minimum thresholds

### Performance Testing

**Load Testing**

- Test job execution with large datasets (100K+ records)
- Measure execution time and memory usage
- Verify chunk processing efficiency
- Test concurrent job executions

**Stress Testing**

- Test with database connection pool exhaustion
- Test with Kafka broker slowness
- Test with disk I/O bottlenecks
- Verify graceful degradation

### Test Execution

**Continuous Integration**

- Run all unit tests on every commit
- Run integration tests on pull requests
- Run property-based tests with 100 iterations in CI
- Run extended property tests (1000 iterations) nightly

**Test Organization**

```
src/test/java/
├── unit/
│   ├── config/
│   ├── processor/
│   ├── reader/
│   ├── writer/
│   └── tasklet/
├── integration/
│   ├── job/
│   ├── api/
│   └── kafka/
└── property/
    ├── TokenCleanupProperties.java
    ├── DurationAlertProperties.java
    ├── ReportGenerationProperties.java
    ├── ArchiveProperties.java
    ├── ConsistencyCheckProperties.java
    └── ApiProperties.java
```
