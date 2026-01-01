package ma.emsi.batchservice.config;

import ma.emsi.batchservice.entity.JobExecutionHistory;
import ma.emsi.batchservice.repository.JobExecutionHistoryRepository;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Custom InfoContributor that adds batch job information to the /actuator/info
 * endpoint.
 * Provides details about scheduled jobs, execution counts, and last execution
 * status.
 */
@Component
public class BatchJobInfoContributor implements InfoContributor {

    private final JobExecutionHistoryRepository executionHistoryRepository;

    private static final List<String> JOB_NAMES = Arrays.asList(
            "tokenCleanupJob",
            "dureeDoctoratAlertJob",
            "monthlyReportJob",
            "archiveJob",
            "dataConsistencyJob");

    private static final Map<String, String> JOB_DESCRIPTIONS = new HashMap<>();
    private static final Map<String, String> JOB_SCHEDULES = new HashMap<>();

    static {
        JOB_DESCRIPTIONS.put("tokenCleanupJob", "Removes expired refresh tokens and password reset tokens");
        JOB_DESCRIPTIONS.put("dureeDoctoratAlertJob", "Monitors doctoral duration and sends alerts");
        JOB_DESCRIPTIONS.put("monthlyReportJob", "Generates comprehensive monthly statistical reports");
        JOB_DESCRIPTIONS.put("archiveJob", "Archives old enrollment and defense records");
        JOB_DESCRIPTIONS.put("dataConsistencyJob", "Verifies and corrects data inconsistencies");

        JOB_SCHEDULES.put("tokenCleanupJob", "Daily at 2:00 AM (0 0 2 * * ?)");
        JOB_SCHEDULES.put("dureeDoctoratAlertJob", "Every Monday at 8:00 AM (0 0 8 ? * MON)");
        JOB_SCHEDULES.put("monthlyReportJob", "1st of each month at 9:00 AM (0 0 9 1 * ?)");
        JOB_SCHEDULES.put("archiveJob", "Quarterly on 1st at 3:00 AM (0 0 3 1 1,4,7,10 ?)");
        JOB_SCHEDULES.put("dataConsistencyJob", "Daily at 11:00 PM (0 0 23 * * ?)");
    }

    public BatchJobInfoContributor(JobExecutionHistoryRepository executionHistoryRepository) {
        this.executionHistoryRepository = executionHistoryRepository;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> batchInfo = new HashMap<>();

        // Add service information
        batchInfo.put("service", "Batch Service");
        batchInfo.put("description", "Automated batch processing and maintenance tasks for doctoral management portal");
        batchInfo.put("totalScheduledJobs", JOB_NAMES.size());

        // Add job details
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (String jobName : JOB_NAMES) {
            Map<String, Object> jobInfo = new HashMap<>();
            jobInfo.put("name", jobName);
            jobInfo.put("description", JOB_DESCRIPTIONS.get(jobName));
            jobInfo.put("schedule", JOB_SCHEDULES.get(jobName));

            // Get execution statistics
            List<JobExecutionHistory> executions = executionHistoryRepository
                    .findByJobNameOrderByStartTimeDesc(jobName);

            if (!executions.isEmpty()) {
                long totalExecutions = executions.size();
                long successfulExecutions = executions.stream()
                        .filter(e -> "COMPLETED".equals(e.getStatus()))
                        .count();
                long failedExecutions = executions.stream()
                        .filter(e -> "FAILED".equals(e.getStatus()))
                        .count();

                jobInfo.put("totalExecutions", totalExecutions);
                jobInfo.put("successfulExecutions", successfulExecutions);
                jobInfo.put("failedExecutions", failedExecutions);

                // Last execution info
                JobExecutionHistory lastExecution = executions.get(0);
                Map<String, Object> lastExecutionInfo = new HashMap<>();
                lastExecutionInfo.put("status", lastExecution.getStatus());
                lastExecutionInfo.put("startTime", lastExecution.getStartTime());
                lastExecutionInfo.put("endTime", lastExecution.getEndTime());
                lastExecutionInfo.put("itemsProcessed", lastExecution.getItemsProcessed());

                jobInfo.put("lastExecution", lastExecutionInfo);
            } else {
                jobInfo.put("totalExecutions", 0);
                jobInfo.put("successfulExecutions", 0);
                jobInfo.put("failedExecutions", 0);
                jobInfo.put("lastExecution", null);
            }

            jobs.add(jobInfo);
        }

        batchInfo.put("jobs", jobs);

        // Add global statistics
        List<JobExecutionHistory> allExecutions = executionHistoryRepository.findAll();
        Map<String, Object> globalStats = new HashMap<>();
        globalStats.put("totalExecutions", allExecutions.size());
        globalStats.put("successfulExecutions", allExecutions.stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()))
                .count());
        globalStats.put("failedExecutions", allExecutions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .count());

        if (!allExecutions.isEmpty()) {
            double successRate = (double) allExecutions.stream()
                    .filter(e -> "COMPLETED".equals(e.getStatus()))
                    .count() / allExecutions.size() * 100.0;
            globalStats.put("successRate", String.format("%.2f%%", successRate));
        } else {
            globalStats.put("successRate", "N/A");
        }

        batchInfo.put("globalStatistics", globalStats);

        builder.withDetail("batch", batchInfo);
    }
}
