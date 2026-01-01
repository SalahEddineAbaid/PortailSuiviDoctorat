package ma.emsi.batchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for user statistics collected for monthly reports.
 * Contains comprehensive user metrics for the previous month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    /**
     * Total number of active users
     */
    private Long totalActiveUsers;

    /**
     * Distribution of users by role (ADMIN, DOCTORANT, DIRECTEUR, etc.)
     * Key: role name, Value: count
     */
    private Map<String, Long> roleDistribution;

    /**
     * Number of new users registered in the period
     */
    private Long newUsersCount;

    /**
     * Connection rate (percentage of users who logged in during the period)
     */
    private Double connectionRate;
}
