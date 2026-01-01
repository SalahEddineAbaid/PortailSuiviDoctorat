package ma.emsi.batchservice.config;

import lombok.RequiredArgsConstructor;
import ma.emsi.batchservice.listener.ReportJobListener;
import ma.emsi.batchservice.tasklet.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration for the monthly report generation batch job.
 * This job collects statistics from all services, generates a PDF report,
 * and distributes it to administrators.
 * 
 * Schedule: Monthly on the 1st at 9:00 AM (CRON: "0 0 9 1 * ?")
 */
@Configuration
@RequiredArgsConstructor
public class MonthlyReportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // Tasklets
    private final CollectEnrollmentStatsTasklet collectEnrollmentStatsTasklet;
    private final CollectDefenseStatsTasklet collectDefenseStatsTasklet;
    private final CollectNotificationStatsTasklet collectNotificationStatsTasklet;
    private final CollectUserStatsTasklet collectUserStatsTasklet;
    private final GeneratePdfTasklet generatePdfTasklet;
    private final SendReportNotificationTasklet sendReportNotificationTasklet;

    // Listener
    private final ReportJobListener reportJobListener;

    /**
     * Monthly report generation job.
     * Executes six tasklet steps in sequence to collect statistics,
     * generate PDF, and send notifications.
     */
    @Bean
    public Job monthlyReportJob() {
        return new JobBuilder("monthlyReportJob", jobRepository)
                .listener(reportJobListener)
                .start(collectEnrollmentStatsStep())
                .next(collectDefenseStatsStep())
                .next(collectNotificationStatsStep())
                .next(collectUserStatsStep())
                .next(generatePdfStep())
                .next(sendReportNotificationStep())
                .build();
    }

    /**
     * Step 1: Collect enrollment statistics from inscription database.
     */
    @Bean
    public Step collectEnrollmentStatsStep() {
        return new StepBuilder("collectEnrollmentStatsStep", jobRepository)
                .tasklet(collectEnrollmentStatsTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2: Collect defense statistics from defense database.
     */
    @Bean
    public Step collectDefenseStatsStep() {
        return new StepBuilder("collectDefenseStatsStep", jobRepository)
                .tasklet(collectDefenseStatsTasklet, transactionManager)
                .build();
    }

    /**
     * Step 3: Collect notification statistics from notification database.
     */
    @Bean
    public Step collectNotificationStatsStep() {
        return new StepBuilder("collectNotificationStatsStep", jobRepository)
                .tasklet(collectNotificationStatsTasklet, transactionManager)
                .build();
    }

    /**
     * Step 4: Collect user statistics from user database.
     */
    @Bean
    public Step collectUserStatsStep() {
        return new StepBuilder("collectUserStatsStep", jobRepository)
                .tasklet(collectUserStatsTasklet, transactionManager)
                .build();
    }

    /**
     * Step 5: Generate PDF report from collected statistics.
     * Creates a comprehensive PDF with charts, tables, and KPI dashboard.
     */
    @Bean
    public Step generatePdfStep() {
        return new StepBuilder("generatePdfStep", jobRepository)
                .tasklet(generatePdfTasklet, transactionManager)
                .build();
    }

    /**
     * Step 6: Send report notification to administrators.
     * Sends email with PDF attachment and publishes Kafka event.
     */
    @Bean
    public Step sendReportNotificationStep() {
        return new StepBuilder("sendReportNotificationStep", jobRepository)
                .tasklet(sendReportNotificationTasklet, transactionManager)
                .build();
    }
}
