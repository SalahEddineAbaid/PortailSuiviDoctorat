package ma.emsi.notificationservice.consumers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.services.NotificationProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for notification events.
 * Listens to the notifications topic and delegates processing to NotificationProcessingService.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {
    
    private final NotificationProcessingService notificationProcessingService;
    
    /**
     * Consumes notification events from the Kafka notifications topic.
     * Requirement 1.1: Consume event and deserialize into NotificationDTO
     * 
     * JSON deserialization is configured via:
     * - KafkaConsumerConfig with JsonDeserializer
     * - Trusted packages set to "*" to allow all packages
     * - ErrorHandlingDeserializer wrapper for graceful error handling
     * 
     * The consumer delegates all processing to NotificationProcessingService which handles:
     * - Requirement 1.2: Email validation
     * - Requirement 1.3: Type validation
     * - Requirement 1.4: Template selection
     * - Requirement 1.5: Variable interpolation
     * - Requirement 1.6: Success persistence
     * - Requirement 1.7: Failure persistence
     * 
     * @param notificationDTO the deserialized notification
     * @param partition the Kafka partition
     * @param offset the message offset
     */
    @KafkaListener(
        topics = "${kafka.topic.notifications:notifications}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotification(
            @Payload NotificationDTO notificationDTO,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        try {
            // Requirement 13.1: Structured logging for notification received (DEBUG level)
            log.debug("=== Notification Received ===");
            log.debug("Kafka Topic: notifications");
            log.debug("Partition: {}", partition);
            log.debug("Offset: {}", offset);
            log.debug("Type: {}", notificationDTO.getType());
            log.debug("Destinataire: {}", notificationDTO.getDestinataire());
            log.debug("Sujet: {}", notificationDTO.getSujet());
            log.debug("Priorite: {}", notificationDTO.getPriorite());
            log.debug("Donnees: {}", notificationDTO.getDonnees());
            log.debug("============================");
            
            log.info("Received notification from Kafka - Type: {}, Destinataire: {}, Partition: {}, Offset: {}", 
                     notificationDTO.getType(), notificationDTO.getDestinataire(), partition, offset);
            
            // Delegate to processing service (Requirements 1.2-1.7)
            notificationProcessingService.processNotification(notificationDTO);
            
            log.debug("Successfully processed notification from offset {}", offset);
            
        } catch (Exception e) {
            // Log the error but don't rethrow - we don't want to block the consumer
            // The NotificationProcessingService handles persistence of failures
            log.error("Error processing notification from Kafka (Partition: {}, Offset: {}): {}", 
                      partition, offset, e.getMessage(), e);
            
            // Note: The notification has already been persisted with FAILED status
            // by the NotificationProcessingService.handleFailure method
        }
    }
}
