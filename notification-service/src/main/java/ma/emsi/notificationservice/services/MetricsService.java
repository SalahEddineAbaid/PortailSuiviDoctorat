package ma.emsi.notificationservice.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing custom metrics using Micrometer.
 * Tracks notification processing metrics including sent, failed, pending, retry, and DLQ counts,
 * as well as email send duration.
 * 
 * Requirements: 13.5, 13.6, 13.7
 */
@Service
@Slf4j
public class MetricsService {
    
    private final Counter sentCounter;
    private final Counter failedCounter;
    private final Counter pendingCounter;
    private final Counter retryCounter;
    private final Counter dlqCounter;
    private final Timer emailSendDurationTimer;
    
    /**
     * Constructor that initializes all metrics.
     * 
     * @param meterRegistry the Micrometer meter registry
     */
    public MetricsService(MeterRegistry meterRegistry) {
        // Requirement 13.5: Counter for notifications.sent.total
        this.sentCounter = Counter.builder("notifications.sent.total")
            .description("Total number of notifications successfully sent")
            .register(meterRegistry);
        
        // Requirement 13.6: Counter for notifications.failed.total
        this.failedCounter = Counter.builder("notifications.failed.total")
            .description("Total number of notifications that failed to send")
            .register(meterRegistry);
        
        // Counter for notifications.pending.total
        this.pendingCounter = Counter.builder("notifications.pending.total")
            .description("Total number of notifications in pending status")
            .register(meterRegistry);
        
        // Counter for notifications.retry.total
        this.retryCounter = Counter.builder("notifications.retry.total")
            .description("Total number of notification retry attempts")
            .register(meterRegistry);
        
        // Counter for notifications.dlq.total
        this.dlqCounter = Counter.builder("notifications.dlq.total")
            .description("Total number of notifications sent to Dead Letter Queue")
            .register(meterRegistry);
        
        // Requirement 13.7: Histogram for notifications.email.send.duration
        this.emailSendDurationTimer = Timer.builder("notifications.email.send.duration")
            .description("Duration of email send operations")
            .publishPercentiles(0.5, 0.95, 0.99) // Median, 95th, and 99th percentiles
            .register(meterRegistry);
        
        log.info("MetricsService initialized with custom notification metrics");
    }
    
    /**
     * Increments the sent notifications counter.
     * Requirement 13.5: Increment notifications.sent.total counter metric
     */
    public void incrementSentCounter() {
        sentCounter.increment();
        log.debug("Incremented sent counter. Current count: {}", sentCounter.count());
    }
    
    /**
     * Increments the failed notifications counter.
     * Requirement 13.6: Increment notifications.failed.total counter metric
     */
    public void incrementFailedCounter() {
        failedCounter.increment();
        log.debug("Incremented failed counter. Current count: {}", failedCounter.count());
    }
    
    /**
     * Increments the pending notifications counter.
     */
    public void incrementPendingCounter() {
        pendingCounter.increment();
        log.debug("Incremented pending counter. Current count: {}", pendingCounter.count());
    }
    
    /**
     * Increments the retry notifications counter.
     */
    public void incrementRetryCounter() {
        retryCounter.increment();
        log.debug("Incremented retry counter. Current count: {}", retryCounter.count());
    }
    
    /**
     * Increments the DLQ notifications counter.
     */
    public void incrementDlqCounter() {
        dlqCounter.increment();
        log.debug("Incremented DLQ counter. Current count: {}", dlqCounter.count());
    }
    
    /**
     * Records the duration of an email send operation.
     * Requirement 13.7: Record duration in notifications.email.send.duration histogram metric
     * 
     * @param duration the duration of the email send operation
     */
    public void recordEmailSendDuration(Duration duration) {
        emailSendDurationTimer.record(duration);
        log.debug("Recorded email send duration: {} ms", duration.toMillis());
    }
    
    /**
     * Records the duration of an email send operation in milliseconds.
     * Requirement 13.7: Record duration in notifications.email.send.duration histogram metric
     * 
     * @param durationMillis the duration in milliseconds
     */
    public void recordEmailSendDuration(long durationMillis) {
        emailSendDurationTimer.record(durationMillis, TimeUnit.MILLISECONDS);
        log.debug("Recorded email send duration: {} ms", durationMillis);
    }
    
    /**
     * Wraps a Runnable with timing measurement.
     * Requirement 13.7: Record duration in notifications.email.send.duration histogram metric
     * 
     * @param runnable the operation to time
     */
    public void recordEmailSendDuration(Runnable runnable) {
        emailSendDurationTimer.record(runnable);
    }
    
    /**
     * Gets the current count of sent notifications.
     * 
     * @return the count of sent notifications
     */
    public double getSentCount() {
        return sentCounter.count();
    }
    
    /**
     * Gets the current count of failed notifications.
     * 
     * @return the count of failed notifications
     */
    public double getFailedCount() {
        return failedCounter.count();
    }
    
    /**
     * Gets the current count of pending notifications.
     * 
     * @return the count of pending notifications
     */
    public double getPendingCount() {
        return pendingCounter.count();
    }
    
    /**
     * Gets the current count of retry attempts.
     * 
     * @return the count of retry attempts
     */
    public double getRetryCount() {
        return retryCounter.count();
    }
    
    /**
     * Gets the current count of DLQ notifications.
     * 
     * @return the count of DLQ notifications
     */
    public double getDlqCount() {
        return dlqCounter.count();
    }
}
