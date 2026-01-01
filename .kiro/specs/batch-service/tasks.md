# Implementation Plan: Batch Service

## Overview

This implementation plan breaks down the batch-service development into incremental, testable steps. The service will be built using Spring Boot 3.x with Spring Batch 5.x, implementing five core batch jobs plus REST API for manual execution and monitoring. Each task builds on previous work, with testing integrated throughout to ensure correctness.

## Tasks

- [x] 1. Project Setup and Infrastructure

  - Create Spring Boot project with Spring Batch dependencies
  - Configure Maven/Gradle with required dependencies (Spring Batch, Spring Data JPA, MariaDB, Kafka, iText, JFreeChart, jqwik, Testcontainers)
  - Set up project structure (config, controller, service, repository, dto, entity, reader, processor, writer, tasklet, listener packages)
  - Configure application.properties with all required properties
  - Configure Eureka client for service discovery on port 8085
  - _Requirements: 9.9, 9.10_

- [ ] 2. Multi-Database Configuration

  - [x] 2.1 Create MultiDataSourceConfig class

    - Configure primary DataSource for batchdb with HikariCP
    - Configure secondary DataSources for userdb, inscriptiondb, defensedb, notificationdb
    - Create JdbcTemplate beans for each DataSource
    - Externalize credentials to environment variables
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.10_

  - [ ]\* 2.2 Write unit tests for DataSource configuration
    - Test all DataSources are created and configured
    - Test JdbcTemplate beans are available
    - Test connection pooling configuration
    - _Requirements: 7.1-7.8_

- [x] 3. Database Schema and Entities

  - [x] 3.1 Create Flyway migrations for batch-service tables

    - Create job_execution_history table
    - Create alerte_duree_envoyee table with unique constraint
    - Create inscription_archive table
    - Create defense_archive table
    - Configure Spring Batch to auto-create metadata tables
    - _Requirements: 9.8_

  - [x] 3.2 Create JPA entities

    - JobExecutionHistory entity
    - AlerteDureeEnvoyee entity
    - InscriptionArchive entity
    - DefenseArchive entity
    - _Requirements: 2.7, 4.3, 4.5, 8.10_

  - [ ]\* 3.3 Write unit tests for entities
    - Test entity creation and persistence
    - Test unique constraints
    - Test relationships
    - _Requirements: 2.7, 4.3, 4.5_

- [x] 4. Kafka Configuration

  - [x] 4.1 Create KafkaProducerConfig class

    - Configure ProducerFactory with JSON serialization
    - Create KafkaTemplate bean
    - Configure retry and idempotence
    - _Requirements: 1.6, 2.3, 2.5, 2.6_

  - [x] 4.2 Create Kafka event DTOs

    - AlertEventDTO with all required fields
    - JobFailureEventDTO for failure notifications
    - _Requirements: 2.3, 2.5, 2.6, 8.8_

  - [ ]\* 4.3 Write integration tests for Kafka publishing
    - Test event serialization
    - Test event publishing to notifications topic
    - Use Testcontainers Kafka
    - _Requirements: 1.6, 2.3, 2.5, 2.6_

- [x] 5. Token Cleanup Job Implementation

  - [x] 5.1 Create ExpiredTokenReader

    - Implement JdbcCursorItemReader for refresh tokens
    - Implement JdbcCursorItemReader for password reset tokens
    - Configure fetch size to 100
    - _Requirements: 1.2, 1.3_

  - [x] 5.2 Create TokenDeletionWriter

    - Implement JdbcBatchItemWriter with DELETE SQL
    - Configure batch size to 100
    - _Requirements: 1.4_

  - [x] 5.3 Create TokenCleanupJobListener

    - Implement beforeJob to initialize metrics
    - Implement afterJob to log counts and record metrics
    - Publish Kafka event on failure
    - _Requirements: 1.5, 1.6, 1.7_

  - [x] 5.4 Create TokenCleanupJobConfig

    - Define tokenCleanupJob with two steps
    - Configure cleanupRefreshTokensStep with reader and writer
    - Configure cleanupPasswordResetTokensStep with reader and writer
    - Set chunk size to 100
    - Attach listener
    - _Requirements: 1.1, 1.4, 1.8_

  - [ ]\* 5.5 Write property test for expired token identification

    - **Property 1: Expired Token Identification**
    - **Validates: Requirements 1.2**

  - [ ]\* 5.6 Write property test for chunk-based deletion

    - **Property 3: Chunk-Based Deletion**
    - **Validates: Requirements 1.4**

  - [ ]\* 5.7 Write property test for deletion count logging

    - **Property 4: Deletion Count Logging**
    - **Validates: Requirements 1.5**

  - [ ]\* 5.8 Write property test for failure notification

    - **Property 5: Failure Notification**
    - **Validates: Requirements 1.6**

  - [ ]\* 5.9 Write integration test for complete token cleanup job
    - Test with H2 database and test data
    - Verify tokens are deleted
    - Verify metrics are recorded
    - _Requirements: 1.1-1.8_

