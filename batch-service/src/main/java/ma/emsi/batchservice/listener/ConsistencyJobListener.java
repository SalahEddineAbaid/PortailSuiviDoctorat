package ma.emsi.batchservice.listener;

import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.service.BatchMetricsService;
import ma.emsi.batchservice.service.JobExecutionHistoryService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Listener for the data consistency verification job.
 * Tracks metrics including:
 * - Total anomalies detected
 * - Anomalies auto-corrected
 * - Anomalies requiring manual intervention
 * - Execution duration
 * 
 * Metrics Collection:
 * - User-enrollment anomalies
 * - Enrollment-defense anomalies
 * - Role synchronization changes
 * - Orphaned documents
 * - Notification retry results
 * 
 * Requirements: 5.16, 8.10
 */
@Slf4j
@Component
public class ConsistencyJobListener implements JobExecutionListener {

    private final JobExecutionHistoryService jobExecutionHistoryService;
    private final BatchMetricsService batchMetricsService;

    public ConsistencyJobListener(JobExecutionHistoryService jobExecutionHistoryService,
            BatchMetricsService batchMetricsService) {
        this.jobExecutionHistoryService = jobExecutionHistoryService;
        this.batchMetricsService = batchMetricsService;
    }

    private LocalDateTime startTime;
    private int totalAnomalies = 0;
    private int autoCorrected = 0;
    private int manualInterventionRequired = 0;

    // Detailed metrics
    private int userEnrollmentAnomalies = 0;
    private int enrollmentDefenseAnomalies = 0;
    private int rolesAdded = 0;
    private int rolesTransitioned = 0;
    private int orphanedDocuments = 0;
    private int notificationRetrySuccess = 0;
    private int notificationRetryFailure = 0;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = LocalDateTime.now();
        totalAnomalies = 0;
        autoCorrected = 0;
        manualInterventionRequired = 0;
        userEnrollmentAnomalies = 0;
        enrollmentDefenseAnomalies = 0;
        rolesAdded = 0;
        rolesTransitioned = 0;
        orphanedDocuments = 0;
        notificationRetrySuccess = 0;
        notificationRetryFailure = 0;

        // Record job start in metrics
        batchMetricsService.recordJobStart("dataConsistencyJob");

        log.info("Data consistency verification job started at {}", startTime);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Collect metrics from step executions
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        for (StepExecution stepExecution : stepExecutions) {
            String stepName = stepExecution.getStepName();

            if (stepName.equals("verifyUserEnrollmentConsistencyStep")) {
                userEnrollmentAnomalies = getIntFromContext(stepExecution, "userEnrollmentAnomalies");
                int corrected = getIntFromContext(stepExecution, "userEnrollmentCorrected");
                autoCorrected += corrected;
                // User-enrollment anomalies require manual review after suspension
                manualInterventionRequired += userEnrollmentAnomalies;
            } else if (stepName.equals("verifyEnrollmentDefenseConsistencyStep")) {
                enrollmentDefenseAnomalies = getIntFromContext(stepExecution, "enrollmentDefenseAnomalies");
                int corrected = getIntFromContext(stepExecution, "enrollmentDefenseCorrected");
                autoCorrected += corrected;
                // Enrollment-defense anomalies require manual review after blocking
                manualInterventionRequired += enrollmentDefenseAnomalies;
            } else if (stepName.equals("verifyUserRolesStep")) {
                rolesAdded = getIntFromContext(stepExecution, "rolesAdded");
                rolesTransitioned = getIntFromContext(stepExecution, "rolesTransitioned");
                // Role synchronization is auto-corrected, no manual intervention needed
                autoCorrected += (rolesAdded + rolesTransitioned);
            } else if (stepName.equals("checkOrphanedDocumentsStep")) {
                orphanedDocuments = getIntFromContext(stepExecution, "orphanedDocuments");
                // Orphaned documents are moved but require manual review
                autoCorrected += orphanedDocuments;
                manualInterventionRequired += orphanedDocuments;
            } else if (stepName.equals("retryPendingNotificationsStep")) {
                notificationRetrySuccess = getIntFromContext(stepExecution, "notificationRetrySuccess");
                notificationRetryFailure = getIntFromContext(stepExecution, "notificationRetryFailure");
                // Successful retries are auto-corrected
                autoCorrected += notificationRetrySuccess;
                // Failed retries require manual intervention
                manualInterventionRequired += notificationRetryFailure;
            }
        }

