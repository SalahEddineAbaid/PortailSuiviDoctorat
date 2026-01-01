package ma.emsi.batchservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.service.BatchMetricsService;
import ma.emsi.batchservice.service.JobExecutionHistoryService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Listener for monthly report generation job.
 * Records metrics including PDF size, generation duration, and send status.
 * 
 * Requirements: 3.11, 8.10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportJobListener implements JobExecutionListener {

    private final JobExecutionHistoryService jobExecutionHistoryService;
    private final BatchMetricsService batchMetricsService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private LocalDateTime startTime;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = LocalDateTime.now();

        // Record job start in metrics
        batchMetricsService.recordJobStart("monthlyReportJob");

        log.info("Monthly report job started: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime endTime = LocalDateTime.now();
        long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();

        log.info("Monthly report job completed with status: {}", jobExecution.getStatus());

        // Retrieve metrics from execution context
        var executionContext = jobExecution.getExecutionContext();
        String pdfFilePath = (String) executionContext.get("pdfFilePath");
        Integer emailSuccessCount = (Integer) executionContext.get("emailSuccessCount");
        Integer emailFailureCount = (Integer) executionContext.get("emailFailureCount");

        // Calculate PDF size
        long pdfSizeBytes = 0;
        if (pdfFilePath != null) {
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                pdfSizeBytes = pdfFile.length();
            }
        }

        // Record execution history using service (Requirement 8.10)
        jobExecutionHistoryService.recordJobExecution(jobExecution);

        log.info("Monthly report metrics - Duration: {}ms, PDF Size: {} bytes, Emails Sent: {}, Emails Failed: {}",
                durationMillis, pdfSizeBytes,
                emailSuccessCount != null ? emailSuccessCount : 0,
                emailFailureCount != null ? emailFailureCount : 0);

        // Record metrics
        int itemsProcessed = emailSuccessCount != null ? emailSuccessCount : 0;
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            batchMetricsService.recordJobSuccess("monthlyReportJob", durationMillis, itemsProcessed);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            batchMetricsService.recordJobFailure("monthlyReportJob", durationMillis);
            // Send failure notification
            sendFailureNotification(jobExecution, endTime);
        }
    }

    /**
     * Send failure notification to Kafka.
     */
    private void sendFailureNotification(JobExecution jobExecution, LocalDateTime endTime) {
        try {
            Map<String, Object> failureEvent = new HashMap<>();
            failureEvent.put("type", "JOB_FAILURE");
            failureEvent.put("jobName", jobExecution.getJobInstance().getJobName());
            failureEvent.put("failureTime", endTime.toString());
            failureEvent.put("exitMessage", jobExecution.getExitStatus().getExitDescription());
            failureEvent.put("priority", "URGENT");

            kafkaTemplate.send("notifications", "job-failure", failureEvent);
            log.info("Job failure notification sent to Kafka");
        } catch (Exception e) {
            log.error("Failed to send job failure notification", e);
        }
    }
}
