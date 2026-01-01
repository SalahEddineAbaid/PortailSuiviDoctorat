package ma.emsi.batchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for notification statistics collected for monthly reports.
 * Contains comprehensive notification metrics for the previous month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDTO {

    /**
     * Total number of notifications sent
     */
    private Long totalNotificationsSent;

    /**
     * Distribution of notifications by type (EMAIL, SMS, PUSH, etc.)
     * Key: notification type, Value: count
     */
    private Map<String, Long> distributionByType;

    /**
     * Success rate (percentage of notifications successfully delivered)
     */
    private Double successRate;

    /**
     * Number of failed notifications
     */
    private Long failedNotificationsCount;

    /**
     * Average send time in milliseconds
     */
    private Double averageSendTimeMs;
}
