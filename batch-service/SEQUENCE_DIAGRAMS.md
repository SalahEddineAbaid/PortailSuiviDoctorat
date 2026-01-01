# Batch Service Sequence Diagrams

This document contains sequence diagrams illustrating the key workflows in the batch-service.

## Table of Contents

1. [Job Execution Flow](#1-job-execution-flow)
2. [Manual Job Trigger Flow](#2-manual-job-trigger-flow)
3. [Failure Notification Flow](#3-failure-notification-flow)
4. [Archive Process Flow](#4-archive-process-flow)

---

## 1. Job Execution Flow

This diagram shows the standard execution flow for a scheduled batch job.

```mermaid
sequenceDiagram
    participant Scheduler as Job Scheduler
    participant JobLauncher as Job Launcher
    participant Job as Batch Job
    participant Listener as Job Listener
    participant Reader as Item Reader
    participant Processor as Item Processor
    participant Writer as Item Writer
    participant DB as Database
    participant Metrics as Metrics Service

    Note over Scheduler: CRON trigger fires
    Scheduler->>JobLauncher: Launch job with parameters
    JobLauncher->>Job: Start job execution
    Job->>Listener: beforeJob()
    Listener->>Metrics: Initialize metrics

    loop For each step
        Job->>Reader: Read chunk of items
        Reader->>DB: Query data
        DB-->>Reader: Return data
        Reader-->>Job: Return items

        loop For each item in chunk
            Job->>Processor: Process item
            Processor-->>Job: Return processed item
        end

        Job->>Writer: Write chunk
        Writer->>DB: Persist data
        DB-->>Writer: Confirm write
        Writer-->>Job: Chunk complete
    end

    Job->>Listener: afterJob()
    Listener->>DB: Record execution history
    Listener->>Metrics: Update metrics
    Listener-->>Job: Listener complete
    Job-->>JobLauncher: Job complete
    JobLauncher-->>Scheduler: Execution finished
```

---

## 2. Manual Job Trigger Flow

This diagram shows the flow when an administrator manually triggers a job via REST API.

```mermaid
sequenceDiagram
    participant Admin as Administrator
    participant Gateway as API Gateway
    participant Controller as Batch Controller
    participant RateLimiter as Rate Limiter
    participant Service as Batch Job Service
    participant JobLauncher as Job Launcher
    participant Job as Batch Job
    participant AuditLog as Audit Log Service
    participant DB as Database

    Admin->>Gateway: POST /api/batch/jobs/{jobName}/run
    Note over Admin,Gateway: JWT Token in Authorization header

    Gateway->>Gateway: Validate JWT token
    Gateway->>Gateway: Check ROLE_ADMIN

    alt Invalid token or insufficient role
        Gateway-->>Admin: 403 Forbidden
    else Valid authentication
        Gateway->>Controller: Forward request
        Controller->>RateLimiter: Check rate limit

        alt Rate limit exceeded
            RateLimiter-->>Controller: Limit exceeded
            Controller-->>Admin: 429 Too Many Requests
        else Within rate limit
            RateLimiter-->>Controller: Allow request
            Controller->>Service: triggerJob(jobName)

            alt Invalid job name
                Service-->>Controller: IllegalArgumentException
                Controller-->>Admin: 400 Bad Request
            else Valid job name
                Service->>AuditLog: Log manual trigger
                AuditLog->>DB: Insert audit entry

                Service->>JobLauncher: Launch job
                JobLauncher->>Job: Start execution
                Job-->>JobLauncher: Execution started
                JobLauncher-->>Service: Return execution ID

                Service-->>Controller: JobTriggerResponseDTO
                Controller-->>Admin: 200 OK with execution details

                Note over Job: Job continues executing asynchronously
            end
        end
    end
```

---

## 3. Failure Notification Flow

This diagram shows what happens when a batch job fails.

```mermaid
sequenceDiagram
    participant Job as Batch Job
    participant Listener as Job Listener
    participant FailureService as Failure Notification Service
    participant Kafka as Kafka Broker
    participant EmailService as Email Service
    participant DB as Database
    participant NotificationService as Notification Service
    participant Admin as Administrator

    Job->>Job: Exception occurs during execution
    Job->>Listener: afterJob(FAILED status)

    Listener->>DB: Record failed execution
    DB-->>Listener: Execution recorded

    Listener->>FailureService: notifyFailure(jobName, error)

    par Kafka Notification
        FailureService->>Kafka: Publish JobFailureEvent
        Kafka->>NotificationService: Consume event
        NotificationService->>Admin: Send notification
    and Email Notification
        FailureService->>EmailService: Send failure email
        EmailService->>Admin: Email with error details
    end

    FailureService-->>Listener: Notifications sent
    Listener-->>Job: Failure handling complete

    Note over Admin: Administrator receives<br/>failure notifications via<br/>email and system notification
```

---

## 4. Archive Process Flow

This diagram shows the complete archive process for old enrollment and defense records.

```mermaid
sequenceDiagram
    participant Scheduler as Job Scheduler
    participant Job as Archive Job
    participant Reader as Archive Reader
    participant Processor as Archive Processor
    participant Writer as Archive Writer
    participant InscriptionDB as Inscription DB
    participant DefenseDB as Defense DB
    participant BatchDB as Batch DB
    participant FileSystem as File System
    participant Encryption as Encryption Service
    participant AuditLog as Audit Log Service

    Scheduler->>Job: Trigger quarterly archive job

    Note over Job: Step 1: Archive Enrollments

    loop For each chunk of old enrollments
        Job->>Reader: Read enrollment candidates
        Reader->>InscriptionDB: Query old enrollments
        InscriptionDB-->>Reader: Return enrollments
        Reader-->>Job: Return chunk

        loop For each enrollment
            Job->>Processor: Process enrollment
            Processor->>FileSystem: Locate associated documents
            FileSystem-->>Processor: Return document paths
            Processor->>Processor: Compress documents to ZIP
            Processor->>Encryption: Encrypt ZIP file
            Encryption-->>Processor: Return encrypted file
            Processor-->>Job: Return ArchivePackage
        end

        Job->>Writer: Write archive chunk

        par Parallel operations in transaction
            Writer->>BatchDB: Insert into inscription_archive
            Writer->>FileSystem: Write encrypted ZIP
            Writer->>AuditLog: Create audit trail entry
            Writer->>InscriptionDB: Update archived flag
            Writer->>FileSystem: Delete original documents
        end

        Writer-->>Job: Chunk archived
    end

    Note over Job: Step 2: Archive Defenses

    loop For each chunk of old defenses
        Job->>Reader: Read defense candidates
        Reader->>DefenseDB: Query old defenses
        DefenseDB-->>Reader: Return defenses
        Reader-->>Job: Return chunk

        loop For each defense
            Job->>Processor: Process defense
            Processor->>FileSystem: Locate documents
            Processor->>Processor: Compress to ZIP
            Processor->>Encryption: Encrypt ZIP
            Encryption-->>Processor: Return encrypted file
            Processor-->>Job: Return ArchivePackage
        end

        Job->>Writer: Write archive chunk

        par Parallel operations in transaction
            Writer->>BatchDB: Insert into defense_archive
            Writer->>FileSystem: Write encrypted ZIP
            Writer->>AuditLog: Create audit trail entry
            Writer->>DefenseDB: Update archived flag
            Writer->>FileSystem: Delete original documents
        end

        Writer-->>Job: Chunk archived
    end

    Note over Job: Step 3: Clean up logs
    Job->>FileSystem: Delete old application logs (>6 months)
    Job->>FileSystem: Delete old notification logs (>3 months, successful only)
    FileSystem-->>Job: Logs cleaned

    Note over Job: Step 4: Optimize database
    Job->>InscriptionDB: OPTIMIZE TABLE
    Job->>DefenseDB: OPTIMIZE TABLE
    Job->>BatchDB: OPTIMIZE TABLE

    Job-->>Scheduler: Archive job complete
```

---

## Additional Diagrams

### Token Cleanup Job Flow

```mermaid
sequenceDiagram
    participant Scheduler as Job Scheduler
    participant Job as Token Cleanup Job
    participant Reader as Token Reader
    participant Writer as Token Writer
    participant UserDB as User Database
    participant Listener as Job Listener
    participant Metrics as Metrics Service

    Scheduler->>Job: Daily at 2:00 AM
    Job->>Listener: beforeJob()

    Note over Job: Step 1: Clean refresh tokens
    loop For each chunk of 100 tokens
        Job->>Reader: Read expired refresh tokens
        Reader->>UserDB: SELECT * FROM refresh_token WHERE expiry_date < NOW()
        UserDB-->>Reader: Return expired tokens
        Reader-->>Job: Return chunk

        Job->>Writer: Delete chunk
        Writer->>UserDB: DELETE FROM refresh_token WHERE id IN (...)
        UserDB-->>Writer: Confirm deletion
        Writer-->>Job: Chunk deleted
    end

    Note over Job: Step 2: Clean password reset tokens
    loop For each chunk of 100 tokens
        Job->>Reader: Read expired password reset tokens
        Reader->>UserDB: SELECT * FROM password_reset_token WHERE expiry_date < NOW()
        UserDB-->>Reader: Return expired tokens
        Reader-->>Job: Return chunk

        Job->>Writer: Delete chunk
        Writer->>UserDB: DELETE FROM password_reset_token WHERE id IN (...)
        UserDB-->>Writer: Confirm deletion
        Writer-->>Job: Chunk deleted
    end

    Job->>Listener: afterJob()
    Listener->>Metrics: Record deletion counts
    Listener->>Metrics: Record execution duration
    Job-->>Scheduler: Job complete
```

### Duration Alert Job Flow

```mermaid
sequenceDiagram
    participant Scheduler as Job Scheduler
    participant Job as Duration Alert Job
    participant Reader as Enrollment Reader
    participant Processor as Alert Processor
    participant Writer as Kafka Writer
    participant InscriptionDB as Inscription DB
    participant BatchDB as Batch DB
    participant Kafka as Kafka Broker
    participant NotificationService as Notification Service

    Scheduler->>Job: Every Monday at 8:00 AM

    Note over Job: Step 1: Check 3-year threshold
    loop For each chunk of 50 enrollments
        Job->>Reader: Read enrollments approaching 3 years
        Reader->>InscriptionDB: Query with date calculations
        InscriptionDB-->>Reader: Return enrollments
        Reader-->>Job: Return chunk

        loop For each enrollment
            Job->>Processor: Process enrollment
            Processor->>Processor: Calculate exact duration
            Processor->>BatchDB: Check if alert already sent

            alt Alert not sent
                Processor->>Processor: Build AlertEventDTO
                Processor-->>Job: Return alert event
            else Alert already sent
                Processor-->>Job: Return null (skip)
            end
        end

        Job->>Writer: Write alerts
        Writer->>Kafka: Publish alert events
        Writer->>BatchDB: Insert into alerte_duree_envoyee
        Kafka->>NotificationService: Consume events
        Writer-->>Job: Alerts sent
    end

    Note over Job: Step 2: Check 6-year threshold
    Note over Job: (Similar process with high priority)

    Note over Job: Step 3: Check exceeded 6 years
    loop For each enrollment exceeding 6 years
        Job->>Processor: Process enrollment
        Processor->>Processor: Build urgent alert
        Processor-->>Job: Return alert + status update

        Job->>Writer: Write alerts and updates
        Writer->>Kafka: Publish urgent alerts
        Writer->>InscriptionDB: UPDATE status = 'BLOQUÉ'
        Writer->>BatchDB: Record alert sent
        Writer-->>Job: Complete
    end

    Job-->>Scheduler: Job complete
```

### Monthly Report Generation Flow

```mermaid
sequenceDiagram
    participant Scheduler as Job Scheduler
    participant Job as Monthly Report Job
    participant StatsTasklet as Stats Collection Tasklets
    participant PdfTasklet as PDF Generation Tasklet
    participant NotifyTasklet as Notification Tasklet
    participant InscriptionDB as Inscription DB
    participant DefenseDB as Defense DB
    participant NotificationDB as Notification DB
    participant UserDB as User DB
    participant FileSystem as File System
    participant EmailService as Email Service
    participant Kafka as Kafka Broker

    Scheduler->>Job: 1st of month at 9:00 AM

    Note over Job: Step 1-4: Collect statistics
    Job->>StatsTasklet: Collect enrollment stats
    StatsTasklet->>InscriptionDB: Execute aggregation queries
    InscriptionDB-->>StatsTasklet: Return statistics
    StatsTasklet-->>Job: Store in execution context

    Job->>StatsTasklet: Collect defense stats
    StatsTasklet->>DefenseDB: Execute aggregation queries
    DefenseDB-->>StatsTasklet: Return statistics
    StatsTasklet-->>Job: Store in execution context

    Job->>StatsTasklet: Collect notification stats
    StatsTasklet->>NotificationDB: Execute aggregation queries
    NotificationDB-->>StatsTasklet: Return statistics
    StatsTasklet-->>Job: Store in execution context

    Job->>StatsTasklet: Collect user stats
    StatsTasklet->>UserDB: Execute aggregation queries
    UserDB-->>StatsTasklet: Return statistics
    StatsTasklet-->>Job: Store in execution context

    Note over Job: Step 5: Generate PDF
    Job->>PdfTasklet: Generate PDF report
    PdfTasklet->>PdfTasklet: Retrieve stats from context
    PdfTasklet->>PdfTasklet: Create PDF with iText
    PdfTasklet->>PdfTasklet: Add header, KPIs, charts, tables
    PdfTasklet->>FileSystem: Save as rapport_YYYY_MM.pdf
    FileSystem-->>PdfTasklet: File saved
    PdfTasklet-->>Job: Store PDF path in context

    Note over Job: Step 6: Send notifications
    Job->>NotifyTasklet: Send report notifications
    NotifyTasklet->>UserDB: Query all ROLE_ADMIN users
    UserDB-->>NotifyTasklet: Return admin users

    par Send to all admins
        loop For each admin
            NotifyTasklet->>EmailService: Send email with PDF attachment
            EmailService-->>NotifyTasklet: Email sent
        end
    and Publish to Kafka
        NotifyTasklet->>Kafka: Publish report event with PDF reference
        Kafka-->>NotifyTasklet: Event published
    end

    NotifyTasklet-->>Job: Notifications sent
    Job-->>Scheduler: Job complete
```

### Data Consistency Check Flow

```mermaid
sequenceDiagram
    participant Scheduler as Job Scheduler
    participant Job as Consistency Check Job
    participant Tasklet as Verification Tasklets
    participant InscriptionDB as Inscription DB
    participant DefenseDB as Defense DB
    participant UserDB as User DB
    participant FileSystem as File System
    participant NotificationDB as Notification DB
    participant Kafka as Kafka Broker
    participant ReportTasklet as Anomaly Report Tasklet

    Scheduler->>Job: Daily at 11:00 PM

    Note over Job: Step 1: User-Enrollment Consistency
    Job->>Tasklet: Verify user-enrollment consistency
    Tasklet->>InscriptionDB: Query all enrollments
    loop For each enrollment
        Tasklet->>UserDB: Check if user exists
        alt User not found
            Tasklet->>InscriptionDB: UPDATE status = 'SUSPENDU'
            Tasklet->>Tasklet: Log anomaly
            Tasklet->>Kafka: Publish admin notification
        end
    end
    Tasklet-->>Job: Anomalies detected: X

    Note over Job: Step 2: Enrollment-Defense Consistency
    Job->>Tasklet: Verify enrollment-defense consistency
    Tasklet->>DefenseDB: Query all defense requests
    loop For each defense
        Tasklet->>InscriptionDB: Check enrollment status
        alt Invalid enrollment
            Tasklet->>DefenseDB: Block defense request
            Tasklet->>Tasklet: Log anomaly
            Tasklet->>Kafka: Notify director and admin
        end
    end
    Tasklet-->>Job: Anomalies detected: Y

    Note over Job: Step 3: User Role Synchronization
    Job->>Tasklet: Verify and sync user roles
    Tasklet->>InscriptionDB: Query validated enrollments
    Tasklet->>UserDB: Check and add ROLE_DOCTORANT_ACTIF
    Tasklet->>DefenseDB: Query completed defenses
    Tasklet->>UserDB: Remove ROLE_DOCTORANT_ACTIF, add ROLE_DOCTEUR
    Tasklet-->>Job: Roles synchronized: Z

    Note over Job: Step 4: Orphaned Documents
    Job->>Tasklet: Check for orphaned documents
    Tasklet->>FileSystem: List all files in uploads
    loop For each file
        Tasklet->>InscriptionDB: Check for reference
        Tasklet->>DefenseDB: Check for reference
        alt No reference found
            Tasklet->>FileSystem: Move to uploads/orphelins
            Tasklet->>Tasklet: Log orphaned file
        end
    end
    Tasklet-->>Job: Orphans found: W

    Note over Job: Step 5: Retry Pending Notifications
    Job->>Tasklet: Retry stale notifications
    Tasklet->>NotificationDB: Query PENDING > 24 hours
    loop For each stale notification
        Tasklet->>Tasklet: Retry sending
        alt Retry successful
            Tasklet->>NotificationDB: UPDATE status = 'SENT'
        else Retry failed
            Tasklet->>NotificationDB: UPDATE status = 'FAILED'
            Tasklet->>Kafka: Notify technical admin
        end
    end
    Tasklet-->>Job: Retries: success=A, failed=B

    Note over Job: Step 6: Generate Anomaly Report (conditional)
    alt Anomalies detected
        Job->>ReportTasklet: Generate anomaly report
        ReportTasklet->>ReportTasklet: Create PDF with anomaly details
        ReportTasklet->>FileSystem: Save anomaly report
        ReportTasklet->>EmailService: Send to technical admin
        ReportTasklet-->>Job: Report sent
    end

    Job-->>Scheduler: Job complete
```

---

## Diagram Legend

### Participants

- **Scheduler**: Spring @Scheduled annotation triggering jobs via CRON
- **Job Launcher**: Spring Batch JobLauncher for executing jobs
- **Batch Job**: Spring Batch Job with configured steps
- **Item Reader**: Spring Batch ItemReader for reading data
- **Item Processor**: Spring Batch ItemProcessor for transforming data
- **Item Writer**: Spring Batch ItemWriter for writing data
- **Job Listener**: Spring Batch JobExecutionListener for metrics and logging
- **Database**: MariaDB databases (batchdb, userdb, inscriptiondb, defensedb, notificationdb)
- **Kafka Broker**: Apache Kafka for event publishing
- **File System**: Local file system for reports, archives, and uploads
- **Encryption Service**: AES-256 encryption for archive files
- **Audit Log Service**: Service for creating immutable audit trail entries
- **Email Service**: Spring Mail for sending email notifications
- **Notification Service**: External notification microservice consuming Kafka events

### Flow Types

- **Solid arrows (→)**: Synchronous calls
- **Dashed arrows (-->)**: Return values
- **Par blocks**: Parallel execution
- **Alt blocks**: Conditional execution
- **Loop blocks**: Iterative execution
- **Note blocks**: Explanatory comments

---

## Usage

These diagrams can be rendered using any Mermaid-compatible tool:

1. **GitHub/GitLab**: Automatically renders Mermaid diagrams in markdown
2. **Mermaid Live Editor**: https://mermaid.live/
3. **VS Code**: Install Mermaid Preview extension
4. **IntelliJ IDEA**: Built-in Mermaid support in markdown files

---

**Last Updated**: January 2025  
**Version**: 1.0.0
