package ma.emsi.batchservice.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tasklet for cleaning up old log files and notification logs.
 * 
 * Cleanup rules:
 * - Delete application logs older than 6 months
 * - Delete notification logs older than 3 months (successful only)
 * - Retain all error logs indefinitely
 * 
 * Requirements: 4.6, 4.7
 */
@Component
public class CleanupLogsTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(CleanupLogsTasklet.class);

    private final JdbcTemplate notificationJdbcTemplate;

    @Value("${batch.archive.retention.logs.application.months:6}")
    private int applicationLogRetentionMonths;

    @Value("${batch.archive.retention.logs.notification.months:3}")
    private int notificationLogRetentionMonths;

    @Value("${logging.file.name:logs/batch-service.log}")
    private String logFilePath;

    public CleanupLogsTasklet(@Qualifier("notificationJdbcTemplate") JdbcTemplate notificationJdbcTemplate) {
        this.notificationJdbcTemplate = notificationJdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info("Starting log cleanup tasklet");

        int applicationLogsDeleted = cleanupApplicationLogs();
        int notificationLogsDeleted = cleanupNotificationLogs();

        // Store metrics in execution context
        chunkContext.getStepContext()
                .getStepExecution()
                .getExecutionContext()
                .putInt("applicationLogsDeleted", applicationLogsDeleted);
        chunkContext.getStepContext()
                .getStepExecution()
                .getExecutionContext()
                .putInt("notificationLogsDeleted", notificationLogsDeleted);

        logger.info("Log cleanup completed: {} application logs deleted, {} notification logs deleted",
                applicationLogsDeleted, notificationLogsDeleted);

        return RepeatStatus.FINISHED;
    }

    /**
     * Cleans up application log files older than the retention period.
     * Retains all error logs indefinitely.
     * 
     * @return Number of log files deleted
     */
    private int cleanupApplicationLogs() {
        int deletedCount = 0;

        try {
            // Get the log directory
            File logFile = new File(logFilePath);
            File logDir = logFile.getParentFile();

            if (logDir == null || !logDir.exists() || !logDir.isDirectory()) {
                logger.warn("Log directory not found: {}", logDir);
                return 0;
            }

            // Calculate cutoff date
            LocalDate cutoffDate = LocalDate.now().minusMonths(applicationLogRetentionMonths);
            logger.info("Deleting application logs older than {} (cutoff: {})",
                    applicationLogRetentionMonths + " months", cutoffDate);

            // List all log files
            File[] logFiles = logDir
                    .listFiles((dir, name) -> name.endsWith(".log") || name.matches(".*\\.log\\.\\d{4}-\\d{2}-\\d{2}"));

            if (logFiles == null) {
                logger.warn("No log files found in directory: {}", logDir);
                return 0;
            }

            for (File file : logFiles) {
                try {
                    // Skip error log files (retain indefinitely)
                    if (file.getName().contains("error")) {
                        logger.debug("Skipping error log file: {}", file.getName());
                        continue;
                    }

                    // Check if file is older than cutoff date
                    LocalDateTime fileModifiedDate = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(file.lastModified()),
                            java.time.ZoneId.systemDefault());

                    if (fileModifiedDate.toLocalDate().isBefore(cutoffDate)) {
                        if (file.delete()) {
                            deletedCount++;
                            logger.debug("Deleted old log file: {}", file.getName());
                        } else {
                            logger.warn("Failed to delete log file: {}", file.getName());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing log file {}: {}", file.getName(), e.getMessage());
                }
            }

            logger.info("Deleted {} application log files", deletedCount);
        } catch (Exception e) {
            logger.error("Error during application log cleanup: {}", e.getMessage(), e);
        }

        return deletedCount;
    }

    /**
     * Cleans up notification logs older than the retention period.
     * Only deletes successfully sent notifications.
     * Retains all failed notifications indefinitely.
     * 
     * @return Number of notification log records deleted
     */
    private int cleanupNotificationLogs() {
        try {
            // Calculate cutoff date
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(notificationLogRetentionMonths);
            logger.info("Deleting notification logs older than {} (cutoff: {})",
                    notificationLogRetentionMonths + " months", cutoffDate);

            // Delete only successful notifications older than cutoff
            // Retain all failed/error notifications indefinitely
            String sql = "DELETE FROM notification_log " +
                    "WHERE status = 'SENT' " +
                    "  AND sent_date < ? " +
                    "  AND (error_message IS NULL OR error_message = '')";

            int deletedCount = notificationJdbcTemplate.update(sql, cutoffDate);

            logger.info("Deleted {} notification log records", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            logger.error("Error during notification log cleanup: {}", e.getMessage(), e);
            // Don't fail the entire job if notification log cleanup fails
            return 0;
        }
    }
}
