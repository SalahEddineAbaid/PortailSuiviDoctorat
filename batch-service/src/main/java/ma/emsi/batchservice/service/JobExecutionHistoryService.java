package ma.emsi.batchservice.service;

import ma.emsi.batchservice.entity.JobExecutionHistory;
import ma.emsi.batchservice.repository.JobExecutionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing job execution history.
 * Provides functionality for recording job execution details, retrieving
 * history,
 * and calculating metrics.
 */
@Service
public class JobExecutionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutionHistoryService.class);

    private final JobExecutionHistoryRepository repository;

    public JobExecutionHistoryService(JobExecutionHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Record a job execution in the history table.
     *
     * @param jobExecution Spring Batch JobExecution object
     * @return Saved JobExecutionHistory entity
     */
    @Transactional
    public JobExecutionHistory recordJobExecution(JobExecution jobExecution) {
        logger.debug("Recording job execution for: {}", jobExecution.getJobInstance().getJobName());

        // Calculate total items processed and failed across all steps
        int totalItemsProcessed = 0;
        int totalItemsFailed = 0;

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            totalItemsProcessed += stepExecution.getReadCount();
            totalItemsFailed += stepExecution.getWriteSkipCount() + stepExecution.getProcessSkipCount();
        }

        // Build the history record
        LocalDateTime startTime = jobExecution.getStartTime() != null
                ? jobExecution.getStartTime()
                : LocalDateTime.now();
        LocalDateTime endTime = jobExecution.getEndTime() != null
                ? jobExecution.getEndTime()
                : LocalDateTime.now();

        JobExecutionHistory history = JobExecutionHistory.builder()
                .jobName(jobExecution.getJobInstance().getJobName())
                .startTime(startTime)
                .endTime(endTime)
                .status(jobExecution.getStatus().name())
                .exitMessage(jobExecution.getExitStatus().getExitDescription())
                .itemsProcessed(totalItemsProcessed)
                .itemsFailed(totalItemsFailed)
                .build();

        JobExecutionHistory saved = repository.save(history);
        logger.info("Recorded job execution history: jobName={}, status={}, itemsProcessed={}, itemsFailed={}",
                saved.getJobName(), saved.getStatus(), saved.getItemsProcessed(), saved.getItemsFailed());

        return saved;
    }

    /**
     * Get execution history for a specific job.
     *
     * @param jobName Name of the job
     * @return List of execution history records
     */
    public List<JobExecutionHistory> getJobHistory(String jobName) {
        logger.debug("Retrieving execution history for job: {}", jobName);
        return repository.findByJobNameOrderByStartTimeDesc(jobName);
    }

    /**
     * Get execution history within a date range.
     *
     * @param start Start date/time
     * @param end   End date/time
     * @return List of execution history records
     */
    public List<JobExecutionHistory> getHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        logger.debug("Retrieving execution history between {} and {}", start, end);
        return repository.findByStartTimeBetween(start, end);
    }

    /**
     * Get all failed executions.
     *
     * @return List of failed execution history records
     */
    public List<JobExecutionHistory> getFailedExecutions() {
        logger.debug("Retrieving all failed executions");
        return repository.findByStatus("FAILED");
    }

    /**
     * Calculate metrics for a specific job.
     *
     * @param jobName Name of the job
     * @return Map of metric names to values
     */
    public Map<String, Object> calculateJobMetrics(String jobName) {
        logger.debug("Calculating metrics for job: {}", jobName);

        List<JobExecutionHistory> executions = repository.findByJobNameOrderByStartTimeDesc(jobName);

        Map<String, Object> metrics = new HashMap<>();

        if (executions.isEmpty()) {
            metrics.put("totalExecutions", 0);
            metrics.put("successfulExecutions", 0);
            metrics.put("failedExecutions", 0);
            metrics.put("successRate", 0.0);
            metrics.put("averageDuration", "N/A");
            metrics.put("totalItemsProcessed", 0);
            metrics.put("averageItemsProcessed", 0.0);
            return metrics;
        }

        // Calculate counts
        long totalExecutions = executions.size();
        long successfulExecutions = executions.stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()))
                .count();
        long failedExecutions = executions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .count();

        // Calculate success rate
        double successRate = (double) successfulExecutions / totalExecutions * 100.0;

        // Calculate average duration
        List<Duration> durations = executions.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .map(e -> Duration.between(e.getStartTime(), e.getEndTime()))
                .toList();

        String averageDuration = "N/A";
        if (!durations.isEmpty()) {
            long averageSeconds = durations.stream()
                    .mapToLong(Duration::getSeconds)
                    .sum() / durations.size();
            long minutes = averageSeconds / 60;
            long seconds = averageSeconds % 60;
            averageDuration = String.format("%dm %ds", minutes, seconds);
        }

        // Calculate items processed
        long totalItemsProcessed = executions.stream()
                .filter(e -> e.getItemsProcessed() != null)
                .mapToLong(e -> e.getItemsProcessed())
                .sum();

        double averageItemsProcessed = executions.stream()
                .filter(e -> e.getItemsProcessed() != null)
                .mapToInt(e -> e.getItemsProcessed())
                .average()
                .orElse(0.0);

        // Build metrics map
        metrics.put("totalExecutions", totalExecutions);
        metrics.put("successfulExecutions", successfulExecutions);
        metrics.put("failedExecutions", failedExecutions);
        metrics.put("successRate", successRate);
        metrics.put("averageDuration", averageDuration);
        metrics.put("totalItemsProcessed", totalItemsProcessed);
        metrics.put("averageItemsProcessed", averageItemsProcessed);

        // Last execution info
        if (!executions.isEmpty()) {
            JobExecutionHistory lastExecution = executions.get(0);
            metrics.put("lastExecutionStatus", lastExecution.getStatus());
            metrics.put("lastExecutionTime", lastExecution.getStartTime());
        }

        return metrics;
    }

    /**
     * Calculate global metrics across all jobs.
     *
     * @return Map of global metric names to values
     */
    public Map<String, Object> calculateGlobalMetrics() {
        logger.debug("Calculating global metrics");

        List<JobExecutionHistory> allExecutions = repository.findAll();

        Map<String, Object> metrics = new HashMap<>();

        if (allExecutions.isEmpty()) {
            metrics.put("totalExecutions", 0);
            metrics.put("successfulExecutions", 0);
            metrics.put("failedExecutions", 0);
            metrics.put("successRate", 0.0);
            metrics.put("averageDuration", "N/A");
            metrics.put("totalItemsProcessed", 0);
            return metrics;
        }

        // Calculate counts
        long totalExecutions = allExecutions.size();
        long successfulExecutions = allExecutions.stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()))
                .count();
        long failedExecutions = allExecutions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .count();

        // Calculate success rate
        double successRate = (double) successfulExecutions / totalExecutions * 100.0;

        // Calculate average duration
        List<Duration> durations = allExecutions.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .map(e -> Duration.between(e.getStartTime(), e.getEndTime()))
                .toList();

        String averageDuration = "N/A";
        if (!durations.isEmpty()) {
            long averageSeconds = durations.stream()
                    .mapToLong(Duration::getSeconds)
                    .sum() / durations.size();
            long minutes = averageSeconds / 60;
            long seconds = averageSeconds % 60;
            averageDuration = String.format("%dm %ds", minutes, seconds);
        }

        // Calculate total items processed
        long totalItemsProcessed = allExecutions.stream()
                .filter(e -> e.getItemsProcessed() != null)
                .mapToLong(e -> e.getItemsProcessed())
                .sum();

        // Build metrics map
        metrics.put("totalExecutions", totalExecutions);
        metrics.put("successfulExecutions", successfulExecutions);
        metrics.put("failedExecutions", failedExecutions);
        metrics.put("successRate", successRate);
        metrics.put("averageDuration", averageDuration);
        metrics.put("totalItemsProcessed", totalItemsProcessed);

        // Last failure info
        allExecutions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .max((e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()))
                .ifPresent(lastFailure -> {
                    metrics.put("lastFailureJobName", lastFailure.getJobName());
                    metrics.put("lastFailureTime", lastFailure.getStartTime());
                    metrics.put("lastFailureMessage", lastFailure.getExitMessage());
                });

        return metrics;
    }

    /**
     * Get the most recent execution for a specific job.
     *
     * @param jobName Name of the job
     * @return Most recent execution, or null if none found
     */
    public JobExecutionHistory getLastExecution(String jobName) {
        logger.debug("Retrieving last execution for job: {}", jobName);

        List<JobExecutionHistory> executions = repository.findByJobNameOrderByStartTimeDesc(jobName);
        return executions.isEmpty() ? null : executions.get(0);
    }

    /**
     * Delete old execution history records (for cleanup).
     *
     * @param olderThan Delete records older than this date/time
     * @return Number of records deleted
     */
    @Transactional
    public int deleteOldHistory(LocalDateTime olderThan) {
        logger.info("Deleting execution history older than: {}", olderThan);

        List<JobExecutionHistory> oldRecords = repository.findAll().stream()
                .filter(e -> e.getStartTime().isBefore(olderThan))
                .toList();

        int count = oldRecords.size();
        repository.deleteAll(oldRecords);

        logger.info("Deleted {} old execution history records", count);
        return count;
    }
}
