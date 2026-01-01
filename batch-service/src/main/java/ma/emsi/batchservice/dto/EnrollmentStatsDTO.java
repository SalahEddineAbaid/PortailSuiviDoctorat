package ma.emsi.batchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for enrollment statistics collected for monthly reports.
 * Contains comprehensive enrollment metrics for the previous month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatsDTO {

    /**
     * Total number of enrollments in the period
     */
    private Long totalEnrollments;

    /**
     * Distribution of enrollments by status (EN_ATTENTE, VALIDÉ, REJETÉ, etc.)
     * Key: status name, Value: count
     */
    private Map<String, Long> statusDistribution;

    /**
     * Number of reinscriptions (students re-enrolling)
     */
    private Long reinscriptionsCount;

    /**
     * Number of dérogations (special authorizations) granted
     */
    private Long derogationsGranted;

    /**
     * Number of dérogations requested
     */
    private Long derogationsRequested;

    /**
     * Distribution of enrollments by discipline
     * Key: discipline name, Value: count
     */
    private Map<String, Long> disciplineDistribution;

    /**
     * Distribution of enrollments by laboratory
     * Key: laboratory name, Value: count
     */
    private Map<String, Long> laboratoryDistribution;

    /**
     * Average processing time in days from submission to validation/rejection
     */
    private Double averageProcessingTimeDays;

    /**
     * Director validation rate (percentage of enrollments validated by directors)
     */
    private Double directorValidationRate;

    /**
     * Admin validation rate (percentage of enrollments validated by admins)
     */
    private Double adminValidationRate;
}
