package ma.emsi.batchservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduling batch jobs.
 * 
 * This class enables Spring's scheduling support and defines scheduled methods
 * for each batch job with their respective CRON expressions.
 * 
 * Job Schedules:
 * - Token Cleanup: Daily at 2:00 AM (CRON: "0 0 2 * * ?")
 * - Duration Alert: Every Monday at 8:00 AM (CRON: "0 0 8 ? * MON")
 * - Monthly Report: 1st of each month at 9:00 AM (CRON: "0 0 9 1 * ?")
 * - Archive: Quarterly on 1st at 3:00 AM (CRON: "0 0 3 1 1,4,7,10 ?")
 * - Consistency Check: Daily at 11:00 PM (CRON: "0 0 23 * * ?")
 * 
 * Each scheduled method uses JobLauncher to execute the corresponding job
 * with unique JobParameters (timestamp) to ensure each execution is treated
 * as a new instance.
 * 
 * Requirements: 1.1, 1.8, 2.1, 2.9, 3.1, 3.10, 4.1, 4.12, 5.1, 5.15
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class JobSchedulerConfig {

    private final JobLauncher jobLauncher;

    @Qualifier("tokenCleanupJob")
    private final Job tokenCleanupJob;

    @Qualifier("dureeDoctoratAlertJob")
    private final Job dureeDoctoratAlertJob;

    @Qualifier("monthlyReportJob")
    private final Job monthlyReportJob;

    @Qualifier("archiveJob")
    private final Job archiveJob;

    @Qualifier("dataConsistencyJob")
    private final Job dataConsistencyJob;

    /**
     * Scheduled execution of token cleanup job.
     * 
     * Runs daily at 2:00 AM to delete expired refresh tokens and password reset
     * tokens from the user database.
     * 
     * CRON Expression: "0 0 2 * * ?"
     * - Second: 0
     * - Minute: 0
     * - Hour: 2 (2:00 AM)
     * - Day of Month: * (every day)
     * - Month: * (every month)
     * - Day of Week: ? (no specific day)
     * 
     * Requirements: 1.1, 1.8
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runTokenCleanupJob() {
        try {
            log.info("Starting scheduled execution of tokenCleanupJob");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(tokenCleanupJob, jobParameters);
            log.info("Completed scheduled execution of tokenCleanupJob");
        } catch (Exception e) {
            log.error("Error executing scheduled tokenCleanupJob", e);
        }
    }

    /**
     * Scheduled execution of doctoral duration alert job.
     * 
     * Runs every Monday at 8:00 AM to check doctoral duration thresholds
     * and send alerts for students approaching 3-year, 6-year, or exceeding
     * 6-year limits.
     * 
     * CRON Expression: "0 0 8 ? * MON"
     * - Second: 0
     * - Minute: 0
     * - Hour: 8 (8:00 AM)
     * - Day of Month: ? (no specific day)
     * - Month: * (every month)
     * - Day of Week: MON (Monday)
     * 
     * Requirements: 2.1, 2.9
     */
    @Scheduled(cron = "0 0 8 ? * MON")
    public void runDureeDoctoratAlertJob() {
        try {
            log.info("Starting scheduled execution of dureeDoctoratAlertJob");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(dureeDoctoratAlertJob, jobParameters);
            log.info("Completed scheduled execution of dureeDoctoratAlertJob");
        } catch (Exception e) {
            log.error("Error executing scheduled dureeDoctoratAlertJob", e);
        }
    }

    /**
     * Scheduled execution of monthly report generation job.
     * 
     * Runs on the 1st of each month at 9:00 AM to collect statistics from
     * all services, generate a comprehensive PDF report, and distribute it
     * to administrators.
     * 
     * CRON Expression: "0 0 9 1 * ?"
     * - Second: 0
     * - Minute: 0
     * - Hour: 9 (9:00 AM)
     * - Day of Month: 1 (1st of month)
     * - Month: * (every month)
     * - Day of Week: ? (no specific day)
     * 
     * Requirements: 3.1, 3.10
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    public void runMonthlyReportJob() {
        try {
            log.info("Starting scheduled execution of monthlyReportJob");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(monthlyReportJob, jobParameters);
            log.info("Completed scheduled execution of monthlyReportJob");
        } catch (Exception e) {
            log.error("Error executing scheduled monthlyReportJob", e);
        }
    }

    /**
     * Scheduled execution of archive job.
     * 
     * Runs quarterly (January, April, July, October) on the 1st at 3:00 AM
     * to archive old enrollment and defense records, clean up logs, and
     * optimize database tables.
     * 
     * CRON Expression: "0 0 3 1 1,4,7,10 ?"
     * - Second: 0
     * - Minute: 0
     * - Hour: 3 (3:00 AM)
     * - Day of Month: 1 (1st of month)
     * - Month: 1,4,7,10 (January, April, July, October)
     * - Day of Week: ? (no specific day)
     * 
     * Requirements: 4.1, 4.12
     */
    @Scheduled(cron = "0 0 3 1 1,4,7,10 ?")
    public void runArchiveJob() {
        try {
            log.info("Starting scheduled execution of archiveJob");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(archiveJob, jobParameters);
            log.info("Completed scheduled execution of archiveJob");
        } catch (Exception e) {
            log.error("Error executing scheduled archiveJob", e);
        }
    }

    /**
     * Scheduled execution of data consistency verification job.
     * 
     * Runs daily at 11:00 PM to verify and correct data inconsistencies
     * across microservice databases, including user-enrollment consistency,
     * enrollment-defense consistency, user role synchronization, orphaned
     * document detection, and pending notification retries.
     * 
     * CRON Expression: "0 0 23 * * ?"
     * - Second: 0
     * - Minute: 0
     * - Hour: 23 (11:00 PM)
     * - Day of Month: * (every day)
     * - Month: * (every month)
     * - Day of Week: ? (no specific day)
     * 
     * Requirements: 5.1, 5.15
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void runDataConsistencyJob() {
        try {
            log.info("Starting scheduled execution of dataConsistencyJob");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(dataConsistencyJob, jobParameters);
            log.info("Completed scheduled execution of dataConsistencyJob");
        } catch (Exception e) {
            log.error("Error executing scheduled dataConsistencyJob", e);
        }
    }
}