        // Calculate total anomalies
        totalAnomalies = userEnrollmentAnomalies + enrollmentDefenseAnomalies +
                rolesAdded + rolesTransitioned + orphanedDocuments +
                notificationRetrySuccess + notificationRetryFailure;

        // Log summary
        String status = jobExecution.getStatus().toString();
        log.info("Data consistency verification job completed with status: {}", status);
        log.info("Execution duration: {} minutes {} seconds",
                duration.toMinutes(), duration.toSecondsPart());
        log.info("=== Consistency Check Summary ===");
        log.info("Total anomalies detected: {}", totalAnomalies);
        log.info("Anomalies auto-corrected: {}", autoCorrected);
        log.info("Anomalies requiring manual intervention: {}", manualInterventionRequired);
        log.info("=== Detailed Breakdown ===");
        log.info("User-enrollment anomalies: {}", userEnrollmentAnomalies);
        log.info("Enrollment-defense anomalies: {}", enrollmentDefenseAnomalies);
        log.info("Roles added: {}", rolesAdded);
        log.info("Roles transitioned: {}", rolesTransitioned);
        log.info("Orphaned documents: {}", orphanedDocuments);
        log.info("Notification retry success: {}", notificationRetrySuccess);
        log.info("Notification retry failure: {}", notificationRetryFailure);

        // Store metrics in job execution context for potential retrieval
        jobExecution.getExecutionContext().putInt("totalAnomalies", totalAnomalies);
        jobExecution.getExecutionContext().putInt("autoCorrected", autoCorrected);
        jobExecution.getExecutionContext().putInt("manualInterventionRequired", manualInterventionRequired);
        jobExecution.getExecutionContext().putInt("userEnrollmentAnomalies", userEnrollmentAnomalies);
        jobExecution.getExecutionContext().putInt("enrollmentDefenseAnomalies", enrollmentDefenseAnomalies);
        jobExecution.getExecutionContext().putInt("rolesAdded", rolesAdded);
        jobExecution.getExecutionContext().putInt("rolesTransitioned", rolesTransitioned);
        jobExecution.getExecutionContext().putInt("orphanedDocuments", orphanedDocuments);
        jobExecution.getExecutionContext().putInt("notificationRetrySuccess", notificationRetrySuccess);
        jobExecution.getExecutionContext().putInt("notificationRetryFailure", notificationRetryFailure);
        jobExecution.getExecutionContext().putLong("durationSeconds", duration.getSeconds());

        // Record execution history using service (Requirement 8.10)
        jobExecutionHistoryService.recordJobExecution(jobExecution);

        // Record metrics
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchMetricsService.recordJobSuccess("dataConsistencyJob", duration.toMillis(), totalAnomalies);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            batchMetricsService.recordJobFailure("dataConsistencyJob", duration.toMillis());
            log.error("Data consistency verification job failed: {}", jobExecution.getAllFailureExceptions());
        }

        // Log warning if manual intervention is required
        if (manualInterventionRequired > 0) {
            log.warn("ATTENTION: {} anomalies require manual intervention", manualInterventionRequired);
            log.warn("Please review the anomaly report for details");
        }
    }

    /**
     * Helper method to safely retrieve integer values from step execution context.
     */
    private int getIntFromContext(StepExecution stepExecution, String key) {
        Integer value = (Integer) stepExecution.getExecutionContext().get(key);
        return value != null ? value : 0;
    }
}