- [x] 6. Checkpoint - Token Cleanup Job Complete

  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Duration Alert Job Implementation

  - [x] 7.1 Create EnrollmentReader for duration alerts

    - Implement JpaPagingItemReader from inscriptiondb
    - Query active enrollments with date calculations
    - Configure page size to 50
    - _Requirements: 2.2, 2.4, 2.8_

  - [x] 7.2 Create DureeAlertProcessor

    - Calculate exact duration from first enrollment date
    - Determine alert type (3_ANS, 6_ANS, DEPASSEMENT)
    - Check alerte_duree_envoyee table for duplicates
    - Build AlertEventDTO with all required fields
    - For DEPASSEMENT, prepare status update
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 7.3 Create KafkaAlertWriter

    - Implement ItemWriter using KafkaTemplate
    - Publish AlertEventDTO to notifications topic
    - Insert record into alerte_duree_envoyee table
    - Update enrollment status for DEPASSEMENT cases
    - _Requirements: 2.3, 2.5, 2.6, 2.7_

  - [x] 7.4 Create DureeAlertJobListener

    - Track counts of 3-year, 6-year, and exceeded alerts
    - Record metrics on job completion
    - _Requirements: 2.10_

  - [x] 7.5 Create DureeDoctoratAlertJobConfig

    - Define dureeDoctoratAlertJob with three steps
    - Configure check3YearThresholdStep
    - Configure check6YearThresholdStep
    - Configure checkExceeded6YearStep
    - Set chunk size to 50
    - Attach listener
    - Configure CRON: "0 0 8 ? \* MON"
    - _Requirements: 2.1, 2.8, 2.9_

  - [ ]\* 7.6 Write property test for 3-year threshold identification

    - **Property 7: Three-Year Threshold Identification**
    - **Validates: Requirements 2.2**

  - [ ]\* 7.7 Write property test for 3-year alert publishing

    - **Property 8: Three-Year Alert Publishing**
    - **Validates: Requirements 2.3**

  - [ ]\* 7.8 Write property test for 6-year threshold identification

    - **Property 9: Six-Year Threshold Identification**
    - **Validates: Requirements 2.4**

  - [ ]\* 7.9 Write property test for alert idempotence

    - **Property 12: Alert Idempotence**
    - **Validates: Requirements 2.7**

  - [ ]\* 7.10 Write property test for exceeded duration handling

    - **Property 11: Exceeded Duration Handling**
    - **Validates: Requirements 2.6**

  - [ ]\* 7.11 Write integration test for complete duration alert job
    - Test with various enrollment dates
    - Verify correct alerts are sent
    - Verify no duplicate alerts
    - Verify status updates for exceeded cases
    - _Requirements: 2.1-2.10_

- [x] 8. Checkpoint - Duration Alert Job Complete

  - Ensure all tests pass, ask the user if questions arise.

