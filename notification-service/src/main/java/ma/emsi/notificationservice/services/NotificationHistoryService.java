package ma.emsi.notificationservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.dtos.NotificationStatsDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing notification history and persistence.
 * Handles CRUD operations for notifications and provides statistics.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.3, 6.4, 6.5, 6.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHistoryService {
    
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;
    
    /**
     * Save a new notification with PENDING status.
     * Requirement 5.1: WHEN a notification is received from Kafka THEN create a Notification entity with status PENDING
     * 
     * @param notificationDTO the notification data transfer object
     * @return the persisted notification entity
     */
    @Transactional
    public Notification saveNotification(NotificationDTO notificationDTO) {
        log.debug("Saving notification for destinataire: {}, type: {}", 
                  notificationDTO.getDestinataire(), notificationDTO.getType());
        
        // Serialize donnees map to JSON
        String donneesJson = null;
        if (notificationDTO.getDonnees() != null && !notificationDTO.getDonnees().isEmpty()) {
            try {
                donneesJson = objectMapper.writeValueAsString(notificationDTO.getDonnees());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize donnees to JSON", e);
                donneesJson = "{}";
            }
        }
        
        Notification notification = Notification.builder()
                .type(notificationDTO.getType())
                .destinataire(notificationDTO.getDestinataire())
                .sujet(notificationDTO.getSujet())
                .messageTexte(notificationDTO.getMessageTexte())
                .statut(StatutNotification.PENDING)
                .priorite(notificationDTO.getPriorite())
                .donnees(donneesJson)
                .nombreTentatives(0)
                .build();
        
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved with id: {}, status: PENDING", saved.getId());
        
        return saved;
    }
    
    /**
     * Update notification status and set dateEnvoi for SENT status.
     * Requirement 5.2: WHEN a notification is successfully sent THEN update status to SENT and set dateEnvoi
     * Requirement 5.3: WHEN a notification send fails THEN update status to FAILED and record erreurMessage
     * Requirement 5.5: WHEN a notification is retried THEN update status to RETRYING
     * 
     * @param notificationId the notification ID
     * @param statut the new status
     * @param erreurMessage optional error message for FAILED status
     */
    @Transactional
    public void updateNotificationStatus(Long notificationId, StatutNotification statut, String erreurMessage) {
        log.debug("Updating notification {} to status: {}", notificationId, statut);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
        
        notification.setStatut(statut);
        
        // Set dateEnvoi when status is SENT
        if (statut == StatutNotification.SENT) {
            notification.setDateEnvoi(LocalDateTime.now());
            log.info("Notification {} marked as SENT at {}", notificationId, notification.getDateEnvoi());
        }
        
        // Set error message for FAILED status
        if (statut == StatutNotification.FAILED && erreurMessage != null) {
            notification.setErreurMessage(erreurMessage);
            log.error("Notification {} marked as FAILED: {}", notificationId, erreurMessage);
        }
        
        notificationRepository.save(notification);
    }
    
    /**
     * Increment the retry counter for a notification.
     * Requirement 5.4: WHEN a notification is retried THEN increment nombreTentatives counter
     * 
     * @param notificationId the notification ID
     */
    @Transactional
    public void incrementRetryCount(Long notificationId) {
        log.debug("Incrementing retry count for notification {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
        
        notification.setNombreTentatives(notification.getNombreTentatives() + 1);
        notificationRepository.save(notification);
        
        // Increment retry counter metric
        metricsService.incrementRetryCounter();
        
        log.info("Notification {} retry count incremented to {}", 
                 notificationId, notification.getNombreTentatives());
    }
    
    /**
     * Get all notifications for a specific user email address.
     * Requirement 6.3: WHEN a user requests notifications by email THEN return only notifications for that email
     * 
     * @param email the user email address
     * @param pageable pagination parameters
     * @return page of notifications for the user
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByUser(String email, Pageable pageable) {
        log.debug("Retrieving notifications for user: {}", email);
        return notificationRepository.findByDestinataire(email, pageable);
    }
    
    /**
     * Get all notifications with a specific status.
     * Requirement 6.4: WHEN an admin requests notifications by status THEN return all notifications with that status
     * 
     * @param statut the notification status
     * @param pageable pagination parameters
     * @return page of notifications with the specified status
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByStatus(StatutNotification statut, Pageable pageable) {
        log.debug("Retrieving notifications with status: {}", statut);
        return notificationRepository.findByStatut(statut, pageable);
    }
    
    /**
     * Get notification statistics including success rate.
     * Requirement 6.5: WHEN an admin requests stats THEN return total, sent, failed, pending counts and success rate
     * 
     * @return notification statistics DTO
     */
    @Transactional(readOnly = true)
    public NotificationStatsDTO getNotificationsStats() {
        log.debug("Calculating notification statistics");
        
        Long total = notificationRepository.countTotal();
        Long sent = notificationRepository.countByStatut(StatutNotification.SENT);
        Long failed = notificationRepository.countByStatut(StatutNotification.FAILED);
        Long pending = notificationRepository.countByStatut(StatutNotification.PENDING);
        Long retrying = notificationRepository.countByStatut(StatutNotification.RETRYING);
        
        NotificationStatsDTO stats = NotificationStatsDTO.builder()
                .total(total)
                .sent(sent)
                .failed(failed)
                .pending(pending)
                .retrying(retrying)
                .build();
        
        // Calculate success rate
        stats.calculateSuccessRate();
        
        log.info("Notification stats - Total: {}, Sent: {}, Failed: {}, Pending: {}, Retrying: {}, Success Rate: {}%",
                 total, sent, failed, pending, retrying, stats.getSuccessRate());
        
        return stats;
    }
    
    /**
     * Retry a failed notification by resetting its status to PENDING.
     * Requirement 6.6: WHEN an admin requests retry THEN retry sending the failed notification
     * 
     * @param notificationId the notification ID to retry
     * @return the updated notification
     */
    @Transactional
    public Notification retryFailedNotification(Long notificationId) {
        log.debug("Retrying failed notification {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
        
        // Only retry if notification is in FAILED status
        if (notification.getStatut() != StatutNotification.FAILED) {
            log.warn("Cannot retry notification {} with status {}", notificationId, notification.getStatut());
            throw new IllegalStateException("Can only retry notifications with FAILED status");
        }
        
        // Reset status to PENDING for reprocessing
        notification.setStatut(StatutNotification.PENDING);
        notification.setErreurMessage(null);
        
        Notification updated = notificationRepository.save(notification);
        log.info("Notification {} reset to PENDING for retry", notificationId);
        
        return updated;
    }
}
