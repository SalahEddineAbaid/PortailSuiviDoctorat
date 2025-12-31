package ma.emsi.notificationservice.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Event listener for Retry events.
 * Logs retry attempts at INFO level for monitoring.
 * 
 * Requirement 13.3: WHEN a retry is attempted THEN log at INFO level
 */
@Component
@Slf4j
public class RetryEventListener {
    
    private final RetryRegistry retryRegistry;
    
    public RetryEventListener(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }
    
    /**
     * Initialize event listeners for all retry instances after bean construction.
     */
    @PostConstruct
    public void init() {
        retryRegistry.getAllRetries().forEach(this::registerEventListeners);
    }
    
    /**
     * Register event listeners for a specific retry instance.
     * 
     * @param retry the retry instance to register listeners for
     */
    private void registerEventListeners(Retry retry) {
        String retryName = retry.getName();
        
        // Listen for retry attempts
        retry.getEventPublisher()
            .onRetry(event -> logRetryAttempt(retryName, event));
        
        // Listen for successful retries
        retry.getEventPublisher()
            .onSuccess(event -> logRetrySuccess(retryName, event));
        
        // Listen for retry exhaustion (all retries failed)
        retry.getEventPublisher()
            .onError(event -> logRetryExhausted(retryName, event));
        
        log.info("Retry event listeners registered for: {}", retryName);
    }
    
    /**
     * Log retry attempts at INFO level.
     * Requirement 13.3: WHEN a retry is attempted THEN log at INFO level
     * 
     * @param retryName the name of the retry instance
     * @param event the retry event
     */
    private void logRetryAttempt(String retryName, RetryOnRetryEvent event) {
        Retry retry = getRetry(retryName);
        log.info("=== Retry Attempt ===");
        log.info("Retry Instance: {}", retryName);
        log.info("Attempt Number: {} of {}", 
                 event.getNumberOfRetryAttempts(), 
                 retry.getRetryConfig().getMaxAttempts());
        log.info("Wait Interval: {} ms", event.getWaitInterval().toMillis());
        log.info("Last Exception: {}", event.getLastThrowable().getMessage());
        log.info("Timestamp: {}", event.getCreationTime());
        log.info("====================");
    }
    
    /**
     * Log successful retry at INFO level.
     * 
     * @param retryName the name of the retry instance
     * @param event the success event
     */
    private void logRetrySuccess(String retryName, RetryOnSuccessEvent event) {
        if (event.getNumberOfRetryAttempts() > 0) {
            log.info("Retry '{}' succeeded after {} attempt(s)", 
                     retryName, event.getNumberOfRetryAttempts());
        }
    }
    
    /**
     * Log retry exhaustion at ERROR level.
     * 
     * @param retryName the name of the retry instance
     * @param event the error event
     */
    private void logRetryExhausted(String retryName, RetryOnErrorEvent event) {
        log.error("=== Retry Exhausted ===");
        log.error("Retry Instance: {}", retryName);
        log.error("Total Attempts: {}", event.getNumberOfRetryAttempts());
        log.error("Final Exception: {}", event.getLastThrowable().getMessage());
        log.error("Timestamp: {}", event.getCreationTime());
        log.error("All retry attempts failed. Notification will be marked as FAILED.");
        log.error("=======================");
    }
    
    /**
     * Helper method to get retry configuration.
     * 
     * @param retryName the name of the retry instance
     * @return the retry instance
     */
    private Retry getRetry(String retryName) {
        return retryRegistry.retry(retryName);
    }
}
