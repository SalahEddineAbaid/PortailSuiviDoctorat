package ma.emsi.batchservice.config;

import ma.emsi.batchservice.dto.event.AlertEventDTO;
import ma.emsi.batchservice.listener.DureeAlertJobListener;
import ma.emsi.batchservice.model.Inscription;
import ma.emsi.batchservice.processor.DureeAlertProcessor;
import ma.emsi.batchservice.writer.KafkaAlertWriter;
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
import org.springframework.kafka.KafkaException;
import org.springframework.retry.RetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration for doctoral duration alert batch job.
 * 
 * Defines a job with three steps:
 * 1. check3YearThresholdStep - Alerts for doctorants approaching 3-year
 * threshold
 * 2. check6YearThresholdStep - Alerts for doctorants approaching 6-year
 * threshold
 * 3. checkExceeded6YearStep - Alerts for doctorants who exceeded 6-year limit
 * 
 * All steps use chunk-based processing with chunk size of 50.
 * Job is scheduled to run every Monday at 8:00 AM using CRON expression "0 0 8
 * ? * MON".
 * 
 * Each step:
 * - Reads enrollments from inscriptiondb based on duration thresholds
 * - Processes to calculate duration and build alert events
 * - Writes to Kafka notifications topic and records in database
 * 
 * Requirements: 2.1, 2.8, 2.9
 */
@Configuration
public class DureeDoctoratAlertJobConfig {

        @Value("${batch.chunk.duration-alert:50}")
        private int chunkSize;

        @Value("${batch.skip-limit.duration-alert:10}")
        private int skipLimit;

        /**
         * Defines the duration alert job with three steps.
         * Attaches DureeAlertJobListener for metrics and failure notifications.
         * 
         * @param jobRepository           Spring Batch job repository
         * @param check3YearThresholdStep Step for 3-year threshold alerts
         * @param check6YearThresholdStep Step for 6-year threshold alerts
         * @param checkExceeded6YearStep  Step for exceeded 6-year alerts
         * @param dureeAlertJobListener   Listener for job lifecycle events
         * @return Configured duration alert job
         */
        @Bean
        public Job dureeDoctoratAlertJob(
                        JobRepository jobRepository,
                        @Qualifier("check3YearThresholdStep") Step check3YearThresholdStep,
                        @Qualifier("check6YearThresholdStep") Step check6YearThresholdStep,
                        @Qualifier("checkExceeded6YearStep") Step checkExceeded6YearStep,
                        DureeAlertJobListener dureeAlertJobListener) {

                return new JobBuilder("dureeDoctoratAlertJob", jobRepository)
                                .listener(dureeAlertJobListener)
                                .start(check3YearThresholdStep)
                                .next(check6YearThresholdStep)
                                .next(checkExceeded6YearStep)
                                .build();
        }

