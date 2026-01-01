package ma.emsi.batchservice.config;

import ma.emsi.batchservice.listener.ConsistencyJobListener;
import ma.emsi.batchservice.tasklet.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration for the data consistency verification job.
 * 
 * Job structure:
 * 1. verifyUserEnrollmentConsistencyStep - Check user-enrollment consistency
 * (tasklet)
 * 2. verifyEnrollmentDefenseConsistencyStep - Check enrollment-defense
 * consistency (tasklet)
 * 3. verifyUserRolesStep - Synchronize user roles (tasklet)
 * 4. checkOrphanedDocumentsStep - Identify and move orphaned files (tasklet)
 * 5. retryPendingNotificationsStep - Retry stale pending notifications
 * (tasklet)
 * 6. generateAnomalyReportStep - Generate PDF report if anomalies detected
 * (tasklet, conditional)
 * 
 * Scheduling: Daily at 11:00 PM (CRON: "0 0 23 * * ?")
 * 
 * All steps are tasklet-based for complex verification logic.
 * The anomaly report step executes conditionally only if anomalies are
 * detected.
 * 
 * Requirements: 5.1, 5.15
 */
@Configuration
public class DataConsistencyJobConfig {

    /**
     * Data consistency verification job definition.
     * Executes daily to verify and correct data inconsistencies across services.
     */
    @Bean
    public Job dataConsistencyJob(
            JobRepository jobRepository,
            @Qualifier("verifyUserEnrollmentConsistencyStep") Step verifyUserEnrollmentConsistencyStep,
            @Qualifier("verifyEnrollmentDefenseConsistencyStep") Step verifyEnrollmentDefenseConsistencyStep,
            @Qualifier("verifyUserRolesStep") Step verifyUserRolesStep,
            @Qualifier("checkOrphanedDocumentsStep") Step checkOrphanedDocumentsStep,
            @Qualifier("retryPendingNotificationsStep") Step retryPendingNotificationsStep,
            @Qualifier("generateAnomalyReportStep") Step generateAnomalyReportStep,
            ConsistencyJobListener consistencyJobListener) {

        return new JobBuilder("dataConsistencyJob", jobRepository)
                .listener(consistencyJobListener)
                .start(verifyUserEnrollmentConsistencyStep)
                .next(verifyEnrollmentDefenseConsistencyStep)
                .next(verifyUserRolesStep)
                .next(checkOrphanedDocumentsStep)
                .next(retryPendingNotificationsStep)
                .next(generateAnomalyReportStep)
                .build();
    }

    /**
     * Step 1: Verify user-enrollment consistency.
     * Checks that each enrollment has a valid user in user-service.
     * For missing users: logs anomaly, marks enrollment SUSPENDU, notifies admin.
     */
    @Bean
    public Step verifyUserEnrollmentConsistencyStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            VerifyUserEnrollmentConsistencyTasklet verifyUserEnrollmentConsistencyTasklet) {

        return new StepBuilder("verifyUserEnrollmentConsistencyStep", jobRepository)
                .tasklet(verifyUserEnrollmentConsistencyTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2: Verify enrollment-defense consistency.
     * Checks that each defense request has a valid enrollment with status VALIDÉ.
     * For invalid: logs anomaly, blocks defense, notifies director and admin.
     */
    @Bean
    public Step verifyEnrollmentDefenseConsistencyStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            VerifyEnrollmentDefenseConsistencyTasklet verifyEnrollmentDefenseConsistencyTasklet) {

        return new StepBuilder("verifyEnrollmentDefenseConsistencyStep", jobRepository)
                .tasklet(verifyEnrollmentDefenseConsistencyTasklet, transactionManager)
                .build();
    }

    /**
     * Step 3: Verify and synchronize user roles.
     * Adds ROLE_DOCTORANT_ACTIF to users with validated enrollments.
     * Transitions users with completed defenses to ROLE_DOCTEUR.
     */
    @Bean
    public Step verifyUserRolesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            VerifyUserRolesTasklet verifyUserRolesTasklet) {

        return new StepBuilder("verifyUserRolesStep", jobRepository)
                .tasklet(verifyUserRolesTasklet, transactionManager)
                .build();
    }

    /**
     * Step 4: Check for orphaned documents.
     * Lists all files in uploads directory and identifies files without database
     * references.
     * Moves orphaned files to uploads/orphelins directory.
     */
    @Bean
    public Step checkOrphanedDocumentsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CheckOrphanedDocumentsTasklet checkOrphanedDocumentsTasklet) {

        return new StepBuilder("checkOrphanedDocumentsStep", jobRepository)
                .tasklet(checkOrphanedDocumentsTasklet, transactionManager)
                .build();
    }

    /**
     * Step 5: Retry pending notifications.
     * Queries notifications with status PENDING older than 24 hours.
     * Retries sending each notification.
     * If retry fails, marks as FAILED and notifies technical admin.
     */
    @Bean
    public Step retryPendingNotificationsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RetryPendingNotificationsTasklet retryPendingNotificationsTasklet) {

        return new StepBuilder("retryPendingNotificationsStep", jobRepository)
                .tasklet(retryPendingNotificationsTasklet, transactionManager)
                .build();
    }

    /**
     * Step 6: Generate anomaly report (conditional).
     * Checks if anomalies were detected in previous steps.
     * If yes, generates PDF anomaly report with type, count, and corrective
     * actions.
     * Sends report via email to technical admin.
     * 
     * This step always executes but only generates a report if anomalies are
     * detected.
     */
    @Bean
    public Step generateAnomalyReportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            GenerateAnomalyReportTasklet generateAnomalyReportTasklet) {

        return new StepBuilder("generateAnomalyReportStep", jobRepository)
                .tasklet(generateAnomalyReportTasklet, transactionManager)
                .build();
    }
}
