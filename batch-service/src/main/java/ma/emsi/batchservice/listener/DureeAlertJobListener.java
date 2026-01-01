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
 * JobExecutionListener for duration alert job.
 * 
 * Tracks metrics for:
 * - 3-year threshold alerts sent
 * - 6-year threshold alerts sent
 * - Exceeded duration alerts sent (DEPASSEMENT)
 * 
 * Records execution history and publishes Kafka events on failure.
 * 
 * Requirements: 2.10, 8.10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DureeAlertJobListener implements JobExecutionListener {

    private final JobExecutionHistoryService jobExecutionHistoryService;
    private final BatchMetricsService batchMetricsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private LocalDateTime startTime;
    private int threeYearAlerts = 0;
    private int sixYearAlerts = 0;
    private int exceededAlerts = 0;

    /**
     * Called before job execution starts.
     * Initializes metrics and records start time.
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = LocalDateTime.now();
        threeYearAlerts = 0;
        sixYearAlerts = 0;
        exceededAlerts = 0;

        // Record job start in metrics
        batchMetricsService.recordJobStart("dureeDoctoratAlertJob");

        log.info("Starting duration alert job - Job ID: {}", jobExecution.getJobId());
    }

    /**
     * Called after job execution completes.
     * Logs alert counts, records metrics, and publishes failure notification if
     * needed.
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Calculate alert counts from step execution contexts
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            String stepName = stepExecution.getStepName();
            long writeCount = stepExecution.getWriteCount();

            switch (stepName) {
                case "check3YearThresholdStep" -> threeYearAlerts = (int) writeCount;
                case "check6YearThresholdStep" -> sixYearAlerts = (int) writeCount;
                case "checkExceeded6YearStep" -> exceededAlerts = (int) writeCount;
            }
        });

        int totalAlerts = threeYearAlerts + sixYearAlerts + exceededAlerts;

        // Log alert counts (Requirement 2.10)
        log.info("Duration alert job completed - Status: {}", jobExecution.getStatus());
        log.info("3-year threshold alerts sent: {}", threeYearAlerts);
        log.info("6-year threshold alerts sent: {}", sixYearAlerts);
        log.info("Exceeded duration alerts sent (DEPASSEMENT): {}", exceededAlerts);
        log.info("Total alerts sent: {}", totalAlerts);
        log.info("Execution duration: {} seconds", duration.getSeconds());

        // Record execution history using service (Requirement 8.10)
        jobExecutionHistoryService.recordJobExecution(jobExecution);

        // Record metrics
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchMetricsService.recordJobSuccess("dureeDoctoratAlertJob", duration.toMillis(), totalAlerts);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            batchMetricsService.recordJobFailure("dureeDoctoratAlertJob", duration.toMillis());
            // Publish Kafka event on failure
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
            event.setJobName("dureeDoctoratAlertJob");
            event.setExecutionId(jobExecution.getJobId());
            event.setFailureTime(LocalDateTime.now());
            event.setExitCode(jobExecution.getExitStatus().getExitCode());
            event.setExitMessage(jobExecution.getExitStatus().getExitDescription());
            event.setPriority("URGENT");
            event.setRestartable(true);
            event.setSuggestedAction(
                    "Review logs for detailed error information. Check database connectivity and Kafka availability. " +
                            "Manually restart job after resolving issue.");

            // Extract exception message if available
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                Throwable exception = jobExecution.getAllFailureExceptions().get(0);
                event.setExceptionMessage(exception.getMessage());
                event.setExceptionClass(exception.getClass().getName());
            }

            kafkaTemplate.send("notifications", "job-failure", event);
            log.info("Published failure notification to Kafka for duration alert job");
        } catch (Exception e) {
            log.error("Failed to publish failure notification to Kafka", e);
        }
    }

    /**
     * Gets the count of 3-year threshold alerts sent.
     * Used for testing and monitoring.
     */
    public int getThreeYearAlerts() {
        return threeYearAlerts;
    }

    /**
     * Gets the count of 6-year threshold alerts sent.
     * Used for testing and monitoring.
     */
    public int getSixYearAlerts() {
        return sixYearAlerts;
    }

    /**
     * Gets the count of exceeded duration alerts sent.
     * Used for testing and monitoring.
     */
    public int getExceededAlerts() {
        return exceededAlerts;
    }
}