        /**
         * Defines the step for checking 3-year threshold.
         * 
         * Identifies doctorants where:
         * - Duration is between 2y9m and 3y
         * - No dérogation has been granted
         * - Sends NORMAL priority alerts
         * 
         * Includes retry policies for database and Kafka failures.
         * 
         * @param jobRepository            Spring Batch job repository
         * @param transactionManager       Transaction manager for chunk transactions
         * @param threeYearThresholdReader Reader for 3-year threshold enrollments
         * @param dureeAlertProcessor      Processor to build alert events
         * @param kafkaAlertWriter         Writer to publish alerts and record in
         *                                 database
         * @param databaseRetryPolicy      Retry policy for database operations
         * @param kafkaRetryPolicy         Retry policy for Kafka operations
         * @return Configured 3-year threshold step
         */
        @Bean
        public Step check3YearThresholdStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("threeYearThresholdReader") JdbcCursorItemReader<Inscription> threeYearThresholdReader,
                        DureeAlertProcessor dureeAlertProcessor,
                        KafkaAlertWriter kafkaAlertWriter,
                        @Qualifier("databaseRetryPolicy") RetryPolicy databaseRetryPolicy,
                        @Qualifier("kafkaRetryPolicy") RetryPolicy kafkaRetryPolicy) {

                return new StepBuilder("check3YearThresholdStep", jobRepository)
                                .<Inscription, AlertEventDTO>chunk(chunkSize, transactionManager)
                                .reader(threeYearThresholdReader)
                                .processor(dureeAlertProcessor)
                                .writer(kafkaAlertWriter)
                                .faultTolerant()
                                .retryPolicy(databaseRetryPolicy)
                                .skip(TransientDataAccessException.class)
                                .skip(KafkaException.class)
                                .skip(IllegalArgumentException.class)
                                .skipLimit(skipLimit)
                                .build();
        }

        /**
         * Defines the step for checking 6-year threshold.
         * 
         * Identifies doctorants where:
         * - Duration is between 5y9m and 6y
         * - Sends HIGH priority alerts
         * 
         * Includes retry policies for database and Kafka failures.
         * 
         * @param jobRepository          Spring Batch job repository
         * @param transactionManager     Transaction manager for chunk transactions
         * @param sixYearThresholdReader Reader for 6-year threshold enrollments
         * @param dureeAlertProcessor    Processor to build alert events
         * @param kafkaAlertWriter       Writer to publish alerts and record in database
         * @param databaseRetryPolicy    Retry policy for database operations
         * @param kafkaRetryPolicy       Retry policy for Kafka operations
         * @return Configured 6-year threshold step
         */
        @Bean
        public Step check6YearThresholdStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("sixYearThresholdReader") JdbcCursorItemReader<Inscription> sixYearThresholdReader,
                        DureeAlertProcessor dureeAlertProcessor,
                        KafkaAlertWriter kafkaAlertWriter,
                        @Qualifier("databaseRetryPolicy") RetryPolicy databaseRetryPolicy,
                        @Qualifier("kafkaRetryPolicy") RetryPolicy kafkaRetryPolicy) {

                return new StepBuilder("check6YearThresholdStep", jobRepository)
                                .<Inscription, AlertEventDTO>chunk(chunkSize, transactionManager)
                                .reader(sixYearThresholdReader)
                                .processor(dureeAlertProcessor)
                                .writer(kafkaAlertWriter)
                                .faultTolerant()
                                .retryPolicy(databaseRetryPolicy)
                                .skip(TransientDataAccessException.class)
                                .skip(KafkaException.class)
                                .skip(IllegalArgumentException.class)
                                .skipLimit(skipLimit)
                                .build();
        }

        /**
         * Defines the step for checking exceeded 6-year limit.
         * 
         * Identifies doctorants where:
         * - Duration exceeds 6 years
         * - No exceptional dérogation has been granted
         * - Sends URGENT priority alerts
         * - Updates enrollment status to BLOQUÉ
         * 
         * Includes retry policies for database and Kafka failures.
         * 
         * @param jobRepository         Spring Batch job repository
         * @param transactionManager    Transaction manager for chunk transactions
         * @param exceededSixYearReader Reader for exceeded 6-year enrollments
         * @param dureeAlertProcessor   Processor to build alert events
         * @param kafkaAlertWriter      Writer to publish alerts, record in database,
         *                              and update status
         * @param databaseRetryPolicy   Retry policy for database operations
         * @param kafkaRetryPolicy      Retry policy for Kafka operations
         * @return Configured exceeded 6-year step
         */
        @Bean
        public Step checkExceeded6YearStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("exceededSixYearReader") JdbcCursorItemReader<Inscription> exceededSixYearReader,
                        DureeAlertProcessor dureeAlertProcessor,
                        KafkaAlertWriter kafkaAlertWriter,
                        @Qualifier("databaseRetryPolicy") RetryPolicy databaseRetryPolicy,
                        @Qualifier("kafkaRetryPolicy") RetryPolicy kafkaRetryPolicy) {

                return new StepBuilder("checkExceeded6YearStep", jobRepository)
                                .<Inscription, AlertEventDTO>chunk(chunkSize, transactionManager)
                                .reader(exceededSixYearReader)
                                .processor(dureeAlertProcessor)
                                .writer(kafkaAlertWriter)
                                .faultTolerant()
                                .retryPolicy(databaseRetryPolicy)
                                .skip(TransientDataAccessException.class)
                                .skip(KafkaException.class)
                                .skip(IllegalArgumentException.class)
                                .skipLimit(skipLimit)
                                .build();
        }
}
