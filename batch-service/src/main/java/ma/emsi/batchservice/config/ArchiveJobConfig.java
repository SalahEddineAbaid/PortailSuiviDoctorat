package ma.emsi.batchservice.config;

import ma.emsi.batchservice.dto.ArchivePackage;
import ma.emsi.batchservice.listener.ArchiveJobListener;
import ma.emsi.batchservice.model.Defense;
import ma.emsi.batchservice.model.Inscription;
import ma.emsi.batchservice.processor.ArchiveProcessor;
import ma.emsi.batchservice.tasklet.CleanupLogsTasklet;
import ma.emsi.batchservice.tasklet.DatabaseOptimizationTasklet;
import ma.emsi.batchservice.writer.ArchiveWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

/**
 * Configuration for the archive job.
 * 
 * Job structure:
 * 1. archiveEnrollmentsStep - Archive old enrollments (chunk-based, size 20)
 * 2. archiveDefensesStep - Archive old defenses (chunk-based, size 20)
 * 3. cleanupLogsStep - Delete old logs (tasklet)
 * 4. optimizeDatabaseStep - Optimize database tables (tasklet)
 * 
 * Scheduling: Quarterly on the 1st at 3:00 AM (CRON: "0 0 3 1 1,4,7,10 ?")
 * 
 * Requirements: 4.1, 4.9, 4.12
 */
@Configuration
public class ArchiveJobConfig {

        @Value("${batch.chunk.archive:20}")
        private int chunkSize;

        @Value("${batch.skip-limit.archive:5}")
        private int skipLimit;

        /**
         * Archive job definition.
         * Executes quarterly to archive old records and clean up logs.
         */
        @Bean
        public Job archiveJob(
                        JobRepository jobRepository,
                        @Qualifier("archiveEnrollmentsStep") Step archiveEnrollmentsStep,
                        @Qualifier("archiveDefensesStep") Step archiveDefensesStep,
                        @Qualifier("cleanupLogsStep") Step cleanupLogsStep,
                        @Qualifier("optimizeDatabaseStep") Step optimizeDatabaseStep,
                        ArchiveJobListener archiveJobListener) {

                return new JobBuilder("archiveJob", jobRepository)
                                .listener(archiveJobListener)
                                .start(archiveEnrollmentsStep)
                                .next(archiveDefensesStep)
                                .next(cleanupLogsStep)
                                .next(optimizeDatabaseStep)
                                .build();
        }

        /**
         * Step 1: Archive old enrollments.
         * Chunk-based processing with size 20.
         * Includes retry policies for database and file system failures.
         */
        @Bean
        public Step archiveEnrollmentsStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("enrollmentArchiveCandidateReader") JdbcCursorItemReader<Inscription> reader,
                        ArchiveProcessor processor,
                        ArchiveWriter writer,
                        @Qualifier("databaseRetryPolicy") RetryPolicy databaseRetryPolicy,
                        @Qualifier("fileSystemRetryPolicy") RetryPolicy fileSystemRetryPolicy) {

                return new StepBuilder("archiveEnrollmentsStep", jobRepository)
                                .<Inscription, ArchivePackage>chunk(chunkSize, transactionManager)
                                .reader(reader)
                                .processor(processor)
                                .writer(writer)
                                .faultTolerant()
                                .retryPolicy(databaseRetryPolicy)
                                .skip(TransientDataAccessException.class)
                                .skip(IOException.class)
                                .skipLimit(skipLimit)
                                .build();
        }

        /**
         * Step 2: Archive old defenses.
         * Chunk-based processing with size 20.
         * Includes retry policies for database and file system failures.
         */
        @Bean
        public Step archiveDefensesStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("defenseArchiveCandidateReader") JdbcCursorItemReader<Defense> reader,
                        ArchiveProcessor processor,
                        ArchiveWriter writer,
                        @Qualifier("databaseRetryPolicy") RetryPolicy databaseRetryPolicy,
                        @Qualifier("fileSystemRetryPolicy") RetryPolicy fileSystemRetryPolicy) {

                return new StepBuilder("archiveDefensesStep", jobRepository)
                                .<Defense, ArchivePackage>chunk(chunkSize, transactionManager)
                                .reader(reader)
                                .processor(processor)
                                .writer(writer)
                                .faultTolerant()
                                .retryPolicy(databaseRetryPolicy)
                                .skip(TransientDataAccessException.class)
                                .skip(IOException.class)
                                .skipLimit(skipLimit)
                                .build();
        }

        /**
         * Step 3: Clean up old logs.
         * Tasklet-based step.
         */
        @Bean
        public Step cleanupLogsStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        CleanupLogsTasklet cleanupLogsTasklet) {

                return new StepBuilder("cleanupLogsStep", jobRepository)
                                .tasklet(cleanupLogsTasklet, transactionManager)
                                .build();
        }

        /**
         * Step 4: Optimize database tables.
         * Tasklet-based step.
         */
        @Bean
        public Step optimizeDatabaseStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        DatabaseOptimizationTasklet databaseOptimizationTasklet) {

                return new StepBuilder("optimizeDatabaseStep", jobRepository)
                                .tasklet(databaseOptimizationTasklet, transactionManager)
                                .build();
        }
}
