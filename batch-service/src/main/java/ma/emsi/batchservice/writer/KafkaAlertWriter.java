package ma.emsi.batchservice.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.event.AlertEventDTO;
import ma.emsi.batchservice.entity.AlerteDureeEnvoyee;
import ma.emsi.batchservice.repository.AlerteDureeEnvoyeeRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Writer for duration alert job.
 * 
 * Performs three operations for each alert:
 * 1. Publishes AlertEventDTO to Kafka notifications topic
 * 2. Inserts record into alerte_duree_envoyee table (for idempotence)
 * 3. Updates enrollment status to BLOQUÉ for DEPASSEMENT cases
 * 
 * All operations are performed within a transaction to ensure consistency.
 * 
 * Requirements: 2.3, 2.5, 2.6, 2.7
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaAlertWriter implements ItemWriter<AlertEventDTO> {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AlerteDureeEnvoyeeRepository alerteDureeEnvoyeeRepository;

    @Qualifier("inscriptionJdbcTemplate")
    private final JdbcTemplate inscriptionJdbcTemplate;

    @Value("${spring.kafka.topics.notifications:notifications}")
    private String notificationsTopic;

    /**
     * Alert type constants.
     */
    private static final String ALERT_TYPE_DEPASSEMENT = "ALERTE_DUREE_DEPASSEMENT";

    /**
     * Writes a chunk of alert events.
     * 
     * For each alert:
     * 1. Publishes to Kafka
     * 2. Records in alerte_duree_envoyee table
     * 3. Updates enrollment status if DEPASSEMENT
     * 
     * @param chunk Chunk of AlertEventDTO to write
     */
    @Override
    @Transactional
    public void write(Chunk<? extends AlertEventDTO> chunk) throws Exception {
        for (AlertEventDTO alert : chunk) {
            try {
                // 1. Publish to Kafka notifications topic
                publishToKafka(alert);

                // 2. Record alert in database for idempotence
                recordAlertSent(alert);

                // 3. Update enrollment status if DEPASSEMENT
                if (ALERT_TYPE_DEPASSEMENT.equals(alert.getType())) {
                    updateEnrollmentStatus(alert);
                }

                log.info("Successfully processed alert {} for enrollment {}",
                        alert.getType(), alert.getInscriptionId());

            } catch (Exception e) {
                log.error("Failed to write alert {} for enrollment {}: {}",
                        alert.getType(), alert.getInscriptionId(), e.getMessage(), e);
                throw e; // Rollback transaction on failure
            }
        }
    }

    /**
     * Publishes alert event to Kafka notifications topic.
     * 
     * Uses doctorant ID as the message key for partitioning.
     * 
     * @param alert The alert event to publish
     */
    private void publishToKafka(AlertEventDTO alert) {
        String key = String.valueOf(alert.getDoctorantId());

        kafkaTemplate.send(notificationsTopic, key, alert)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish alert to Kafka for enrollment {}: {}",
                                alert.getInscriptionId(), ex.getMessage());
                    } else {
                        log.debug("Published alert to Kafka topic {} for enrollment {}",
                                notificationsTopic, alert.getInscriptionId());
                    }
                });
    }

    /**
     * Records alert in alerte_duree_envoyee table to prevent duplicates.
     * 
     * @param alert The alert event
     */
    private void recordAlertSent(AlertEventDTO alert) {
        // Extract alert type suffix (3_ANS, 6_ANS, DEPASSEMENT)
        String typeAlerte = extractAlertTypeSuffix(alert.getType());

        AlerteDureeEnvoyee alertRecord = AlerteDureeEnvoyee.builder()
                .inscriptionId(alert.getInscriptionId())
                .doctorantId(alert.getDoctorantId())
                .typeAlerte(typeAlerte)
                .dateEnvoi(LocalDateTime.now())
                .build();

        alerteDureeEnvoyeeRepository.save(alertRecord);

        log.debug("Recorded alert {} in database for enrollment {}",
                typeAlerte, alert.getInscriptionId());
    }

    /**
     * Updates enrollment status to BLOQUÉ for DEPASSEMENT cases.
     * 
     * This prevents the doctorant from continuing without regularization.
     * 
     * @param alert The alert event
     */
    private void updateEnrollmentStatus(AlertEventDTO alert) {
        String updateSql = """
                UPDATE inscription
                SET statut = 'BLOQUÉ',
                    updated_at = NOW()
                WHERE id = ?
                """;

        int rowsUpdated = inscriptionJdbcTemplate.update(updateSql, alert.getInscriptionId());

        if (rowsUpdated > 0) {
            log.info("Updated enrollment {} status to BLOQUÉ due to exceeded 6-year limit",
                    alert.getInscriptionId());
        } else {
            log.warn("Failed to update enrollment {} status - enrollment not found",
                    alert.getInscriptionId());
        }
    }

    /**
     * Extracts the alert type suffix from the full alert type.
     * 
     * Example: "ALERTE_DUREE_3_ANS" -> "3_ANS"
     * 
     * @param fullType The full alert type
     * @return The alert type suffix
     */
    private String extractAlertTypeSuffix(String fullType) {
        if (fullType == null) {
            return "UNKNOWN";
        }

        if (fullType.startsWith("ALERTE_DUREE_")) {
            return fullType.substring("ALERTE_DUREE_".length());
        }

        return fullType;
    }
}
