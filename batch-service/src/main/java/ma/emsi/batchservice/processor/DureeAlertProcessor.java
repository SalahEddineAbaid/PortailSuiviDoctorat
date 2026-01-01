package ma.emsi.batchservice.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.event.AlertEventDTO;
import ma.emsi.batchservice.model.Inscription;
import ma.emsi.batchservice.repository.AlerteDureeEnvoyeeRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

/**
 * Processor for duration alert job.
 * 
 * Processes enrollment records to:
 * - Calculate exact duration from first enrollment date
 * - Determine alert type (3_ANS, 6_ANS, DEPASSEMENT)
 * - Check for duplicate alerts (idempotence)
 * - Build AlertEventDTO with all required fields
 * - Prepare status update for DEPASSEMENT cases
 * 
 * Returns null if alert has already been sent (prevents duplicates).
 * 
 * Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 2.7
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DureeAlertProcessor implements ItemProcessor<Inscription, AlertEventDTO> {

    private final AlerteDureeEnvoyeeRepository alerteDureeEnvoyeeRepository;

    /**
     * Alert type constants.
     */
    private static final String ALERT_TYPE_3_ANS = "3_ANS";
    private static final String ALERT_TYPE_6_ANS = "6_ANS";
    private static final String ALERT_TYPE_DEPASSEMENT = "DEPASSEMENT";

    /**
     * Priority level constants.
     */
    private static final String PRIORITY_NORMAL = "NORMAL";
    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_URGENT = "URGENT";

    /**
     * Threshold constants in months.
     */
    private static final int THREE_YEAR_THRESHOLD_MONTHS = 36;
    private static final int SIX_YEAR_THRESHOLD_MONTHS = 72;

    @Override
    public AlertEventDTO process(Inscription inscription) throws Exception {
        log.debug("Processing enrollment {} for doctorant {}",
                inscription.getId(), inscription.getDoctorantId());

        // Calculate duration from first enrollment date
        LocalDate firstEnrollmentDate = inscription.getDatePremiereInscription();
        LocalDate currentDate = LocalDate.now();
        Period period = Period.between(firstEnrollmentDate, currentDate);

        int totalMonths = period.getYears() * 12 + period.getMonths();

        // Determine alert type based on duration
        String alertType = determineAlertType(totalMonths, inscription);

        if (alertType == null) {
            // Should not happen as readers filter correctly, but safety check
            log.warn("Could not determine alert type for enrollment {}", inscription.getId());
            return null;
        }

        // Check if alert has already been sent (idempotence)
        if (alerteDureeEnvoyeeRepository.existsByInscriptionIdAndTypeAlerte(
                inscription.getId(), alertType)) {
            log.debug("Alert {} already sent for enrollment {}, skipping",
                    alertType, inscription.getId());
            return null;
        }

        // Build AlertEventDTO
        AlertEventDTO alertEvent = buildAlertEvent(inscription, alertType, period, totalMonths);

        log.info("Created {} alert for doctorant {} (enrollment {})",
                alertType, inscription.getDoctorantNom(), inscription.getId());

        return alertEvent;
    }

    /**
     * Determines the alert type based on duration and enrollment status.
     * 
     * @param totalMonths Total months since first enrollment
     * @param inscription The enrollment record
     * @return Alert type (3_ANS, 6_ANS, DEPASSEMENT) or null if no alert needed
     */
    private String determineAlertType(int totalMonths, Inscription inscription) {
        // Check for exceeded 6-year limit (>= 72 months without exceptional dérogation)
        if (totalMonths >= SIX_YEAR_THRESHOLD_MONTHS &&
                !Boolean.TRUE.equals(inscription.getDerogationExceptionnelle())) {
            return ALERT_TYPE_DEPASSEMENT;
        }

        // Check for 6-year threshold (69-71 months)
        if (totalMonths >= 69 && totalMonths < SIX_YEAR_THRESHOLD_MONTHS) {
            return ALERT_TYPE_6_ANS;
        }

        // Check for 3-year threshold (33-35 months without dérogation)
        if (totalMonths >= 33 && totalMonths < THREE_YEAR_THRESHOLD_MONTHS &&
                !Boolean.TRUE.equals(inscription.getDerogationAccordee())) {
            return ALERT_TYPE_3_ANS;
        }

        return null;
    }

    /**
     * Builds AlertEventDTO with all required fields.
     * 
     * @param inscription The enrollment record
     * @param alertType   The alert type
     * @param period      The period since first enrollment
     * @param totalMonths Total months since first enrollment
     * @return Fully populated AlertEventDTO
     */
    private AlertEventDTO buildAlertEvent(Inscription inscription, String alertType,
            Period period, int totalMonths) {
        // Format current duration
        String dureeActuelle = formatDuration(period);

        // Calculate time remaining (null for DEPASSEMENT)
        String tempsRestant = calculateTimeRemaining(alertType, totalMonths);

        // Determine threshold
        String seuil = alertType.equals(ALERT_TYPE_3_ANS) ? "3 ans" : "6 ans";

        // Determine action required
        String actionRequise = determineActionRequise(alertType);

        // Determine priority
        String priority = determinePriority(alertType);

        return AlertEventDTO.builder()
                .type("ALERTE_DUREE_" + alertType)
                .doctorantEmail(inscription.getDoctorantEmail())
                .doctorantNom(inscription.getDoctorantNom())
                .directeurEmail(inscription.getDirecteurEmail())
                .datePremiereInscription(inscription.getDatePremiereInscription())
                .dureeActuelle(dureeActuelle)
                .tempsRestant(tempsRestant)
                .seuil(seuil)
                .actionRequise(actionRequise)
                .priority(priority)
                .inscriptionId(inscription.getId())
                .doctorantId(inscription.getDoctorantId())
                .build();
    }

    /**
     * Formats duration as "X ans Y mois".
     * 
     * @param period The period to format
     * @return Formatted duration string
     */
    private String formatDuration(Period period) {
        int years = period.getYears();
        int months = period.getMonths();

        if (years == 0) {
            return months + " mois";
        } else if (months == 0) {
            return years + " an" + (years > 1 ? "s" : "");
        } else {
            return years + " an" + (years > 1 ? "s" : "") + " " + months + " mois";
        }
    }

    /**
     * Calculates time remaining until threshold.
     * 
     * @param alertType   The alert type
     * @param totalMonths Total months since first enrollment
     * @return Formatted time remaining or null for DEPASSEMENT
     */
    private String calculateTimeRemaining(String alertType, int totalMonths) {
        if (alertType.equals(ALERT_TYPE_DEPASSEMENT)) {
            return null; // Already exceeded
        }

        int targetMonths = alertType.equals(ALERT_TYPE_3_ANS) ? THREE_YEAR_THRESHOLD_MONTHS : SIX_YEAR_THRESHOLD_MONTHS;

        int remainingMonths = targetMonths - totalMonths;

        if (remainingMonths <= 0) {
            return "0 mois";
        } else if (remainingMonths == 1) {
            return "1 mois";
        } else {
            return remainingMonths + " mois";
        }
    }

    /**
     * Determines the action required based on alert type.
     * 
     * @param alertType The alert type
     * @return Action required message
     */
    private String determineActionRequise(String alertType) {
        return switch (alertType) {
            case ALERT_TYPE_3_ANS ->
                "Demander dérogation avant expiration du délai de 3 ans";
            case ALERT_TYPE_6_ANS ->
                "Planifier soutenance dans les délais ou demander dérogation exceptionnelle";
            case ALERT_TYPE_DEPASSEMENT ->
                "Régularisation urgente requise - Délai de 6 ans dépassé";
            default -> "Action requise";
        };
    }

    /**
     * Determines the priority level based on alert type.
     * 
     * @param alertType The alert type
     * @return Priority level
     */
    private String determinePriority(String alertType) {
        return switch (alertType) {
            case ALERT_TYPE_3_ANS -> PRIORITY_NORMAL;
            case ALERT_TYPE_6_ANS -> PRIORITY_HIGH;
            case ALERT_TYPE_DEPASSEMENT -> PRIORITY_URGENT;
            default -> PRIORITY_NORMAL;
        };
    }
}
