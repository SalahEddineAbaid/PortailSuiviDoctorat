package ma.emsi.batchservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for doctoral duration alert events published to Kafka.
 * Used by the duration alert job to notify doctorants and directors
 * when critical duration thresholds are reached.
 * 
 * Alert Types:
 * - ALERTE_DUREE_3_ANS: Approaching 3-year threshold (2y9m - 3y)
 * - ALERTE_DUREE_6_ANS: Approaching 6-year threshold (5y9m - 6y)
 * - ALERTE_DEPASSEMENT_6_ANS: Exceeded 6-year limit (urgent)
 * 
 * Priority Levels:
 * - NORMAL: 3-year threshold alerts
 * - HIGH: 6-year threshold alerts
 * - URGENT: Exceeded 6-year limit
 * 
 * Requirements: 2.3, 2.5, 2.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventDTO {

    /**
     * Type of alert being sent.
     * Values: ALERTE_DUREE_3_ANS, ALERTE_DUREE_6_ANS, ALERTE_DEPASSEMENT_6_ANS
     */
    private String type;

    /**
     * Email address of the doctorant receiving the alert.
     */
    private String doctorantEmail;

    /**
     * Full name of the doctorant.
     */
    private String doctorantNom;

    /**
     * Email address of the thesis director.
     */
    private String directeurEmail;

    /**
     * Date of first enrollment in the doctoral program.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate datePremiereInscription;

    /**
     * Current duration in the doctoral program.
     * Format: "X ans Y mois" (e.g., "2 ans 10 mois")
     */
    private String dureeActuelle;

    /**
     * Time remaining until threshold.
     * Format: "X mois" or "X jours" (e.g., "2 mois", "15 jours")
     * Null for ALERTE_DEPASSEMENT_6_ANS (already exceeded)
     */
    private String tempsRestant;

    /**
     * Threshold being approached or exceeded.
     * Values: "3 ans", "6 ans"
     */
    private String seuil;

    /**
     * Action required by the doctorant or director.
     * Examples:
     * - "Demander dérogation avant expiration"
     * - "Régularisation urgente requise"
     * - "Planifier soutenance dans les délais"
     */
    private String actionRequise;

    /**
     * Priority level of the alert.
     * Values: NORMAL, HIGH, URGENT
     */
    private String priority;

    /**
     * Enrollment ID for tracking and idempotence.
     */
    private Long inscriptionId;

    /**
     * Doctorant user ID for tracking.
     */
    private Long doctorantId;
}
