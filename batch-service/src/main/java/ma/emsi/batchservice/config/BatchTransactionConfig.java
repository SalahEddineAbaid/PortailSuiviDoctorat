package ma.emsi.batchservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for transaction management across all batch jobs.
 * 
 * Configures:
 * - Transaction timeouts per job type
 * - Rollback rules for different exception types
 * - Deadlock detection and retry mechanisms
 * - Transaction isolation levels
 * 
 * Transaction Timeouts:
 * - Token cleanup: 60 seconds per chunk
 * - Duration alerts: 120 seconds per chunk
 * - Archive operations: 300 seconds per chunk (due to file I/O)
 * - Report generation: 600 seconds (10 minutes for PDF generation)
 * - Consistency checks: 180 seconds per chunk
 * 
 * Requirements: Error Handling section
 */
@Configuration
public class BatchTransactionConfig {

    private static final Logger logger = LoggerFactory.getLogger(BatchTransactionConfig.class);

    // Transaction timeout constants (in seconds)
    public static final int TOKEN_CLEANUP_TIMEOUT = 60;
    public static final int DURATION_ALERT_TIMEOUT = 120;
    public static final int ARCHIVE_TIMEOUT = 300;
    public static final int REPORT_GENERATION_TIMEOUT = 600;
    public static final int CONSISTENCY_CHECK_TIMEOUT = 180;
    public static final int DEFAULT_TIMEOUT = 120;

    /**
     * Creates a transaction manager for token cleanup operations.
     * Timeout: 60 seconds per chunk.
     */
    @Bean(name = "tokenCleanupTransactionManager")
    public PlatformTransactionManager tokenCleanupTransactionManager(
            @Qualifier("userDataSource") DataSource userDataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(userDataSource);
        transactionManager.setDefaultTimeout(TOKEN_CLEANUP_TIMEOUT);
        return transactionManager;
    }

    /**
     * Creates a transaction manager for duration alert operations.
     * Timeout: 120 seconds per chunk.
     */
    @Bean(name = "durationAlertTransactionManager")
    public PlatformTransactionManager durationAlertTransactionManager(
            @Qualifier("inscriptionDataSource") DataSource inscriptionDataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(inscriptionDataSource);
        transactionManager.setDefaultTimeout(DURATION_ALERT_TIMEOUT);
        return transactionManager;
    }

    /**
     * Creates a transaction manager for archive operations.
     * Timeout: 300 seconds per chunk (due to file I/O operations).
     */
    @Bean(name = "archiveTransactionManager")
    public PlatformTransactionManager archiveTransactionManager(
            @Qualifier("batchDataSource") DataSource batchDataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(batchDataSource);
        transactionManager.setDefaultTimeout(ARCHIVE_TIMEOUT);
        return transactionManager;
    }

    /**
     * Creates a transaction manager for report generation operations.
     * Timeout: 600 seconds (10 minutes for PDF generation and statistics
     * collection).
     */
    @Bean(name = "reportGenerationTransactionManager")
    public PlatformTransactionManager reportGenerationTransactionManager(
            @Qualifier("batchDataSource") DataSource batchDataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(batchDataSource);
        transactionManager.setDefaultTimeout(REPORT_GENERATION_TIMEOUT);
        return transactionManager;
    }

    /**
     * Creates a transaction manager for consistency check operations.
     * Timeout: 180 seconds per chunk.
     */
    @Bean(name = "consistencyCheckTransactionManager")
    public PlatformTransactionManager consistencyCheckTransactionManager(
            @Qualifier("batchDataSource") DataSource batchDataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(batchDataSource);
        transactionManager.setDefaultTimeout(CONSISTENCY_CHECK_TIMEOUT);
        return transactionManager;
    }

    /**
     * Creates a RetryTemplate specifically for handling database deadlocks.
     * 
     * Deadlock handling strategy:
     * - Detect deadlock exceptions
     * - Retry up to 3 times
     * - Use exponential backoff: 500ms, 1s, 2s
     * - Log each retry attempt
     * 
     * @return Configured RetryTemplate for deadlock handling
     */
    @Bean(name = "deadlockRetryTemplate")
    public RetryTemplate deadlockRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy for deadlocks
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(DeadlockLoserDataAccessException.class, true);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure exponential backoff for deadlock retries
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500L); // 500ms
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(2000L); // 2 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Add retry listener for logging
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                    RetryContext context,
                    RetryCallback<T, E> callback,
                    Throwable throwable) {
                if (throwable instanceof DeadlockLoserDataAccessException) {
                    logger.warn("Deadlock detected, retry attempt {}: {}",
                            context.getRetryCount(),
                            throwable.getMessage());
                }
            }
        });

        return retryTemplate;
    }

    /**
     * Creates a transaction definition for read-only operations.
     * Used for report generation and statistics collection.
     */
    @Bean(name = "readOnlyTransactionDefinition")
    public DefaultTransactionDefinition readOnlyTransactionDefinition() {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(true);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setTimeout(DEFAULT_TIMEOUT);
        return definition;
    }

    /**
     * Creates a transaction definition for write operations with serializable
     * isolation.
     * Used for critical operations like archive and status updates.
     */
    @Bean(name = "serializableTransactionDefinition")
    public DefaultTransactionDefinition serializableTransactionDefinition() {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(false);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        definition.setTimeout(ARCHIVE_TIMEOUT);
        return definition;
    }

    /**
     * Creates a transaction definition for standard write operations.
     * Used for most batch operations.
     */
    @Bean(name = "defaultWriteTransactionDefinition")
    public DefaultTransactionDefinition defaultWriteTransactionDefinition() {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(false);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setTimeout(DEFAULT_TIMEOUT);
        return definition;
    }
}
