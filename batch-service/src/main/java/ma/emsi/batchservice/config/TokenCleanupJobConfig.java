package ma.emsi.batchservice.config;

import ma.emsi.batchservice.listener.TokenCleanupJobListener;
import ma.emsi.batchservice.model.Token;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;

/**
 * Configuration for token cleanup batch job.
 * Defines a job with two steps:
 * 1. cleanupRefreshTokensStep - Deletes expired refresh tokens
 * 2. cleanupPasswordResetTokensStep - Deletes expired password reset tokens
 * 
 * Both steps use chunk-based processing with chunk size of 100.
 * Job is scheduled to run daily at 2:00 AM using CRON expression "0 0 2 * * ?".
 * 
 * Validates: Requirements 1.1, 1.4, 1.8
 */
@Configuration
public class TokenCleanupJobConfig {

        @Value("${batch.chunk.token-cleanup:100}")
        private int chunkSize;

        @Value("${batch.skip-limit.token-cleanup:10}")
        private int skipLimit;

        /**
         * Defines the token cleanup job with two steps.
         * Attaches TokenCleanupJobListener for metrics and failure notifications.
         * 
         * @param jobRepository                  Spring Batch job repository
         * @param cleanupRefreshTokensStep       Step for cleaning refresh tokens
         * @param cleanupPasswordResetTokensStep Step for cleaning password reset tokens
         * @param tokenCleanupJobListener        Listener for job lifecycle events
         * @return Configured token cleanup job
         */
        @Bean
        public Job tokenCleanupJob(
                        JobRepository jobRepository,
                        @Qualifier("cleanupRefreshTokensStep") Step cleanupRefreshTokensStep,
                        @Qualifier("cleanupPasswordResetTokensStep") Step cleanupPasswordResetTokensStep,
                        TokenCleanupJobListener tokenCleanupJobListener) {

                return new JobBuilder("tokenCleanupJob", jobRepository)
                                .listener(tokenCleanupJobListener)
                                .start(cleanupRefreshTokensStep)
                                .next(cleanupPasswordResetTokensStep)
                                .build();
        }

        /**
         * Defines the step for cleaning up expired refresh tokens.
         * Uses chunk-based processing with configurable chunk size (default 100).
         * Includes retry policy for transient failures and skip policy for non-fatal
         * errors.
         * 
         * @param jobRepository              Spring Batch job repository
         * @param transactionManager         Transaction manager for chunk transactions
         * @param expiredRefreshTokenReader  Reader for expired refresh tokens
         * @param refreshTokenDeletionWriter Writer for deleting refresh tokens
         * @param databaseRetryPolicy        Retry policy for database operations
         * @return Configured refresh token cleanup step
         */
        @Bean
        public Step cleanupRefreshTokensStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("expiredRefreshTokenReader") JdbcCursorItemReader<Token> expiredRefreshTokenReader,
                        @Qualifier("refreshTokenDeletionWriter") JdbcBatchItemWriter<Token> refreshTokenDeletionWriter,
                        @Qualifier("databaseRetryPolicy") RetryPolicy databaseRetryPolicy) {

                return new StepBuilder("cleanupRefreshTokensStep", jobRepository)
                                .<Token, Token>chunk(chunkSize, transactionManager)
                                .reader(expiredRefreshTokenReader)
                                .writer(refreshTokenDeletionWriter)
                                .faultTolerant()
                                .retryPolicy(databaseRetryPolicy)
                                .skip(TransientDataAccessException.class)
                                .skip(DeadlockLoserDataAccessException.class)
                                .skip(SQLException.class)
                                .skipLimit(skipLimit)
                                .build();
        }

        /**
         * Defines the step for cleaning up expired password reset tokens.
         * Uses chunk-based processing with configurable chunk size (default 100).
         * Includes retry policy for transient failures and skip policy for non-fatal
         * errors.
         * 
         * @param jobRepository                    Spring Batch job repository
         * @param transactionManager               Transaction manager for chunk
         *                                         transactions
         * @param expiredPasswordResetTokenReader  Reader for expired password reset
         *                                         tokens
         * @param passwordResetTokenDeletionWriter Writer for deleting password reset
         *                                         tokens
         * @param databaseRetryPolicy              Retry policy for database operations
         * @return Configured password reset token cleanup step
         */
        @Bean
        public Step cleanupPasswordResetTokensStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        @Qualifier("expiredPasswordResetTokenReader") JdbcCursorItemReader<Token> expiredPasswordResetTokenReader,
                        @Qualifier("passwordResetTokenDeletionWriter") JdbcBatchItemWriter<Token> passwordResetTokenDeletionWriter,
                        @Qualifier("databaseRetryPolicy") RetryPolicy databaseRetryPolicy) {

                return new StepBuilder("cleanupPasswordResetTokensStep", jobRepository)
                                .<Token, Token>chunk(chunkSize, transactionManager)
                                .reader(expiredPasswordResetTokenReader)
                                .writer(passwordResetTokenDeletionWriter)
                                .faultTolerant()
                                .retryPolicy(databaseRetryPolicy)
                                .skip(TransientDataAccessException.class)
                                .skip(DeadlockLoserDataAccessException.class)
                                .skip(SQLException.class)
                                .skipLimit(skipLimit)
                                .build();
        }
}
