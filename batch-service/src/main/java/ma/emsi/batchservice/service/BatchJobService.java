package ma.emsi.batchservice.service;

import ma.emsi.batchservice.dto.*;
import ma.emsi.batchservice.entity.JobExecutionHistory;
import ma.emsi.batchservice.repository.JobExecutionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing batch job execution and monitoring.
 * Provides functionality for manual job triggering, execution history
 * retrieval,
 * and global statistics calculation.
 */
@Service
public class BatchJobService {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobService.class);

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final ApplicationContext applicationContext;
    private final JobExecutionHistoryRepository executionHistoryRepository;

    // Job metadata: name -> (description, cron)
    private static final Map<String, JobMetadata> JOB_METADATA = new HashMap<>();

    static {
        JOB_METADATA.put("tokenCleanupJob", new JobMetadata(
                "Token Cleanup Job",
                "Removes expired refresh tokens and password reset tokens from the database",
                "0 0 2 * * ?"));
        JOB_METADATA.put("dureeDoctoratAlertJob", new JobMetadata(
                "Doctoral Duration Alert Job",
                "Monitors doctoral duration and sends alerts at critical thresholds (3 years, 6 years, exceeded)",
                "0 0 8 ? * MON"));
        JOB_METADATA.put("monthlyReportJob", new JobMetadata(
                "Monthly Report Generation Job",
                "Generates comprehensive monthly statistical reports with PDF output",
                "0 0 9 1 * ?"));
        JOB_METADATA.put("archiveJob", new JobMetadata(
                "Archive Job",
                "Archives old enrollment and defense records with encryption",
                "0 0 3 1 1,4,7,10 ?"));
        JOB_METADATA.put("dataConsistencyJob", new JobMetadata(
                "Data Consistency Verification Job",
                "Verifies and corrects data inconsistencies across microservice databases",
                "0 0 23 * * ?"));
    }

    public BatchJobService(JobLauncher jobLauncher,
            JobExplorer jobExplorer,
            JobOperator jobOperator,
            ApplicationContext applicationContext,
            JobExecutionHistoryRepository executionHistoryRepository) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.applicationContext = applicationContext;
        this.executionHistoryRepository = executionHistoryRepository;
    }

    /**
     * Manually trigger a batch job execution.
     *
     * @param jobName Name of the job to execute
     * @return Response containing execution ID, status, start time, and
     *         confirmation message
     * @throws NoSuchJobException if job name is not found
     */
    public JobTriggerResponseDTO triggerJob(String jobName) throws Exception {
        logger.info("Manual trigger requested for job: {}", jobName);

        // Get the job bean from application context
        Job job = applicationContext.getBean(jobName, Job.class);

        // Create unique job parameters to allow multiple executions
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("trigger", "manual")
                .toJobParameters();

        // Launch the job
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        logger.info("Job {} triggered successfully with execution ID: {}", jobName, jobExecution.getId());

        return new JobTriggerResponseDTO(
                jobExecution.getId(),
                jobExecution.getStatus().name(),
                LocalDateTime.now(),
                String.format("Job '%s' has been triggered successfully", jobName));
    }

    /**
     * List all available batch jobs with metadata.
     *
     * @return List of job information including name, description, CRON expression,
     *         last execution, and status
     */
    public List<JobInfoDTO> listJobs() {
        logger.debug("Listing all available batch jobs");

        List<JobInfoDTO> jobInfoList = new ArrayList<>();

        for (Map.Entry<String, JobMetadata> entry : JOB_METADATA.entrySet()) {
            String jobName = entry.getKey();
            JobMetadata metadata = entry.getValue();

            // Get last execution from history
            List<JobExecutionHistory> history = executionHistoryRepository
                    .findByJobNameOrderByStartTimeDesc(jobName);

            LocalDateTime lastExecution = null;
            String lastStatus = null;
            if (!history.isEmpty()) {
                JobExecutionHistory lastRun = history.get(0);
                lastExecution = lastRun.getStartTime();
                lastStatus = lastRun.getStatus();
            }

            // Check if job is currently running
            Boolean isRunning = isJobRunning(jobName);

            jobInfoList.add(new JobInfoDTO(
                    jobName,
                    metadata.description,
                    metadata.cronExpression,
                    lastExecution,
                    lastStatus,
                    isRunning));
        }

        return jobInfoList;
    }

    /**
     * Get paginated execution history for a specific job.
     *
     * @param jobName Job name
     * @param page    Page number (0-indexed)
     * @param size    Page size
     * @return List of job executions
     */
    public List<JobExecutionDTO> getJobExecutionHistory(String jobName, int page, int size) {
        logger.debug("Retrieving execution history for job: {} (page: {}, size: {})", jobName, page, size);

        List<JobExecutionHistory> history = executionHistoryRepository
                .findByJobNameOrderByStartTimeDesc(jobName);

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, history.size());

        if (start >= history.size()) {
            return Collections.emptyList();
        }

        return history.subList(start, end).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed execution information for a specific execution ID.
     *
     * @param executionId Execution ID
     * @return Detailed execution information
     */
    public JobExecutionDTO getExecutionDetails(Long executionId) {
        logger.debug("Retrieving execution details for ID: {}", executionId);

        JobExecutionHistory execution = executionHistoryRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

        return convertToDTO(execution);
    }

    /**
     * Stop a running job execution.
     *
     * @param executionId Execution ID to stop
     * @return Confirmation message
     */
    public String stopJobExecution(Long executionId) throws Exception {
        logger.info("Attempting to stop job execution: {}", executionId);

        try {
            jobOperator.stop(executionId);
            logger.info("Job execution {} stopped successfully", executionId);
            return String.format("Job execution %d has been stopped", executionId);
        } catch (Exception e) {
            logger.error("Failed to stop job execution {}: {}", executionId, e.getMessage());
            throw new RuntimeException("Failed to stop job execution: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate global statistics across all jobs.
     *
     * @return Global statistics including total executions, success rate, average
     *         duration, etc.
     */
    public GlobalStatsDTO getGlobalStatistics() {
        logger.debug("Calculating global batch job statistics");

        List<JobExecutionHistory> allExecutions = executionHistoryRepository.findAll();

        long totalExecutions = allExecutions.size();
        long successfulExecutions = allExecutions.stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()))
                .count();
        long failedExecutions = allExecutions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .count();

        double successRate = totalExecutions > 0
                ? (double) successfulExecutions / totalExecutions * 100
                : 0.0;

        // Calculate average duration
        String averageDuration = calculateAverageDuration(allExecutions);

        // Get last failure
        JobFailureDTO lastFailure = getLastFailure(allExecutions);

        return new GlobalStatsDTO(
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                successRate,
                averageDuration,
                lastFailure);
    }

    /**
     * Check if a job is currently running.
     */
    private Boolean isJobRunning(String jobName) {
        try {
            Set<Long> runningExecutions = jobOperator.getRunningExecutions(jobName);
            return !runningExecutions.isEmpty();
        } catch (NoSuchJobException e) {
            return false;
        }
    }

    /**
     * Convert JobExecutionHistory entity to DTO.
     */
    private JobExecutionDTO convertToDTO(JobExecutionHistory execution) {
        String duration = formatDuration(execution.getStartTime(), execution.getEndTime());

        return new JobExecutionDTO(
                execution.getId(),
                execution.getJobName(),
                execution.getStatus(),
                execution.getStartTime(),
                execution.getEndTime(),
                duration,
                execution.getItemsProcessed(),
                execution.getItemsFailed(),
                execution.getExitMessage());
    }

    /**
     * Format duration between start and end times.
     */
    private String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "N/A";
        }

        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        return String.format("%dm %ds", minutes, seconds);
    }

    /**
     * Calculate average duration across all executions.
     */
    private String calculateAverageDuration(List<JobExecutionHistory> executions) {
        List<Duration> durations = executions.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .map(e -> Duration.between(e.getStartTime(), e.getEndTime()))
                .collect(Collectors.toList());

        if (durations.isEmpty()) {
            return "N/A";
        }

        long averageSeconds = durations.stream()
                .mapToLong(Duration::getSeconds)
                .sum() / durations.size();

        long minutes = averageSeconds / 60;
        long seconds = averageSeconds % 60;

        return String.format("%dm %ds", minutes, seconds);
    }

    /**
     * Get the last failure from execution history.
     */
    private JobFailureDTO getLastFailure(List<JobExecutionHistory> executions) {
        return executions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .max(Comparator.comparing(JobExecutionHistory::getStartTime))
                .map(e -> new JobFailureDTO(
                        e.getJobName(),
                        e.getStartTime(),
                        e.getExitMessage()))
                .orElse(null);
    }

    /**
     * Inner class to hold job metadata.
     */
    private static class JobMetadata {
        final String description;
        final String cronExpression;

        JobMetadata(String name, String description, String cronExpression) {
            this.description = description;
            this.cronExpression = cronExpression;
        }
    }
}