- [-] 9. Monthly Report Generation Job Implementation

  - [x] 9.1 Create statistics DTOs

    - EnrollmentStatsDTO with all required fields
    - DefenseStatsDTO with all required fields
    - NotificationStatsDTO with all required fields
    - UserStatsDTO with all required fields
    - MonthlyReportDataDTO to aggregate all statistics
    - _Requirements: 3.2, 3.3, 3.4, 3.5_

  - [x] 9.2 Create CollectEnrollmentStatsTasklet

    - Use inscriptionJdbcTemplate for queries
    - Collect all enrollment statistics for previous month
    - Store in execution context
    - _Requirements: 3.2_

  - [x] 9.3 Create CollectDefenseStatsTasklet

    - Use defenseJdbcTemplate for queries
    - Collect all defense statistics for previous month
    - Store in execution context
    - _Requirements: 3.3_

  - [x] 9.4 Create CollectNotificationStatsTasklet

    - Use notificationJdbcTemplate for queries
    - Collect all notification statistics for previous month
    - Store in execution context
    - _Requirements: 3.4_

  - [x] 9.5 Create CollectUserStatsTasklet

    - Use userJdbcTemplate for queries
    - Collect all user statistics for previous month
    - Store in execution context
    - _Requirements: 3.5_

  - [x] 9.6 Create GeneratePdfTasklet

    - Retrieve all statistics from execution context
    - Use iText 7 to create PDF document
    - Add header with logo and title
    - Create KPI dashboard section
    - Generate charts with JFreeChart (bar charts, pie charts)
    - Add detailed tables for each statistic category
    - Add alerts section if anomalies exist
    - Add footer with generation timestamp
    - Save to configured reports directory with format rapport_YYYY_MM.pdf
    - _Requirements: 3.6, 3.7_

  - [x] 9.7 Create SendReportNotificationTasklet

    - Retrieve PDF path from execution context
    - Query all users with ROLE_ADMIN from userdb
    - Send email with PDF attachment to each admin
    - Publish Kafka event with PDF reference
    - Log send status
    - _Requirements: 3.8, 3.9_

  - [x] 9.8 Create ReportJobListener

    - Record PDF size, generation duration, and send status
    - _Requirements: 3.11_

  - [x] 9.9 Create MonthlyReportJobConfig

    - Define monthlyReportJob with six tasklet steps
    - Configure all collection tasklets in sequence
    - Configure PDF generation tasklet
    - Configure notification tasklet
    - Attach listener
    - Configure CRON: "0 0 9 1 \* ?"
    - _Requirements: 3.1, 3.10_

  - [ ]\* 9.10 Write property test for enrollment statistics completeness

    - **Property 14: Enrollment Statistics Completeness**
    - **Validates: Requirements 3.2**

  - [ ]\* 9.11 Write property test for PDF structure completeness

    - **Property 18: PDF Structure Completeness**
    - **Validates: Requirements 3.6**

  - [ ]\* 9.12 Write property test for report file naming

    - **Property 19: Report File Naming**
    - **Validates: Requirements 3.7**

  - [ ]\* 9.13 Write property test for admin email distribution

    - **Property 20: Admin Email Distribution**
    - **Validates: Requirements 3.8**

  - [ ]\* 9.14 Write integration test for complete monthly report job
    - Test with sample statistics data
    - Verify PDF is generated with all sections
    - Verify file naming convention
    - Verify email distribution
    - Verify Kafka event publishing
    - _Requirements: 3.1-3.11_

