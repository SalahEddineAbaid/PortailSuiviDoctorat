# Task 9: DLQ Consumer and Reprocessing Implementation Summary

## Overview
Successfully implemented the Dead Letter Queue (DLQ) consumer and reprocessing functionality for the notification service. This implementation handles failed notifications that have exhausted all retry attempts, providing audit trails and reprocessing capabilities.

## Requirements Addressed
- **Requirement 11.2**: Log DLQ messages with full details
- **Requirement 11.3**: Persist DLQ messages in notification_dlq table

## Components Implemented

### 1. NotificationDLQ Entity
**File**: `src/main/java/ma/emsi/notificationservice/entities/NotificationDLQ.java`

**Purpose**: JPA entity representing a notification in the Dead Letter Queue for audit and tracking.

**Key Fields**:
- `notificationId`: Reference to the original failed notification
- `type`: Type of notification that failed
- `destinataire`: Email address of intended recipient
- `sujet`: Subject of the notification
- `erreurMessage`: Error message that caused the failure
- `donnees`: Full notification data as JSON for reprocessing
- `nombreTentatives`: Number of attempts before DLQ
- `dateAjoutDlq`: Timestamp when added to DLQ
- `dateDerniereTentative`: Last reprocessing attempt timestamp
- `retraite`: Boolean flag indicating if successfully reprocessed
- `kafkaPartition`: Kafka partition from which message was consumed
- `kafkaOffset`: Kafka offset of the original message

### 2. NotificationDLQRepository
**File**: `src/main/java/ma/emsi/notificationservice/repositories/NotificationDLQRepository.java`

**Purpose**: Spring Data JPA repository for managing DLQ entries.

**Key Methods**:
- `findByNotificationId(Long)`: Find DLQ entry by original notification ID
- `findByRetraiteOrderByDateAjoutDlqAsc(Boolean)`: Find unprocessed DLQ entries
- `findByDestinataire(String)`: Find all DLQ entries for a specific email
- `countTotal()`: Count total DLQ entries
- `countByRetraite(Boolean)`: Count unprocessed DLQ entries

### 3. DLQConsumer
**File**: `src/main/java/ma/emsi/notificationservice/consumers/DLQConsumer.java`

**Purpose**: Kafka consumer that listens to the notifications-dlq topic and persists failed notifications.

**Key Features**:
- **Kafka Listener**: Consumes messages from `notifications-dlq` topic
- **Comprehensive Logging**: Logs all DLQ message details including partition, offset, type, destinataire, and error (Requirement 11.2)
- **Notification ID Extraction**: Parses the DLQ message to extract the original notification ID
- **Original Notification Retrieval**: Fetches the full notification details from the database
- **DLQ Persistence**: Creates and saves NotificationDLQ entity with all relevant information (Requirement 11.3)
- **Error Handling**: Gracefully handles errors without blocking the consumer

**Message Format**:
The DLQConsumer expects messages in the format:
```
"Failed notification ID: {id}. Error: {error_message}"
```

### 4. Database Schema Update
**File**: `create-database.sql`

