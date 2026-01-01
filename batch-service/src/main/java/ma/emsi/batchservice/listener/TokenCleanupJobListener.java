package ma.emsi.batchservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.event.JobFailureEventDTO;
import ma.emsi.batchservice.service.BatchMetricsService;
import ma.emsi.batchservice.service.JobExecutionHistoryService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * JobExecutionListener for token cleanup job.
 * Tracks metrics for refresh tokens and password reset tokens deleted.
 * Records execution history and publishes Kafka events on failure.
 * 
 * Validates: Requirements 1.5, 1.6, 1.7, 8.10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJobListener implements JobExecutionListener {

    private final JobExecutionHistoryService jobExecutionHistoryService;
    private final BatchMetricsService batchMetricsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private LocalDateTime startTime;
    private int refreshTokensDeleted = 0;
    private int passwordResetTokensDeleted = 0;

    /**
     * Called before job execution starts.
     * Initializes metrics and records start time.
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = LocalDateTime.now();
        refreshTokensDeleted = 0;
        passwordResetTokensDeleted = 0;

        // Record job start in metrics
        batchMetricsService.recordJobStart("tokenCleanupJob");

        log.info("Starting token cleanup job - Job ID: {}", jobExecution.getJobId());
    }

    /**
     * Called after job execution completes.
     * Logs deletion counts, records metrics, and publishes failure notification if
     * needed.
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Calculate total tokens deleted from step execution contexts
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            String stepName = stepExecution.getStepName();
            long writeCount = stepExecution.getWriteCount();

            if ("cleanupRefreshTokensStep".equals(stepName)) {
                refreshTokensDeleted = (int) writeCount;
            } else if ("cleanupPasswordResetTokensStep".equals(stepName)) {
                passwordResetTokensDeleted = (int) writeCount;
            }
        });

        int totalDeleted = refreshTokensDeleted + passwordResetTokensDeleted;

        // Log deletion counts (Requirement 1.5)
        log.info("Token cleanup job completed - Status: {}", jobExecution.getStatus());
        log.info("Refresh tokens deleted: {}", refreshTokensDeleted);
        log.info("Password reset tokens deleted: {}", passwordResetTokensDeleted);
        log.info("Total tokens deleted: {}", totalDeleted);
        log.info("Execution duration: {} seconds", duration.getSeconds());

        // Record execution history using service (Requirement 1.7, 8.10)
        jobExecutionHistoryService.recordJobExecution(jobExecution);

        // Record metrics
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchMetricsService.recordJobSuccess("tokenCleanupJob", duration.toMillis(), totalDeleted);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            batchMetricsService.recordJobFailure("tokenCleanupJob", duration.toMillis());
            // Publish Kafka event on failure (Requirement 1.6)
            publishFailureNotification(jobExecution);
        }
    }

    /**
     * Publishes a Kafka event to notify admins of job failure.
     */
    private void publishFailureNotification(JobExecution jobExecution) {
        try {
            JobFailureEventDTO event = new JobFailureEventDTO();
            event.setType("JOB_FAILURE");
            event.setJobName("tokenCleanupJob");
            event.setExecutionId(jobExecution.getJobId());
            event.setFailureTime(LocalDateTime.now());
            event.setExitCode(jobExecution.getExitStatus().getExitCode());
            event.setExitMessage(jobExecution.getExitStatus().getExitDescription());
            event.setPriority("URGENT");
            event.setRestartable(true);
            event.setSuggestedAction(
                    "Review logs for detailed error information and manually restart job after resolving issue");

            // Extract exception message if available
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                Throwable exception = jobExecution.getAllFailureExceptions().get(0);
                event.setExceptionMessage(exception.getMessage());
                event.setExceptionClass(exception.getClass().getName());
            }

            kafkaTemplate.send("notifications", "job-failure", event);
            log.info("Published failure notification to Kafka for token cleanup job");
        } catch (Exception e) {
            log.error("Failed to publish failure notification to Kafka", e);
        }
    }
}