- [x] 10. Checkpoint - Monthly Report Job Complete

  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Archive Job Implementation

  - [x] 11.1 Create ArchiveCandidateReader for enrollments

    - Implement JdbcCursorItemReader from inscriptiondb
    - Query enrollments with status VALIDÉ or REJETÉ, older than 1 year, not archived
    - Configure fetch size to 20
    - _Requirements: 4.2, 4.9_

  - [x] 11.2 Create ArchiveCandidateReader for defenses

    - Implement JdbcCursorItemReader from defensedb
    - Query completed defenses older than 1 year with signed PV, not archived
    - Configure fetch size to 20
    - _Requirements: 4.4, 4.9_

  - [x] 11.3 Create ArchiveProcessor

    - Copy record data to archive entity
    - Locate associated documents in file system
    - Compress documents to ZIP format
    - Encrypt ZIP file with AES-256
    - Generate archive location path
    - Prepare audit trail entry
    - _Requirements: 4.3, 4.5, 4.10, 4.11_

  - [x] 11.4 Create ArchiveWriter

    - Insert into inscription_archive or defense_archive table
    - Write encrypted ZIP to archives directory
    - Insert audit trail entry
    - Update original record (set archived = true)
    - Delete original documents
    - All operations in single transaction
    - _Requirements: 4.3, 4.5, 4.11_

  - [x] 11.5 Create CleanupLogsTasklet

    - Delete application logs older than 6 months
    - Delete notification logs older than 3 months (successful only)
    - Retain all error logs indefinitely
    - _Requirements: 4.6, 4.7_

  - [x] 11.6 Create DatabaseOptimizationTasklet

    - Execute OPTIMIZE TABLE on affected tables
    - Regenerate table statistics
    - Verify index integrity
    - Log optimization results
    - _Requirements: 4.8_

  - [x] 11.7 Create ArchiveJobListener

    - Track enrollments archived, defenses archived, disk space freed
    - Record metrics on job completion
    - _Requirements: 4.13_

  - [x] 11.8 Create ArchiveJobConfig

    - Define archiveJob with four steps
    - Configure archiveEnrollmentsStep with reader, processor, writer
    - Configure archiveDefensesStep with reader, processor, writer
    - Configure cleanupLogsTasklet
    - Configure optimizeDatabaseTasklet
    - Set chunk size to 20
    - Attach listener
    - Configure CRON: "0 0 3 1 1,4,7,10 ?"
    - _Requirements: 4.1, 4.9, 4.12_

  - [ ]\* 11.9 Write property test for enrollment archive identification

    - **Property 23: Enrollment Archive Identification**
    - **Validates: Requirements 4.2**

  - [ ]\* 11.10 Write property test for enrollment archival process

    - **Property 24: Enrollment Archival Process**
    - **Validates: Requirements 4.3**

  - [ ]\* 11.11 Write property test for archive encryption

    - **Property 29: Archive Encryption**
    - **Validates: Requirements 4.10**

  - [ ]\* 11.12 Write property test for error log retention

    - **Property 28: Error Log Retention**
    - **Validates: Requirements 4.7**

  - [ ]\* 11.13 Write integration test for complete archive job
    - Test with old enrollments and defenses
    - Verify archival process
    - Verify encryption/decryption
    - Verify log cleanup
    - Verify database optimization
    - _Requirements: 4.1-4.13_

- [x] 12. Checkpoint - Archive Job Complete

  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Data Consistency Verification Job Implementation

  - [x] 13.1 Create VerifyUserEnrollmentConsistencyTasklet

    - Query all enrollments from inscriptiondb
    - Check each enrollment's user exists in userdb
    - For missing users: log anomaly, mark enrollment SUSPENDU, notify admin
    - Track anomaly count
    - _Requirements: 5.2, 5.3_

  - [x] 13.2 Create VerifyEnrollmentDefenseConsistencyTasklet

    - Query all defense requests from defensedb
    - Check each defense has valid enrollment with status VALIDÉ
    - For invalid: log anomaly, block defense, notify director and admin
    - Track anomaly count
    - _Requirements: 5.4, 5.5_

  - [x] 13.3 Create VerifyUserRolesTasklet

    - Query doctorants with validated enrollments
    - Check for ROLE_DOCTORANT_ACTIF, add if missing
    - Query doctorants with completed defenses
    - Remove ROLE_DOCTORANT_ACTIF, add ROLE_DOCTEUR
    - Synchronize roles
    - Track role changes
    - _Requirements: 5.6, 5.7_

  - [x] 13.4 Create CheckOrphanedDocumentsTasklet

    - List all files in uploads directory
    - Query database for file references
    - Identify orphaned files
    - Move orphans to uploads/orphelins directory
    - Log orphaned file list
    - Track orphan count
    - _Requirements: 5.8, 5.9_

  - [x] 13.5 Create RetryPendingNotificationsTasklet

    - Query notifications with status PENDING older than 24 hours
    - Retry sending each notification
    - If retry fails, mark as FAILED and notify technical admin
    - Track retry success/failure counts
    - _Requirements: 5.10, 5.11, 5.12_

  - [x] 13.6 Create GenerateAnomalyReportTasklet

    - Check if anomalies were detected in previous steps
    - If yes, generate PDF anomaly report with type, count, corrective actions
    - Send report via email to technical admin
    - _Requirements: 5.13, 5.14_

  - [x] 13.7 Create ConsistencyJobListener

    - Track total anomalies, auto-corrected, manual intervention required
    - Record metrics on job completion
    - _Requirements: 5.16_

  - [x] 13.8 Create DataConsistencyJobConfig

    - Define dataConsistencyJob with six tasklet steps
    - Configure all verification tasklets in sequence
    - Configure anomaly report tasklet (conditional execution)
    - Attach listener
    - Configure CRON: "0 0 23 \* \* ?"
    - _Requirements: 5.1, 5.15_

  - [ ]\* 13.9 Write property test for user-enrollment consistency check

    - **Property 32: User-Enrollment Consistency Check**
    - **Validates: Requirements 5.2, 5.3**

  - [ ]\* 13.10 Write property test for enrollment-defense consistency check

    - **Property 33: Enrollment-Defense Consistency Check**
    - **Validates: Requirements 5.4, 5.5**

  - [ ]\* 13.11 Write property test for doctorate completion role transition

    - **Property 35: Doctorate Completion Role Transition**
    - **Validates: Requirements 5.7**

  - [ ]\* 13.12 Write property test for orphaned document detection

    - **Property 36: Orphaned Document Detection**
    - **Validates: Requirements 5.8, 5.9**

  - [ ]\* 13.13 Write integration test for complete consistency check job
    - Test with intentional inconsistencies
    - Verify all inconsistencies are detected
    - Verify corrective actions
    - Verify anomaly report generation
    - _Requirements: 5.1-5.16_

