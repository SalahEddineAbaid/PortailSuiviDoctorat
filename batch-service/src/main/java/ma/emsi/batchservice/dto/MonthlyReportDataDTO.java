package ma.emsi.batchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Aggregated DTO containing all statistics for monthly report generation.
 * This DTO is passed through the job execution context between tasklets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDataDTO {

    /**
     * Enrollment statistics for the report period
     */
    private EnrollmentStatsDTO enrollmentStats;

    /**
     * Defense statistics for the report period
     */
    private DefenseStatsDTO defenseStats;

    /**
     * Notification statistics for the report period
     */
    private NotificationStatsDTO notificationStats;

    /**
     * User statistics for the report period
     */
    private UserStatsDTO userStats;

    /**
     * The month for which this report is generated (first day of the month)
     */
    private LocalDate reportMonth;
}
