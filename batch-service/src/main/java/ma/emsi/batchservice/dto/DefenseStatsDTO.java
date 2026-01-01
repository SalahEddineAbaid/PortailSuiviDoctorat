package ma.emsi.batchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for defense statistics collected for monthly reports.
 * Contains comprehensive defense metrics for the previous month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefenseStatsDTO {

    /**
     * Total number of defense requests submitted
     */
    private Long defenseRequestsCount;

    /**
     * Number of completed defenses
     */
    private Long completedDefensesCount;

    /**
     * Distribution of defenses by mention (HONORABLE, TRÈS HONORABLE, etc.)
     * Key: mention name, Value: count
     */
    private Map<String, Long> mentionDistribution;

    /**
     * Total number of juries formed
     */
    private Long juryCount;

    /**
     * Number of jury reports submitted
     */
    private Long submittedReportsCount;

    /**
     * Jury member acceptance rate (percentage of invitations accepted)
     */
    private Double juryMemberAcceptanceRate;

    /**
     * Average time in days from defense request to authorization
     */
    private Double averageTimeRequestToAuthorizationDays;

    /**
     * Average time in days from authorization to actual defense
     */
    private Double averageTimeAuthorizationToDefenseDays;
}
