package ma.emsi.batchservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ma.emsi.batchservice.entity.JobExecutionHistory;
import ma.emsi.batchservice.repository.JobExecutionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing custom batch job metrics exposed via Micrometer.
 * Provides metrics for job executions, success/failure rates, durations, and
 * items processed.
 */
@Service
public class BatchMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(BatchMetricsService.class);

    private final MeterRegistry meterRegistry;
    private final JobExecutionHistoryRepository executionHistoryRepository;
    private final JobOperator jobOperator;

    // Job names for metrics
    private static final List<String> JOB_NAMES = Arrays.asList(
            "tokenCleanupJob",
            "dureeDoctoratAlertJob",
            "monthlyReportJob",
            "archiveJob",
            "dataConsistencyJob");

    // Counters for each job
    private final Map<String, Counter> executionCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> durationTimers = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> itemsProcessedGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> lastStatusGauges = new ConcurrentHashMap<>();

    // Global metrics
    private final AtomicInteger activeJobsCount = new AtomicInteger(0);
    private final AtomicInteger scheduledJobsCount = new AtomicInteger(JOB_NAMES.size());

    public BatchMetricsService(MeterRegistry meterRegistry,
            JobExecutionHistoryRepository executionHistoryRepository,
            JobOperator jobOperator) {
        this.meterRegistry = meterRegistry;
        this.executionHistoryRepository = executionHistoryRepository;
        this.jobOperator = jobOperator;

        initializeMetrics();
        logger.info("BatchMetricsService initialized with metrics for {} jobs", JOB_NAMES.size());
    }

    /**
     * Initialize all metrics for each job.
     */
    private void initializeMetrics() {
        for (String jobName : JOB_NAMES) {
            // Execution counter
            executionCounters.put(jobName,
                    Counter.builder("batch.job.executions.total")
                            .tag("job", jobName)
                            .description("Total number of executions for job " + jobName)
                            .register(meterRegistry));

            // Success counter
            successCounters.put(jobName,
                    Counter.builder("batch.job.executions.success")
                            .tag("job", jobName)
                            .description("Number of successful executions for job " + jobName)
                            .register(meterRegistry));

            // Failure counter
            failureCounters.put(jobName,
                    Counter.builder("batch.job.executions.failure")
                            .tag("job", jobName)
                            .description("Number of failed executions for job " + jobName)
                            .register(meterRegistry));

            // Duration timer
            durationTimers.put(jobName,
                    Timer.builder("batch.job.duration")
                            .tag("job", jobName)
                            .description("Execution duration for job " + jobName)
                            .register(meterRegistry));

            // Items processed gauge
            AtomicLong itemsProcessed = new AtomicLong(0);
            itemsProcessedGauges.put(jobName, itemsProcessed);
            Gauge.builder("batch.job.items.processed", itemsProcessed, AtomicLong::get)
                    .tag("job", jobName)
                    .description("Total items processed by job " + jobName)
                    .register(meterRegistry);

            // Last execution status gauge (0 = unknown, 1 = success, 2 = failed, 3 =
            // running)
            AtomicInteger lastStatus = new AtomicInteger(0);
            lastStatusGauges.put(jobName, lastStatus);
            Gauge.builder("batch.job.last.status", lastStatus, AtomicInteger::get)
                    .tag("job", jobName)
                    .description(
                            "Last execution status for job " + jobName + " (0=unknown, 1=success, 2=failed, 3=running)")
                    .register(meterRegistry);
        }

        // Global metrics
        Gauge.builder("batch.jobs.active", activeJobsCount, AtomicInteger::get)
                .description("Number of currently active (running) jobs")
                .register(meterRegistry);

        Gauge.builder("batch.jobs.scheduled", scheduledJobsCount, AtomicInteger::get)
                .description("Number of scheduled jobs")
                .register(meterRegistry);

        // Global success rate gauge
        Gauge.builder("batch.jobs.success.rate", this, BatchMetricsService::calculateGlobalSuccessRate)
                .description("Overall success rate across all jobs (percentage)")
                .register(meterRegistry);

        logger.info("Initialized metrics for jobs: {}", JOB_NAMES);
    }

    /**
     * Record a job execution start.
     *
     * @param jobName Name of the job
     */
    public void recordJobStart(String jobName) {
        logger.debug("Recording job start for: {}", jobName);

        Counter executionCounter = executionCounters.get(jobName);
        if (executionCounter != null) {
            executionCounter.increment();
        }

        // Update last status to running
        AtomicInteger lastStatus = lastStatusGauges.get(jobName);
        if (lastStatus != null) {
            lastStatus.set(3); // 3 = running
        }

        updateActiveJobsCount();
    }

    /**
     * Record a successful job execution.
     *
     * @param jobName        Name of the job
     * @param durationMillis Execution duration in milliseconds
     * @param itemsProcessed Number of items processed
     */
    public void recordJobSuccess(String jobName, long durationMillis, int itemsProcessed) {
        logger.debug("Recording job success for: {} (duration: {}ms, items: {})",
                jobName, durationMillis, itemsProcessed);

        Counter successCounter = successCounters.get(jobName);
        if (successCounter != null) {
            successCounter.increment();
        }

        Timer durationTimer = durationTimers.get(jobName);
        if (durationTimer != null) {
            durationTimer.record(durationMillis, TimeUnit.MILLISECONDS);
        }

        AtomicLong itemsGauge = itemsProcessedGauges.get(jobName);
        if (itemsGauge != null) {
            itemsGauge.addAndGet(itemsProcessed);
        }

        // Update last status to success
        AtomicInteger lastStatus = lastStatusGauges.get(jobName);
        if (lastStatus != null) {
            lastStatus.set(1); // 1 = success
        }

        updateActiveJobsCount();
    }

    /**
     * Record a failed job execution.
     *
     * @param jobName        Name of the job
     * @param durationMillis Execution duration in milliseconds
     */
    public void recordJobFailure(String jobName, long durationMillis) {
        logger.debug("Recording job failure for: {} (duration: {}ms)", jobName, durationMillis);

        Counter failureCounter = failureCounters.get(jobName);
        if (failureCounter != null) {
            failureCounter.increment();
        }

        Timer durationTimer = durationTimers.get(jobName);
        if (durationTimer != null) {
            durationTimer.record(durationMillis, TimeUnit.MILLISECONDS);
        }

        // Update last status to failed
        AtomicInteger lastStatus = lastStatusGauges.get(jobName);
        if (lastStatus != null) {
            lastStatus.set(2); // 2 = failed
        }

        updateActiveJobsCount();
    }

    /**
     * Update the count of active (running) jobs.
     */
    private void updateActiveJobsCount() {
        int activeCount = 0;
        for (String jobName : JOB_NAMES) {
            try {
                Set<Long> runningExecutions = jobOperator.getRunningExecutions(jobName);
                if (!runningExecutions.isEmpty()) {
                    activeCount++;
                }
            } catch (NoSuchJobException e) {
                // Job not found, skip
            }
        }
        activeJobsCount.set(activeCount);
    }

    /**
     * Calculate global success rate across all jobs.
     *
     * @return Success rate as a percentage (0-100)
     */
    private double calculateGlobalSuccessRate() {
        List<JobExecutionHistory> allExecutions = executionHistoryRepository.findAll();

        if (allExecutions.isEmpty()) {
            return 0.0;
        }

        long successfulExecutions = allExecutions.stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()))
                .count();

        return (double) successfulExecutions / allExecutions.size() * 100.0;
    }

    /**
     * Get current metric values for a specific job.
     *
     * @param jobName Name of the job
     * @return Map of metric names to values
     */
    public Map<String, Object> getJobMetrics(String jobName) {
        Map<String, Object> metrics = new HashMap<>();

        Counter executionCounter = executionCounters.get(jobName);
        if (executionCounter != null) {
            metrics.put("total_executions", executionCounter.count());
        }

        Counter successCounter = successCounters.get(jobName);
        if (successCounter != null) {
            metrics.put("successful_executions", successCounter.count());
        }

        Counter failureCounter = failureCounters.get(jobName);
        if (failureCounter != null) {
            metrics.put("failed_executions", failureCounter.count());
        }

        Timer durationTimer = durationTimers.get(jobName);
        if (durationTimer != null) {
            metrics.put("average_duration_ms", durationTimer.mean(TimeUnit.MILLISECONDS));
            metrics.put("max_duration_ms", durationTimer.max(TimeUnit.MILLISECONDS));
        }

        AtomicLong itemsProcessed = itemsProcessedGauges.get(jobName);
        if (itemsProcessed != null) {
            metrics.put("total_items_processed", itemsProcessed.get());
        }

        AtomicInteger lastStatus = lastStatusGauges.get(jobName);
        if (lastStatus != null) {
            metrics.put("last_status", getStatusString(lastStatus.get()));
        }

        return metrics;
    }

    /**
     * Get global metrics across all jobs.
     *
     * @return Map of global metric names to values
     */
    public Map<String, Object> getGlobalMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("active_jobs", activeJobsCount.get());
        metrics.put("scheduled_jobs", scheduledJobsCount.get());
        metrics.put("success_rate", calculateGlobalSuccessRate());

        // Calculate total executions across all jobs
        double totalExecutionsDouble = executionCounters.values().stream()
                .mapToDouble(Counter::count)
                .sum();
        metrics.put("total_executions", (long) totalExecutionsDouble);

        // Calculate total successes
        double totalSuccessesDouble = successCounters.values().stream()
                .mapToDouble(Counter::count)
                .sum();
        metrics.put("total_successes", (long) totalSuccessesDouble);

        // Calculate total failures
        double totalFailuresDouble = failureCounters.values().stream()
                .mapToDouble(Counter::count)
                .sum();
        metrics.put("total_failures", (long) totalFailuresDouble);

        return metrics;
    }

    /**
     * Convert status code to string.
     */
    private String getStatusString(int statusCode) {
        switch (statusCode) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "SUCCESS";
            case 2:
                return "FAILED";
            case 3:
                return "RUNNING";
            default:
                return "UNKNOWN";
        }
    }
}
