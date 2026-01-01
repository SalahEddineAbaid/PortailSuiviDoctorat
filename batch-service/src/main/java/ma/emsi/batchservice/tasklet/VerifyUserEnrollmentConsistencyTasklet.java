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
 * Tasklet for verifying user-enrollment consistency across services.
 * Checks that each enrollment has a valid user in user-service.
 * For missing users: logs anomaly, marks enrollment SUSPENDU, notifies admin.
 * 
 * Consistency Check:
 * - Query all enrollments from inscriptiondb
 * - For each enrollment, verify user exists in userdb
 * - If user missing: suspend enrollment and notify admin
 * 
 * Corrective Actions:
 * - Update enrollment status to SUSPENDU
 * - Log anomaly with details
 * - Publish Kafka notification to admin
 * 
 * Requirements: 5.2, 5.3
 */
@Slf4j
@Component
public class VerifyUserEnrollmentConsistencyTasklet implements Tasklet {

    private final JdbcTemplate inscriptionJdbcTemplate;
    private final JdbcTemplate userJdbcTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VerifyUserEnrollmentConsistencyTasklet(
            @Qualifier("inscriptionJdbcTemplate") JdbcTemplate inscriptionJdbcTemplate,
            @Qualifier("userJdbcTemplate") JdbcTemplate userJdbcTemplate,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.inscriptionJdbcTemplate = inscriptionJdbcTemplate;
        this.userJdbcTemplate = userJdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting user-enrollment consistency verification");

        int anomalyCount = 0;
        int correctedCount = 0;

        try {
            // Query all enrollments with their user IDs
            String enrollmentQuery = """
                    SELECT id, doctorant_id, statut
                    FROM inscription
                    WHERE statut NOT IN ('SUSPENDU', 'REJETÉ')
                    """;

            List<Map<String, Object>> enrollments = inscriptionJdbcTemplate.queryForList(enrollmentQuery);
            log.info("Found {} active enrollments to verify", enrollments.size());

            for (Map<String, Object> enrollment : enrollments) {
                Long enrollmentId = ((Number) enrollment.get("id")).longValue();
                Long doctorantId = ((Number) enrollment.get("doctorant_id")).longValue();
                String status = (String) enrollment.get("statut");

                // Check if user exists in userdb
                String userCheckQuery = "SELECT COUNT(*) FROM user WHERE id = ?";
                Integer userCount = userJdbcTemplate.queryForObject(userCheckQuery, Integer.class, doctorantId);

                if (userCount == null || userCount == 0) {
                    // User does not exist - this is an anomaly
                    anomalyCount++;
                    log.warn("Anomaly detected: Enrollment {} references non-existent user {}",
                            enrollmentId, doctorantId);

                    // Corrective action: Mark enrollment as SUSPENDU
                    String updateQuery = """
                            UPDATE inscription
                            SET statut = 'SUSPENDU',
                                updated_at = ?
                            WHERE id = ?
                            """;
                    inscriptionJdbcTemplate.update(updateQuery, LocalDateTime.now(), enrollmentId);
                    correctedCount++;

                    // Publish Kafka notification to admin
                    publishAnomalyNotification(enrollmentId, doctorantId, "USER_NOT_FOUND");

                    log.info("Corrective action applied: Enrollment {} marked as SUSPENDU", enrollmentId);
                }
            }

            // Store metrics in execution context for listener
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("userEnrollmentAnomalies", anomalyCount);
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("userEnrollmentCorrected", correctedCount);

            log.info("User-enrollment consistency verification completed. Anomalies: {}, Corrected: {}",
                    anomalyCount, correctedCount);

        } catch (Exception e) {
            log.error("Error during user-enrollment consistency verification", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Publishes anomaly notification to Kafka for admin alerting.
     */
    private void publishAnomalyNotification(Long enrollmentId, Long doctorantId, String anomalyType) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "DATA_CONSISTENCY_ANOMALY",
                    "anomalyType", anomalyType,
                    "enrollmentId", enrollmentId,
                    "doctorantId", doctorantId,
                    "message", "Enrollment references non-existent user",
                    "correctiveAction", "Enrollment marked as SUSPENDU",
                    "priority", "HIGH",
                    "timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send("notifications", "anomaly-" + enrollmentId, notification);
            log.debug("Anomaly notification published for enrollment {}", enrollmentId);
        } catch (Exception e) {
            log.error("Failed to publish anomaly notification for enrollment {}", enrollmentId, e);
            // Don't fail the tasklet if notification fails
        }
    }
}
