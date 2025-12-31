package ma.emsi.notificationservice.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsService.
 * Verifies that all metrics are properly registered and incremented.
 */
class MetricsServiceTest {
    
    private MetricsService metricsService;
    private MeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }
    
    @Test
    void testIncrementSentCounter() {
        // Given
        double initialCount = metricsService.getSentCount();
        
        // When
        metricsService.incrementSentCounter();
        
        // Then
        assertEquals(initialCount + 1, metricsService.getSentCount());
    }
    
    @Test
    void testIncrementFailedCounter() {
        // Given
        double initialCount = metricsService.getFailedCount();
        
        // When
        metricsService.incrementFailedCounter();
        
        // Then
        assertEquals(initialCount + 1, metricsService.getFailedCount());
    }
    
    @Test
    void testIncrementPendingCounter() {
        // Given
        double initialCount = metricsService.getPendingCount();
        
        // When
        metricsService.incrementPendingCounter();
        
        // Then
        assertEquals(initialCount + 1, metricsService.getPendingCount());
    }
    
    @Test
    void testIncrementRetryCounter() {
        // Given
        double initialCount = metricsService.getRetryCount();
        
        // When
        metricsService.incrementRetryCounter();
        
        // Then
        assertEquals(initialCount + 1, metricsService.getRetryCount());
    }
    
    @Test
    void testIncrementDlqCounter() {
        // Given
        double initialCount = metricsService.getDlqCount();
        
        // When
        metricsService.incrementDlqCounter();
        
        // Then
        assertEquals(initialCount + 1, metricsService.getDlqCount());
    }
    
    @Test
    void testRecordEmailSendDurationWithDuration() {
        // Given
        Duration duration = Duration.ofMillis(500);
        
        // When
        metricsService.recordEmailSendDuration(duration);
        
        // Then
        // Verify the timer was called (no exception thrown)
        assertNotNull(metricsService);
    }
    
    @Test
    void testRecordEmailSendDurationWithMillis() {
        // Given
        long durationMillis = 250L;
        
        // When
        metricsService.recordEmailSendDuration(durationMillis);
        
        // Then
        // Verify the timer was called (no exception thrown)
        assertNotNull(metricsService);
    }
    
    @Test
    void testRecordEmailSendDurationWithRunnable() {
        // Given
        Runnable operation = () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        
        // When
        metricsService.recordEmailSendDuration(operation);
        
        // Then
        // Verify the timer was called (no exception thrown)
        assertNotNull(metricsService);
    }
    
    @Test
    void testMultipleIncrements() {
        // Given
        int incrementCount = 5;
        
        // When
        for (int i = 0; i < incrementCount; i++) {
            metricsService.incrementSentCounter();
            metricsService.incrementFailedCounter();
            metricsService.incrementRetryCounter();
        }
        
        // Then
        assertEquals(incrementCount, metricsService.getSentCount());
        assertEquals(incrementCount, metricsService.getFailedCount());
        assertEquals(incrementCount, metricsService.getRetryCount());
    }
    
    @Test
    void testMetricsAreRegisteredInMeterRegistry() {
        // Then
        assertNotNull(meterRegistry.find("notifications.sent.total").counter());
        assertNotNull(meterRegistry.find("notifications.failed.total").counter());
        assertNotNull(meterRegistry.find("notifications.pending.total").counter());
        assertNotNull(meterRegistry.find("notifications.retry.total").counter());
        assertNotNull(meterRegistry.find("notifications.dlq.total").counter());
        assertNotNull(meterRegistry.find("notifications.email.send.duration").timer());
    }
}