- [x] 14. Checkpoint - Data Consistency Job Complete

  - Ensure all tests pass, ask the user if questions arise.

- [x] 15. Job Scheduling Configuration

  - [x] 15.1 Create JobSchedulerConfig class

    - Enable scheduling with @EnableScheduling
    - Create @Scheduled methods for each job with CRON expressions
    - Token cleanup: "0 0 2 \* \* ?"
    - Duration alert: "0 0 8 ? \* MON"
    - Monthly report: "0 0 9 1 \* ?"
    - Archive: "0 0 3 1 1,4,7,10 ?"
    - Consistency check: "0 0 23 \* \* ?"
    - Use JobLauncher to execute jobs
    - _Requirements: 1.1, 1.8, 2.1, 2.9, 3.1, 3.10, 4.1, 4.12, 5.1, 5.15_

  - [ ]\* 15.2 Write unit tests for CRON expressions
    - Verify each CRON expression is valid
    - Verify expressions match requirements
    - _Requirements: 1.8, 2.9, 3.10, 4.12, 5.15_

- [x] 16. REST API for Manual Job Execution

  - [x] 16.1 Create BatchJobService

    - Implement manual job triggering with JobLauncher
    - Implement job listing with metadata
    - Implement execution history retrieval
    - Implement execution details retrieval
    - Implement job stopping
    - Implement global statistics calculation
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [x] 16.2 Create BatchJobController

    - POST /{jobName}/run endpoint
    - GET / endpoint for job listing
    - GET /{jobName}/executions endpoint with pagination
    - GET /executions/{executionId} endpoint
    - POST /executions/{executionId}/stop endpoint
    - GET /stats endpoint
    - Add @PreAuthorize("hasRole('ADMIN')") to all endpoints
    - Add @RateLimiter to trigger endpoint
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.11_

  - [x] 16.3 Create BatchReportController

    - GET / endpoint for report listing
    - GET /{fileName}/download endpoint with streaming
    - Add @PreAuthorize("hasRole('ADMIN')")
    - _Requirements: 6.8, 6.9_

  - [x] 16.4 Create DTOs for API responses

    - JobExecutionDTO
    - JobInfoDTO
    - GlobalStatsDTO
    - ReportInfoDTO
    - _Requirements: 6.2, 6.3, 6.5, 6.7, 6.8_

  - [ ]\* 16.5 Write property test for admin authorization enforcement

    - **Property 42: Admin Authorization Enforcement**
    - **Validates: Requirements 6.1**

  - [ ]\* 16.6 Write property test for job trigger response completeness

    - **Property 43: Job Trigger Response Completeness**
    - **Validates: Requirements 6.2**

  - [ ]\* 16.7 Write property test for JWT authentication requirement

    - **Property 51: JWT Authentication Requirement**
    - **Validates: Requirements 6.10**

  - [ ]\* 16.8 Write property test for rate limiting enforcement

    - **Property 52: Rate Limiting Enforcement**
    - **Validates: Requirements 6.11**

  - [ ]\* 16.9 Write integration tests for REST API
    - Test all endpoints with MockMvc
    - Test authorization with different roles
    - Test rate limiting
    - Test error responses
    - _Requirements: 6.1-6.11_

