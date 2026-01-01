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
 * Tasklet for verifying enrollment-defense consistency across services.
 * Checks that each defense request has a valid enrollment with status VALIDÉ.
 * For invalid: logs anomaly, blocks defense, notifies director and admin.
 * 
 * Consistency Check:
 * - Query all defense requests from defensedb
 * - For each defense, verify enrollment exists and has status VALIDÉ
 * - If invalid: block defense and notify director and admin
 * 
 * Corrective Actions:
 * - Update defense request status to BLOQUÉ
 * - Log anomaly with details
 * - Publish Kafka notifications to director and admin
 * 
 * Requirements: 5.4, 5.5
 */
@Slf4j
@Component
public class VerifyEnrollmentDefenseConsistencyTasklet implements Tasklet {

    private final JdbcTemplate defenseJdbcTemplate;
    private final JdbcTemplate inscriptionJdbcTemplate;
    private final JdbcTemplate userJdbcTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VerifyEnrollmentDefenseConsistencyTasklet(
            @Qualifier("defenseJdbcTemplate") JdbcTemplate defenseJdbcTemplate,
            @Qualifier("inscriptionJdbcTemplate") JdbcTemplate inscriptionJdbcTemplate,
            @Qualifier("userJdbcTemplate") JdbcTemplate userJdbcTemplate,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.defenseJdbcTemplate = defenseJdbcTemplate;
        this.inscriptionJdbcTemplate = inscriptionJdbcTemplate;
        this.userJdbcTemplate = userJdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting enrollment-defense consistency verification");

        int anomalyCount = 0;
        int correctedCount = 0;

        try {
            // Query all defense requests that are not already blocked or completed
            String defenseQuery = """
                    SELECT dr.id, dr.inscription_id, dr.statut
                    FROM defense_request dr
                    WHERE dr.statut NOT IN ('BLOQUÉ', 'REJETÉ')
                    """;

            List<Map<String, Object>> defenseRequests = defenseJdbcTemplate.queryForList(defenseQuery);
            log.info("Found {} active defense requests to verify", defenseRequests.size());

            for (Map<String, Object> defense : defenseRequests) {
                Long defenseId = ((Number) defense.get("id")).longValue();
                Long inscriptionId = ((Number) defense.get("inscription_id")).longValue();
                String defenseStatus = (String) defense.get("statut");

                // Check if enrollment exists and has status VALIDÉ
                String enrollmentCheckQuery = """
                        SELECT statut, doctorant_id
                        FROM inscription
                        WHERE id = ?
                        """;

                List<Map<String, Object>> enrollments = inscriptionJdbcTemplate.queryForList(
                        enrollmentCheckQuery, inscriptionId);

                boolean isValid = false;
                Long doctorantId = null;
                String enrollmentStatus = null;

                if (!enrollments.isEmpty()) {
                    Map<String, Object> enrollment = enrollments.get(0);
                    enrollmentStatus = (String) enrollment.get("statut");
                    doctorantId = ((Number) enrollment.get("doctorant_id")).longValue();
                    isValid = "VALIDÉ".equals(enrollmentStatus);
                }

                if (!isValid) {
                    // Invalid enrollment - this is an anomaly
                    anomalyCount++;
                    String reason = enrollments.isEmpty()
                            ? "Enrollment not found"
                            : "Enrollment status is " + enrollmentStatus + " (expected VALIDÉ)";

                    log.warn("Anomaly detected: Defense {} references invalid enrollment {}. Reason: {}",
                            defenseId, inscriptionId, reason);

                    // Corrective action: Block defense request
                    String updateQuery = """
                            UPDATE defense_request
                            SET statut = 'BLOQUÉ',
                                updated_at = ?
                            WHERE id = ?
                            """;
                    defenseJdbcTemplate.update(updateQuery, LocalDateTime.now(), defenseId);
                    correctedCount++;

                    // Get director email if possible
                    String directorEmail = null;
                    if (doctorantId != null) {
                        directorEmail = getDirectorEmail(inscriptionId);
                    }

                    // Publish Kafka notifications to director and admin
                    publishAnomalyNotification(defenseId, inscriptionId, reason, directorEmail);

                    log.info("Corrective action applied: Defense {} marked as BLOQUÉ", defenseId);
                }
            }

            // Store metrics in execution context for listener
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("enrollmentDefenseAnomalies", anomalyCount);
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("enrollmentDefenseCorrected", correctedCount);

            log.info("Enrollment-defense consistency verification completed. Anomalies: {}, Corrected: {}",
                    anomalyCount, correctedCount);

        } catch (Exception e) {
            log.error("Error during enrollment-defense consistency verification", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Retrieves director email for the enrollment.
     */
    private String getDirectorEmail(Long inscriptionId) {
        try {
            String query = """
                    SELECT u.email
                    FROM inscription i
                    JOIN user u ON i.directeur_id = u.id
                    WHERE i.id = ?
                    """;
            List<String> emails = inscriptionJdbcTemplate.queryForList(query, String.class, inscriptionId);
            return emails.isEmpty() ? null : emails.get(0);
        } catch (Exception e) {
            log.warn("Failed to retrieve director email for enrollment {}", inscriptionId, e);
            return null;
        }
    }

    /**
     * Publishes anomaly notification to Kafka for director and admin alerting.
     */
    private void publishAnomalyNotification(Long defenseId, Long inscriptionId, String reason, String directorEmail) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "DATA_CONSISTENCY_ANOMALY",
                    "anomalyType", "INVALID_ENROLLMENT_FOR_DEFENSE",
                    "defenseId", defenseId,
                    "inscriptionId", inscriptionId,
                    "reason", reason,
                    "message", "Defense request references invalid enrollment",
                    "correctiveAction", "Defense request marked as BLOQUÉ",
                    "directorEmail", directorEmail != null ? directorEmail : "unknown",
                    "priority", "HIGH",
                    "timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send("notifications", "anomaly-defense-" + defenseId, notification);
            log.debug("Anomaly notification published for defense {}", defenseId);
        } catch (Exception e) {
            log.error("Failed to publish anomaly notification for defense {}", defenseId, e);
            // Don't fail the tasklet if notification fails
        }
    }
}
