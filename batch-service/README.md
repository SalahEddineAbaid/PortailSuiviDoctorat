# Batch Service

## Table of Contents

- [Overview](#overview)
- [Service Responsibilities](#service-responsibilities)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation and Configuration](#installation-and-configuration)
- [Scheduled Jobs](#scheduled-jobs)
- [API Documentation](#api-documentation)
- [Monitoring and Metrics](#monitoring-and-metrics)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Development](#development)
- [Testing](#testing)

## Overview

The batch-service is a critical microservice for the doctoral management portal that automates periodic tasks and batch processing operations. Built on Spring Boot 3.x and Spring Batch 5.x, it operates autonomously to maintain system health, ensure data integrity, and provide comprehensive reporting across the microservices architecture.

This service accesses multiple microservice databases, processes data in configurable chunks, and publishes events to Kafka for cross-service communication. It runs on port **8085** and registers with Eureka for service discovery.

## Service Responsibilities

The batch-service handles five core categories of automated tasks:

### 1. Token Cleanup

- **Purpose**: Maintain database performance and security
- **Actions**: Automatically removes expired JWT refresh tokens and password reset tokens
- **Schedule**: Daily at 2:00 AM
- **Impact**: Prevents token table bloat, improves query performance

### 2. Doctoral Duration Alerts

- **Purpose**: Ensure timely PhD completion and regulatory compliance
- **Actions**: Monitors doctoral program duration and sends proactive alerts at critical thresholds
- **Thresholds**:
  - 3-year warning (2 years 9 months)
  - 6-year critical alert (5 years 9 months)
  - Exceeded duration (6+ years) - automatically blocks enrollment
- **Schedule**: Every Monday at 8:00 AM
- **Impact**: Helps directors manage student progress, prevents regulatory violations

### 3. Monthly Statistical Reports

- **Purpose**: Provide comprehensive system insights for decision-making
- **Actions**: Generates PDF reports with statistics, charts, and KPIs
- **Content**: Enrollment stats, defense stats, notification stats, user stats, anomaly alerts
- **Schedule**: 1st of each month at 9:00 AM
- **Distribution**: Automatically emailed to all administrators

### 4. Data Archiving

- **Purpose**: Optimize database performance and storage efficiency
- **Actions**: Archives old enrollment and defense records with encryption
- **Features**:
  - Compresses documents to ZIP format
  - Encrypts archives with AES-256
  - Cleans up old logs (6 months for app logs, 3 months for notification logs)
  - Optimizes database tables after archiving
- **Schedule**: Quarterly (1st of Jan, Apr, Jul, Oct) at 3:00 AM
- **Impact**: Reduces database size, maintains optimal query performance

### 5. Data Consistency Verification

- **Purpose**: Maintain data integrity across microservices
- **Actions**: Verifies and auto-corrects data inconsistencies
- **Checks**:
  - User-enrollment consistency
  - Enrollment-defense consistency
  - User role synchronization
  - Orphaned document detection
  - Stale notification retry
- **Schedule**: Daily at 11:00 PM
- **Impact**: Prevents data corruption, ensures referential integrity

## Architecture

### High-Level Architecture Diagram

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
```

│ ┌──────▼──────────────────▼──────────────────▼───────┐ │
│ │ Job Configurations │ │
│ │ (TokenCleanup, DureeAlert, Report, Archive, etc.) │ │
│ └──────┬───────────────────────────────────────┬──────┘ │
│ │ │ │
│ ┌──────▼──────────┐ ┌──────────────┐ ┌──────▼──────────┐ │
│ │ ItemReaders │ │ Processors │ │ ItemWriters │ │
│ │ (JDBC/JPA/File) │ │ (Transform) │ │ (DB/Kafka/File) │ │
│ └─────────────────┘ └──────────────┘ └─────────────────┘ │
│ │
└───────────┬───────────────────────────────────┬─────────────────┘
│ │
┌───────▼────────┐ ┌───────▼────────┐
│ Multi-Database│ │ Kafka Broker │
│ Access │ │ (Events) │
├────────────────┤ └────────────────┘
│ • batchdb │
│ • userdb │
│ • inscriptiondb│
│ • defensedb │
│ • notificationdb│
└────────────────┘

```

### Component Overview

- **Job Scheduler**: Executes jobs based on CRON expressions
- **REST API Controllers**: Provides manual job execution and monitoring endpoints
- **Job Configurations**: Defines job structure, steps, and execution flow
- **ItemReaders**: Read data from databases or files
- **ItemProcessors**: Transform and validate data
- **ItemWriters**: Write processed data to databases, Kafka, or files
- **Listeners**: Track execution metrics, handle failures, send notifications

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.5.6 |
| Batch Processing | Spring Batch | 5.x |
| Language | Java | 17 |
| Database | MariaDB | 10.6+ |
| Connection Pool | HikariCP | Latest |
| Messaging | Apache Kafka | 3.x |
| Service Discovery | Eureka Client | Latest |
| PDF Generation | iText 7 | 7.2.5 |
```

| Charts | JFreeChart | 1.5.4 |
| Security | Spring Security + JWT | Latest |
| Rate Limiting | Resilience4j | 2.1.0 |
| Metrics | Micrometer + Prometheus | Latest |
| Testing | JUnit 5 + jqwik + Testcontainers | Latest |
| Database Migration | Flyway | Latest |

## Prerequisites

Before running the batch-service, ensure you have:

### Required Software

- **Java 17** or higher
- **Maven 3.6+** for building
- **MariaDB 10.6+** for all databases
- **Apache Kafka 3.x** for event messaging
- **Eureka Server** for service discovery

### Required Databases

The service requires access to five MariaDB databases:

1. **batchdb** - Primary database for batch metadata (read/write)
2. **userdb** - User service database (read/delete on token tables)
3. **inscriptiondb** - Enrollment service database (read-only)
4. **defensedb** - Defense service database (read-only)
5. **notificationdb** - Notification service database (read-only)

### Required Infrastructure

- **Kafka Topics**: `notifications` topic must exist
- **File System**: Write access to reports and archives directories
- **Network**: Access to Eureka server and all databases

## Installation and Configuration

### 1. Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd batch-service

# Build the project
mvn clean install

# Skip tests during build (optional)
mvn clean install -DskipTests
```

### 2. Database Configuration

Configure database connections in `application.properties` or via environment variables:

```properties
# Primary DataSource (batchdb)
spring.datasource.url=jdbc:mariadb://localhost:3306/batchdb
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

# User Service DataSource

datasource.userdb.jdbc-url=jdbc:mariadb://localhost:3306/userdb
datasource.userdb.username=${USER_DB_USERNAME}
datasource.userdb.password=${USER_DB_PASSWORD}

# Inscription Service DataSource

datasource.inscriptiondb.jdbc-url=jdbc:mariadb://localhost:3306/inscriptiondb
datasource.inscriptiondb.username=${INSCRIPTION_DB_USERNAME}
datasource.inscriptiondb.password=${INSCRIPTION_DB_PASSWORD}

# Defense Service DataSource

datasource.defensedb.jdbc-url=jdbc:mariadb://localhost:3306/defensedb
datasource.defensedb.username=${DEFENSE_DB_USERNAME}
datasource.defensedb.password=${DEFENSE_DB_PASSWORD}

# Notification Service DataSource

datasource.notificationdb.jdbc-url=jdbc:mariadb://localhost:3306/notificationdb
datasource.notificationdb.username=${NOTIFICATION_DB_USERNAME}
datasource.notificationdb.password=${NOTIFICATION_DB_PASSWORD}

````

### 3. Kafka Configuration

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
````

### 4. Eureka Configuration

```properties
# Eureka Client Configuration
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
eureka.instance.prefer-ip-address=true
spring.application.name=batch-service
server.port=8085
```

### 5. Batch Configuration

```properties
# Spring Batch Configuration
spring.batch.job.enabled=false
spring.batch.jdbc.initialize-schema=always

# Reports Directory
batch.reports.directory=${REPORTS_DIR:./reports}

# Archives Directory
batch.archives.directory=${ARCHIVES_DIR:./archives}

# Encryption Key (use secure key vault in production)
batch.encryption.key=${ENCRYPTION_KEY}
```

### 6. Security Configuration

```properties
# JWT Configuration
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

### 7. Environment Variables

Create a `.env` file or set environment variables:

```bash

```

export DB_USERNAME=batch_user
export DB_PASSWORD=secure_password
export USER_DB_USERNAME=user_service_reader
export USER_DB_PASSWORD=secure_password
export INSCRIPTION_DB_USERNAME=inscription_reader
export INSCRIPTION_DB_PASSWORD=secure_password
export DEFENSE_DB_USERNAME=defense_reader
export DEFENSE_DB_PASSWORD=secure_password
export NOTIFICATION_DB_USERNAME=notification_reader
export NOTIFICATION_DB_PASSWORD=secure_password
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export EUREKA_SERVER_URL=http://localhost:8761/eureka/
export JWT_SECRET=your_jwt_secret_key
export ENCRYPTION_KEY=your_aes_256_encryption_key
export REPORTS_DIR=/var/batch-service/reports
export ARCHIVES_DIR=/var/batch-service/archives

````

### 8. Run the Service

```bash
# Using Maven
mvn spring-boot:run

# Using Java
java -jar target/batch-service-0.0.1-SNAPSHOT.jar

# With custom profile
java -jar target/batch-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
````

The service will start on port **8085** and register with Eureka.

## Scheduled Jobs

### Job Schedule Overview

| Job Name                  | CRON Expression      | Schedule                | Description                        |
| ------------------------- | -------------------- | ----------------------- | ---------------------------------- |
| **tokenCleanupJob**       | `0 0 2 * * ?`        | Daily at 2:00 AM        | Remove expired tokens              |
| **dureeDoctoratAlertJob** | `0 0 8 ? * MON`      | Monday at 8:00 AM       | Check doctoral duration thresholds |
| **monthlyReportJob**      | `0 0 9 1 * ?`        | 1st of month at 9:00 AM | Generate monthly reports           |
| **archiveJob**            | `0 0 3 1 1,4,7,10 ?` | Quarterly at 3:00 AM    | Archive old records                |
| **dataConsistencyJob**    | `0 0 23 * * ?`       | Daily at 11:00 PM       | Verify data consistency            |

### Job Details

#### 1. Token Cleanup Job

**Job Name**: `tokenCleanupJob`

**Purpose**: Remove expired authentication and password reset tokens from the user database.

**Steps**:

1. Clean up expired refresh tokens (chunk size: 100)
2. Clean up expired password reset tokens (chunk size: 100)

**Processing**:

- Identifies tokens where `expiry_date < NOW()`
- Deletes in batches of 100 per transaction
- Logs deletion counts
- Records execution metrics

**Failure Handling**:

- Publishes Kafka event to notifications topic
- Sends email alert to technical team
- Logs detailed error information

#### 2. Doctoral Duration Alert Job

**Job Name**: `dureeDoctoratAlertJob`

**Purpose**: Monitor doctoral program duration and send proactive alerts at critical thresholds.

**Steps**:

1. Check 3-year threshold (chunk size: 50)
2. Check 6-year threshold (chunk size: 50)
3. Check exceeded 6-year threshold (chunk size: 50)

**Alert Types**:

- **3-Year Warning** (2y 9m): Normal priority, suggests requesting extension
- **6-Year Critical** (5y 9m): High priority, urgent action required
- **Exceeded Duration** (6y+): Urgent priority, automatically blocks enrollment

**Processing**:

- Calculates exact duration from first enrollment date
- Checks if alert already sent (prevents duplicates)
- Publishes Kafka events to notifications topic
- Updates enrollment status to BLOQUÉ for exceeded cases

**Idempotence**: Uses `alerte_duree_envoyee` table to prevent duplicate alerts.

#### 3. Monthly Report Generation Job

**Job Name**: `monthlyReportJob`

**Purpose**: Generate comprehensive monthly statistical reports with PDF output.

**Steps**:

1. Collect enrollment statistics
2. Collect defense statistics
3. Collect notification statistics
4. Collect user statistics
5. Generate PDF with charts and tables
6. Send report to all administrators

**Report Content**:

- **Header**: Logo, title, report period
- **KPI Dashboard**: Key metrics at a glance
- **Charts**: Bar charts, pie charts for visual analysis
- **Detailed Tables**: Complete statistics by category
- **Alerts Section**: Anomalies and warnings
- **Footer**: Generation timestamp

**Distribution**:

- Saved to reports directory as `rapport_YYYY_MM.pdf`
- Emailed to all users with ROLE_ADMIN
- Published to Kafka with PDF reference

#### 4. Archive Job

**Job Name**: `archiveJob`

**Purpose**: Archive historical enrollment and defense records to optimize database performance.

**Steps**:

1. Archive old enrollments (chunk size: 20)
2. Archive old defenses (chunk size: 20)
3. Clean up old logs
4. Optimize database tables

**Archive Criteria**:

- **Enrollments**: Status VALIDÉ or REJETÉ, older than 1 year
- **Defenses**: Status COMPLETED, signed PV available, older than 1 year

**Archive Process**:

1. Copy record to archive table
2. Locate and compress associated documents to ZIP
3. Encrypt ZIP file with AES-256
4. Write encrypted file to archives directory
5. Create audit trail entry
6. Delete original documents
7. Mark record as archived

**Log Cleanup**:

- Application logs: Delete older than 6 months
- Notification logs: Delete older than 3 months (successful only)
- Error logs: Retain indefinitely

**Database Optimization**:

- Execute OPTIMIZE TABLE on affected tables
- Regenerate table statistics
- Verify index integrity

#### 5. Data Consistency Verification Job

**Job Name**: `dataConsistencyJob`

**Purpose**: Verify and auto-correct data inconsistencies across microservices.

**Steps**:

1. Verify user-enrollment consistency
2. Verify enrollment-defense consistency
3. Verify and synchronize user roles
4. Check for orphaned documents
5. Retry pending notifications
6. Generate anomaly report (if anomalies detected)

**Consistency Checks**:

**User-Enrollment Consistency**:

- Verifies each enrollment has a valid user
- Action: Mark enrollment as SUSPENDU, notify admin

**Enrollment-Defense Consistency**:

- Verifies each defense has a valid VALIDÉ enrollment
- Action: Block defense, notify director and admin

**User Role Synchronization**:

- Adds ROLE_DOCTORANT_ACTIF for validated enrollments
- Transitions to ROLE_DOCTEUR for completed defenses

**Orphaned Document Detection**:

- Identifies files without database references
- Action: Move to `uploads/orphelins` directory

**Stale Notification Retry**:

- Retries notifications in PENDING status > 24 hours
- Action: Mark as FAILED if retry fails, notify technical admin

**Anomaly Reporting**:

- Generates PDF report with anomaly details
- Includes type, count, and corrective actions
- Emailed to technical admin

## API Documentation

### Base URL

```
http://localhost:8085/api/batch
```

### Authentication

All endpoints require:

- **JWT Token**: Valid JWT token in Authorization header
- **Role**: ROLE_ADMIN

```bash
Authorization: Bearer <jwt_token>
```

### Endpoints

#### 1. Trigger Job Manually

**POST** `/jobs/{jobName}/run`

Manually trigger a batch job execution. Rate-limited to prevent abuse.

**Path Parameters**:

- `jobName` (string, required): Name of the job to trigger
  - Valid values: `tokenCleanupJob`, `dureeDoctoratAlertJob`, `monthlyReportJob`, `archiveJob`, `dataConsistencyJob`

**Response** (200 OK):

```json
{
  "executionId": 12345,
  "jobName": "tokenCleanupJob",
  "status": "STARTED",
  "startTime": "2024-01-15T02:00:00",
  "message": "Job started successfully"
}
```

**Error Responses**:

- `400 Bad Request`: Invalid job name
- `403 Forbidden`: User does not have ROLE_ADMIN
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Job execution failed

**Example**:

```bash
curl -X POST http://localhost:8085/api/batch/jobs/tokenCleanupJob/run \
  -H "Authorization: Bearer <jwt_token>"
```

#### 2. List All Jobs

**GET** `/jobs`

Get a list of all available batch jobs with metadata.

**Response** (200 OK):

```json
[
  {
    "name": "tokenCleanupJob",
    "description": "Remove expired authentication and password reset tokens",
    "cronExpression": "0 0 2 * * ?",
    "lastExecution": "2024-01-15T02:00:00",
    "lastStatus": "COMPLETED",
    "isRunning": false
  },
  {
    "name": "dureeDoctoratAlertJob",
    "description": "Monitor doctoral duration and send alerts",
    "cronExpression": "0 0 8 ? * MON",
    "lastExecution": "2024-01-15T08:00:00",
    "lastStatus": "COMPLETED",
    "isRunning": false
  }
]
```

**Example**:

```bash
curl -X GET http://localhost:8085/api/batch/jobs \
  -H "Authorization: Bearer <jwt_token>"
```

#### 3. Get Job Execution History

**GET** `/jobs/{jobName}/executions`

Get paginated execution history for a specific job.

**Path Parameters**:

- `jobName` (string, required): Name of the job

**Query Parameters**:

- `page` (integer, optional, default: 0): Page number
- `size` (integer, optional, default: 20): Page size

**Response** (200 OK):

```json
[
  {
    "executionId": 12345,
    "jobName": "tokenCleanupJob",
    "status": "COMPLETED",
    "startTime": "2024-01-15T02:00:00",
    "endTime": "2024-01-15T02:05:23",
    "duration": "5m 23s",
    "itemsProcessed": 1523,
    "itemsFailed": 0,
    "exitMessage": "Job completed successfully"
  }
]
```

**Example**:

```bash
curl -X GET "http://localhost:8085/api/batch/jobs/tokenCleanupJob/executions?page=0&size=10" \
  -H "Authorization: Bearer <jwt_token>"
```

#### 4. Get Execution Details

**GET** `/jobs/executions/{executionId}`

Get detailed information for a specific job execution.

**Path Parameters**:

- `executionId` (long, required): Execution ID

**Response** (200 OK):

```json
{
  "executionId": 12345,
  "jobName": "tokenCleanupJob",
  "status": "COMPLETED",
  "startTime": "2024-01-15T02:00:00",
  "endTime": "2024-01-15T02:05:23",
  "duration": "5m 23s",
  "itemsProcessed": 1523,
  "itemsFailed": 0,
  "exitMessage": "Job completed successfully. Deleted 1200 refresh tokens and 323 password reset tokens."
}
```

**Error Responses**:

- `404 Not Found`: Execution ID not found

**Example**:

```bash
curl -X GET http://localhost:8085/api/batch/jobs/executions/12345 \
  -H "Authorization: Bearer <jwt_token>"
```

#### 5. Stop Job Execution

**POST** `/jobs/executions/{executionId}/stop`

Stop a running job execution.

**Path Parameters**:

- `executionId` (long, required): Execution ID to stop

**Response** (200 OK):

```json
"Job execution 12345 stopped successfully"
```

**Error Responses**:

- `400 Bad Request`: Execution is not running or cannot be stopped
- `404 Not Found`: Execution ID not found

**Example**:

```bash
curl -X POST http://localhost:8085/api/batch/jobs/executions/12345/stop \
  -H "Authorization: Bearer <jwt_token>"
```

#### 6. Get Global Statistics

**GET** `/jobs/stats`

Get global statistics across all batch jobs.

**Response** (200 OK):

```json
{
  "totalExecutions": 1523,
  "successfulExecutions": 1498,
  "failedExecutions": 25,
  "successRate": 98.36,
  "averageDuration": "4m 32s",
  "lastFailure": {
    "jobName": "archiveJob",
    "executionId": 12340,
    "failureTime": "2024-01-14T03:00:00",
    "errorMessage": "Disk space exhausted"
  }
}
```

**Example**:

```bash
curl -X GET http://localhost:8085/api/batch/jobs/stats \
  -H "Authorization: Bearer <jwt_token>"
```

#### 7. List Generated Reports

**GET** `/reports`

Get a list of all generated monthly reports.

**Response** (200 OK):

```json
[
  {
    "fileName": "rapport_2024_01.pdf",
    "generatedDate": "2024-01-01T09:00:00",
    "fileSize": 2458624,
    "downloadUrl": "/api/batch/reports/rapport_2024_01.pdf/download"
  },
  {
    "fileName": "rapport_2023_12.pdf",
    "generatedDate": "2023-12-01T09:00:00",
    "fileSize": 2312456,
    "downloadUrl": "/api/batch/reports/rapport_2023_12.pdf/download"
  }
]
```

**Example**:

```bash
curl -X GET http://localhost:8085/api/batch/reports \
  -H "Authorization: Bearer <jwt_token>"
```

#### 8. Download Report

**GET** `/reports/{fileName}/download`

Download a specific monthly report PDF file.

**Path Parameters**:

- `fileName` (string, required): Name of the report file (e.g., `rapport_2024_01.pdf`)

**Response** (200 OK):

- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="rapport_2024_01.pdf"`
- Body: PDF file stream

**Error Responses**:

- `400 Bad Request`: Invalid filename (security check)
- `404 Not Found`: Report file not found

**Example**:

```bash
curl -X GET http://localhost:8085/api/batch/reports/rapport_2024_01.pdf/download \
  -H "Authorization: Bearer <jwt_token>" \
  -o rapport_2024_01.pdf
```

## Monitoring and Metrics

### Spring Boot Actuator Endpoints

The service exposes several Actuator endpoints for monitoring:

#### Health Check

**GET** `/actuator/health`

Returns the health status of the service and all DataSources.

```json
{
  "status": "UP",
  "components": {
    "batchDataSource": {
      "status": "UP"
    },
    "userDataSource": {
      "status": "UP"
    },
    "inscriptionDataSource": {
      "status": "UP"
    },
    "defenseDataSource": {
      "status": "UP"
    },
    "notificationDataSource": {
      "status": "UP"
    },
    "kafka": {
      "status": "UP"
    }
  }
}
```

#### Metrics

**GET** `/actuator/metrics`

Lists all available metrics.

**GET** `/actuator/metrics/{metricName}`

Get specific metric details.

**Available Batch Metrics**:

- `batch.job.executions.total` - Total job executions
- `batch.job.executions.success` - Successful executions
- `batch.job.executions.failure` - Failed executions
- `batch.job.duration` - Job execution duration
- `batch.job.items.processed` - Items processed per job
- `batch.active.jobs` - Currently active jobs
- `batch.scheduled.jobs` - Total scheduled jobs
- `batch.success.rate` - Overall success rate

#### Prometheus Metrics

**GET** `/actuator/prometheus`

Returns metrics in Prometheus format for scraping.

### Monitoring Best Practices

1. **Set up Prometheus scraping**:

   ```yaml
   scrape_configs:
     - job_name: "batch-service"
       metrics_path: "/actuator/prometheus"
       static_configs:
         - targets: ["localhost:8085"]
   ```

2. **Create Grafana dashboards** for:

   - Job execution success rate
   - Job execution duration trends
   - Items processed per job
   - Failure alerts and notifications

3. **Configure alerts** for:
   - Job failures (immediate alert)
   - Success rate drops below 95%
   - Execution duration exceeds threshold
   - Database connection failures

### Logging

The service uses SLF4J with Logback for logging.

**Log Levels**:

- `ERROR`: Job failures, database errors, critical issues
- `WARN`: Retries, rate limiting, data inconsistencies
- `INFO`: Job start/completion, major operations
- `DEBUG`: Detailed execution flow, step-by-step processing

**Log Configuration** (`application.properties`):

```properties
logging.level.ma.emsi.batchservice=INFO
logging.level.org.springframework.batch=INFO
logging.file.name=logs/batch-service.log
logging.file.max-size=10MB
logging.file.max-history=30
```

## Security

### Authentication and Authorization

- **JWT Authentication**: All API endpoints require valid JWT token
- **Role-Based Access**: Only users with `ROLE_ADMIN` can access batch endpoints
- **Token Validation**: JWT tokens validated via API Gateway

### Data Protection

- **Archive Encryption**: All archived files encrypted with AES-256
- **Key Management**: Encryption keys stored in secure key vault
- **Database Credentials**: Externalized to environment variables
- **Audit Logging**: All operations logged with user and timestamp

### Rate Limiting

Job trigger endpoints are rate-limited using Resilience4j:

```properties
resilience4j.ratelimiter.instances.jobTrigger.limit-for-period=10
resilience4j.ratelimiter.instances.jobTrigger.limit-refresh-period=60s
resilience4j.ratelimiter.instances.jobTrigger.timeout-duration=0s
```

**Limits**:

- 10 job triggers per minute per user
- Returns `429 Too Many Requests` when exceeded

### Security Best Practices

1. **Use dedicated service accounts** with minimum required permissions
2. **Rotate encryption keys** regularly
3. **Enable audit logging** for all operations
4. **Monitor failed authentication attempts**
5. **Use HTTPS** in production
6. **Implement network segmentation** for database access

## Troubleshooting

### Common Issues and Solutions

#### 1. Job Fails to Start

**Symptoms**: Job execution returns error immediately

**Possible Causes**:

- Database connection failure
- Invalid job name
- Missing configuration

**Solutions**:

```bash
# Check database connectivity
curl http://localhost:8085/actuator/health

# Check logs for detailed error
tail -f logs/batch-service.log

# Verify job name
curl http://localhost:8085/api/batch/jobs -H "Authorization: Bearer <token>"
```

#### 2. Job Execution Hangs

**Symptoms**: Job starts but never completes

**Possible Causes**:

- Database deadlock
- Large dataset causing timeout
- Network connectivity issues

**Solutions**:

```bash
# Check active jobs
curl http://localhost:8085/api/batch/jobs/stats -H "Authorization: Bearer <token>"

# Stop hanging execution
curl -X POST http://localhost:8085/api/batch/jobs/executions/{id}/stop \
  -H "Authorization: Bearer <token>"

# Check database locks
# Run in MariaDB: SHOW PROCESSLIST;
```

#### 3. Kafka Connection Failure

**Symptoms**: Jobs complete but notifications not sent

**Possible Causes**:

- Kafka broker unavailable
- Topic doesn't exist
- Network connectivity issues

**Solutions**:

```bash
# Check Kafka broker status
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Verify notifications topic exists
kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic notifications

# Check service logs
grep -i kafka logs/batch-service.log
```

#### 4. Archive Job Fails with Disk Space Error

**Symptoms**: Archive job fails with "Disk space exhausted" error

**Solutions**:

```bash
# Check disk space
df -h

# Clean up old archives manually
cd /var/batch-service/archives
find . -name "*.zip" -mtime +365 -delete

# Increase disk space or configure cleanup policy
```

#### 5. PDF Generation Fails

**Symptoms**: Monthly report job fails during PDF generation

**Possible Causes**:

- Missing fonts
- Insufficient memory
- Chart generation error

**Solutions**:

```bash
# Increase JVM heap size
java -Xmx2g -jar batch-service.jar

# Check for font issues in logs
grep -i "font" logs/batch-service.log

# Verify iText dependencies
mvn dependency:tree | grep itext
```

#### 6. Rate Limit Exceeded

**Symptoms**: API returns 429 Too Many Requests

**Solutions**:

```bash
# Wait for rate limit window to reset (60 seconds)
sleep 60

# Or adjust rate limit configuration in application.properties
resilience4j.ratelimiter.instances.jobTrigger.limit-for-period=20
```

### Debug Mode

Enable debug logging for troubleshooting:

```properties
logging.level.ma.emsi.batchservice=DEBUG
logging.level.org.springframework.batch=DEBUG
logging.level.org.springframework.jdbc=DEBUG
```

### Health Check Commands

```bash
# Check service health
curl http://localhost:8085/actuator/health

# Check all DataSources
curl http://localhost:8085/actuator/health | jq '.components'

# Check Eureka registration
curl http://localhost:8761/eureka/apps/BATCH-SERVICE

# Check metrics
curl http://localhost:8085/actuator/metrics/batch.job.executions.total
```

## Development

### Project Structure

```
batch-service/
├── src/main/java/ma/emsi/batchservice/
│   ├── config/                    # Configuration classes
│   │   ├── MultiDataSourceConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   ├── TokenCleanupJobConfig.java
│   │   ├── DureeDoctoratAlertJobConfig.java
│   │   ├── MonthlyReportJobConfig.java
│   │   ├── ArchiveJobConfig.java
│   │   ├── DataConsistencyJobConfig.java
│   │   ├── JobSchedulerConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── RateLimitingConfig.java
│   │   └── ActuatorConfig.java
│   ├── controller/                # REST controllers
│   │   ├── BatchJobController.java
│   │   └── BatchReportController.java
│   ├── service/                   # Service layer
│   │   ├── BatchJobService.java
│   │   ├── JobExecutionHistoryService.java
│   │   ├── EncryptionService.java
│   │   ├── AuditLoggingService.java
│   │   ├── FailureNotificationService.java
│   │   └── GracefulDegradationService.java
│   ├── repository/                # Data access layer
│   │   ├── JobExecutionHistoryRepository.java
│   │   ├── AlerteDureeEnvoyeeRepository.java
│   │   ├── InscriptionArchiveRepository.java
│   │   └── DefenseArchiveRepository.java
│   ├── dto/                       # Data transfer objects
│   │   ├── JobExecutionDTO.java
│   │   ├── JobInfoDTO.java
│   │   ├── GlobalStatsDTO.java
│   │   └── event/
│   │       └── AlertEventDTO.java
│   ├── entity/                    # JPA entities
│   │   ├── JobExecutionHistory.java
│   │   ├── AlerteDureeEnvoyee.java
│   │   ├── InscriptionArchive.java
│   │   └── DefenseArchive.java
│   ├── reader/                    # Spring Batch ItemReaders
│   │   ├── ExpiredRefreshTokenReaderConfig.java
│   │   ├── ExpiredPasswordResetTokenReaderConfig.java
│   │   ├── EnrollmentReaderConfig.java
│   │   ├── EnrollmentArchiveCandidateReaderConfig.java
│   │   └── DefenseArchiveCandidateReaderConfig.java
│   ├── processor/                 # Spring Batch ItemProcessors
│   │   ├── DureeAlertProcessor.java
│   │   └── ArchiveProcessor.java
│   ├── writer/                    # Spring Batch ItemWriters
│   │   ├── RefreshTokenDeletionWriterConfig.java
│   │   ├── PasswordResetTokenDeletionWriterConfig.java
│   │   ├── KafkaAlertWriter.java
│   │   └── ArchiveWriter.java
│   ├── tasklet/                   # Spring Batch Tasklets
│   │   ├── CollectEnrollmentStatsTasklet.java
│   │   ├── CollectDefenseStatsTasklet.java
│   │   ├── CollectNotificationStatsTasklet.java
│   │   ├── CollectUserStatsTasklet.java
│   │   ├── GeneratePdfTasklet.java
│   │   ├── SendReportNotificationTasklet.java
│   │   ├── CleanupLogsTasklet.java
│   │   ├── DatabaseOptimizationTasklet.java
│   │   ├── VerifyUserEnrollmentConsistencyTasklet.java
│   │   ├── VerifyEnrollmentDefenseConsistencyTasklet.java
│   │   ├── VerifyUserRolesTasklet.java
│   │   ├── CheckOrphanedDocumentsTasklet.java
│   │   ├── RetryPendingNotificationsTasklet.java
│   │   └── GenerateAnomalyReportTasklet.java
│   └── listener/                  # Spring Batch Listeners
│       ├── TokenCleanupJobListener.java
│       ├── DureeAlertJobListener.java
│       ├── ReportJobListener.java
│       ├── ArchiveJobListener.java
│       └── ConsistencyJobListener.java
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/              # Flyway migrations
│       ├── V1__create_batch_service_tables.sql
│       ├── V2__create_archive_audit_trail.sql
│       └── V3__create_audit_log_table.sql
└── src/test/                      # Test classes
    ├── java/
    └── resources/
        └── application-test.properties
```

### Adding a New Batch Job

To add a new batch job to the service:

1. **Create Job Configuration Class**:

```java
@Configuration
public class MyNewJobConfig {

    @Bean
    public Job myNewJob(JobRepository jobRepository, Step myStep) {
        return new JobBuilder("myNewJob", jobRepository)
                .start(myStep)
                .listener(myJobListener())
                .build();
    }

    @Bean
    public Step myStep(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("myStep", jobRepository)
                .<InputType, OutputType>chunk(100, transactionManager)
                .reader(myReader())
                .processor(myProcessor())
                .writer(myWriter())
                .build();
    }
}
```

2. **Add to JobSchedulerConfig**:

```java
@Scheduled(cron = "0 0 12 * * ?")
public void runMyNewJob() {
    try {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(myNewJob, params);
    } catch (Exception e) {
        logger.error("Failed to run myNewJob", e);
    }
}
```

3. **Update BatchJobService** to include the new job in listings

4. **Add tests** for the new job

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=TokenCleanupJobConfigTest

# Run property-based tests only
mvn test -Dtest="**/*PropertyTest"

# Run integration tests only
mvn test -Dtest="**/*IntegrationTest"

# Run with coverage
mvn test jacoco:report
```

### Test Categories

#### Unit Tests

- Test individual components in isolation
- Mock external dependencies
- Fast execution

#### Property-Based Tests (jqwik)

- Test universal properties across many inputs
- Validate correctness properties from design document
- Minimum 100 iterations per property

#### Integration Tests (Testcontainers)

- Test complete job workflows
- Use real MariaDB and Kafka containers
- Verify end-to-end functionality

### Test Configuration

Test properties in `src/test/resources/application-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.batch.job.enabled=false
spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}
```

### Writing Tests

Example unit test:

```java
@SpringBootTest
class TokenCleanupJobConfigTest {

    @Autowired
    private Job tokenCleanupJob;

    @Test
    void testJobConfiguration() {
        assertNotNull(tokenCleanupJob);
        assertEquals("tokenCleanupJob", tokenCleanupJob.getName());
    }
}
```

Example property-based test:

```java
@Property
void expiredTokenIdentification(@ForAll @IntRange(min = 1, max = 1000) int expiredCount) {
    // Test that expired token identification is accurate
    // Validates: Requirements 1.2
}
```

## Performance Considerations

### Chunk Size Tuning

Different jobs use different chunk sizes based on data characteristics:

- **Token Cleanup**: 100 (small records, high volume)
- **Duration Alerts**: 50 (medium complexity, moderate volume)
- **Archive**: 20 (large records with file operations)

Adjust chunk sizes based on:

- Record size
- Processing complexity
- Transaction timeout
- Memory constraints

### Connection Pool Configuration

```properties
# HikariCP settings for each DataSource
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Memory Configuration

```bash
# Recommended JVM settings for production
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar batch-service.jar
```

### Database Optimization

- Create indexes on frequently queried columns
- Regularly run OPTIMIZE TABLE (done automatically by archive job)
- Monitor slow query log
- Use connection pooling

## Deployment

### Docker Deployment

Create `Dockerfile`:

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/batch-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
docker build -t batch-service:latest .
docker run -p 8085:8085 \
  -e DB_USERNAME=batch_user \
  -e DB_PASSWORD=secure_password \
  batch-service:latest
```

### Docker Compose

```yaml
version: "3.8"
services:
  batch-service:
    image: batch-service:latest
    ports:
      - "8085:8085"
    environment:
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - EUREKA_SERVER_URL=http://eureka:8761/eureka/
    volumes:
      - ./reports:/var/batch-service/reports
      - ./archives:/var/batch-service/archives
    depends_on:
      - mariadb
      - kafka
      - eureka
```

### Production Checklist

- [ ] Configure all database connections with production credentials
- [ ] Set up secure key vault for encryption keys
- [ ] Configure HTTPS/TLS for all endpoints
- [ ] Set up Prometheus monitoring and Grafana dashboards
- [ ] Configure log aggregation (ELK stack or similar)
- [ ] Set up automated backups for batchdb
- [ ] Configure email server for notifications
- [ ] Test all scheduled jobs in staging environment
- [ ] Set up alerting for job failures
- [ ] Document runbook for common issues
- [ ] Configure rate limiting appropriately
- [ ] Set up network segmentation for database access
- [ ] Enable audit logging
- [ ] Configure log rotation
- [ ] Set up health check monitoring

## Contributing

### Code Style

- Follow Java naming conventions
- Use Lombok for boilerplate reduction
- Add JavaDoc for public methods
- Keep methods focused and small
- Write tests for new features

### Commit Messages

```
feat: Add new consistency check for user roles
fix: Resolve deadlock in archive job
docs: Update API documentation
test: Add property tests for duration alerts
refactor: Simplify PDF generation logic
```

### Pull Request Process

1. Create feature branch from `main`
2. Implement changes with tests
3. Ensure all tests pass
4. Update documentation
5. Submit pull request with description
6. Address review comments
7. Merge after approval

## License

Copyright © 2024 EMSI. All rights reserved.

## Support

For issues, questions, or contributions:

- **Email**: support@emsi.ma
- **Documentation**: See `.kiro/specs/batch-service/` for detailed specifications
- **Issue Tracker**: [Project Issue Tracker]

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: January 2025  
**Maintained by**: EMSI Development Team
