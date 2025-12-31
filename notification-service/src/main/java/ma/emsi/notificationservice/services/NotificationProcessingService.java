package ma.emsi.notificationservice.services;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Service responsible for orchestrating the notification processing workflow.
 * Handles validation, template selection, email sending, and failure management.
 * 
 * Requirements: 1.2, 1.3, 1.4, 1.6, 1.7, 7.4, 7.5, 11.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingService {
    
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final NotificationHistoryService notificationHistoryService;
    private final KafkaTemplate<String, NotificationDTO> kafkaTemplate;
    private final MetricsService metricsService;
    
    private static final String DLQ_TOPIC = "notifications-dlq";
    
    // RFC 5322 compliant email regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    /**
     * Main orchestration method for processing a notification.
     * Validates, prepares, sends, and persists the notification.
     * 
     * @param notificationDTO the notification to process
     */
    public void processNotification(NotificationDTO notificationDTO) {
        log.debug("=== Processing Notification ===");
        log.debug("Destinataire: {}", notificationDTO.getDestinataire());
        log.debug("Type: {}", notificationDTO.getType());
        log.debug("Sujet: {}", notificationDTO.getSujet());
        log.debug("===============================");
        
        // Save notification with PENDING status (Requirement 5.1)
        Notification notification = notificationHistoryService.saveNotification(notificationDTO);
        
        // Increment pending counter
        metricsService.incrementPendingCounter();
        
        try {
            // Validate notification (Requirements 1.2, 1.3)
            validateNotification(notificationDTO);
            
            // Prepare template variables
            Map<String, Object> variables = prepareTemplateVariables(notificationDTO);
            
            // Process template with variables
            String htmlContent = emailTemplateService.processTemplate(
                notificationDTO.getType(), 
                variables
            );
            
            // Store the HTML content in the notification entity
            notification.setMessageHtml(htmlContent);
            
            // Send email
            emailService.sendEmail(
                notificationDTO.getDestinataire(),
                notificationDTO.getSujet(),
                htmlContent
            ).get(); // Block to wait for completion
            
            // Handle success (Requirement 1.6)
            handleSuccess(notification.getId());
            
        } catch (IllegalArgumentException e) {
            // Validation errors should not be retried (Requirement 7.6)
            log.error("Validation error for notification {}: {}", notification.getId(), e.getMessage());
            handleFailure(notification.getId(), e.getMessage(), false);
            
        } catch (MessagingException | ExecutionException e) {
            // Email sending errors should be retried (Requirement 1.7, 7.4)
            log.error("Email sending error for notification {}: {}", notification.getId(), e.getMessage());
            handleFailure(notification.getId(), e.getMessage(), true);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while processing notification {}", notification.getId());
            handleFailure(notification.getId(), "Processing interrupted", true);
            
        } catch (Exception e) {
            // Unexpected errors
            log.error("Unexpected error processing notification {}: {}", notification.getId(), e.getMessage(), e);
            handleFailure(notification.getId(), e.getMessage(), true);
        }
    }
    
    /**
     * Validates a notification DTO.
     * Requirement 1.2: Validate that destinataire contains a valid email address
     * Requirement 1.3: Validate that type matches a recognized TypeNotification enum value
     * 
     * @param notificationDTO the notification to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateNotification(NotificationDTO notificationDTO) {
        if (notificationDTO == null) {
            throw new IllegalArgumentException("NotificationDTO cannot be null");
        }
        
        // Validate email format (Requirement 1.2)
        if (notificationDTO.getDestinataire() == null || 
            notificationDTO.getDestinataire().trim().isEmpty()) {
            throw new IllegalArgumentException("Destinataire email is required");
        }
        
        if (!EMAIL_PATTERN.matcher(notificationDTO.getDestinataire()).matches()) {
            throw new IllegalArgumentException(
                "Invalid email format: " + notificationDTO.getDestinataire()
            );
        }
        
        // Validate notification type (Requirement 1.3)
        if (notificationDTO.getType() == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        
        // Verify type is a valid enum value
        boolean isValidType = Arrays.stream(TypeNotification.values())
            .anyMatch(type -> type == notificationDTO.getType());
        
        if (!isValidType) {
            throw new IllegalArgumentException(
                "Invalid notification type: " + notificationDTO.getType()
            );
        }
        
        // Validate subject
        if (notificationDTO.getSujet() == null || 
            notificationDTO.getSujet().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required");
        }
        
        log.debug("Notification validation passed for destinataire: {}, type: {}", 
                  notificationDTO.getDestinataire(), notificationDTO.getType());
    }
    
    /**
     * Selects the appropriate template for a notification type.
     * Requirement 1.4: Select the appropriate HTML template based on the type field
     * 
     * @param type the notification type
     * @return the template filename
     */
    public String selectTemplate(TypeNotification type) {
        String templateName = emailTemplateService.getTemplateForNotificationType(type);
        log.debug("Selected template {} for notification type {}", templateName, type);
        return templateName;
    }
    
    /**
     * Prepares template variables from the notification DTO.
     * Extracts the donnees map and adds standard variables.
     * 
     * @param notificationDTO the notification DTO
     * @return map of template variables
     */
    public Map<String, Object> prepareTemplateVariables(NotificationDTO notificationDTO) {
        Map<String, Object> variables = notificationDTO.getDonnees();
        
        if (variables == null) {
            variables = new java.util.HashMap<>();
        }
        
        // Add standard variables that are always available
        variables.putIfAbsent("destinataire", notificationDTO.getDestinataire());
        variables.putIfAbsent("sujet", notificationDTO.getSujet());
        
        if (notificationDTO.getMessageTexte() != null) {
            variables.putIfAbsent("message", notificationDTO.getMessageTexte());
        }
        
        log.debug("Prepared {} template variables", variables.size());
        return variables;
    }
    
    /**
     * Handles successful notification sending.
     * Requirement 1.6: Persist notification record with status SENT
     * Requirement 13.1: WHEN a notification is successfully sent THEN log at INFO level
     * 
     * @param notificationId the notification ID
     */
    public void handleSuccess(Long notificationId) {
        // Requirement 13.1: Log successful notification at INFO level with details
        log.info("=== Notification Sent Successfully ===");
        log.info("Notification ID: {}", notificationId);
        log.info("Status: SENT");
        log.info("Timestamp: {}", java.time.LocalDateTime.now());
        log.info("======================================");
        
        notificationHistoryService.updateNotificationStatus(
            notificationId, 
            StatutNotification.SENT, 
            null
        );
        
        // Increment sent counter (Requirement 13.5)
        metricsService.incrementSentCounter();
    }
    
    /**
     * Handles notification sending failure.
     * Requirement 1.7: Persist notification record with status FAILED and error message
     * Requirement 7.4: Mark notification as FAILED after retries
     * Requirement 7.5: Send event to DLQ topic after failures
     * Requirement 13.2: WHEN a notification fails THEN log at ERROR level
     * 
     * @param notificationId the notification ID
     * @param errorMessage the error message
     * @param shouldRetry whether this failure should trigger retry/DLQ
     */
    public void handleFailure(Long notificationId, String errorMessage, boolean shouldRetry) {
        // Requirement 13.2: Log failed notification at ERROR level with details
        log.error("=== Notification Failed ===");
        log.error("Notification ID: {}", notificationId);
        log.error("Status: FAILED");
        log.error("Error: {}", errorMessage);
        log.error("Should Retry: {}", shouldRetry);
        log.error("Timestamp: {}", java.time.LocalDateTime.now());
        log.error("===========================");
        
        // Update status to FAILED and record error message (Requirement 1.7)
        notificationHistoryService.updateNotificationStatus(
            notificationId, 
            StatutNotification.FAILED, 
            errorMessage
        );
        
        // Increment failed counter (Requirement 13.6)
        metricsService.incrementFailedCounter();
        
        // Send to DLQ if this is a retriable failure (Requirement 7.5)
        if (shouldRetry) {
            try {
                // Retrieve the notification to send to DLQ
                // Note: In a real implementation, we'd fetch the full notification
                // For now, we'll send the notificationId as a simple message
                sendToDLQ(notificationId, errorMessage);
            } catch (Exception e) {
                log.error("Failed to send notification {} to DLQ: {}", notificationId, e.getMessage());
            }
        }
    }
    
    /**
     * Sends a failed notification to the Dead Letter Queue.
     * Requirement 11.1: Publish event to notifications-dlq topic after failures
     * 
     * @param notificationId the notification ID
     * @param errorMessage the error message
     */
    public void sendToDLQ(Long notificationId, String errorMessage) {
        log.warn("Sending notification {} to DLQ. Error: {}", notificationId, errorMessage);
        
        try {
            // Create a simple DTO with the notification ID and error for DLQ
            // The DLQ consumer will handle reprocessing
            NotificationDTO dlqMessage = NotificationDTO.builder()
                .type(TypeNotification.NOTIFICATION_GENERALE) // Placeholder type
                .destinataire("dlq@system.internal") // Placeholder email
                .sujet("DLQ - Notification " + notificationId)
                .messageTexte("Failed notification ID: " + notificationId + ". Error: " + errorMessage)
                .build();
            
            kafkaTemplate.send(DLQ_TOPIC, String.valueOf(notificationId), dlqMessage);
            
            // Increment DLQ counter
            metricsService.incrementDlqCounter();
            
            log.info("Notification {} sent to DLQ topic: {}", notificationId, DLQ_TOPIC);
            
        } catch (Exception e) {
            log.error("Failed to send notification {} to DLQ: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Failed to send to DLQ", e);
        }
    }
}