- [x] 17. Checkpoint - REST API Complete

  - Ensure all tests pass, ask the user if questions arise.

- [x] 18. Monitoring and Metrics Implementation

  - [x] 18.1 Create custom Actuator metrics

    - Create MeterRegistry beans for job metrics
    - Expose metrics for each job: executions, success, failure, duration, items processed
    - Expose global metrics: active jobs, scheduled jobs, success rate
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 18.2 Configure Spring Boot Actuator

    - Enable health checks for all DataSources
    - Enable Prometheus metrics endpoint
    - Enable info endpoint with job information
    - Configure management endpoints exposure
    - _Requirements: 8.5, 8.6, 8.7_

  - [x] 18.3 Create JobExecutionHistoryService

    - Implement recording of job execution details
    - Implement retrieval of execution history
    - Implement metrics calculation
    - _Requirements: 8.10_

  - [x] 18.4 Update all job listeners to record execution history

    - Call JobExecutionHistoryService in afterJob()
    - Record all execution details
    - _Requirements: 8.10_

  - [ ]\* 18.5 Write property test for job execution metrics exposure

    - **Property 53: Job Execution Metrics Exposure**
    - **Validates: Requirements 8.1**

  - [ ]\* 18.6 Write property test for job failure detection and notification

    - **Property 57: Job Failure Detection and Notification**
    - **Validates: Requirements 8.8, 8.9**

  - [ ]\* 18.7 Write property test for execution history recording

    - **Property 58: Execution History Recording**
    - **Validates: Requirements 8.10**

  - [ ]\* 18.8 Write integration tests for Actuator endpoints
    - Test health endpoint
    - Test metrics endpoint
    - Test Prometheus endpoint
    - _Requirements: 8.5, 8.6, 8.7_

- [x] 19. Security Implementation

  - [x] 19.1 Create SecurityConfig class

    - Configure JWT authentication via API Gateway
    - Require ROLE_ADMIN for /api/batch/\*\* endpoints
    - Configure CORS if needed
    - _Requirements: 10.1, 10.2_

  - [x] 19.2 Create EncryptionService

    - Implement AES-256 encryption for archive files
    - Implement decryption for archive retrieval
    - Integrate with key vault for key management
    - _Requirements: 10.4, 10.5_

  - [x] 19.3 Create AuditLoggingService

    - Implement audit logging for all job executions
    - Implement audit logging for archive access
    - Implement audit logging for manual job triggers
    - Prevent audit log deletion
    - _Requirements: 10.6, 10.7, 10.8, 10.9_

  - [x] 19.4 Create RateLimitingConfig

    - Configure rate limiting for job trigger endpoints
    - Use Resilience4j RateLimiter
    - _Requirements: 10.10_

  - [ ]\* 19.5 Write property test for REST endpoint authorization

    - **Property 59: REST Endpoint Authorization**
    - **Validates: Requirements 10.1**

  - [ ]\* 19.6 Write property test for archive file encryption

    - **Property 61: Archive File Encryption**
    - **Validates: Requirements 10.4**

  - [ ]\* 19.7 Write property test for audit log immutability

    - **Property 65: Audit Log Immutability**
    - **Validates: Requirements 10.9**

  - [ ]\* 19.8 Write integration tests for security
    - Test JWT validation
    - Test role-based authorization
    - Test encryption/decryption
    - Test audit logging
    - Test rate limiting
    - _Requirements: 10.1, 10.2, 10.4, 10.6-10.10_

- [x] 20. Checkpoint - Security and Monitoring Complete

  - Ensure all tests pass, ask the user if questions arise.

