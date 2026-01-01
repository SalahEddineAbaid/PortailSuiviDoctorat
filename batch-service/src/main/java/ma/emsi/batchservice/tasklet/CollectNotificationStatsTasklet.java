package ma.emsi.batchservice.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.NotificationStatsDTO;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tasklet to collect notification statistics from notification database for the
 * previous month.
 * Stores the collected statistics in the job execution context for use by
 * subsequent steps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectNotificationStatsTasklet implements Tasklet {

    @Qualifier("notificationJdbcTemplate")
    private final JdbcTemplate notificationJdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting notification statistics collection...");

        // Calculate previous month date range
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = previousMonth.atDay(1);
        LocalDate endDate = previousMonth.atEndOfMonth();

        log.info("Collecting notification statistics for period: {} to {}", startDate, endDate);

        // Collect total notifications sent
        Long totalNotificationsSent = notificationJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Collect distribution by type
        Map<String, Long> distributionByType = new HashMap<>();
        List<Map<String, Object>> typeResults = notificationJdbcTemplate.queryForList(
                "SELECT type, COUNT(*) as count FROM notification " +
                        "WHERE DATE(created_at) BETWEEN ? AND ? GROUP BY type",
                startDate, endDate);
        for (Map<String, Object> row : typeResults) {
            distributionByType.put((String) row.get("type"), ((Number) row.get("count")).longValue());
        }

        // Calculate success rate
        Long successfulNotifications = notificationJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE status = 'SENT' " +
                        "AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Double successRate = totalNotificationsSent > 0
                ? (successfulNotifications.doubleValue() / totalNotificationsSent.doubleValue()) * 100
                : 0.0;

        // Collect failed notifications count
        Long failedNotificationsCount = notificationJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE status = 'FAILED' " +
                        "AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Calculate average send time
        Double averageSendTimeMs = notificationJdbcTemplate.queryForObject(
                "SELECT AVG(send_duration_ms) FROM notification " +
                        "WHERE status = 'SENT' AND DATE(created_at) BETWEEN ? AND ?",
                Double.class, startDate, endDate);
        if (averageSendTimeMs == null) {
            averageSendTimeMs = 0.0;
        }

        // Build DTO
        NotificationStatsDTO stats = NotificationStatsDTO.builder()
                .totalNotificationsSent(totalNotificationsSent)
                .distributionByType(distributionByType)
                .successRate(successRate)
                .failedNotificationsCount(failedNotificationsCount)
                .averageSendTimeMs(averageSendTimeMs)
                .build();

        // Store in execution context
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("notificationStats", stats);

        log.info("Notification statistics collected successfully: {} total notifications, {:.2f}% success rate",
                totalNotificationsSent, successRate);

        return RepeatStatus.FINISHED;
    }
}
