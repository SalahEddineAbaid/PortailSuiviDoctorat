package ma.emsi.batchservice.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Tasklet for retrying pending notifications that are stuck.
 * 
 * Stale Notification Detection:
 * - Query notifications with status PENDING older than 24 hours
 * - Retry sending each notification
 * - Track success/failure counts
 * 
 * Corrective Actions:
 * - Retry sending notification via Kafka
 * - If retry succeeds: update status to SENT
 * - If retry fails: mark as FAILED and notify technical admin
 * 
 * Requirements: 5.10, 5.11, 5.12
 */
@Slf4j
@Component
public class RetryPendingNotificationsTasklet implements Tasklet {

    private final JdbcTemplate notificationJdbcTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RetryPendingNotificationsTasklet(
            @Qualifier("notificationJdbcTemplate") JdbcTemplate notificationJdbcTemplate,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.notificationJdbcTemplate = notificationJdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting retry of pending notifications");

        int retrySuccessCount = 0;
        int retryFailureCount = 0;

        try {
            // Query notifications with status PENDING older than 24 hours
            String query = """
                    SELECT id, recipient_email, subject, message, type, priority, created_at
                    FROM notification
                    WHERE status = 'PENDING'
                    AND created_at < ?
                    ORDER BY created_at ASC
                    """;

            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
            List<Map<String, Object>> pendingNotifications = notificationJdbcTemplate.queryForList(query, cutoffTime);

            log.info("Found {} stale pending notifications to retry", pendingNotifications.size());

            for (Map<String, Object> notification : pendingNotifications) {
                Long notificationId = ((Number) notification.get("id")).longValue();
                String recipientEmail = (String) notification.get("recipient_email");
                String subject = (String) notification.get("subject");
                String message = (String) notification.get("message");
                String type = (String) notification.get("type");
                String priority = (String) notification.get("priority");

                log.info("Retrying notification {} for recipient {}", notificationId, recipientEmail);

                // Attempt to retry sending via Kafka
                boolean retrySuccess = retryNotification(notificationId, recipientEmail, subject, message, type,
                        priority);

                if (retrySuccess) {
                    // Update status to SENT
                    String updateQuery = """
                            UPDATE notification
                            SET status = 'SENT',
                                sent_at = ?,
                                updated_at = ?
                            WHERE id = ?
                            """;
                    notificationJdbcTemplate.update(updateQuery, LocalDateTime.now(), LocalDateTime.now(),
                            notificationId);
                    retrySuccessCount++;
                    log.info("Successfully retried notification {}", notificationId);
                } else {
                    // Mark as FAILED and notify technical admin
                    String updateQuery = """
                            UPDATE notification
                            SET status = 'FAILED',
                                error_message = 'Retry failed after 24 hours',
                                updated_at = ?
                            WHERE id = ?
                            """;
                    notificationJdbcTemplate.update(updateQuery, LocalDateTime.now(), notificationId);
                    retryFailureCount++;

                    // Notify technical admin
                    notifyTechnicalAdmin(notificationId, recipientEmail, type);

                    log.warn("Failed to retry notification {}, marked as FAILED", notificationId);
                }
            }

            // Store metrics in execution context for listener
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("notificationRetrySuccess", retrySuccessCount);
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("notificationRetryFailure", retryFailureCount);

            log.info("Pending notifications retry completed. Success: {}, Failure: {}",
                    retrySuccessCount, retryFailureCount);

        } catch (Exception e) {
            log.error("Error during pending notifications retry", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Attempts to retry sending a notification via Kafka.
     */
    private boolean retryNotification(Long notificationId, String recipientEmail, String subject,
            String message, String type, String priority) {
        try {
            Map<String, Object> notificationEvent = Map.of(
                    "notificationId", notificationId,
                    "recipientEmail", recipientEmail,
                    "subject", subject,
                    "message", message,
                    "type", type != null ? type : "GENERAL",
                    "priority", priority != null ? priority : "NORMAL",
                    "retryAttempt", true,
                    "timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send("notifications", "retry-" + notificationId, notificationEvent).get();
            log.debug("Successfully published retry notification {} to Kafka", notificationId);
            return true;
        } catch (Exception e) {
            log.error("Failed to retry notification {} via Kafka", notificationId, e);
            return false;
        }
    }

    /**
     * Notifies technical admin about failed notification retry.
     */
    private void notifyTechnicalAdmin(Long notificationId, String originalRecipient, String type) {
        try {
            Map<String, Object> adminNotification = Map.of(
                    "type", "NOTIFICATION_RETRY_FAILED",
                    "notificationId", notificationId,
                    "originalRecipient", originalRecipient,
                    "originalType", type != null ? type : "UNKNOWN",
                    "message", "Notification retry failed after 24 hours in PENDING status",
                    "priority", "HIGH",
                    "actionRequired", "Manual investigation required",
                    "timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send("notifications", "admin-alert-" + notificationId, adminNotification);
            log.debug("Notified technical admin about failed notification {}", notificationId);
        } catch (Exception e) {
            log.error("Failed to notify technical admin about notification {}", notificationId, e);
            // Don't fail the tasklet if admin notification fails
        }
    }
}
