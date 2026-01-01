package ma.emsi.batchservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.KafkaException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for retry policies and error handling across all batch jobs.
 * 
 * Implements exponential backoff retry strategy for transient failures:
 * - Initial interval: 1 second
 * - Multiplier: 2.0 (doubles each retry)
 * - Maximum interval: 16 seconds
 * - Maximum retry attempts: 5
 * 
 * Retryable exceptions:
 * - TransientDataAccessException (database connection issues)
 * - DeadlockLoserDataAccessException (database deadlocks)
 * - SQLException (general database errors)
 * - KafkaException (Kafka broker issues)
 * - IOException (file system issues)
 * 
 * Requirements: Error Handling section
 */
@Configuration
public class BatchRetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(BatchRetryConfig.class);

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_BACKOFF_INTERVAL = 1000L; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_BACKOFF_INTERVAL = 16000L; // 16 seconds

    /**
     * Creates a RetryTemplate with exponential backoff for transient failures.
     * 
     * Retry sequence: 1s, 2s, 4s, 8s, 16s (5 attempts total)
     * 
     * @return Configured RetryTemplate
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_BACKOFF_INTERVAL);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);
        backOffPolicy.setMaxInterval(MAX_BACKOFF_INTERVAL);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Configure retry policy with retryable exceptions
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(TransientDataAccessException.class, true);
        retryableExceptions.put(DeadlockLoserDataAccessException.class, true);
        retryableExceptions.put(SQLException.class, true);
        retryableExceptions.put(KafkaException.class, true);
        retryableExceptions.put(IOException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_RETRY_ATTEMPTS, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Add retry listener for logging
        retryTemplate.registerListener(new org.springframework.retry.RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    org.springframework.retry.RetryContext context,
                    org.springframework.retry.RetryCallback<T, E> callback,
                    Throwable throwable) {
                logger.warn("Retry attempt {} failed: {}",
                        context.getRetryCount(),
                        throwable.getMessage());
            }
        });

        return retryTemplate;
    }

    /**
     * Creates a RetryPolicy for database operations with deadlock handling.
     * 
     * Specifically handles:
     * - Deadlock detection and retry (up to 3 attempts)
     * - Transient database connection issues
     * 
     * @return Configured RetryPolicy for database operations
     */
    @Bean
    public RetryPolicy databaseRetryPolicy() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(DeadlockLoserDataAccessException.class, true);
        retryableExceptions.put(TransientDataAccessException.class, true);
        retryableExceptions.put(SQLException.class, true);

        return new SimpleRetryPolicy(3, retryableExceptions);
    }

    /**
     * Creates a RetryPolicy for Kafka operations.
     * 
     * Handles Kafka broker unavailability and network issues.
     * 
     * @return Configured RetryPolicy for Kafka operations
     */
    @Bean
    public RetryPolicy kafkaRetryPolicy() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(KafkaException.class, true);

        return new SimpleRetryPolicy(3, retryableExceptions);
    }

    /**
     * Creates a RetryPolicy for file system operations.
     * 
     * Handles temporary file system issues like locks or permissions.
     * 
     * @return Configured RetryPolicy for file operations
     */
    @Bean
    public RetryPolicy fileSystemRetryPolicy() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(IOException.class, true);

        return new SimpleRetryPolicy(2, retryableExceptions);
    }

    /**
     * Creates a classifier for skip policies.
     * 
     * Determines which exceptions should cause items to be skipped vs. retried.
     * 
     * Skippable exceptions (non-fatal):
     * - IllegalArgumentException (invalid data format)
     * - NullPointerException (missing optional fields)
     * 
     * Fatal exceptions (not skippable):
     * - Database connection failures
     * - Kafka broker unavailability
     * 
     * @return Classifier for exception handling
     */
    @Bean
    public Classifier<Throwable, Boolean> skipPolicyClassifier() {
        return throwable -> {
            // Skippable exceptions (data validation issues)
            if (throwable instanceof IllegalArgumentException ||
                    throwable instanceof NullPointerException) {
                logger.warn("Skipping item due to validation error: {}", throwable.getMessage());
                return true;
            }

            // Fatal exceptions (infrastructure issues)
            if (throwable instanceof TransientDataAccessException ||
                    throwable instanceof DeadlockLoserDataAccessException ||
                    throwable instanceof KafkaException ||
                    throwable instanceof SQLException) {
                logger.error("Fatal error, not skipping: {}", throwable.getMessage());
                return false;
            }

            // Default: skip unknown exceptions after logging
            logger.warn("Unknown exception, skipping: {}", throwable.getMessage());
            return true;
        };
    }
}