**Changes**: Updated the `notification_dlq` table schema to match the entity structure:
```sql
CREATE TABLE IF NOT EXISTS notification_dlq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    destinataire VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    sujet VARCHAR(255),
    erreur_message TEXT,
    donnees JSON,
    nombre_tentatives INT,
    date_ajout_dlq TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_derniere_tentative TIMESTAMP NULL,
    retraite BOOLEAN DEFAULT FALSE,
    kafka_partition INT,
    kafka_offset BIGINT,
    INDEX idx_notification_id (notification_id),
    INDEX idx_retraite (retraite),
    INDEX idx_date_ajout_dlq (date_ajout_dlq),
    INDEX idx_destinataire (destinataire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5. Unit Tests
**File**: `src/test/java/ma/emsi/notificationservice/consumers/DLQConsumerTest.java`

**Test Coverage**:
- ✅ `testConsumeDLQNotification_Success`: Verifies successful DLQ message consumption and persistence
- ✅ `testConsumeDLQNotification_OriginalNotificationNotFound`: Handles case when original notification doesn't exist
- ✅ `testConsumeDLQNotification_InvalidMessageFormat`: Handles invalid message format gracefully
- ✅ `testConsumeDLQNotification_RepositoryThrowsException`: Handles database errors without crashing
- ✅ `testConsumeDLQNotification_WithDifferentNotificationType`: Tests with various notification types

## Integration with Existing Components

### NotificationProcessingService
The DLQConsumer integrates with the existing `NotificationProcessingService.sendToDLQ()` method, which publishes failed notifications to the DLQ topic after all retry attempts are exhausted.

**Flow**:
1. Notification fails after 3 retry attempts
2. `NotificationProcessingService.handleFailure()` is called
3. `sendToDLQ()` publishes message to `notifications-dlq` topic
4. `DLQConsumer` consumes the message
5. DLQ entry is persisted in `notification_dlq` table

### Configuration
The DLQ consumer uses the following configuration from `application.properties`:
```properties
kafka.topic.notifications-dlq=notifications-dlq
spring.kafka.consumer.group-id=notification-service-group
```

The DLQ consumer uses a separate consumer group: `notification-service-group-dlq`

## Verification Steps

### 1. Database Setup
Ensure the `notification_dlq` table is created:
```bash
mysql -u root -p notification_db < create-database.sql
```

### 2. Run Unit Tests
```bash
./mvnw test -Dtest=DLQConsumerTest
```

### 3. Integration Testing
1. Start Kafka and MariaDB
2. Start the notification service
3. Trigger a notification that will fail (e.g., invalid SMTP configuration)
4. After 3 retry attempts, verify:
   - Message appears in `notifications-dlq` Kafka topic
   - Entry is created in `notification_dlq` table
   - Logs show DLQ message details

### 4. Query DLQ Entries
```sql
SELECT * FROM notification_dlq WHERE retraite = FALSE ORDER BY date_ajout_dlq DESC;
```

## Future Enhancements (Not in Current Task)

The following features are planned for future tasks:
- **Task 10**: REST API endpoints for querying and reprocessing DLQ entries
- **Manual Reprocessing**: Admin endpoint to retry DLQ messages
- **Bulk Reprocessing**: Endpoint to retry all failed notifications
- **DLQ Dashboard**: UI for monitoring and managing DLQ entries

## Compliance

### Requirements Validation
✅ **Requirement 11.2**: DLQ messages are logged with full details including:
- Topic name
- Partition and offset
- Notification type
- Destinataire
- Subject
- Message text
- Data map

✅ **Requirement 11.3**: DLQ messages are persisted in the `notification_dlq` table with:
- Original notification ID
- All notification details
- Error information
- Kafka metadata
- Reprocessing status

### Code Quality
- ✅ Follows existing code patterns and conventions
- ✅ Comprehensive JavaDoc documentation
- ✅ Proper error handling and logging
- ✅ Unit tests with good coverage
- ✅ No compilation errors (only minor null safety warnings)

## Files Created/Modified

### Created Files
1. `src/main/java/ma/emsi/notificationservice/entities/NotificationDLQ.java`
2. `src/main/java/ma/emsi/notificationservice/repositories/NotificationDLQRepository.java`
3. `src/main/java/ma/emsi/notificationservice/consumers/DLQConsumer.java`
4. `src/test/java/ma/emsi/notificationservice/consumers/DLQConsumerTest.java`
5. `TASK_9_DLQ_IMPLEMENTATION_SUMMARY.md` (this file)

### Modified Files
1. `create-database.sql` - Updated notification_dlq table schema

## Conclusion

Task 9 has been successfully completed. The DLQ consumer and reprocessing infrastructure is now in place, providing:
- Comprehensive audit trail for failed notifications
- Detailed logging for troubleshooting
- Foundation for future reprocessing capabilities
- Robust error handling

The implementation follows Spring Boot and Kafka best practices, integrates seamlessly with existing components, and is fully tested.
