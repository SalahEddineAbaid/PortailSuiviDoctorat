/**
 * Event DTOs for Kafka message publishing.
 * 
 * This package contains Data Transfer Objects (DTOs) for events published
 * to Kafka topics by the batch-service. These events enable asynchronous
 * communication with other microservices, particularly the
 * notification-service.
 * 
 * Event Types:
 * - AlertEventDTO: Doctoral duration alerts (3-year, 6-year, exceeded
 * thresholds)
 * - JobFailureEventDTO: Batch job failure notifications for admin alerting
 * 
 * All events are serialized to JSON using Spring Kafka's JsonSerializer
 * and published to the "notifications" topic.
 * 
 * Requirements: 1.6, 2.3, 2.5, 2.6, 8.8
 */
package ma.emsi.batchservice.dto.event;