- [x] 21. Error Handling and Resilience

  - [x] 21.1 Configure retry policies for all jobs

    - Configure RetryTemplate with exponential backoff
    - Set maximum retry attempts
    - Configure skip policies for non-fatal errors
    - _Requirements: Error Handling section_

  - [x] 21.2 Configure transaction management

    - Set appropriate transaction timeouts per job
    - Configure rollback rules
    - Handle deadlock detection and retry
    - _Requirements: Error Handling section_

  - [x] 21.3 Implement failure notification mechanism

    - Create FailureNotificationService
    - Publish Kafka events on job failure
    - Send email alerts to technical team
    - Include detailed error information
    - _Requirements: 8.8, 8.9_

  - [x] 21.4 Implement graceful degradation

    - Handle database connection failures
    - Handle Kafka broker unavailability
    - Handle disk space exhaustion
    - Handle file permission errors
    - _Requirements: Error Handling section_

  - [ ]\* 21.5 Write integration tests for error scenarios
    - Test database connection failure
    - Test Kafka unavailability
    - Test disk space exhaustion
    - Test transaction rollback
    - Test retry mechanisms
    - _Requirements: Error Handling section_

- [x] 22. Documentation

  - [x] 22.1 Create comprehensive README.md

    - Service description and responsibilities
    - Architecture diagram
    - Prerequisites and dependencies
    - Installation and configuration instructions
    - Job descriptions with CRON schedules
    - API documentation
    - Monitoring and troubleshooting guide
    - _Requirements: All_

  - [x] 22.2 Configure SpringDoc OpenAPI

    - Add springdoc-openapi dependency
    - Document all REST endpoints
    - Add request/response examples
    - Add authentication documentation
    - _Requirements: 6.1-6.11_

  - [x] 22.3 Create sequence diagrams
    - Job execution flow
    - Manual job trigger flow
    - Failure notification flow
    - Archive process flow
    - _Requirements: All_

- [ ] 23. Integration Testing with Testcontainers

  - [ ]\* 23.1 Create IntegrationTestBase class

    - Configure Testcontainers for MariaDB
    - Configure Testcontainers for Kafka
    - Set up test data fixtures
    - Configure test properties
    - _Requirements: All_

  - [ ]\* 23.2 Write end-to-end integration tests
    - Test complete token cleanup workflow
    - Test complete duration alert workflow
    - Test complete monthly report workflow
    - Test complete archive workflow
    - Test complete consistency check workflow
    - Test manual job execution via API
    - _Requirements: All_

- [ ] 24. Performance Testing

  - [ ]\* 24.1 Write performance tests for large datasets

    - Test token cleanup with 100K+ tokens
    - Test duration alert with 10K+ enrollments
    - Test archive with 1K+ records
    - Measure execution time and memory usage
    - _Requirements: All_

  - [ ]\* 24.2 Write stress tests
    - Test with database connection pool exhaustion
    - Test with Kafka broker slowness
    - Test with disk I/O bottlenecks
    - Verify graceful degradation
    - _Requirements: Error Handling section_

- [ ] 25. Final Integration and Deployment Preparation

  - [ ] 25.1 Create Docker configuration

    - Create Dockerfile for batch-service
    - Create docker-compose.yml for local testing
    - Configure environment variables
    - _Requirements: All_

  - [ ] 25.2 Create deployment scripts

    - Database migration scripts
    - Service startup scripts
    - Health check scripts
    - _Requirements: All_

  - [ ] 25.3 Verify Eureka registration

    - Test service discovery
    - Verify health checks
    - Test failover scenarios
    - _Requirements: 9.10_

  - [ ]\* 25.4 Run complete test suite
    - Run all unit tests
    - Run all property-based tests
    - Run all integration tests
    - Verify code coverage meets 70% minimum
    - _Requirements: All_

- [ ] 26. Final Checkpoint - Batch Service Complete
  - Ensure all tests pass, verify all requirements are met, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end workflows
- The implementation follows a bottom-up approach: infrastructure → jobs → API → monitoring → security
- Each job is implemented and tested independently before moving to the next
- Testing is integrated throughout to catch issues early
