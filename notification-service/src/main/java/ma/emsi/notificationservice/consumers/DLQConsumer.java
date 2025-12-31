package ma.emsi.notificationservice.consumers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.entities.NotificationDLQ;
import ma.emsi.notificationservice.repositories.NotificationDLQRepository;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import ma.emsi.notificationservice.services.MetricsService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Kafka consumer for Dead Letter Queue (DLQ) notifications.
 * Listens to the notifications-dlq topic and persists failed notifications for audit.
 * 
 * Requirements: 11.2, 11.3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DLQConsumer {
    
    private final NotificationDLQRepository notificationDLQRepository;
    private final NotificationRepository notificationRepository;
    private final MetricsService metricsService;
    
    /**
     * Consumes notification events from the Kafka notifications-dlq topic.
     * 
     * Requirement 11.2: Log DLQ messages with full details
     * Requirement 11.3: Persist DLQ messages in notification_dlq table
     * 
     * @param notificationDTO the failed notification
     * @param partition the Kafka partition
     * @param offset the message offset
     */
    @KafkaListener(
        topics = "${kafka.topic.notifications-dlq:notifications-dlq}",
        groupId = "${spring.kafka.consumer.group-id}-dlq",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDLQNotification(
            @Payload NotificationDTO notificationDTO,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        try {
            // Requirement 11.2: Log DLQ messages with full details
            log.warn("=== DLQ Message Received ===");
            log.warn("Topic: notifications-dlq");
            log.warn("Partition: {}", partition);
            log.warn("Offset: {}", offset);
            log.warn("Type: {}", notificationDTO.getType());
            log.warn("Destinataire: {}", notificationDTO.getDestinataire());
            log.warn("Sujet: {}", notificationDTO.getSujet());
            log.warn("Message: {}", notificationDTO.getMessageTexte());
            log.warn("Donnees: {}", notificationDTO.getDonnees());
            log.warn("===========================");
            
            // Extract notification ID from the message text (format: "Failed notification ID: X. Error: ...")
            Long notificationId = extractNotificationId(notificationDTO.getMessageTexte());
            
            if (notificationId == null) {
                log.error("Could not extract notification ID from DLQ message: {}", notificationDTO.getMessageTexte());
                return;
            }
            
            // Retrieve the original notification to get full details
            Optional<Notification> originalNotificationOpt = notificationRepository.findById(notificationId);
            
            if (originalNotificationOpt.isEmpty()) {
                log.error("Original notification {} not found in database", notificationId);
                return;
            }
            
            Notification originalNotification = originalNotificationOpt.get();
            
            // Requirement 11.3: Persist DLQ messages in notification_dlq table
            NotificationDLQ dlqEntry = NotificationDLQ.builder()
                .notificationId(notificationId)
                .type(originalNotification.getType())
                .destinataire(originalNotification.getDestinataire())
                .sujet(originalNotification.getSujet())
                .erreurMessage(originalNotification.getErreurMessage())
                .donnees(originalNotification.getDonnees())
                .nombreTentatives(originalNotification.getNombreTentatives())
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .retraite(false)
                .build();
            
            notificationDLQRepository.save(dlqEntry);
            
            // Note: DLQ counter is already incremented in NotificationProcessingService.sendToDLQ()
            // when the message is sent to the DLQ topic
            
            log.info("DLQ entry persisted for notification {} (DLQ ID: {})", 
                     notificationId, dlqEntry.getId());
            
        } catch (Exception e) {
            // Log the error but don't rethrow - we don't want to block the DLQ consumer
            log.error("Error processing DLQ message (Partition: {}, Offset: {}): {}", 
                      partition, offset, e.getMessage(), e);
        }
    }
    
    /**
     * Extracts the notification ID from the DLQ message text.
     * Expected format: "Failed notification ID: X. Error: ..."
     * 
     * @param messageText the message text
     * @return the notification ID, or null if not found
     */
    private Long extractNotificationId(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return null;
        }
        
        try {
            // Parse the message format: "Failed notification ID: X. Error: ..."
            String prefix = "Failed notification ID: ";
            int startIndex = messageText.indexOf(prefix);
            
            if (startIndex == -1) {
                return null;
            }
            
            startIndex += prefix.length();
            int endIndex = messageText.indexOf(".", startIndex);
            
            if (endIndex == -1) {
                endIndex = messageText.length();
            }
            
            String idString = messageText.substring(startIndex, endIndex).trim();
            return Long.parseLong(idString);
            
        } catch (Exception e) {
            log.error("Failed to extract notification ID from message: {}", messageText, e);
            return null;
        }
    }
}
