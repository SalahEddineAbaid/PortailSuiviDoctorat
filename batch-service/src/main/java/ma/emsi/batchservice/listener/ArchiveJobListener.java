package ma.emsi.batchservice.listener;

import ma.emsi.batchservice.service.BatchMetricsService;
import ma.emsi.batchservice.service.JobExecutionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Listener for the archive job.
 * Tracks metrics including:
 * - Number of enrollments archived
 * - Number of defenses archived
 * - Disk space freed
 * - Execution duration
 * 
 * Requirements: 4.13, 8.10
 */
@Component
public class ArchiveJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveJobListener.class);

    private final JobExecutionHistoryService jobExecutionHistoryService;
    private final BatchMetricsService batchMetricsService;

    public ArchiveJobListener(JobExecutionHistoryService jobExecutionHistoryService,
            BatchMetricsService batchMetricsService) {
        this.jobExecutionHistoryService = jobExecutionHistoryService;
        this.batchMetricsService = batchMetricsService;
    }

    private LocalDateTime startTime;
    private long enrollmentsArchived = 0;
    private long defensesArchived = 0;
    private long diskSpaceFreed = 0;
    private int applicationLogsDeleted = 0;
    private int notificationLogsDeleted = 0;
    private int tablesOptimized = 0;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = LocalDateTime.now();
        enrollmentsArchived = 0;
        defensesArchived = 0;
        diskSpaceFreed = 0;
        applicationLogsDeleted = 0;
        notificationLogsDeleted = 0;
        tablesOptimized = 0;

        // Record job start in metrics
        batchMetricsService.recordJobStart("archiveJob");

        logger.info("Archive job started at {}", startTime);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Collect metrics from step executions
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        for (StepExecution stepExecution : stepExecutions) {
            String stepName = stepExecution.getStepName();

            if (stepName.equals("archiveEnrollmentsStep")) {
                enrollmentsArchived = stepExecution.getWriteCount();
                // Calculate disk space freed from execution context
                Long uncompressedSize = (Long) stepExecution.getExecutionContext().get("uncompressedSize");
                Long compressedSize = (Long) stepExecution.getExecutionContext().get("compressedSize");
                if (uncompressedSize != null && compressedSize != null) {
                    diskSpaceFreed += (uncompressedSize - compressedSize);
                }
            } else if (stepName.equals("archiveDefensesStep")) {
                defensesArchived = stepExecution.getWriteCount();
                // Calculate disk space freed from execution context
                Long uncompressedSize = (Long) stepExecution.getExecutionContext().get("uncompressedSize");
                Long compressedSize = (Long) stepExecution.getExecutionContext().get("compressedSize");
                if (uncompressedSize != null && compressedSize != null) {
                    diskSpaceFreed += (uncompressedSize - compressedSize);
                }
            } else if (stepName.equals("cleanupLogsStep")) {
                Integer appLogs = (Integer) stepExecution.getExecutionContext().get("applicationLogsDeleted");
                Integer notifLogs = (Integer) stepExecution.getExecutionContext().get("notificationLogsDeleted");
                if (appLogs != null) {
                    applicationLogsDeleted = appLogs;
                }
                if (notifLogs != null) {
                    notificationLogsDeleted = notifLogs;
                }
            } else if (stepName.equals("optimizeDatabaseStep")) {
                Integer tables = (Integer) stepExecution.getExecutionContext().get("tablesOptimized");
                if (tables != null) {
                    tablesOptimized = tables;
                }
            }
        }

        // Log summary
        String status = jobExecution.getStatus().toString();
        logger.info("Archive job completed with status: {}", status);
        logger.info("Execution duration: {} minutes {} seconds",
                duration.toMinutes(), duration.toSecondsPart());
        logger.info("Enrollments archived: {}", enrollmentsArchived);
        logger.info("Defenses archived: {}", defensesArchived);
        logger.info("Disk space freed: {} MB", diskSpaceFreed / (1024 * 1024));
        logger.info("Application logs deleted: {}", applicationLogsDeleted);
        logger.info("Notification logs deleted: {}", notificationLogsDeleted);
        logger.info("Database tables optimized: {}", tablesOptimized);

        // Store metrics in job execution context for potential retrieval
        jobExecution.getExecutionContext().putLong("enrollmentsArchived", enrollmentsArchived);
        jobExecution.getExecutionContext().putLong("defensesArchived", defensesArchived);
        jobExecution.getExecutionContext().putLong("diskSpaceFreed", diskSpaceFreed);
        jobExecution.getExecutionContext().putInt("applicationLogsDeleted", applicationLogsDeleted);
        jobExecution.getExecutionContext().putInt("notificationLogsDeleted", notificationLogsDeleted);
        jobExecution.getExecutionContext().putInt("tablesOptimized", tablesOptimized);
        jobExecution.getExecutionContext().putLong("durationSeconds", duration.getSeconds());

        // Record execution history using service (Requirement 8.10)
        jobExecutionHistoryService.recordJobExecution(jobExecution);

        // Record metrics
        int totalItemsProcessed = (int) (enrollmentsArchived + defensesArchived);
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchMetricsService.recordJobSuccess("archiveJob", duration.toMillis(), totalItemsProcessed);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            batchMetricsService.recordJobFailure("archiveJob", duration.toMillis());
            logger.error("Archive job failed: {}", jobExecution.getAllFailureExceptions());
        }
    }
}
